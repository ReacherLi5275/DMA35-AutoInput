# DMA35 Auto Input — Android 原型 v1.0

这是**源代码工程，不是已经编译的 APK**。当前环境没有 Android SDK/Gradle，尚未完成真实编译、设备安装或 Augmentir 验证。

## 开发环境
- Android Studio（推荐 JDK 17，Android SDK 35）
- HONOR V30 PRO / HarmonyOS 4.2（需要自行允许 APK 安装）
- DMA35 蓝牙打印模式

## 编译
1. 解压 ZIP，在 Android Studio 中选择 **Open**，打开 `DMA35AutoInput` 文件夹。
2. 等待 Gradle Sync；若提示缺少 SDK 35，请按提示安装。
3. Build > Build Bundle(s) / APK(s) > Build APK(s)。
4. 安装 `app/build/outputs/apk/debug/app-debug.apk` 到允许安装 APK 的测试手机。

## 测试
1. 开启手机蓝牙，启动 App，授予蓝牙权限，点击 Start RFCOMM server。
2. 在手机系统蓝牙设置中临时设为可被发现（如支持）。
3. DMA35 配置 Bluetooth Printer Target，重新搜索 HONOR 手机，连接并发送测量。
4. 回到 App 点击 Refresh status，确认最新密度。
5. 在系统设置启用 DMA35 Keyboard，并切换到该输入法。
6. 点击本 App 的测试密度输入框，点击键盘中的 Enable auto input，然后从 DMA35 再发送一条测量记录。
7. 确认数字被写入测试框。每次更换输入框需要重新点击 Enable auto input。

## 已知限制与安全注意
- 当前采用 RFCOMM 标准 SPP UUID，基于此前第三方 Android Terminal 成功接收的配置。
- 需确认华为蓝牙后台权限、电池优化、蓝牙可发现状态；不同系统行为可能不同。
- 当前解析以换行结束的 `Density@25...: number`，适配测试记录格式，尚未验证所有设备输出编码。
- 原型没有完整测量 ID 与当前 Augmentir 记录绑定，**不得用于正式生产数据**。
- 当前使用单次显式启用的 IME，防止自动写入错误字段；尚未验证 Augmentir 字段验证/提交行为。
- targetSdk 32 仅为早期兼容性原型；正式发布需更新 SDK、安全及后台服务实现。
- 如应用崩溃、蓝牙监听失败或 IME 无法输入，请记录 Android Studio Logcat 错误以便修复。
