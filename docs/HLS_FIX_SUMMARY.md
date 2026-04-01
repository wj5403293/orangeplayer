# HLS Discontinuity Seek 问题修复方案总结

## 问题描述

使用 IJK 播放器播放包含 `#EXT-X-DISCONTINUITY` 的 m3u8 视频时，seek 会跳转到错误位置。

**测试视频：**
```
https://c1.rrcdnbf6.com/video/sanrenxingbiyouwomei/第01集/index.m3u8
```

**现象：**
- 用户 seek 到 280 秒
- 实际跳转到 511 秒
- 差异：231 秒

**根本原因：**
- TS 文件内部 PTS 是全局连续的（580秒）
- m3u8 声明的时长只计算正片（302秒）
- IJK 信任 TS 内部 PTS，导致时间不匹配

## 解决方案对比

### 方案 A：使用 ExoPlayer（推荐 ⭐⭐⭐⭐⭐）

**优点：**
- ✅ 不需要编译
- ✅ ExoPlayer 原生支持 discontinuity
- ✅ 维护成本低
- ✅ 立即可用
- ✅ 性能好

**实现：**
```java
// 在 PlayerEngineSelector.java 中
if (url.contains(".m3u8")) {
    return PlayerConstants.ENGINE_EXO;
}
```

**时间：** 5 分钟
**成功率：** 100%

### 方案 B：编译修复版 IJK（可尝试 ⭐⭐）

**优点：**
- ✅ 使用修复版 FFmpeg fork
- ✅ 从底层解决问题

**缺点：**
- ❌ 编译时间长（4-8小时）
- ❌ 可能遇到兼容性问题
- ❌ 维护成本高
- ❌ 成功率不确定

**步骤：**
1. 运行 `scripts/compile_ijk_with_hls_fix.sh`（在 WSL 中）
2. 运行 `scripts/copy_ijk_so.sh`
3. 重新编译项目测试

**时间：** 4-8 小时
**成功率：** 60-70%

## 推荐方案

**强烈建议使用方案 A（ExoPlayer）**

理由：
1. ExoPlayer 已经完美解决了这个问题
2. 不需要编译，立即可用
3. 维护成本低
4. 用户体验好（自动切换，无感知）
5. 这是服务器端的问题，播放器端很难完美解决

## 如果坚持编译 IJK

已提供自动化脚本：
- `scripts/compile_ijk_with_hls_fix.sh` - 编译脚本
- `scripts/copy_ijk_so.sh` - 复制 SO 文件
- `docs/COMPILE_IJK_WITH_HLS_FIX.md` - 详细文档

**注意：**
- 需要 WSL Ubuntu 环境
- 需要 4-8 小时编译时间
- 可能遇到兼容性问题
- 建议先备份原始 SO 文件

