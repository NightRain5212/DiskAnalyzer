

# 💿 Disk Analyzer (磁盘空间分析器)

  
**Disk Analyzer** 是一款现代、高效且美观的磁盘空间可视化分析工具。它采用 JavaFX 构建，拥有基于壁纸的动态配色系统（Material You 风格），旨在帮助用户快速发现并清理占用大量空间的文件。

## ✨ 核心特性 (Features)

### 🎨 极致的 UI/UX 设计

  * **动态配色引擎 (Theme Engine)**：自动提取当前壁纸的主色调，生成协调的 Material Design 3 风格配色方案（支持 Content, Expressive, Neutral 等多种风格），并应用到全局 UI。
  * **磨砂玻璃质感 (Glassmorphism)**：界面采用现代化的半透明磨砂效果，深色模式下极具质感。
  * **丝滑动画**：从启动页的圆形扩散到弹窗的缩放淡入，每一处交互都经过精心调优。

### 🚀 高性能扫描

  * **Java NIO 驱动**：使用 `Files.newDirectoryStream` 替代传统的 IO 操作，实现极速流式扫描，大幅降低内存占用，秒级响应大文件夹。
  * **实时仪表盘**：扫描过程中实时显示已扫描文件数、总大小、耗时及瞬时速度。

### 📊 多维可视化

  * **交互式饼图**：直观展示文件夹占比，支持鼠标悬停高亮和点击钻取（Drill-down）。
  * **智能分类视图**：不仅可以按目录查看，还支持按文件类型（视频、图片、代码、压缩包等）统计空间占用。
  * **面包屑导航**：顶部提供可点击的面包屑路径，方便快速跳转父级目录。

### 🛠 实用文件管理

  * **右键上下文菜单**：支持在资源管理器中打开文件、查看详细属性（权限、时间、隐藏状态）。
  * **安全删除**：内置删除确认弹窗，支持递归删除文件夹。
  * **双向高亮联动**：悬停列表项时高亮扇形区域，反之亦然。


## 🛠️ 技术栈 (Tech Stack)

  * **编程语言**: Java 21
  * **GUI 框架**: JavaFX 21 (Modular)
  * **构建工具**: Maven
  * **打包工具**: `jpackage` (via `badass-jlink-plugin`)
  * **关键技术**:
      * `ForkJoinPool` (并行扫描)
      * `Java NIO` (高性能文件 I/O)
      * `K-Means` 变种算法 (颜色提取)

## 💻 快速开始 (Getting Started)

### 环境要求

  * JDK 21 或更高版本
  * Maven 3.8+

### 运行项目

在项目根目录下执行以下命令启动应用：

```bash
mvn clean javafx:run
```

### 构建安装包 (Build Installer)

本项目支持生成跨平台的原生安装包（Windows .msi/.exe, macOS .dmg, Linux .deb）。

**Windows 构建示例:**

```bash
# 1. 生成运行时镜像
mvn clean javafx:jlink

# 2. 生成 MSI 安装包 (需安装 WiX Toolset)
mvn jpackage:jpackage
```

构建完成后，安装包将位于 `target/jpackage/` 目录下。

## 📂 项目结构 (Project Structure)

```
src/main/java/diskanalyzer/
├── MainApp.java           # 程序入口，负责全局 UI 容器和主题管理
├── DiskScanner.java       # 基于 RecursiveTask 的高性能文件扫描器
├── ThemeEngine.java       # 核心配色引擎，负责颜色提取与调色板生成
├── ThemeStyle.java        # 预设的配色风格枚举
├── FileNode.java          # 文件树数据结构
└── NativeReportView.java  # 报告视图逻辑（图表、列表、交互）
```

## ⚙️ 自定义配置

在 **设置 (Settings)** 界面中，你可以：

1.  **更换壁纸**：软件会自动根据新壁纸重新计算全局配色。
2.  **切换风格**：选择不同的色彩算法（如“鲜艳”、“中性”、“单色”等）。
3.  **管理分类**：自定义“类型视图”中的文件后缀规则（例如添加 `.psd` 到“设计”分类）。

## 🤝 贡献 (Contributing)

欢迎提交 Issue 或 Pull Request！

1.  Fork 本仓库
2.  创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3.  提交你的修改 (`git commit -m 'Add some AmazingFeature'`)
4.  推送到分支 (`git push origin feature/AmazingFeature`)
5.  开启一个 Pull Request

## 👤 关于作者 (About)

  * **开发者**: NightRainLone
  * **版本**: v1.1.0
  * **联系邮箱**: 498187073@qq.com

## 📄 许可证 (License)

本项目采用 [MIT License](https://www.google.com/search?q=LICENSE) 许可证。

-----

