/*
 * Copyright (C) 2026  Enlpot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.enlpot.daydo.core.tasks.Category
import com.enlpot.daydo.tasks.data.database.TaskDatabase
import com.enlpot.daydo.tasks.data.database.TaskEntity
import com.enlpot.daydo.tasks.data.toCategoryEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val DB_NAME = "restore_tx_test.db"

/**
 * 锁定恢复路径"清空 + 写入"的原子性。
 *
 * 背景：`@Transaction` 标在 `@Database` 子类的 open 方法上会被 Room **静默忽略** （KSP 生成的 `*_Impl`
 * 不会覆写该方法），`replaceAll` 曾因此退化为 N 条各自提交的语句—— 中途失败就会出现"表已清空、新数据未写入"的全量数据丢失。现改用数据库级
 * `withWriteTransaction`， 本测试先写入一份用户数据，再用一份必然失败的备份触发中途异常，断言原数据仍然存在。
 */
@RunWith(AndroidJUnit4::class)
class RestoreTransactionTest {
    private lateinit var db: TaskDatabase

    @Before
    fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        ctx.deleteDatabase(DB_NAME)
        db =
            Room.databaseBuilder<TaskDatabase>(ctx, ctx.getDatabasePath(DB_NAME).absolutePath)
                .addMigrations(TaskDatabase.MIGRATION_9_10, TaskDatabase.MIGRATION_10_11)
                .build()
    }

    @After
    fun tearDown() {
        db.close()
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(DB_NAME)
    }

    @Test
    fun replaceAll_midwayFailure_rollsBackAndKeepsOriginalData() = runBlocking {
        // 1) 写入"用户原有数据"
        db.replaceAll(
            tasks = listOf(TaskEntity(id = 1, categoryId = null, title = "原有任务")),
            categories =
                listOf(Category(id = 1, name = "原有分类", color = "#FF0000").toCategoryEntity()),
        )
        assertThat(db.taskDao().getTaskById(1)?.title).isEqualTo("原有任务")

        // 2) 再用一份"中途失败"的备份恢复：先清空两表，随后插入的任务引用不存在的分类 → 外键失败
        val result = runCatching {
            db.replaceAll(
                tasks = listOf(TaskEntity(id = 2, categoryId = 9999, title = "新任务")),
                categories = emptyList(),
            )
        }
        assertThat(result.isFailure).isTrue()

        // 3) 断言原数据仍在：若无事务，此处表已被清空 → 全量数据丢失
        assertThat(db.taskDao().getTaskById(1)?.title).isEqualTo("原有任务")
    }
}
