## v1.2.0 (2026-01-16)

### ✨ 新功能
- 支持 GIF、动态 WebP、动态 HEIF 等动图播放 (#1)
  - 集成 Coil ImageDecoderDecoder 解码器
  - 无需额外操作，图片查看器自动播放动图

### 🔧 优化
- 关于页面版本号改为动态获取（`BuildConfig.VERSION_NAME`）
- GitHub Actions 发布流程优化，使用 CHANGELOG 作为发布说明

### 📦 依赖更新
- 新增 `coil-gif` 库支持动画图片解码
