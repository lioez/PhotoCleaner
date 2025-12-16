# 清图 (PhotoCleaner)

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="App Icon" width="120" />
</p>

<p align="center">
  <b>让相册回归清爽 | Make Your Gallery Clean Again</b>
</p>

<p align="center">
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Language-Kotlin-purple.svg" alt="Kotlin"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg" alt="Jetpack Compose"></a>
  <a href="https://developer.android.com/topic/libraries/architecture/viewmodel"><img src="https://img.shields.io/badge/Arch-MVVM-green.svg" alt="MVVM"></a>
  <img src="https://img.shields.io/badge/AI-Powered-orange.svg" alt="AI Powered">
</p>

---

## 📖 简介 (Introduction)

**清图** 是一款专为整理手机相册打造的极简 Android 工具应用。它摒弃了繁琐的操作，采用类似 Tinder 的“左滑删除、右滑保留”交互模式，让整理照片变得像刷短视频一样轻松、解压。

应用完全在本地运行，无需联网，不上传任何数据，最大程度保障您的隐私安全。

## ✨ 核心功能 (Features)

*   **👆 极速整理**：左滑暂存删除，右滑保留照片，手势操作丝滑流畅。
*   **🗑️ 安全回收**：
    *   **暂存区**：误删了？没关系，暂存区给您“后悔药”。
    *   **系统联动**：确认删除后，照片移入系统回收站（Android 11+），双重保险。
*   **↩️ 撤销机制**：手滑了？点击撤销按钮立即回退上一步操作。
*   **🔍 沉浸查看**：
    *   点击查看大图，支持双指缩放、双击放大。
    *   集成 Telephoto 库，提供原生级的丝滑缩放体验。
*   **⚡ 性能优化**：
    *   智能预加载缓冲池 (Smart Preload Buffer)，告别滑动白屏。
    *   后台静默预渲染，大图秒开。
*   **🎨 现代设计**：
    *   Material 3 设计语言。
    *   完美适配深色模式 (Dark Mode)。
    *   Edge-to-Edge 全屏体验。

## 🤖 AI 协作声明 (AI Collaboration)

本项目是一个探索性的实验项目，**约 80% 的代码与架构设计由人工智能 (AI) 辅助完成**。

*   **AI 角色**：担任了主要的代码生成、逻辑构建、UI 布局设计以及性能优化方案的提供者。
*   **开发者(本人)**：负责需求定义、Prompt 工程、代码审查 (Code Review)、Bug 调试以及最终的整合与发布。


## 🛠️ 技术栈 (Tech Stack)

*   **语言**: [Kotlin](https://kotlinlang.org/)
*   **UI 框架**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
*   **架构**: MVVM (Model-View-ViewModel)
*   **图片加载**: [Coil](https://coil-kt.github.io/coil/)
*   **图片缩放**: [Telephoto](https://github.com/saket/telephoto)
*   **异步处理**: Kotlin Coroutines & Flow

## 📸 截图 (Screenshots)

*(在此处添加应用截图，例如：主界面、回收站、关于页面)*

| 主界面 | 回收站 | 关于页面 |
|:---:|:---:|:---:|
| <img src="" alt="Main" width="200"/> | <img src="" alt="Trash" width="200"/> | <img src="" alt="About" width="200"/> |

## 📥 下载与安装 (Download)

目前您可以直接 Clone 本仓库并使用 Android Studio 编译安装。

```bash
git clone https://github.com/lioez/PhotoCleaner.git
```

*(后续发布 Release 包后在此处更新下载链接)*

## 📄 许可证 (License)

本项目采用 MIT 许可证。详情请参阅 [LICENSE](LICENSE) 文件。

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/lioez">loez</a>
</p>
