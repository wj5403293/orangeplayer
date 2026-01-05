# OrangePlayer 一键发布脚本
# 用法: .\release.ps1 v1.0.1

param(
    [Parameter(Mandatory=$true)]
    [string]$Version
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  OrangePlayer 发布脚本" -ForegroundColor Cyan
Write-Host "  版本: $Version" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 检查版本格式
if ($Version -notmatch "^v\d+\.\d+\.\d+$") {
    Write-Host "错误: 版本格式应为 vX.X.X (例如 v1.0.1)" -ForegroundColor Red
    exit 1
}

# 检查是否有未提交的更改
$status = git status --porcelain
if ($status) {
    Write-Host "警告: 有未提交的更改" -ForegroundColor Yellow
    git status --short
    $confirm = Read-Host "是否继续? (y/n)"
    if ($confirm -ne "y") {
        exit 1
    }
    git add -A
    git commit -m "Release $Version"
}

# 推送代码
Write-Host "`n推送代码到 main 分支..." -ForegroundColor Green
git push origin main

# 检查 tag 是否已存在
$existingTag = git tag -l $Version
if ($existingTag) {
    Write-Host "Tag $Version 已存在，删除旧 tag..." -ForegroundColor Yellow
    git tag -d $Version
    git push origin :refs/tags/$Version 2>$null
}

# 创建新 tag
Write-Host "`n创建 tag: $Version" -ForegroundColor Green
git tag $Version

# 推送 tag
Write-Host "推送 tag 到远程仓库..." -ForegroundColor Green
git push origin $Version

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  发布完成!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "GitHub Actions 将自动构建并发布 APK"
Write-Host "查看进度: https://github.com/706412584/orangeplayer/actions"
Write-Host ""
Write-Host "JitPack 将自动构建库"
Write-Host "查看进度: https://jitpack.io/#706412584/orangeplayer"
Write-Host ""
Write-Host "依赖方式:"
Write-Host "  implementation 'com.github.706412584:orangeplayer:$Version'"
