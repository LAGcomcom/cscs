# SMS Sync - Android短信同步工具

这是一个 Android 应用程序，用于监听本地接收到的短信内容，保存到本地数据库，并自动上传至远程服务器。同时支持短信导出、表格展示、权限检测等功能，适合在数据备份、监控系统、开发测试等场景中使用。
本项目为应对“运营商对个人开发者发送短信的限制”而开发，原则上可以适配大部分安卓手机
项目需配有后端以正常运行，后端用以接收已经收到的短信。后端开发可参考API接口项。

## ✨ 功能特性

- 📥 自动监听收到的短信内容（包括号码、正文、时间）
- 💾 本地数据库持久保存（使用 Room 持久化）
- ☁️ 自动将短信上传至远程 API（通过 JSON POST 上传）
- 📤 一键导出所有短信为 CSV 文件
- 🧾 通过 RecyclerView 以表格形式展示历史短信
- 🔒 权限动态请求，适配 Android 11+

## 📦 安装与使用

1. 使用 Android Studio 打开本项目（建议 Arctic Fox 以上版本）
2. 将项目构建为 APK 或直接安装到设备
3. 首次运行时允许以下权限：
   - 读取短信（READ_SMS）
   - 接收短信（RECEIVE_SMS）
   - 读取手机状态（READ_PHONE_STATE）
4. 短信到来时自动入库并上传
5. 点击 “导出” 按钮可生成 CSV 文件（保存在 `Documents/sms_export/` 目录）

## 🛠️ 技术栈

- **语言**：Kotlin
- **UI框架**：AndroidX + RecyclerView
- **本地数据库**：Room（SQLite 封装）
- **网络请求**：OkHttp + JSON
- **异步编程**：Kotlin Coroutine + LifecycleScope

## 🗃️ 数据结构

### 数据表：`LocalSms`

| 字段名     | 类型       | 描述             |
|------------|------------|------------------|
| `id`       | Int        | 自增主键         |
| `address`  | String     | 短信发送者号码   |
| `body`     | String     | 短信内容         |
| `timestamp`| Long       | 接收时间（毫秒） |

## 📤 API 上传接口

- **接口地址**：`https://focapi.feiyang.ac.cn/v1/user/smscatcher`
- **请求方式**：POST
- **Content-Type**：`application/json`
- **请求字段**：
```json
{
  "address": "106xxxxxxx",
  "body": "123456",
  "timestamp": 1710000000000
}
