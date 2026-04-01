#!/bin/bash
set -euo pipefail

cleanup_proxy() {
    unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY all_proxy no_proxy NO_PROXY
}

enable_wsl_proxy_if_available() {
    local proxy_port="${CLASH_PROXY_PORT:-7887}"
    local host_ip="127.0.0.1"

    if [ -f /etc/resolv.conf ]; then
        host_ip=$(awk '/nameserver/ {print $2; exit}' /etc/resolv.conf 2>/dev/null || true)
    fi

    if [ -z "$host_ip" ]; then
        echo "[proxy] 未找到 WSL 网关地址，使用直连"
        cleanup_proxy
        return
    fi

    if timeout 2 bash -c "</dev/tcp/${host_ip}/${proxy_port}" >/dev/null 2>&1; then
        local proxy_url="http://${host_ip}:${proxy_port}"
        export http_proxy="$proxy_url"
        export https_proxy="$proxy_url"
        export HTTP_PROXY="$proxy_url"
        export HTTPS_PROXY="$proxy_url"
        export no_proxy="127.0.0.1,localhost"
        export NO_PROXY="$no_proxy"
        echo "[proxy] 检测到 Clash 代理可用，临时启用: $proxy_url"
    else
        echo "[proxy] Clash 代理不可用，使用直连"
        cleanup_proxy
    fi
}

trap cleanup_proxy EXIT
enable_wsl_proxy_if_available

echo "========================================"
echo "  编译带 HLS Discontinuity 修复的 IJK"
echo "========================================"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ -d /home/xcwl ]; then
    BASE_HOME=/home/xcwl
else
    BASE_HOME=$HOME
fi

WORK_DIR="${BASE_HOME}/ijkplayer-build-hls-fix"
IJK_VERSION="k0.8.8"
IJK_DIR="${WORK_DIR}/ijkplayer"

mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

echo "[1/8] 安装依赖..."
sudo apt update
sudo apt install -y git yasm nasm build-essential wget unzip

echo "[2/8] 下载 IJKPlayer..."
if [ ! -d "$IJK_DIR" ]; then
    echo "  克隆 IJKPlayer 仓库..."
    git clone https://github.com/bilibili/ijkplayer.git
    cd "$IJK_DIR"
    echo "  切换到稳定版本 $IJK_VERSION..."
    git checkout -B "$IJK_VERSION" "$IJK_VERSION"
else
    echo "  IJKPlayer 已存在，跳过下载"
    cd "$IJK_DIR"
fi

echo "[3/8] 应用 GSYVideoPlayer 的 16K 补丁..."
PATCH_DIR="${PROJECT_ROOT}/GSYVideoPlayer-source/16kpatch"

if [ ! -d "$PATCH_DIR" ]; then
    echo "  错误: 找不到补丁目录 $PATCH_DIR"
    exit 1
fi

echo "  应用主补丁 (ndk_r22_16k_commit.patch)..."
git apply --check "$PATCH_DIR/ndk_r22_16k_commit.patch" || echo "  警告: 补丁检查失败，尝试强制应用..."
git apply "$PATCH_DIR/ndk_r22_16k_commit.patch" || echo "  警告: 主补丁应用失败，继续..."

echo "  应用 soundtouch 补丁..."
cd ijkmedia/ijksoundtouch
git apply --check "$PATCH_DIR/ndk_r22_soundtouch.patch" || echo "  警告: soundtouch 补丁检查失败..."
git apply "$PATCH_DIR/ndk_r22_soundtouch.patch" || echo "  警告: soundtouch 补丁应用失败，继续..."
cd ../..

echo "  应用 ijkyuv 补丁..."
cd ijkmedia/ijkyuv
git apply --check "$PATCH_DIR/ndk_r22_ijkyuv.patch" || echo "  警告: ijkyuv 补丁检查失败..."
git apply "$PATCH_DIR/ndk_r22_ijkyuv.patch" || echo "  警告: ijkyuv 补丁应用失败，继续..."
cd ../..

echo "[4/8] 修改 init-android.sh 使用修复版 FFmpeg..."
if [ ! -f "init-android.sh.bak" ]; then
    echo "  备份原始 init-android.sh..."
    cp init-android.sh init-android.sh.bak
fi

echo "  替换 FFmpeg 源为 CarGuo/FFmpeg (包含 HLS 修复)..."
# 使用 CarGuo 的 FFmpeg，它已经包含了很多修复
sed -i 's|IJK_FFMPEG_UPSTREAM=.*|IJK_FFMPEG_UPSTREAM=https://github.com/CarGuo/FFmpeg.git|g' init-android.sh
sed -i 's|IJK_FFMPEG_FORK=.*|IJK_FFMPEG_FORK=https://github.com/CarGuo/FFmpeg.git|g' init-android.sh
sed -i 's|IJK_FFMPEG_COMMIT=.*|IJK_FFMPEG_COMMIT=ijk-n4.3-20260301-007|g' init-android.sh

echo "[5/8] 初始化 OpenSSL 和 FFmpeg..."
echo "  初始化 OpenSSL（这可能需要几分钟）..."
./init-android-openssl.sh

echo "  初始化 FFmpeg（这可能需要 10-20 分钟，取决于网络速度）..."
./init-android.sh

echo "[6/8] 应用 FFmpeg n4.3 补丁..."
if [ -d "android/ffmpeg" ]; then
    cd android/ffmpeg
    echo "  应用 FFmpeg ijk 补丁..."
    git apply --check "$PATCH_DIR/ndk_r22_ffmpeg_n4.3_ijk.patch" || echo "  警告: FFmpeg 补丁检查失败..."
    git apply "$PATCH_DIR/ndk_r22_ffmpeg_n4.3_ijk.patch" || echo "  警告: FFmpeg 补丁应用失败（可能已包含修复），继续..."
    cd ../..
else
    echo "  警告: FFmpeg 目录不存在，跳过 FFmpeg 补丁"
fi

echo "[7/8] 配置编译选项..."
cd config
if [ -f module.sh ]; then
    rm -f module.sh
fi

# 复制修正版配置文件
echo "  使用修正版 module-lite-more-fixed.sh（添加了缺失的基础协议）"
cp "$PROJECT_ROOT/GSYVideoPlayer-source/module-lite-more-fixed.sh" ./module-lite-more-fixed.sh
ln -s module-lite-more-fixed.sh module.sh
cd ..

echo "[8/8] 编译 OpenSSL..."
cd android/contrib
echo "  清理旧的 OpenSSL 编译产物..."
./compile-openssl.sh clean

echo "  开始编译 OpenSSL（这可能需要 10-30 分钟）..."
./compile-openssl.sh all

echo "[9/10] 编译 FFmpeg（这是最耗时的步骤，预计 2-4 小时）..."
echo "  清理旧的 FFmpeg 编译产物..."
./compile-ffmpeg.sh clean

echo "  开始编译 FFmpeg..."
echo "  提示：这个过程会很长，你可以去喝杯咖啡或者做点别的事情"
echo "  编译开始时间: $(date '+%Y-%m-%d %H:%M:%S')"
./compile-ffmpeg.sh all
echo "  编译结束时间: $(date '+%Y-%m-%d %H:%M:%S')"

echo "[10/10] 编译 IJK..."
cd ..
echo "  开始编译 IJK（这可能需要 30-60 分钟）..."
./compile-ijk.sh all

echo ""
echo "========================================"
echo "  编译完成！"
echo "========================================"
echo ""
echo "SO 文件位置："
echo "  armv7a: $IJK_DIR/android/ijkplayer/ijkplayer-armv7a/src/main/libs/armeabi-v7a/"
echo "  arm64:  $IJK_DIR/android/ijkplayer/ijkplayer-arm64/src/main/libs/arm64-v8a/"
echo "  x86:    $IJK_DIR/android/ijkplayer/ijkplayer-x86/src/main/libs/x86/"
echo "  x86_64: $IJK_DIR/android/ijkplayer/ijkplayer-x86_64/src/main/libs/x86_64/"
echo ""
echo "下一步："
echo "  1. 运行 scripts/copy_ijk_so.sh 复制 SO 文件到项目"
echo "  2. 在 Windows 中重新编译项目: ./gradlew :app:installDebug"
echo "  3. 测试视频: https://c1.rrcdnbf6.com/video/sanrenxingbiyouwomei/第01集/index.m3u8"
echo ""
