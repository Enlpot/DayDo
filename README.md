# DayDo

待办与习惯管理应用（Android 原生，全中文界面）。基于开源项目 [Grit](https://github.com/shub39/Grit)（GPL-3.0）二次开发，离线优先，数据保存在本地，可选 WebDAV 云同步。

当前版本：v1.4.9（2026-09-25）

## 功能特性

- **首页看板**：今日任务 + 今日习惯双 tab 左右滑动切换；存在过期任务时自动出现「已过期」tab
- **待办管理**：
  - 智能分类：所有 / 今天 / 明天 / 最近 7 天 / 已完成 / 已删除 / 收集箱（可在设置中隐藏）
  - 任务卡：勾选完成、点击卡片编辑详情、长按进入多选（全选 / 拖动排序 / 删除，删除二次确认）
  - 任务设置：时间、提醒、重复（每天 / 每周 / 每月 / 每年 / 自定义每 N 天·周·月·年）
  - 重复任务显示重复图标，未设置时间默认当天全天任务；重复任务详情可进入统计（完成率、连续天数、完成时段分布等）
  - 智能排序：默认按创建时间（新的在顶），可手动拖动；重复任务按典型完成时间动态排序，已完成按完成时间倒序
- **习惯打卡**：习惯卡周历点击打卡，支持提醒；统计包含当前连续、连贯性、热力图、日历、周活动趋势等
- **中文体验**：界面与日期时间均为中文（如 2026.9.25 周五），12 / 24 小时制可切换
- **默认起始页面**：可自定义启动时进入首页 / 任务 / 习惯
- **设置中心**：
  - 外观与风格：全局卡片圆角大小（带实时预览）、应用主题、Material You、Amoled、调色板风格
  - 触感反馈：总开关、震动强度（0-100%）、内置提示音（叮咚 / 叮 / 嘀）
  - 备份与同步：本地导出 / 恢复，WebDAV 服务器配置后一键上传 / 下载
  - 生物识别：指纹解锁应用
  - 智能分类显示开关、起始页选择、时间格式等

## 下载

从 [GitHub Releases](https://github.com/Enlpot/DayDo/releases) 下载最新 APK。正式安装包由 GitHub Actions 自动构建并签名（含单元测试验证），命名格式 `DayDo-版本号.apk`，可直接安装到 Android 手机。

## 构建

环境要求：JDK 17+，Android SDK。

```bash
# Linux / macOS
./gradlew :androidApp:assembleFossDebug

# Windows
.\gradlew.bat :androidApp:assembleFossDebug
```

产物路径：`androidApp/build/outputs/apk/foss/debug/androidApp-foss-debug.apk`

正式发布包由 GitHub Actions 构建（release 签名密钥存于仓库 Secrets），本地默认使用 debug 签名构建 release，仅用于自测。

## 更新日志

完整的版本更新日志见 [CHANGELOG.md](CHANGELOG.md)。

## 许可证

[GPL-3.0](LICENSE)。本应用保留 Grit 原作者版权声明，详情见 LICENSE 文件。

## 致谢

[Grit](https://github.com/shub39/Grit) — 原作者 Shubham Gorai，本项目的开发基础，特此致谢。
