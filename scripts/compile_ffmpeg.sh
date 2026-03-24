#!/bin/bash

# FFmpeg 编译脚本 - 单独执行版本

set -e

echo "=== 开始编译 FFmpeg ==="

# 设置环境变量
export NDK_ROOT=~/ffmpeg-build/android-ndk-r21e
export TOOLCHAIN=$NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64
export SYSROOT=$TOOLCHAIN/sysroot
export PATH=$TOOLCHAIN/bin:$PATH
export OPENSSL_DIR=~/ffmpeg-build/openssl-android/arm64

cd ~/ffmpeg-build/ffmpeg-src

echo "步骤 1: 清理之前的编译..."
make clean 2>/dev/null || true

echo "步骤 2: 配置 FFmpeg..."
./configure \
    --prefix=$HOME/ffmpeg-build/ffmpeg-android/arm64 \
    --arch=arm64 \
    --cpu=armv8-a \
    --cross-prefix=aarch64-linux-android- \
    --cc=aarch64-linux-android21-clang \
    --cxx=aarch64-linux-android21-clang++ \
    --sysroot=$SYSROOT \
    --target-os=android \
    --enable-cross-compile \
    --enable-pic \
    --enable-small \
    --enable-optimizations \
    --disable-everything \
    --disable-debug \
    --disable-doc \
    --disable-ffmpeg \
    --disable-ffplay \
    --disable-ffprobe \
    --disable-avdevice \
    --disable-postproc \
    --disable-network \
    --enable-protocol=file,http,https,hls,concat,data \
    --enable-demuxer=hls,mpegts,mpegtsraw,mov,mp4,concat \
    --enable-muxer=mp4,mov,mpegts \
    --enable-decoder=h264,hevc,aac,mp3,mpegvideo \
    --enable-encoder=aac \
    --enable-parser=h264,hevc,aac,aac_latm,mpegaudio \
    --enable-bsf=h264_mp4toannexb,hevc_mp4toannexb,aac_adtstoasc \
    --enable-filter=aresample,aformat,scale \
    --enable-openssl \
    --enable-nonfree \
    --extra-cflags="-I$OPENSSL_DIR/include -fPIC -DANDROID -D__ANDROID_API__=21 -Os" \
    --extra-ldflags="-L$OPENSSL_DIR/lib -Os" \
    --extra-libs="-lssl -lcrypto -ldl -landroid -lm"

echo "步骤 3: 编译 FFmpeg (这步最耗时)..."
echo "请耐心等待 30-60 分钟..."
make -j$(nproc)

echo "步骤 4: 安装..."
make install

echo ""
echo "=== 编译完成! ==="
echo "编译产物位于: ~/ffmpeg-build/ffmpeg-android/arm64/lib/"
ls -lh ~/ffmpeg-build/ffmpeg-android/arm64/lib/
