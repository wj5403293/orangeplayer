# OrangePlayer 一键发布脚本
# 用法: .\release.ps1 v1.0.1

param(
    [Parameter(Mandatory=$true)]
    [string]$Version
)

Write-Host "========================================"
Write-Host "  OrangePlayer 发布脚本"
Write-Host "  版本: $Version"
Write-Host "========================================"

# 检查版本格式
if ($Version -notmatch "^v\d+\.\d+\.\d+$") {
    Write-Host "错误: 版本格式应为 vX.X.X (例如 v1.0.1)"
    exit 1
}

# 检查是否有未提交的更改
$status = git status --porcelain
if ($status) {
    Write-Host "警告: 有未提交的更改"
    git status --short
    $confirm = Read-Host "是否继续? (y/n)"
    if ($confirm -ne "y") {
        exit 1
    }
    git add -A
    git commit -m "Release $Version"
}

# 推送代码
Write-Host ""
Write-Host "推送代码到 main 分支..."
git push origin main

# 检查 tag 是否已存在
$existingTag = git tag -l $Version
if ($existingTag) {
    Write-Host "Tag $Version 已存在，删除旧 tag..."
    git tag -d $Version
    git push origin :refs/tags/$Version 2>$null
}

# 创建新 tag
Write-Host ""
Write-Host "创建 tag: $Version"
git tag $Version

# 推送 tag
Write-Host "推送 tag 到远程仓库..."
git push origin $Version

Write-Host ""
Write-Host "========================================"
Write-Host "  发布完成!"
Write-Host "========================================"
Write-Host ""
Write-Host "GitHub Actions 将自动构建并发布 APK"
Write-Host "查看进度: https://github.com/706412584/orangeplayer/actions"
Write-Host ""
Write-Host "JitPack 将自动构建库"
Write-Host "查看进度: https://jitpack.io/#706412584/orangeplayer"
Write-Host ""
$dep = "implementation 'com.github.706412584:orangeplayer:" + $Version + "'"
Write-Host "依赖方式: $dep"
