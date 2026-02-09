# 更新日志 - 2024年2月9日

## 🐛 Bug 修复

### 修复阿里云播放器画面比例切换问题

**问题描述：**
- 使用阿里云播放器内核时，切换画面比例（全屏裁剪、全屏拉伸）无效
- 画面比例被强制拉回默认比例
- 其他比例（16:9、4:3）正常工作

**根本原因：**
1. 阿里云播放器SDK有自己独立的画面缩放模式控制系统
2. GSY框架的 `GSYVideoType.setShowType()` 对阿里云播放器不生效
3. `VideoScaleManager` 通过 `getGSYVideoManager()` 获取的是全局单例，而非实际播放器实例

**解决方案：**
1. **修复播放器实例获取方式**
   - 通过 `GSYVideoManager.instance().getPlayer()` 获取实际播放器实例
   - 而非直接使用 `GSYVideoManager.instance()` 全局单例

2. **添加阿里云播放器专用支持**
   - 通过反射调用阿里云SDK的 `IPlayer.setScaleMode()` API
   - 支持三种缩放模式：
     - `SCALE_ASPECT_FIT` - 自动缩放适配（保持比例）→ 用于默认/16:9/4:3
     - `SCALE_ASPECT_FILL` - 填充适配（保持比例，裁剪）→ 用于全屏裁剪
     - `SCALE_TO_FILL` - 拉伸适配（不保持比例）→ 用于全屏拉伸

3. **增强错误处理**
   - 添加详细的调试日志
   - 添加延迟重试机制处理播放器未初始化的情况
   - 区分不同的错误类型（ClassNotFoundException、NoSuchMethodException等）

**影响范围：**
- ✅ 阿里云播放器内核
- ✅ 所有画面比例模式
- ✅ 不影响其他播放器内核（ExoPlayer、系统播放器、IJK）

**相关文件：**
- `palyerlibrary/src/main/java/com/orange/playerlibrary/VideoScaleManager.java`
- `palyerlibrary/src/main/java/com/orange/playerlibrary/OrangevideoView.java`

**测试结果：**
- ✅ 阿里云播放器画面比例切换正常
- ✅ 全屏裁剪和全屏拉伸生效
- ✅ 其他播放器内核不受影响

---

## 📝 技术细节

### 阿里云播放器缩放模式映射

| 用户选择 | GSY类型 | 阿里云ScaleMode | 说明 |
|---------|---------|----------------|------|
| 默认 | SCREEN_TYPE_DEFAULT | SCALE_ASPECT_FIT | 保持比例，自动适配 |
| 16:9 | SCREEN_TYPE_16_9 | SCALE_ASPECT_FIT | 保持比例，16:9显示 |
| 4:3 | SCREEN_TYPE_4_3 | SCALE_ASPECT_FIT | 保持比例，4:3显示 |
| 全屏裁剪 | SCREEN_TYPE_FULL | SCALE_ASPECT_FILL | 保持比例，裁剪填充 |
| 全屏拉伸 | SCREEN_MATCH_FULL | SCALE_TO_FILL | 不保持比例，拉伸填充 |

### 关键代码片段

```java
// 正确获取播放器实例
com.shuyu.gsyvideoplayer.GSYVideoManager videoManager = mVideoView.getGSYVideoManager();
Object playerManager = videoManager.getPlayer(); // 关键：获取实际播放器

// 调用阿里云播放器API
Class<?> scaleModeClass = Class.forName("com.aliyun.player.IPlayer$ScaleMode");
Object scaleModeValue = java.lang.Enum.valueOf((Class<Enum>) scaleModeClass, "SCALE_TO_FILL");
java.lang.reflect.Method setScaleModeMethod = aliPlayer.getClass().getMethod("setScaleMode", scaleModeClass);
setScaleModeMethod.invoke(aliPlayer, scaleModeValue);
```

---

## 🔗 相关提交

- `6ef4b10` - fix: 修复阿里云播放器画面比例切换问题
- `0fd48c7` - 修复: m3u8 seek 画面异常和后台自动恢复播放问题

---

## 📚 参考文档

- [阿里云播放器SDK文档](https://help.aliyun.com/document_detail/267445.html)
- [IPlayer.ScaleMode API](https://alisdk-api-doc.oss-cn-hangzhou.aliyuncs.com/player/6.9.0/android-en/com/aliyun/player/IPlayer.ScaleMode.html)
- [项目架构规则](../.kiro/steering/project-rules.md)
