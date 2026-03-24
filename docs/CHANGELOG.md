# OrangePlayer 更新日志
## [1.3.1] - 2026-03-24

### 📦 重大变更：下载库模块化迁移

#### 迁移概述

本次更新将视频下载功能从 `palyerlibrary` 核心模块中独立出来，形成全新的 `orange-downloader` 子模块。这是 OrangePlayer 架构优化的重要一步，旨在实现更灵活的功能组合与更清晰的代码边界。

---

#### 迁移原因

1. **降低耦合度**
   - 原下载库 `com.jeffmony.downloader` 嵌入在播放器核心模块内部，导致播放器与下载功能强绑定
   - 用户即使不需要下载功能，也不得不引入全部下载相关代码和资源

2. **支持按需引入**
   - 新架构允许用户根据实际需求选择是否集成下载功能
   - 未集成下载模块时，播放器会优雅降级并提示用户配置依赖

3. **便于独立维护**
   - 下载模块可独立迭代，不影响播放器核心功能
   - 降低版本发布的耦合风险

4. **提高复用性**
   - `orange-downloader` 可作为独立组件被其他项目引用
   - 不强制依赖播放器模块

---

#### 新库地址

| 项目 | 旧值 | 新值 |
|------|------|------|
| 模块名 | 内嵌于 `palyerlibrary` | `orange-downloader` |
| 包名 | `com.jeffmony.downloader` | `com.orange.downloader` |
| Maven 坐标 | 无独立坐标 | `io.github.706412584:orange-downloader:1.3.2` |

**目录结构对比：**

```
旧结构（1.3.1 及之前）：
palyerlibrary/
└── src/main/java/
    └── com/
        ├── orange/playerlibrary/      # 播放器核心
        └── jeffmony/downloader/       # 下载库（内嵌）

新结构（1.3.2 及之后）：
├── palyerlibrary/                    # 播放器核心模块
│   └── src/main/java/com/orange/playerlibrary/
└── orange-downloader/                # 独立下载模块
    └── src/main/java/com/orange/downloader/
```

---

#### 兼容性说明

##### API 兼容性

| 兼容项 | 说明 |
|--------|------|
| 公开 API | **完全兼容**，所有公开接口签名保持不变 |
| 类名/方法名 | **完全兼容**，仅包名发生变化 |
| 资源文件 | 已重命名添加前缀 `orange_download_`，避免资源冲突 |
| 数据库 | **完全兼容**，表结构与数据格式不变 |
| 下载缓存 | **完全兼容**，已下载的任务可继续使用 |

##### 自动依赖传递

`palyerlibrary` 已通过 `api` 依赖 `orange-downloader`，因此：

- **现有用户无需任何修改**，只需更新版本号即可
- 下载功能会自动传递依赖，保持原有行为

```gradle
// palyerlibrary/build.gradle
dependencies {
    api project(':orange-downloader')  // 自动传递给使用者
}
```

##### 优雅降级机制

当下载模块未正确配置时，播放器会优雅降级而非崩溃：

```java
// VideoEventManager.java 中的降级处理
private void showDownloadManagerDialog() {
    try {
        Class.forName("com.orange.downloader.ui.SimpleDownloadDialogView");
        // 模块可用，正常显示下载对话框
        if (mDownloadDialog == null) {
            mDownloadDialog = new com.orange.downloader.ui.SimpleDownloadDialogView(mContext);
        }
        mDownloadDialog.show();
    } catch (ClassNotFoundException e) {
        // 模块不可用，显示友好提示
        showToast("下载功能未配置\n请添加 orange-downloader 模块依赖");
    }
}
```

---

#### 用户升级指南

##### 场景一：使用 palyerlibrary 完整包（推荐）

如果您通过 `palyerlibrary` 依赖播放器，**无需任何代码修改**：

```gradle
// build.gradle
dependencies {
    // 只需更新版本号，下载模块会自动传递
    implementation 'io.github.706412584:orangeplayer:1.3.2'
}
```

##### 场景二：仅使用播放器核心（不需要下载功能）

如果您只需要播放功能，可以排除下载模块：

```gradle
// build.gradle
dependencies {
    implementation('io.github.706412584:orangeplayer:1.3.2') {
        exclude group: 'io.github.706412584', module: 'orange-downloader'
    }
}
```

> ⚠️ 排除后，调用下载相关 API 时会收到友好提示，不会导致崩溃。

##### 场景三：独立使用下载模块

如果您只需要下载功能，可单独引入：

```gradle
// build.gradle
dependencies {
    implementation 'io.github.706412584:orange-downloader:1.3.2'
}
```

---

#### 代码迁移示例

如果您之前直接引用了下载库内部类，需要更新 import 语句：

```java
// 旧代码（1.3.1 及之前）
import com.jeffmony.downloader.VideoDownloadManager;
import com.jeffmony.downloader.model.VideoTaskItem;
import com.jeffmony.downloader.listener.DownloadListener;

// 新代码（1.3.2 及之后）
import com.orange.downloader.VideoDownloadManager;
import com.orange.downloader.model.VideoTaskItem;
import com.orange.downloader.listener.DownloadListener;
```

**大多数用户无需修改**，因为 `palyerlibrary` 已封装了常用 API：

```java
// 推荐用法（无需关心内部包名）
SimpleDownloadManager manager = SimpleDownloadManager.getInstance(context);
manager.startDownload(url, title);
```

---

#### 资源文件变更

为避免与其他库的资源冲突，下载模块的资源文件已统一添加 `orange_download_` 前缀：

| 旧资源名 | 新资源名 |
|----------|----------|
| `ic_download` | `orange_download_ic_download` |
| `ic_delete` | `orange_download_ic_delete` |
| `ic_delete_all` | `orange_download_ic_delete_all` |
| `dialog_download_list.xml` | `orange_download_dialog_list.xml` |
| `item_download_task.xml` | `orange_download_item_task.xml` |

如果您在自定义 UI 时引用了这些资源，请更新引用名称。

---

#### 常见问题

##### Q1: 升级后下载记录会丢失吗？

**不会。** 下载记录存储在应用私有目录的 SQLite 数据库中，模块迁移不影响数据持久化。已下载的视频可继续播放。

##### Q2: 升级后编译报错 "找不到符号"？

请检查是否有直接引用 `com.jeffmony.downloader` 包下类的代码，将其更新为 `com.orange.downloader`。

##### Q3: 如何确认下载模块是否正确集成？

```java
// 在 Application 或首次使用前检查
try {
    Class.forName("com.orange.downloader.VideoDownloadManager");
    Log.d("TAG", "下载模块已正确集成");
} catch (ClassNotFoundException e) {
    Log.w("TAG", "下载模块未集成，部分功能不可用");
}
```

##### Q4: 可以同时使用新旧包名吗？

**不建议。** 虽然技术上可行，但会导致类冲突和不可预期的行为。请确保项目中只引用新包名。

---

### 🐛 Bug 修复

#### M3U8 去广告流程优化
- **修复去广告时机错误导致切集时未立即显示加载动画的问题**
  - **问题现象**：切集时屏幕短暂显示旧视频画面，等待数秒后黑屏，然后播放去广告后的视频
  - **根本原因**：去广告拦截在 `setUp()` 阶段触发，但 `startPlayLogic()` 未被拦截，导致底层播放器使用旧的 URL 继续播放
  - **解决方案**：将去广告拦截逻辑从 `setUp()` 迁移至 `startPlayLogic()`，在播放启动阶段统一拦截

- **新增独立状态 `STATE_M3U8_AD_REMOVAL`**
  - 参照嗅探模式（`STATE_STARTSNIFFING`）的实现方式，新增独立的 M3U8 去广告状态
  - 去广告开始时立即显示加载动画，结束后隐藏
  - 避免与 GSY 播放器内部状态冲突

- **优化状态管理**
  - 新增 `mPendingM3U8AdRemoval` 标志，防止去广告异步处理期间重复执行播放逻辑
  - 新增 `mM3U8AdRequestToken` 请求令牌，避免旧的异步结果回流串到新播放链路
  - 新增 `mBypassM3U8AdRemovalOnce` 一次性跳过标志，防止播放失败回退原始 URL 时再次进入去广告死循环

### 📝 技术细节
```java
// 去广告状态触发加载动画（类似嗅探模式）
setOrangePlayState(STATE_M3U8_AD_REMOVAL);  // 显示加载动画

// 去广告完成后
setOrangePlayState(STATE_M3U8_AD_REMOVAL_END);  // 隐藏加载动画
bindResolvedVideoSource(playUrl, ...);
startPlayLogic();  // 继续播放流程
```

---

## [1.3.0] - 2026-03-23

### ✨ 新特性
- **记忆播放功能强化**：
  - 新增播放进度实时保存机制，每秒同步进度至数据库，彻底解决应用异常退出/强制杀进程导致进度丢失的问题。
  - 新增智能恢复条件：仅当"观看进度 > 1 分钟"且"距离视频结尾 > 1 分钟"时，才会触发进度记录与恢复。
  - 新增记忆播放与"跳过片头"冲突处理逻辑：若触发记忆恢复，则自动覆盖跳过片头功能，保证观看连贯性。
  - 新增全局记忆播放开关，支持业务层灵活控制功能启停。

#### 使用说明
```java
// 1. 全局开启记忆播放功能（默认关闭）
PlayerSettingsManager.getInstance(context).setMemoryPlayEnabled(true);

// 2. 针对当前视频实例启用记忆播放状态
mVideoView.setKeepVideoPlaying(true);

// 完成以上两步后，播放器将自动在后台高频记录进度，并在下次加载相同视频时自动精准恢复进度。
```

### 🐛 Bug 修复

#### M3U8 去广告重大修复
- **修复 DISCONTINUITY 标记误判导致的所有片段被标记为广告的问题**
  - **问题现象**：播放 https://v.cdnlz22.com/20240815/3725_2916c479/index.m3u8 时，所有 402 个片段都被标记为广告，导致播放失败
  - **根本原因**：M3U8 文件包含 42 个 DISCONTINUITY 标记（HLS 直播流特征），方法 3（DISCONTINUITY 检测）错误地把所有 DISCONTINUITY 之间的片段都标记为广告
  - **解决方案**：优化广告检测逻辑，仅在方法 1（前缀长度检测）和方法 2（路径模式检测）未检测到广告时才执行方法 3，避免对已明确识别的广告进行重复检测和误判
  - **效果**：正确识别真正的广告片段（position 74-77，共 4 个片段），保留所有正片内容，播放流畅无错误

- **修复 M3U8 广告检测超时和回调未执行问题**
  - 增加超时机制（10 秒），防止广告检测无限期挂起
  - 增强日志记录，在关键步骤添加详细日志便于定位问题
  - 确保回调在所有代码路径下都能执行，包括异常情况
  - 修复 mid-roll 广告检测因日志过载导致的假死问题

- **优化中间广告检测策略**
  - DISCONTINUITY 标记超过 50 个时自动跳过中间广告检测（适用于 HLS 直播流）
  - 减少日志输出，使用聚合日志代替逐条日志
  - 改进广告检测算法，优先使用前缀长度和路径模式等更可靠的特征

---

## [1.2.9] - 2026-03-22

### ✨ 新功能

#### 下载功能增强
- **下载完成后自动重命名文件**
  - M3U8 下载合并后自动重命名为用户设置的标题
  - 解决之前文件名为 MD5 哈希值的问题
  - 文件名冲突时自动添加序号

- **本地已下载检查**
  - 新增 `getLocalVideoPath(url)` 方法，检查视频是否已下载
  - 修复多线程断点续传遗留 `range.info` 未清理的问题
  - 弃用后缀名探测方案，改为直接联查本地 SQLite 数据库中 `VideoTaskItem` 的 `SUCCESS` 状态，确保不播放破损/半成品文件
  - **播放前自动拦截**：`setUp()` 时自动检查本地已下载，直接播放本地文件

- **下载路径 API**
  - 新增 `setDownloadPath(path)` 方法，支持自定义下载目录
  - 新增 `getDownloadPath()` 方法，获取当前下载目录
  - 支持动态切换下载路径

- **SimpleDownloadManager 单例模式**
  - 改为单例模式，全局共享同一实例
  - 配置全局生效，避免多次创建导致状态不同步

### 🐛 Bug修复

- **修复 MP4 单文件下载损坏问题**
  - 原因：下载器在执行普通 MP4/FLV 文件下载时，开启了 `MultiSegVideoDownloadTask` 多线程并发写入，导致在部分 Android 系统下 `RandomAccessFile` 发生覆盖截断，破坏了文件结构（如 moov 头损坏）。
  - 解决方案：针对非 M3U8 的普通视频文件，降级回退至 `BaseVideoDownloadTask` 单线程顺序下载，确保文件完整性；且在下载完成后自动还原正确的后缀名（如 `.mp4`）。
- 修复下载按钮点击时未检查本地是否已下载的问题
- 修复 `SimpleDownloadManager` 多次创建导致配置不生效的问题
- 修复 IJKPlayer 播放 Android 10+ 内部存储绝对路径时报错 `-10000` 的问题（增加协议白名单并设置 `safe=0` 选项）

### 📝 API 示例

```java
// 获取下载管理器单例
SimpleDownloadManager manager = SimpleDownloadManager.getInstance(context);

// 设置下载路径
manager.setDownloadPath("/sdcard/MyVideos");

// 检查本地是否已下载
String localPath = manager.getLocalVideoPath(url);
if (localPath != null) {
    // 直接播放本地文件
    videoView.setUp(localPath, true, "标题");
}

// 检查是否正在下载
boolean downloading = manager.isDownloading(url);

// 开始下载（自动检查本地）
String path = manager.startDownloadWithLocalCheck(url, "视频标题");

// OrangeToast 使用
OrangeToast.show(videoView, "提示消息");
OrangeToast.showLong(videoView, "长提示消息");
```

---

## [1.2.8] - 2026-03-20

### 🐛 Bug修复

#### v3打包播放器初始化修复
- **修复v3打包后播放器无法显示的问题**
  - 原因：iApp不支持递归解析子依赖，导致 `gsyijkjava` 丢失
  - 解决方案：在 `iapp.sdk` 中手动添加 `io.github.carguo:gsyijkjava:1.0.0` 依赖
- **修复IJK内核设置不生效的问题**
  - 原因：`setPlayerEngine()` 在 `OrangevideoView` 创建之后调用，但引擎初始化在构造函数中
  - 解决方案：在创建 `OrangevideoView` 之前设置引擎
- **移除系统播放器横竖屏切换的暂停/恢复特殊处理**
  - 原因：`enableMediaCodecTexture()` 已修复 SurfaceTexture 保留问题，不再需要暂停/恢复
  - 效果：系统播放器横竖屏切换更流畅，无短暂暂停

---

## [1.2.7] - 2026-03-20

### ✨ 新功能

#### 播放器音量控制
- 新增 `setPlayerVolume(float volume)` 方法，设置播放器音量 (0.0-1.0)
- 新增 `setPlayerVolumePercent(int percent)` 方法，设置播放器音量百分比 (0-100)
- 音量设置仅影响当前播放器，不影响系统音量

#### 静音控制
- 新增 `setMute(boolean isMute)` 方法，设置静音
- 新增 `isMute()` 方法，获取静音状态
- 新增 `toggleMute()` 方法，切换静音状态

#### 循环播放
- 新增 `setLooping(boolean looping)` 方法，设置循环播放
- 新增 `isLooping()` 方法，获取循环播放状态

#### 截图功能
- 新增 `takeScreenshot(callback)` 方法，截取当前画面
- 新增 `takeScreenshotAndSave(callback)` 方法，截图并保存到相册

#### 缓冲进度
- 新增 `getBufferedPercentage()` 方法，获取缓冲进度百分比 (0-100)

### 🐛 Bug修复

#### M3U8去广告保留AES加密密钥
- **修复去广告后AES加密视频无法播放的问题**
  - 原因：去广告处理时未保留 `#EXT-X-KEY` 标签，导致加密密钥丢失
  - 解决方案：解析时保存每个片段的加密密钥信息，重建m3u8时正确输出
  - 支持完整的加密属性：METHOD、URI、IV

#### M3U8去广告默认关闭
- **去广告功能默认关闭**，需要手动启用
  - 调用 `M3U8AdManager.getInstance(context).setEnabled(true)` 开启
  - 避免对普通视频造成不必要的处理


## [1.2.6] - 2026-03-18

### 🐛 Bug修复

#### M3U8去广告Seek修复（重大修复）
- **修复去广告后seek时间轴不准确的问题**
  - 原因：删除广告片段导致时间轴不连续，播放器seek计算错误
  - 解决方案：广告片段不再删除，改用占位TS文件替换，保持时间轴完整
  - 效果：seek位置准确，不再跳转到错误位置

- **修复开头广告检测问题**
  - 修复开头DISCONTINUITY标记未正确识别的问题
  - 开头广告检测现在独立执行，不受其他检测结果影响
  - 开头广告检测阈值从60秒提高到120秒

- **修复中间广告检测问题**
  - 方法3（DISCONTINUITY之间的广告检测）现在总是执行
  - 添加详细日志便于调试

- **修复DISCONTINUITY标记丢失问题**
  - 正确设置needsDiscontinuity标记
  - 确保输出m3u8保留DISCONTINUITY标记

---

### 🐛 Bug修复

#### M3U8去广告检测优化
- **新增TS白名单功能**，开发者可防止自己的广告被过滤，对播放器实例生效，重启失效
  ```java
  // 添加TS到白名单（支持完整URL或关键字匹配）
  videoView.addTsToWhitelist("https://example.com/my_ad.ts");
  videoView.addTsToWhitelist("/my_ads/");  // 关键字匹配
  
  // 批量添加
  videoView.addTsToWhitelist(urlList);
  
  // 其他操作
  videoView.removeTsFromWhitelist(url);
  videoView.clearTsWhitelist();
  boolean inList = videoView.isTsInWhitelist(url);
  ```
- 修复嗅探完成后弹窗未正确关闭的问题

---

## [1.2.5] - 2026-03-18

### 🐛 Bug修复

#### M3U8去广告检测优化
- 新增文件名数字序列突变检测，识别数字跳跃过大的广告片段
- 新增前缀长度检测，识别文件名前缀长度异常的广告片段
- 优化路径模式提取，正确处理相对路径文件名
- 处理M3U8前先清除视频URL，避免短暂播放之前的视频
- **新增Master Playlist（嵌套m3u8）支持**，自动检测并请求子播放列表

#### M3U8播放优化
- 修复首次播放m3u8时短暂显示其他视频画面的问题
- 优化release()和状态切换的顺序，确保画面正确切换
- `setVideoList()` 不再自动设置第一个视频，避免干扰后续播放

#### 设置同步优化
- 片头尾、倍数、画面比例设置现在只对当前剧集生效
- 同一剧集内切换视频时保持设置
- 切换剧集时重置为默认值

---

## [1.2.4] - 2026-03-17

### ✨ 新功能

#### M3U8去广告功能
- 自动检测并移除m3u8视频中的广告片段
- 支持多种广告检测方式：路径模式变化、DISCONTINUITY标记、短片段组
- 本地缓存处理结果，避免重复请求
- 请求失败自动降级播放原始URL
- 提供同步/异步API接口

#### 画面比例会话记忆
- 同一剧集内切换集数时保持用户选择的画面比例
- 切换到不同剧集时自动重置为默认比例
- 关闭应用后比例设置重置（不持久化）
- 解决了用户在同一剧集切换集数时需要重复选择比例的问题

#### 视频比例API
- 新增 `VideoScaleManager.getCurrentScale()` 获取当前视频比例
- 返回值：`"默认"`, `"16:9"`, `"4:3"`, `"全屏裁剪"`, `"全屏拉伸"`
- 优先返回会话比例（用户当前选择的），其次返回持久化比例

### 🐛 Bug修复

#### 画中画模式优化
- 修复点击恢复按钮时错误执行关闭逻辑的问题
- 区分PiP恢复按钮和X关闭按钮的不同行为：
  - 恢复按钮：继续播放，保持全屏状态
  - X关闭按钮：暂停播放，退出全屏

---

## [1.2.1] - 2026-02-28

### ✨ 新功能

#### 标题栏电量与时间显示
- 全屏模式下标题栏右侧显示电池电量图标和系统时间
- 电量图标根据电量级别自动切换（100%/75%/50%/25%）
- 充电状态显示充电图标
- 电量图标在上，时间文本在下，紧凑布局

### 🐛 Bug修复

#### 画中画模式优化
- 修复用户点击X关闭PiP小窗后视频继续后台播放的问题
- PiP关闭时自动暂停视频播放
- PiP关闭时如果处于全屏状态则自动退出全屏

---

## [1.2.0] - 2026-02-27

### ✨ 新功能

#### 控制器可见性控制
- 新增 `setControllerVisibilityEnabled(boolean)` API，支持临时禁用控制器UI显示
- 禁用后控制器功能保留，但UI不显示，适用于后台控制场景
- 启用/禁用时立即触发控制器显示/隐藏

#### M3U8下载增强
- M3U8下载合并输出为MP4格式
- 用纯Java实现TS合并，移除JeffVideoLib依赖
- 删除未使用的 `JeffVideoLib-extracted` 目录

### 🐛 Bug修复

#### 下载管理修复
- 修复 RecyclerView 并发崩溃问题
- 修复线程池终止崩溃问题
- 修复下载完成图标显示问题
- 修复重复 Toast 提示问题
- 移除 DialogX 依赖，减少第三方依赖

---

## [1.1.9] - 2025-02

### ✨ 新功能

#### 视频嗅探增强
- 添加嗅探自动播放第一个视频功能
- 完善嗅探自动播放功能
- 优化视频嗅探功能，减少 ANR 问题
- 修复视频嗅探时的 ANR 问题
- 最小化 WebView 控件并修复资源拦截逻辑

#### 播放功能
- 实现选集播放和播放模式功能
- 实现智能全屏功能，根据视频宽高比自动选择全屏模式
- 添加视频嗅探开源库对比分析报告

### 🐛 Bug修复

- 修复智能全屏和弹幕组件 ANR 问题
- 修复嗅探状态时准备视频控件的显示问题
- 优化嗅探状态时的 UI 显示
- 修复 ExoPlayer Activity 切换后状态保存问题
- 修复跳过片头片尾功能和临时设置持久化问题

---

## [1.1.3] - 2024-02

### ✨ 新功能

- 在 Maven Central 发布脚本中添加 gsyVideoPlayer-ex_so 模块
- 引入 gsyVideoPlayer-ex_so 模块支持 IJK 播放加密视频
- 在播放错误界面添加设置按钮并优化设置弹窗大小适配

### 🐛 Bug修复

- 修复 GSYVideoPlayer 依赖冲突
- 修复 IJK 播放器可用性检测不完整导致闪退
- 修复 IJK 播放器后台切换进度重置问题
- 修复阿里云播放器画面比例切换问题
- 修复 m3u8 seek 画面异常和后台自动恢复播放问题

---

## [1.1.1] - 2024-01

### 🎉 首次发布

#### 核心功能
- 基于 GSYVideoPlayer 的增强视频播放器
- 支持 Android 4.0+ (API 14+)
- 多播放器内核支持：系统播放器、ExoPlayer、IJK播放器、阿里云播放器

#### 特色功能
- **弹幕支持** - 集成 DanmakuFlameMaster
- **字幕支持** - 支持 SRT/ASS/VTT 格式
- **画面比例** - 支持多种画面比例切换
- **手势控制** - 音量、亮度、进度调节
- **倍速播放** - 支持 0.35x - 10x 倍速
- **投屏功能** - DLNA 投屏支持
- **画中画** - Android 8.0+ 画中画模式
- **视频嗅探** - 网页视频自动嗅探
- **下载功能** - 视频下载管理

#### 播放器内核
| 内核 | 说明 |
|------|------|
| 系统播放器 | MediaPlayer，无需额外依赖 |
| ExoPlayer | 推荐默认，格式支持全 |
| IJK播放器 | 格式支持最全 |
| 阿里云播放器 | 性能最好，需 License |

---

## 版本号说明

采用三段式版本号：`主版本.功能版本.修复版本`

- **主版本** - 重大架构变更或不兼容更新
- **功能版本** - 新增功能或较大改进
- **修复版本** - Bug修复或小改进

---

## 升级指南

### 从 1.1.x 升级到 1.2.0

```gradle
// 更新依赖版本
implementation 'io.github.706412584:orangeplayer:1.2.0'
```

**新API使用示例：**

```java
// 控制器可见性控制
videoView.setControllerVisibilityEnabled(false);  // 禁用控制器UI
videoView.setControllerVisibilityEnabled(true);   // 启用控制器UI

// 查询状态
boolean enabled = videoView.isControllerVisibilityEnabled();
```

### 从 1.0.x 升级到 1.1.x

```gradle
// 更新依赖版本
implementation 'io.github.706412584:orangeplayer:1.1.9'
```

**注意事项：**
- 1.1.x 版本移除了部分废弃的 API
- 建议查看 API 文档了解最新用法

---

## 贡献者

感谢所有为 OrangePlayer 做出贡献的开发者！

## 反馈与支持

- **GitHub Issues**: https://github.com/706412584/orangeplayer/issues
- **QQ**: 706412584
