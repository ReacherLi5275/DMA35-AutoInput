# DMA35 Auto Input v2.0 测试版

基于 v1.0 添加：模拟 DMA35 测量、自动解析 25°C 密度、在 App 私有存储中追加 CSV 记录、使用 Android 系统文件选择器导出 CSV（Excel 可打开）。保留 RFCOMM Server 和专用 IME。

## 测试步骤
1. 安装更新版 APK（包名与 v1 一致，需使用相同签名；GitHub Actions 每次构建的 debug 签名可能不同，若无法覆盖安装，请先备份 v1 数据再卸载）。
2. 点击 SIMULATE MEASUREMENT，顶部应显示 1.2452；点击 EXPORT CSV FOR EXCEL 并保存，Excel 打开应包含 source=SIMULATION 的记录。
3. 测试输入：启用 DMA35 Keyboard，选择该输入法，点击测试输入框，再按键盘内的 Enable auto input (TEST ONLY)，然后模拟一次测量。若两秒内重复同值会被去重，请等待至少两秒。
4. 在 Augmentir 测试时，先选择目标字段，再启用专用输入法并 ARM；仅用于测试表单，不要提交生产记录。
5. DMA35 可用后，启动 RFCOMM Server，连接设备并核对记录。

## 限制
- 本版本未接入 Augmentir API，不会直接写入后台数据库；通过 Android IME 将数值输入当前焦点字段，兼容性待验证。
- CSV 是导出文件，并非实时更新一个已打开的 Excel 工作簿。
- 接收服务的蓝牙记录组装仍需实机验证，MeasNo/Sample ID 在蓝牙接收模式下可能缺失（取决于记录分段），因此不得将其当作已验证的生产追溯系统。
- 不自动点击 Augmentir 的 Complete 或提交表单。
- 项目为源代码，尚未在本环境编译或实测。
