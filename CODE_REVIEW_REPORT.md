# DayDo 代码审查报告

审查日期：2026-10-15  
审查类型：静态代码审查  
范围：`androidApp/src/main`、`androidApp/src/foss`、`shared/core/src/commonMain`、`shared/ui/src/commonMain` 中的应用逻辑、数据访问、数据库、提醒、备份/恢复、设置与主要 UI 状态流；同时检查 AndroidManifest 和当前工作区未提交的四个 UI 文件改动。

## 结论

发现 6 项需要处理或确认的风险：2 项高优先级、4 项中优先级。重点集中在提醒恢复、双数据库备份恢复、生物识别锁兜底以及备份兼容性。

本报告只记录有代码路径支撑的问题。未修改任何源文件；未运行构建或测试。工作区原有未提交改动保持不变。

## 发现

### 1. [P1] Android 14 以下无法可靠清除进程重启前创建的闹钟

位置：`androidApp/src/main/java/com/enlpot/daydo/core/data/notification/NotificationAlarmScheduler.kt:190-200`

`cancelAll()` 在 API 34 以下只遍历内存中的 `scheduledIntents`。进程被杀或重启后集合为空，但 `AlarmManager` 中的 PendingIntent 闹钟仍可能存活。备份恢复调用 `cancelAll()` 后，旧闹钟可能继续触发；若旧任务 ID 被恢复数据复用，接收器会按该 ID 读取新任务，而当前接收器只检查任务状态和 reminder 是否为空，不核对触发时间，因此可能提前发出新任务通知。

建议：设计 API 29–33 可用的持久化取消方案；同时在通知接收路径校验当前任务的 reminder 与本次闹钟计划是否匹配，避免陈旧闹钟触发新数据。

### 2. [P1] 恢复过程跨两个数据库提交，失败时会留下半恢复状态

位置：`androidApp/src/main/java/com/enlpot/daydo/core/data/backup/restore/RestoreImpl.kt:140-149`

习惯库的 `replaceAll()` 完成并提交后，才开始任务库的 `replaceAll()`。两个库各自有事务，但恢复操作没有跨库原子性。若第二步失败，习惯数据已变成备份版本，任务和分类仍是恢复前版本；若调用在两步之间取消，也可能出现相同结果。当前错误类型 `PartialRestore` 已提示可能部分更新，但不能保证数据一致。

建议：为整个恢复实现可回滚策略，或先导入到临时数据库并在验证通过后统一切换；至少保留恢复前数据快照以支持失败回滚。

### 3. [P2] 补期达到 60 次上限时，重复任务不会创建未来实例

位置：`shared/core/src/commonMain/kotlin/com/enlpot/daydo/core/tasks/TaskCompletion.kt:86-105`

回填循环达到 60 次后，`guard < 60` 条件也会阻止创建未来的下一次实例。每日任务如果长期未完成，完成旧实例时最多补出 60 个逾期实例，但不会生成今天之后的任务及提醒，重复链会暂停，直到用户完成其中的实例并再次触发派生逻辑。

建议：限制逾期补建数量时，独立计算并保证创建第一个未来周期实例；对未补建的逾期周期采取明确的跳过或分页策略。

### 4. [P2] 生物识别设置读取超时或失败时会按未启用处理

位置：`shared/ui/src/commonMain/kotlin/com/enlpot/daydo/shared/ui/viewmodel/MainViewModel.kt:90-95`、`androidApp/src/main/java/com/enlpot/daydo/app/MainActivity.kt:86-104`

3 秒兜底会将 `isLoaded` 置为真，即使生物识别设置流尚未发出值；此时 `MainActivity` 用 `isBiometricLockOn != true` 判断放行，`null` 会显示应用内容。若设置流之后才读到 `true`，代码启动认证流程时也没有先将 `showContent` 设为 `false`。因此慢读取或流异常可能在未认证时暴露内容。

建议：在锁配置确认前维持遮挡态；超时不应推断为“未启用”。认证开始时立即隐藏主界面，成功后再显示。

### 5. [P2] WebDAV 接受 HTTP 地址并通过 Basic Auth 发送凭据

位置：`androidApp/src/main/java/com/enlpot/daydo/core/data/webdav/WebDavImpl.kt:227-238`、`androidApp/src/main/AndroidManifest.xml:16-19`

连接方法直接拼接用户输入的 URL，并把用户名和密码编码为 Basic Authorization header。Base64 不是加密；HTTP 下请求和备份内容均可被链路上的观察者读取。应用清单还允许明文流量。

建议：默认仅允许 HTTPS；如需支持可信局域网 HTTP，增加显著提示与显式确认，并避免将凭据发送到非预期主机或重定向目标。

### 6. [P2] 恢复逻辑接受高于当前版本的备份格式

位置：`androidApp/src/main/java/com/enlpot/daydo/core/data/backup/restore/RestoreImpl.kt:97-118`

数据库 schema 有上限校验，但 `backupFormatVersion` 只用 `>= 2` 分支处理。未来格式版本（例如 3）会被当成当前格式 2 解读。若未来版本改变时间戳或字段语义，当前版本可能静默写入错误数据。

建议：明确校验支持的格式版本范围；遇到未知的更高版本时拒绝恢复并给出“应用版本过旧”的提示。

## 需要关注但本次未列为缺陷的事项

- README 标注版本与 `androidApp/build.gradle.kts` 中的版本不一致，属于发布说明维护问题。
- `WebDavImpl` 与本地导出虽分页读取数据库，但最终仍将完整 JSON 构建为一个字符串；大备份会占用较多内存。代码对 OOM 有失败处理，因此本次将其视为容量/性能限制。
- 当前工作区有四个未提交 UI 改动：`GritBottomSheet.kt`、`GritTimePicker.kt`、`HabitUpsertSheet.kt`、`TaskUpsertSheet.kt`。本次检查了 diff，没有发现足以单独列为确定缺陷的问题；这些改动未被修改。

## 验证限制

本次是静态审查，没有执行 Gradle 构建、单元测试、Android 仪器测试或设备上的权限/通知流程验证。报告中的触发场景依据代码调用路径推导，建议修复后补充针对备份失败、旧闹钟残留、长期漏期和生物识别设置延迟的回归验证。
