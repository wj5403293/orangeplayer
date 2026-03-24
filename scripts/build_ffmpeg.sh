#!/bin/bash
set -e

echo "========================================"
echo "  FFmpeg 精简版编译脚本"
echo "========================================"

if [ -d /home/xcwl ]; then
    BASE_HOME=/home/xcwl
else
    BASE_HOME=$HOME
fi

WORK_DIR=$BASE_HOME/ffmpeg-build
mkdir -p $WORK_DIR
cd $WORK_DIR

# 安装工具
echo "[1/5] 安装编译工具..."
sudo apt update
sudo apt install -y build-essential git yasm nasm pkg-config wget unzip

# 设置 NDK
export NDK_ROOT=$WORK_DIR/android-ndk-r21e
export PATH=$NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH
export OPENSSL_DIR=$WORK_DIR/openssl-android/arm64

# 检查 NDK
echo "[2/5] 检查 NDK..."
if [ ! -d "$NDK_ROOT" ]; then
    echo "下载 NDK..."
    wget -O android-ndk-r21e-linux-x86_64.zip https://dl.google.com/android/repository/android-ndk-r21e-linux-x86_64.zip
    unzip -q android-ndk-r21e-linux-x86_64.zip
    rm -f android-ndk-r21e-linux-x86_64.zip
fi
echo "NDK OK"

# 检查 OpenSSL
echo "[3/5] 检查 OpenSSL..."
OPENSSL_MARKER=$OPENSSL_DIR/.android_no_stdio_built
if [ ! -f "$OPENSSL_MARKER" ]; then
    if [ ! -d "openssl-1.1.1w" ]; then
        wget -O openssl-1.1.1w.tar.gz https://www.openssl.org/source/openssl-1.1.1w.tar.gz
        tar -xzf openssl-1.1.1w.tar.gz
        rm -f openssl-1.1.1w.tar.gz
    fi
    rm -rf "$OPENSSL_DIR"
    cd openssl-1.1.1w
    make clean 2>/dev/null || true
    export ANDROID_NDK_HOME=$NDK_ROOT
    export ANDROID_NDK_ROOT=$NDK_ROOT
    export PATH=$NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH
    ./Configure android-arm64 \
        --prefix=$OPENSSL_DIR \
        --openssldir=$OPENSSL_DIR \
        no-shared no-tests no-stdio no-ui-console no-engine no-async
    make -j$(nproc)
    make install_sw
    mkdir -p "$OPENSSL_DIR"
    touch "$OPENSSL_MARKER"
    cd $WORK_DIR
fi
echo "OpenSSL OK"
ls -la $OPENSSL_DIR/lib/

# 检查 FFmpeg
echo "[4/5] 检查 FFmpeg..."
if [ ! -d "ffmpeg-src" ]; then
    wget -O ffmpeg-5.1.tar.xz https://ffmpeg.org/releases/ffmpeg-5.1.tar.xz
    tar -xf ffmpeg-5.1.tar.xz
    mv ffmpeg-5.1 ffmpeg-src
    rm -f ffmpeg-5.1.tar.xz
fi
echo "FFmpeg OK"

# 编译 FFmpeg
echo "[5/5] 编译 FFmpeg..."
cd $WORK_DIR/ffmpeg-src
make clean 2>/dev/null || true

export PKG_CONFIG_PATH=$OPENSSL_DIR/lib/pkgconfig
export PKG_CONFIG_LIBDIR=$OPENSSL_DIR/lib/pkgconfig
unset PKG_CONFIG_SYSROOT_DIR

# 重新生成 openssl.pc，避免 OpenSSL 自带 pc 文件在交叉编译下不兼容
mkdir -p $OPENSSL_DIR/lib/pkgconfig
cat > $OPENSSL_DIR/lib/pkgconfig/openssl.pc << EOF
prefix=$OPENSSL_DIR
exec_prefix=
libdir=${OPENSSL_DIR}/lib
includedir=${OPENSSL_DIR}/include

Name: OpenSSL
Description: Secure Sockets Layer and cryptography libraries
Version: 1.1.1w
Requires: libssl libcrypto
EOF

cat > $OPENSSL_DIR/lib/pkgconfig/libssl.pc << EOF
prefix=$OPENSSL_DIR
exec_prefix=
libdir=${OPENSSL_DIR}/lib
includedir=${OPENSSL_DIR}/include

Name: libssl
Description: OpenSSL SSL/TLS library
Version: 1.1.1w
Requires.private: libcrypto
Libs: -L${OPENSSL_DIR}/lib -lssl
Libs.private: -ldl -lm
Cflags: -I${OPENSSL_DIR}/include
EOF

cat > $OPENSSL_DIR/lib/pkgconfig/libcrypto.pc << EOF
prefix=$OPENSSL_DIR
exec_prefix=
libdir=${OPENSSL_DIR}/lib
includedir=${OPENSSL_DIR}/include

Name: libcrypto
Description: OpenSSL cryptography library
Version: 1.1.1w
Libs: -L${OPENSSL_DIR}/lib -lcrypto
Libs.private: -ldl -lm
Cflags: -I${OPENSSL_DIR}/include
EOF

echo "验证 pkg-config..."
env PKG_CONFIG_PATH=$OPENSSL_DIR/lib/pkgconfig PKG_CONFIG_LIBDIR=$OPENSSL_DIR/lib/pkgconfig pkg-config --exists openssl && echo "openssl pc found"
env PKG_CONFIG_PATH=$OPENSSL_DIR/lib/pkgconfig PKG_CONFIG_LIBDIR=$OPENSSL_DIR/lib/pkgconfig pkg-config --libs --static openssl
env PKG_CONFIG_PATH=$OPENSSL_DIR/lib/pkgconfig PKG_CONFIG_LIBDIR=$OPENSSL_DIR/lib/pkgconfig pkg-config --cflags openssl

echo "配置中..."
env PKG_CONFIG_PATH=$OPENSSL_DIR/lib/pkgconfig PKG_CONFIG_LIBDIR=$OPENSSL_DIR/lib/pkgconfig ./configure \
    --prefix=$WORK_DIR/ffmpeg-android/arm64 \
    --arch=arm64 \
    --cpu=armv8-a \
    --cross-prefix=aarch64-linux-android- \
    --cc=aarch64-linux-android21-clang \
    --cxx=aarch64-linux-android21-clang++ \
    --target-os=android \
    --enable-cross-compile \
    --enable-pic \
    --enable-small \
    --disable-everything \
    --disable-debug \
    --disable-doc \
    --disable-ffmpeg \
    --disable-ffplay \
    --disable-ffprobe \
    --disable-avdevice \
    --disable-postproc \
    --enable-protocol=file,http,https,hls,concat,data \
    --enable-demuxer=hls,mpegts,mpegtsraw,mov,mp4,concat \
    --enable-muxer=mp4,mov,mpegts \
    --enable-decoder=h264,hevc,aac,mp3 \
    --enable-encoder=aac \
    --enable-parser=h264,hevc,aac,aac_latm,mpegaudio \
    --enable-bsf=h264_mp4toannexb,hevc_mp4toannexb,aac_adtstoasc \
    --enable-filter=aresample \
    --enable-openssl \
    --enable-nonfree \
    --pkg-config=pkg-config \
    --extra-cflags="-I$OPENSSL_DIR/include -fPIC -DANDROID -D__ANDROID_API__=21" \
    --extra-ldflags="-L$OPENSSL_DIR/lib" \
    --extra-libs="-lssl -lcrypto -ldl -landroid -lm" \
    --pkg-config-flags="--static"

echo "编译中... (约 30 分钟)"
make -j$(nproc)

echo "安装中..."
make install

echo ""
echo "========================================"
echo "  编译完成!"
echo "========================================"
ls -lh $WORK_DIR/ffmpeg-android/arm64/lib/
echo ""

# 复制到项目
echo "复制到项目目录..."
PROJECT_DIR="/mnt/d/android/projecet_iade/orangeplayer/palyerlibrary/src/main/jniLibs/arm64-v8a"
mkdir -p $PROJECT_DIR
cp $WORK_DIR/ffmpeg-android/arm64/lib/*.a $PROJECT_DIR/ 2>/dev/null || echo "注意：生成的是 .a 静态库"
echo "完成!"
