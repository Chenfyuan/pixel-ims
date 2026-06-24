# Pixel IMS - 项目说明

## 项目概述

Pixel IMS 是一个面向 Google Pixel (Tensor 平台) 的 Android 原生应用，用于调优运营商 IMS 相关配置。通过 Shizuku 获取 shell 级权限，直接操作 Android Telephony 系统服务的 CarrierConfig，实现 VoLTE/VoWiFi/VoNR 等 IMS 能力的开关控制，以及网络兼容性修复。

## 技术栈

- 语言：Kotlin
- UI 框架：Jetpack Compose (Material 3 / Material Expressive Theme)
- 构建：Gradle (Kotlin DSL)
- 权限方案：Shizuku（无需 root）
- 最低版本：Android 13 (API 33)
- 编译/目标版本：API 36 (Android 17)
- 主题：Dynamic Color (Material You)，跟随壁纸取色

## 仓库信息

- GitHub：https://github.com/Chenfyuan/pixel-ims
- 包名：`io.github.vvb2060.ims.mod`
- 命名空间：`io.github.vvb2060.ims`

## 项目结构

```
app/src/main/java/io/github/vvb2060/ims/
├── Application.kt                 # Application 入口
├── ShizukuProvider.kt             # Shizuku 桥接层，所有特权操作的统一入口
├── LogcatRepository.kt            # 日志仓库
├── UpdateInstallReceiver.kt       # 更新安装广播接收器
├── UpdateApkCleanup.kt            # APK 清理
├── model/
│   ├── Feature.kt                 # 功能枚举（VoLTE/VoWiFi/VoNR/5G 等开关定义）
│   ├── FeatureConfigMapper.kt     # Feature 与 CarrierConfig Bundle 的映射
│   ├── SupportModels.kt           # 数据模型（NetworkExitStatus/ConfigBackupSnapshot/ApnDraftConfig/SupportRules）
│   ├── Sim.kt                     # SIM 卡数据模型
│   ├── Shizuku.kt                 # Shizuku 状态枚举
│   ├── System.kt                  # 系统信息模型
│   └── Logcat.kt                  # 日志模型
├── viewmodel/
│   ├── MainViewModel.kt           # 主界面 ViewModel，管理所有业务逻辑
│   ├── LogcatViewModel.kt         # 日志页 ViewModel
│   └── DumpViewModel.kt           # 配置 dump 页 ViewModel
├── ui/
│   ├── MainActivity.kt            # 主界面（Compose 单 Activity）
│   ├── LogcatActivity.kt          # 日志查看页
│   ├── DumpActivity.kt            # 全量配置查看页
│   ├── BaseActivity.kt            # Activity 基类
│   ├── components/                # 可复用 UI 组件
│   └── theme/
│       ├── Theme.kt               # Dynamic Color 主题配置
│       └── Color.kt               # 静态颜色定义（已不再使用，保留备用）
├── privileged/                    # Shizuku 特权操作层
│   ├── ImsModifier.kt             # IMS 配置写入（核心）
│   ├── ImsStatusReader.kt         # IMS 注册状态读取
│   ├── ImsResetter.kt             # IMS 配置重置
│   ├── ConfigReader.kt            # CarrierConfig 读取
│   ├── SimReader.kt               # SIM 卡信息读取
│   ├── CaptivePortalFixer.kt      # 网络验证修复
│   ├── ApnModifier.kt             # APN 配置写入
│   ├── ShizukuCompat.kt           # Shizuku 兼容层
│   └── BrokerInstrumentation.kt   # Broker 桥接
└── tiles/
    └── QsTiles.kt                 # 快捷设置磁贴
```

## 功能模块

| 模块 | 关键文件 | 作用 |
|---|---|---|
| IMS 配置写入 | `ImsModifier.kt`, `ShizukuProvider.kt` | 通过 Shizuku 修改 CarrierConfig，开关 VoLTE/VoWiFi/VoNR/ViLTE/UT/CrossSIM |
| 5G 能力 | `Feature.kt` 中 FIVE_G_* | 控制 5G NR 开关、信号阈值、5G+ 图标显示 |
| TikTok 修复 | `TIKTOK_NETWORK_FIX` | 通过写入随机数字 ISO 覆盖 SIM 国家码，绕过 TikTok 对大陆 SIM 的限制 |
| 网络验证修复 | `CaptivePortalFixer.kt` | 修复"已连接但无法上网"问题（captive portal 检测地址换成国内可达的） |
| 网络出口诊断 | `MainViewModel.checkNetworkExit()` | 检测当前 IP 归属地、Google/TikTok 可达性 |
| 诊断工具 | `MainViewModel.runShizukuDiagnostics()` | 全量检查 Shizuku/SIM/IMS/CarrierConfig 状态 |
| 配置备份/恢复 | `ConfigBackupSnapshot` | 保存和恢复配置快照，支持开机自动恢复 |
| APN 配置 | `ApnModifier.kt` | 辅助写入 APN |
| 应用更新 | `UpdateInstallReceiver.kt` | 应用内检查 GitHub Release 更新 |

## UI 架构

- 单 Activity + Compose：`MainActivity.kt` 包含所有 UI
- 两个 Tab：工具（Tools）和 关于（About）
- 工具页内容：Shizuku Banner → SIM 下拉选择器 → 功能开关卡片（分组：IMS 能力/网络显示/兼容修复）→ Extra 工具卡片组
- 关于页内容：品牌头（居中 Logo + 名称 + 版本）→ 设备信息卡片 → 操作列表卡片

## 设计规范

- 完全遵循 Material 3 设计规范
- Dynamic Color (Material You)
- Typography 全部使用 M3 语义 token（titleMedium/titleSmall/bodyMedium/bodySmall/labelMedium/labelSmall）
- 颜色全部使用 MaterialTheme.colorScheme 语义色，零硬编码色值
- 卡片背景：surfaceContainerLow
- TopAppBar 背景：surfaceContainerHighest
- 间距统一 16dp 网格

## 构建

```bash
# Debug 构建
./gradlew :app:assembleDebug

# Release 构建（需要在 local.properties 配置签名）
./gradlew :app:assembleRelease

# 运行测试
./gradlew :app:testDebugUnitTest

# 安装到设备
adb install -r app/build/outputs/apk/release/app-release.apk
```

## 签名配置

在 `local.properties` 中配置：
```properties
SIGN_KEY_STORE_FILE=/path/to/your.keystore
SIGN_KEY_STORE_PASSWORD=***
SIGN_KEY_ALIAS=***
SIGN_KEY_PASSWORD=***
```

未配置时 debug 构建会自动使用 Android 默认 debug keystore。

## 历史背景

本项目 fork 自 [ryfineZ/carrier-ims-for-pixel](https://github.com/ryfineZ/carrier-ims-for-pixel)，该项目又基于 [Mystery00/TurboIMS](https://github.com/Mystery00/TurboIMS)。

主要改动：
- 移除了全部商业化模块（DoDoPay 打赏、广告系统、商务意向提交）
- UI 全面重构为 Material 3 规范
- 合并为双 Tab 结构（工具 + 关于）
- 重命名为 Pixel IMS
