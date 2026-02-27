@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ======================================
echo Maven Central Publishing Tool
echo ======================================
echo.

REM Read credentials from gradle.properties
for /f "tokens=1,2 delims==" %%a in (..\gradle.properties) do (
    if "%%a"=="mavenCentralUsername" set USERNAME=%%b
    if "%%a"=="mavenCentralPassword" set PASSWORD=%%b
    if "%%a"=="ossrhUsername" set USERNAME=%%b
    if "%%a"=="ossrhPassword" set PASSWORD=%%b
)

if "%USERNAME%"=="" (
    echo [ERROR] mavenCentralUsername or ossrhUsername not found in gradle.properties
    pause
    exit /b 1
)

if "%PASSWORD%"=="" (
    echo [ERROR] mavenCentralPassword or ossrhPassword not found in gradle.properties
    pause
    exit /b 1
)

REM Read version from maven-publish.gradle
for /f "tokens=1,2 delims==" %%a in ('findstr /C:"pomVersion = " ..\maven-publish.gradle') do (
    set VERSION_LINE=%%b
)

REM Remove quotes and spaces
set VERSION=%VERSION_LINE:'=%
set VERSION=%VERSION: =%
set VERSION=%VERSION:"=%

if "%VERSION%"=="" (
    echo [ERROR] Version not found in maven-publish.gradle
    pause
    exit /b 1
)

echo Detected version: %VERSION%
echo.

REM Generate Base64 encoded auth token
powershell -Command "$auth = '%USERNAME%:%PASSWORD%'; $bytes = [System.Text.Encoding]::UTF8.GetBytes($auth); $base64 = [Convert]::ToBase64String($bytes); Write-Host $base64" > temp_token.txt
set /p AUTH_TOKEN=<temp_token.txt
del temp_token.txt

echo Choose operation:
echo.
echo 1. Quick Publish (Recommended) - Build and upload palyerlibrary only
echo 2. Full Publish - Clean, build, and upload palyerlibrary only
echo 3. Publish All Modules - Upload all 11 modules (palyerlibrary + GSYVideoPlayer modules)
echo 4. Check deployment status
echo 5. Check Maven Central sync status
echo 6. Clear all deployments
echo.
set /p CHOICE="Enter option (1-6): "

if "%CHOICE%"=="1" goto QUICK_PUBLISH
if "%CHOICE%"=="2" goto FULL_PUBLISH
if "%CHOICE%"=="3" goto PUBLISH_ALL_MODULES
if "%CHOICE%"=="4" goto CHECK_STATUS
if "%CHOICE%"=="5" goto CHECK_MAVEN
if "%CHOICE%"=="6" goto CLEAR_DEPLOYMENTS

echo Invalid option
pause
exit /b 1

:QUICK_PUBLISH
echo.
echo ========================================
echo Quick Publish (palyerlibrary)
echo ========================================
echo.
echo Publishing module:
echo   - palyerlibrary (OrangePlayer library)
echo.

echo [1/3] Publishing to local repository...
cd ..
call gradlew.bat :palyerlibrary:publishMavenPublicationToLocalRepository
if errorlevel 1 (
    echo [ERROR] Publish failed
    pause
    exit /b 1
)

echo.
echo [2/3] Creating Bundle...
cd palyerlibrary\build\repo
if not exist "..\..\..\temp_bundle_build" mkdir "..\..\..\temp_bundle_build"
xcopy /E /I /Y "io" "..\..\..\temp_bundle_build\io"
cd ..\..\..

REM Wait for file handles to release
timeout /t 3 /nobreak >nul

cd temp_bundle_build
REM Use 7z if available, otherwise PowerShell
where 7z >nul 2>&1
if errorlevel 1 (
    powershell -Command "Compress-Archive -Path io -DestinationPath ..\maven-central\bundle.zip -Force"
) else (
    7z a -tzip "..\maven-central\bundle.zip" io -mx=5
)
cd ..
rmdir /s /q temp_bundle_build

if not exist "maven-central\bundle.zip" (
    echo [ERROR] Bundle creation failed
    pause
    exit /b 1
)

echo [SUCCESS] Bundle created
powershell -Command "Get-Item maven-central\bundle.zip | Select-Object Name, @{Name='Size(KB)';Expression={[math]::Round($_.Length/1KB,2)}}"

echo.
echo [3/3] Uploading to Central Portal...
cd maven-central
curl -X POST -H "Authorization: Bearer %AUTH_TOKEN%" -F "bundle=@bundle.zip" "https://central.sonatype.com/api/v1/publisher/upload?name=orangeplayer-%VERSION%&publishingType=USER_MANAGED" > deployment_response.json

echo.
echo [SUCCESS] Upload complete!
echo.
echo Deployment ID:
type deployment_response.json
echo.
echo.
echo Published module:
echo   - orangeplayer:%VERSION%
echo.
echo Next steps:
echo   1. Visit https://central.sonatype.com/publishing/deployments
echo   2. Wait for validation (about 2-5 minutes)
echo   3. Click "Publish" to release
echo.

cd ..
pause
exit /b 0

:FULL_PUBLISH
echo.
echo ========================================
echo Full Publish (palyerlibrary)
echo ========================================
echo.
echo Publishing module:
echo   - palyerlibrary (OrangePlayer library)
echo.

echo [1/5] Cleaning build...
cd ..
call gradlew.bat clean

echo.
echo [2/5] Building project...
call gradlew.bat :palyerlibrary:build -x test

echo.
echo [3/5] Publishing to local repository...
call gradlew.bat :palyerlibrary:publishMavenPublicationToLocalRepository
if errorlevel 1 (
    echo [ERROR] Build failed
    pause
    exit /b 1
)

echo.
echo [4/5] Creating Bundle...
cd palyerlibrary\build\repo
if not exist "..\..\..\temp_bundle_build" mkdir "..\..\..\temp_bundle_build"
xcopy /E /I /Y "io" "..\..\..\temp_bundle_build\io"
cd ..\..\..

REM Wait for file handles to release
timeout /t 3 /nobreak >nul

cd temp_bundle_build
REM Use 7z if available, otherwise PowerShell
where 7z >nul 2>&1
if errorlevel 1 (
    powershell -Command "Compress-Archive -Path io -DestinationPath ..\maven-central\bundle.zip -Force"
) else (
    7z a -tzip "..\maven-central\bundle.zip" io -mx=5
)
cd ..
rmdir /s /q temp_bundle_build

if not exist "maven-central\bundle.zip" (
    echo [ERROR] Bundle creation failed
    pause
    exit /b 1
)

echo [SUCCESS] Bundle created
powershell -Command "Get-Item maven-central\bundle.zip | Select-Object Name, @{Name='Size(KB)';Expression={[math]::Round($_.Length/1KB,2)}}"

echo.
echo [5/5] Uploading to Central Portal...
cd maven-central
curl -X POST -H "Authorization: Bearer %AUTH_TOKEN%" -F "bundle=@bundle.zip" "https://central.sonatype.com/api/v1/publisher/upload?name=orangeplayer-%VERSION%&publishingType=USER_MANAGED" > deployment_response.json

echo.
echo [SUCCESS] Upload complete!
echo.
echo Deployment ID:
type deployment_response.json
echo.
echo.
echo Published module:
echo   - orangeplayer:%VERSION%
echo.
echo Next steps:
echo   1. Visit https://central.sonatype.com/publishing/deployments
echo   2. Wait for validation (about 2-5 minutes)
echo   3. Click "Publish" to release
echo.

cd ..
pause
exit /b 0

:PUBLISH_ALL_MODULES
echo.
echo ========================================
echo Publish All Modules
echo ========================================
echo.
echo This will publish 11 modules:
echo   1. palyerlibrary (OrangePlayer core)
echo   2. gsyVideoPlayer-base
echo   3. gsyVideoPlayer-proxy_cache
echo   4. gsyVideoPlayer-java
echo   5. gsyVideoPlayer-exo_player2 (ExoPlayer kernel)
echo   6. gsyVideoPlayer-aliplay (AliPlayer kernel)
echo   7. gsyVideoPlayer-armv7a (ARM 32-bit)
echo   8. gsyVideoPlayer-armv64 (ARM 64-bit)
echo   9. gsyVideoPlayer-x86 (x86 32-bit)
echo   10. gsyVideoPlayer-x86_64 (x86 64-bit)
echo   11. gsyVideoPlayer-ex_so (IJK with encryption support, all architectures)
echo.
set /p CONFIRM="Continue? (Y/N): "

if /i not "%CONFIRM%"=="Y" (
    echo Cancelled
    pause
    exit /b 0
)

echo.
echo [1/4] Publishing all modules to local repository...
cd ..

echo   [1/10] Publishing palyerlibrary...
call gradlew.bat :palyerlibrary:publishMavenPublicationToLocalRepository
if errorlevel 1 goto PUBLISH_ERROR

echo   [2/10] Publishing gsyVideoPlayer-base...
call gradlew.bat :gsyVideoPlayer-base:publishReleasePublicationToLocalRepository
if errorlevel 1 goto PUBLISH_ERROR

echo   [3/10] Publishing gsyVideoPlayer-proxy_cache...
call gradlew.bat :gsyVideoPlayer-proxy_cache:publishReleasePublicationToLocalRepository
if errorlevel 1 goto PUBLISH_ERROR

echo   [4/10] Publishing gsyVideoPlayer-java...
call gradlew.bat :gsyVideoPlayer-java:publishReleasePublicationToLocalRepository
if errorlevel 1 goto PUBLISH_ERROR

echo   [5/10] Publishing gsyVideoPlayer-exo_player2...
call gradlew.bat :gsyVideoPlayer-exo_player2:publishReleasePublicationToLocalRepository
if errorlevel 1 goto PUBLISH_ERROR

echo   [6/10] Publishing gsyVideoPlayer-aliplay...
call gradlew.bat :gsyVideoPlayer-aliplay:publishReleasePublicationToLocalRepository
if errorlevel 1 goto PUBLISH_ERROR

echo   [7/10] Publishing gsyVideoPlayer-armv7a...
call gradlew.bat :gsyVideoPlayer-armv7a:publishReleasePublicationToLocalRepository
if errorlevel 1 goto PUBLISH_ERROR

echo   [8/10] Publishing gsyVideoPlayer-armv64...
call gradlew.bat :gsyVideoPlayer-armv64:publishReleasePublicationToLocalRepository
if errorlevel 1 goto PUBLISH_ERROR

echo   [9/10] Publishing gsyVideoPlayer-x86...
call gradlew.bat :gsyVideoPlayer-x86:publishReleasePublicationToLocalRepository
if errorlevel 1 goto PUBLISH_ERROR

echo   [10/11] Publishing gsyVideoPlayer-x86_64...
call gradlew.bat :gsyVideoPlayer-x86_64:publishReleasePublicationToLocalRepository
if errorlevel 1 goto PUBLISH_ERROR

echo   [11/11] Publishing gsyVideoPlayer-ex_so...
call gradlew.bat :gsyVideoPlayer-ex_so:publishReleasePublicationToLocalRepository
if errorlevel 1 goto PUBLISH_ERROR

echo.
echo [SUCCESS] All modules published to local repository
echo.

echo [2/4] Collecting artifacts from all modules...
if exist "temp_bundle_build" rmdir /s /q temp_bundle_build
mkdir temp_bundle_build

REM Copy palyerlibrary artifacts
echo   Copying palyerlibrary...
if exist "palyerlibrary\build\repo\io" (
    xcopy /E /I /Y "palyerlibrary\build\repo\io" "temp_bundle_build\io"
)

REM Copy GSYVideoPlayer module artifacts from each module's build/repo
echo   Copying GSYVideoPlayer modules...
for %%m in (base proxy_cache java exo_player2 aliplay armv7a armv64 x86 x86_64 ex_so) do (
    if exist "GSYVideoPlayer-source\gsyVideoPlayer-%%m\build\repo\io" (
        echo     - gsyVideoPlayer-%%m
        xcopy /E /I /Y "GSYVideoPlayer-source\gsyVideoPlayer-%%m\build\repo\io" "temp_bundle_build\io"
    )
)

echo.
echo [3/4] Creating Bundle...
REM Wait for file handles to release
timeout /t 3 /nobreak >nul

cd temp_bundle_build
REM Use 7z if available, otherwise PowerShell
where 7z >nul 2>&1
if errorlevel 1 (
    powershell -Command "Compress-Archive -Path io -DestinationPath ..\maven-central\bundle.zip -Force"
) else (
    7z a -tzip "..\maven-central\bundle.zip" io -mx=5
)
cd ..
rmdir /s /q temp_bundle_build

if not exist "maven-central\bundle.zip" (
    echo [ERROR] Bundle creation failed
    pause
    exit /b 1
)

echo [SUCCESS] Bundle created
powershell -Command "Get-Item maven-central\bundle.zip | Select-Object Name, @{Name='Size(MB)';Expression={[math]::Round($_.Length/1MB,2)}}"

echo.
echo [4/4] Uploading to Central Portal...
cd maven-central
curl -X POST -H "Authorization: Bearer %AUTH_TOKEN%" -F "bundle=@bundle.zip" "https://central.sonatype.com/api/v1/publisher/upload?name=orangeplayer-all-%VERSION%&publishingType=USER_MANAGED" > deployment_response.json

echo.
echo [SUCCESS] Upload complete!
echo.
echo Deployment ID:
type deployment_response.json
echo.
echo.
echo Published modules (11):
echo   - orangeplayer:%VERSION%
echo   - gsyVideoPlayer-base:%VERSION%
echo   - gsyVideoPlayer-proxy_cache:%VERSION%
echo   - gsyVideoPlayer-java:%VERSION%
echo   - gsyVideoPlayer-exo_player2:%VERSION%
echo   - gsyVideoPlayer-aliplay:%VERSION%
echo   - gsyVideoPlayer-armv7a:%VERSION%
echo   - gsyVideoPlayer-armv64:%VERSION%
echo   - gsyVideoPlayer-x86:%VERSION%
echo   - gsyVideoPlayer-x86_64:%VERSION%
echo   - gsyVideoPlayer-ex_so:%VERSION%
echo.
echo Next steps:
echo   1. Visit https://central.sonatype.com/publishing/deployments
echo   2. Wait for validation (about 5-10 minutes for all modules)
echo   3. Click "Publish" to release all modules
echo.

cd ..
pause
exit /b 0

:PUBLISH_ERROR
echo.
echo [ERROR] Publish failed
cd ..
pause
exit /b 1

:CHECK_STATUS
echo.
set /p DEPLOYMENT_ID="Enter deployment ID: "

if "%DEPLOYMENT_ID%"=="" (
    echo [ERROR] Deployment ID cannot be empty
    pause
    exit /b 1
)

echo.
echo Checking deployment status...
cd maven-central
curl -X GET -H "Authorization: Bearer %AUTH_TOKEN%" "https://central.sonatype.com/api/v1/publisher/status?id=%DEPLOYMENT_ID%" > status_response.json

echo.
type status_response.json
echo.

cd ..
pause
exit /b 0

:CHECK_MAVEN
echo.
echo Checking Maven Central...
curl -s -o nul -w "%%{http_code}" "https://repo1.maven.org/maven2/io/github/706412584/orangeplayer/%VERSION%/orangeplayer-%VERSION%.pom" > temp_status.txt
set /p STATUS=<temp_status.txt
del temp_status.txt

if "%STATUS%"=="200" (
    echo [SUCCESS] Library is visible on Maven Central!
    echo.
    echo Usage:
    echo   implementation 'io.github.706412584:orangeplayer:%VERSION%'
    echo.
    echo View: https://repo1.maven.org/maven2/io/github/706412584/orangeplayer/%VERSION%/
) else (
    echo [PENDING] Still syncing (usually takes 15-30 minutes)
    echo HTTP status code: %STATUS%
)

echo.
pause
exit /b 0

:CLEAR_DEPLOYMENTS
echo.
echo [WARNING] This will delete all unpublished deployments
set /p CONFIRM="Confirm deletion? (Y/N): "

if /i not "%CONFIRM%"=="Y" (
    echo Cancelled
    pause
    exit /b 0
)

echo.
echo Fetching deployment list...
cd maven-central
curl -s -X GET -H "Authorization: Bearer %AUTH_TOKEN%" "https://central.sonatype.com/api/v1/publisher/deployments" > deployments_list.json

echo Deleting...
powershell -Command ^
"$json = Get-Content deployments_list.json -Raw | ConvertFrom-Json; " ^
"$authToken = '%AUTH_TOKEN%'; " ^
"$count = 0; " ^
"foreach ($deployment in $json) { " ^
"    $id = $deployment.deploymentId; " ^
"    $name = $deployment.name; " ^
"    Write-Host \"Deleting: $name ($id)\"; " ^
"    $headers = @{'Authorization' = \"Bearer $authToken\"}; " ^
"    try { " ^
"        Invoke-RestMethod -Uri \"https://central.sonatype.com/api/v1/publisher/deployment/$id\" -Method Delete -Headers $headers | Out-Null; " ^
"        $count++; " ^
"    } catch { } " ^
"} " ^
"Write-Host \"`n[SUCCESS] Deleted $count deployments\""

cd ..
echo.
pause
exit /b 0
