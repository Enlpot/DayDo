# DayDo

待办与习惯管理应用（Android 原生，全中文界面）。基于开源项目 [Grit](https://github.com/shub39/Grit)（GPL-3.0）二次开发，离线优先，数据保存在本地。

## 功能特性

- **首页看板**：今日任务 + 今日习惯，页内双 tab 左右滑动切换
- **待办管理**：
  - 智能分类：所有 / 今天 / 明天 / 最近 7 天 / 已完成 / 已删除 / 收集箱（分类可在设置中隐藏）
  - 任务卡：勾选完成、点击卡片编辑详情、长按进入多选（全选 / 拖动排序 / 删除，删除二次确认）
  - 任务设置：时间、提醒、重复（每天 / 每周 / 每月 / 每年 / 自定义每 N 天·周·月·年）
  - 重复任务显示重复图标，未设置时间默认当天全天任务
- **习惯打卡**：习惯卡打卡记录
- **中文体验**：界面与日期时间均为中文（如 2026.9.24 周四 17:34），12 / 24 小时制可切换
- **默认起始页面**：可自定义启动时进入首页 / 任务 / 习惯
- **设置中心**：
  - 外观与风格：全局卡片圆角大小（带实时预览）、卡片高度（紧凑 / 常规 / 宽松）、应用主题、Material You、Amoled 调色板、调色板风格
  - 触感反馈：总开关、震动强度（0-100%）、内置提示音（系统点击 / 按键音 / 触碰音 / 导航音）
  - 数据备份：导出 / 恢复任务和习惯数据

## 下载

从 [GitHub Releases](https://github.com/Enlpot/DayDo/releases) 下载最新 APK（debug 包，可直接安装到 Android 手机）。

## 构建

环境要求：JDK 17+，Android SDK。

```bash
# Linux / macOS
./gradlew :androidApp:assembleFossDebug

# Windows
.\gradlew.bat :androidApp:assembleFossDebug
```

产物路径：`androidApp/build/outputs/apk/foss/debug/androidApp-foss-debug.apk`

## 更新日志

完整的版本更新日志见 [CHANGELOG.md](CHANGELOG.md)。
## 许可证

[GPL-3.0](LICENSE)。本应用保留 Grit 原作者版权声明，详情见 LICENSE 文件。

## 致谢

[Grit](https://github.com/shub39/Grit) — 原作者 Shubham Gorai，本项目的开发基础，特此致谢。
