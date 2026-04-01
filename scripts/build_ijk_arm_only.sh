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
echo "  编译 IJK (仅 ARM 架构)"
echo "========================================"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ -d /home/xcwl ]; then
    BASE_HOME=/home/xcwl
else
    BASE_HOME=$HOME
fi

WORK_DIR="${BASE_HOME}/ijkplayer-build-hls-fix"
IJK_DIR="${WORK_DIR}/ijkplayer"

if [ ! -d "$IJK_DIR" ]; then
    echo "错误: IJK 目录不存在，请先运行完整脚本"
    exit 1
fi

cd "$IJK_DIR"

echo "[1/5] 手动修复 OpenSSL x86/x86_64 下载失败..."
echo "  跳过 x86 和 x86_64 架构（只编译 ARM）"

echo "[2/5] 配置编译选项..."
cd config
if [ -f module.sh ]; then
    rm -f module.sh
fi
echo "  使用 module-lite-hevc.sh 配置"
ln -s module-lite-hevc.sh module.sh
cd ..

echo "[3/5] 编译 OpenSSL (仅 armv7a 和 arm64)..."
cd android/contrib

# 只编译 ARM 架构
echo "  清理旧的编译产物..."
./compile-openssl.sh clean

echo "  编译 armv7a..."
./compile-openssl.sh armv7a

echo "  编译 arm64..."
./compile-openssl.sh arm64

echo "[4/5] 编译 FFmpeg (仅 armv7a 和 arm64)..."
echo "  清理旧的编译产物..."
./compile-ffmpeg.sh clean

echo "  编译 armv7a FFmpeg（这可能需要 1-2 小时）..."
echo "  编译开始时间: $(date '+%Y-%m-%d %H:%M:%S')"
./compile-ffmpeg.sh armv7a
echo "  armv7a 编译结束时间: $(date '+%Y-%m-%d %H:%M:%S')"

echo "  编译 arm64 FFmpeg（这可能需要 1-2 小时）..."
echo "  编译开始时间: $(date '+%Y-%m-%d %H:%M:%S')"
./compile-ffmpeg.sh arm64
echo "  arm64 编译结束时间: $(date '+%Y-%m-%d %H:%M:%S')"

echo "[5/5] 编译 IJK (仅 armv7a 和 arm64)..."
cd ..

echo "  编译 armv7a IJK..."
./compile-ijk.sh armv7a

echo "  编译 arm64 IJK..."
./compile-ijk.sh arm64

echo ""
echo "========================================"
echo "  编译完成！(仅 ARM 架构)"
echo "========================================"
echo ""
echo "SO 文件位置："
echo "  armv7a: $IJK_DIR/android/ijkplayer/ijkplayer-armv7a/src/main/libs/armeabi-v7a/"
echo "  arm64:  $IJK_DIR/android/ijkplayer/ijkplayer-arm64/src/main/libs/arm64-v8a/"
echo ""
echo "下一步："
echo "  1. 运行 scripts/copy_ijk_so.sh 复制 SO 文件到项目"
echo "  2. 在 Windows 中重新编译项目: ./gradlew :app:installDebug"
echo "  3. 测试视频: https://c1.rrcdnbf6.com/video/sanrenxingbiyouwomei/第01集/index.m3u8"
echo ""
