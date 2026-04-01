#!/bin/bash
set -euo pipefail

echo "========================================"
echo "  复制 IJK SO 文件到项目"
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
    echo "错误: IJK 编译目录不存在: $IJK_DIR"
    echo "请先运行 scripts/build_ijk_with_hls_fix.sh 编译 IJK"
    exit 1
fi

GSY_BASE="${PROJECT_ROOT}/GSYVideoPlayer-source"

copy_so_files() {
    local abi="$1"
    local ijk_module="$2"
    local gsy_module="$3"
    local abi_dir="$4"
    
    local src_dir="${IJK_DIR}/android/ijkplayer/${ijk_module}/src/main/libs/${abi_dir}"
    local dst_dir="${GSY_BASE}/${gsy_module}/src/main/jniLibs/${abi_dir}"
    
    if [ ! -d "$src_dir" ]; then
        echo "  ⚠️  跳过 ${abi}: 源目录不存在"
        return
    fi
    
    echo "  复制 ${abi}..."
    mkdir -p "$dst_dir"
    
    local so_count=0
    for so_file in "$src_dir"/*.so; do
        if [ -f "$so_file" ]; then
            cp "$so_file" "$dst_dir/"
            so_count=$((so_count + 1))
        fi
    done
    
    if [ $so_count -gt 0 ]; then
        echo "    ✓ 已复制 $so_count 个 SO 文件"
        ls -lh "$dst_dir"/*.so | awk '{print "      " $9 " (" $5 ")"}'
    else
        echo "    ⚠️  未找到 SO 文件"
    fi
}

echo ""
echo "[1/4] 复制 armeabi-v7a..."
copy_so_files "armeabi-v7a" "ijkplayer-armv7a" "gsyVideoPlayer-armv7a" "armeabi-v7a"

echo ""
echo "[2/4] 复制 arm64-v8a..."
copy_so_files "arm64-v8a" "ijkplayer-arm64" "gsyVideoPlayer-arm64" "arm64-v8a"

echo ""
echo "[3/4] 复制 x86..."
copy_so_files "x86" "ijkplayer-x86" "gsyVideoPlayer-x86" "x86"

echo ""
echo "[4/4] 复制 x86_64..."
copy_so_files "x86_64" "ijkplayer-x86_64" "gsyVideoPlayer-x86_64" "x86_64"

echo ""
echo "========================================"
echo "  复制完成！"
echo "========================================"
echo ""
echo "下一步："
echo "  1. 在 Windows PowerShell 中运行:"
echo "     ./gradlew clean"
echo "     ./gradlew :app:installDebug"
echo ""
echo "  2. 测试视频:"
echo "     https://c1.rrcdnbf6.com/video/sanrenxingbiyouwomei/第01集/index.m3u8"
echo ""
echo "  3. 验证 seek 是否正常（拖动到 280 秒，应该准确跳转）"
echo ""
