# Android-NDEF-Emulator
一个轻量、易用的 Android 平台 NDEF（NFC Data Exchange Format）模拟器，旨在帮助开发者脱离实体 NFC 硬件/标签依赖，快速调试、验证 NFC 相关功能。

## 功能特性
- 📱 模拟 NDEF 消息的发送与接收，替代实体 NFC 标签/设备
- 📝 自定义 NDEF 数据格式（文本、URL、MIME 类型、智能海报等）
- 🔧 调试 Android NFC 核心 API（`NfcAdapter`、`NdefMessage`、`NdefRecord` 等）
- 💾 支持 NDEF 数据的本地保存、导入/导出
- 🧪 零硬件依赖，降低 NFC 功能测试成本

## 环境要求
- Android Studio 2022.1.1+
- Gradle 7.4+
- Android SDK 21+（Android 5.0 Lollipop）
- JDK 11+

## 快速开始
### 1. 克隆仓库
```bash
git clone https://github.com/[你的用户名]/Android-NDEF-Emulator.git
cd Android-NDEF-Emulator
```

### 2. 导入项目
- 打开 Android Studio，选择「Open」并选中仓库根目录
- 等待 Gradle 同步完成（首次同步可能需要下载依赖，建议开启科学上网）

### 3. 运行项目
- 连接 Android 真机（需开启开发者模式、USB 调试）或启动模拟器
- 点击 Android Studio 「Run」按钮，选择目标设备运行

## 使用指南
### 1. 模拟 NDEF 消息发送
1. 打开应用，进入「NDEF 编辑器」
2. 选择 NDEF 记录类型（如文本、URL）
3. 输入自定义内容，点击「生成 NDEF 消息」
4. 点击「模拟发送」，即可模拟 NFC 标签被读取的行为

### 2. 调试 NDEF 消息接收
1. 在应用设置中开启「监听 NDEF 消息」
2. 生成/导入待测试的 NDEF 消息并发送
3. 查看日志面板中的消息解析结果，验证接收逻辑

### 3. 数据导入/导出
- 导出：编辑好 NDEF 消息后，点击「导出」保存为 `.ndef` 格式文件
- 导入：点击「导入」选择本地 `.ndef` 文件，快速加载预设 NDEF 数据

## 项目结构
```
Android-NDEF-Emulator/
├── app/                     # 核心应用模块
│   ├── src/main/
│   │   ├── java/            # 业务逻辑代码（Kotlin/Java）
│   │   ├── res/             # 资源文件（布局、字符串、图标等）
│   │   └── AndroidManifest.xml # 应用清单（NFC 权限、组件声明）
│   ├── build.gradle         # 应用级构建配置
│   └── proguard-rules.pro   # 代码混淆规则
├── gradle/                  # Gradle 包装器依赖
├── build.gradle             # 项目级构建配置
├── gradlew/gradlew.bat      # 跨平台 Gradle 执行脚本
└── README.md                # 项目说明文档
```

## 核心技术栈
- 构建工具：Gradle + Android Gradle Plugin
- 开发语言：Kotlin（主流）/ Java
- 核心 API：Android `android.nfc` 包（NDEF 相关类）
- UI 框架：Jetpack Compose / Android XML 布局（根据实际实现调整）

## 权限说明
应用需要以下权限（已配置在 `AndroidManifest.xml`）：
```xml
<!-- NFC 核心权限 -->
<uses-permission android:name="android.permission.NFC" />
<!-- 声明应用仅适用于支持 NFC 的设备 -->
<uses-feature android:name="android.hardware.nfc" android:required="true" />
```

## 常见问题
### Q1: 模拟器运行时提示「未检测到 NFC 功能」？
A: 部分 Android 模拟器默认不支持 NFC，建议使用**真机**测试；若需用模拟器，需选择支持 NFC 的镜像（如 Pixel 系列）并在模拟器设置中启用 NFC。

### Q2: 生成的 NDEF 消息无法被其他设备识别？
A: 请检查 NDEF 记录格式是否符合标准（如 URL 需带 `http://` 前缀、文本编码需为 UTF-8），或在日志面板查看消息解析错误信息。


## 致谢
- 感谢 Android 官方提供的 [NFC 开发文档](https://developer.android.com/guide/topics/connectivity/nfc)
- 感谢开源社区提供的 NDEF 数据解析/构建工具类
