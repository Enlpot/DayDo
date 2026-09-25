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
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.enlpot.daydo.habits.data.database.HabitDatabase
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val DB_NAME = "habits_test.db"

@RunWith(AndroidJUnit4::class)
class HabitDBMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath(DB_NAME),
            AndroidSQLiteDriver(),
            HabitDatabase::class,
        )

    @Before
    fun setup() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(DB_NAME)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun migration4to5_containsCorrectData() = runBlocking {
        helper
            .createDatabase(4)
            .apply {
                (1..5).forEach { habit ->
                    val timeEpoch =
                        Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .toInstant(TimeZone.currentSystemDefault())
                            .epochSeconds

                    execSQL(
                        """
                INSERT INTO habit_index (title, description, [index], days, time)
                VALUES (
                    'Habit $habit',
                    'Description for habit $habit',
                    $habit,
                    'MONDAY,TUESDAY',              
                    $timeEpoch  
                )
            """
                            .trimIndent()
                    )

                    (1..3).forEach { offset ->
                        val dateEpoch =
                            Clock.System.now()
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                .date
                                .minus(offset, DateTimeUnit.DAY)
                                .toEpochDays()

                        execSQL(
                            """
                    INSERT INTO habit_status (habitId, date)
                    VALUES (
                        $habit,
                        $dateEpoch        
                    )
                """
                                .trimIndent()
                        )
                    }
                }
            }
            .close()

        val db = helper.runMigrationsAndValidate(5, listOf())

        // --- Habits assertions ---
        db.prepare("SELECT COUNT(*) FROM habit_index").use { stmt ->
            assertThat(stmt.step()).isTrue()
            assertThat(stmt.getLong(0)).isEqualTo(5L) // inserted 5 habits
        }

        db.prepare(
                "SELECT id, title, description, [index], days, time, reminder FROM habit_index ORDER BY id"
            )
            .use { stmt ->
                var count = 0
                while (stmt.step()) {
                    count++
                    val id = stmt.getLong(0)
                    val title = stmt.getText(1)
                    val description = stmt.getText(2)
                    val index = stmt.getLong(3).toInt()
                    val days = stmt.getText(4)
                    val time = stmt.getLong(5)
                    val reminder = stmt.getLong(6).toInt()

                    // Title & description patterns
                    assertThat(title).isEqualTo("Habit $id")
                    assertThat(description).isEqualTo("Description for habit $id")

                    // Index matches habit number
                    assertThat(index).isEqualTo(id.toInt())

                    // Days stored as string
                    assertThat(days).isEqualTo("MONDAY,TUESDAY")

                    // Time is a positive epoch seconds value
                    assertThat(time).isGreaterThan(0)

                    // Reminder default
                    assertThat(reminder).isEqualTo(1)
                }
                assertThat(count).isEqualTo(5)
            }

        // --- HabitStatus assertions ---
        db.prepare("SELECT COUNT(*) FROM habit_status").use { stmt ->
            assertThat(stmt.step()).isTrue()
            // 5 habits × 3 status rows each = 15
            assertThat(stmt.getLong(0)).isEqualTo(15L)
        }

        db.prepare("SELECT habitId, date FROM habit_status ORDER BY habitId, date").use { stmt ->
            var count = 0
            while (stmt.step()) {
                count++
                val habitId = stmt.getLong(0)
                val dateEpoch = stmt.getLong(1)

                // HabitId must reference a valid habit
                assertThat(habitId).isAtLeast(1L)
                assertThat(habitId).isAtMost(5L)

                // Date should be a valid epochDay (not in the future)
                assertThat(dateEpoch).isAtMost(
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays()
                )
            }
            assertThat(count).isEqualTo(15)
        }
        db.close()
    }

    @Test
    fun testAllMigrations() = runBlocking {
        helper.createDatabase(4).close()

        // 全链验证 4→7：autoMigration(4→5) + 手动 migrate_5_6 / migrate_6_7
        helper
            .runMigrationsAndValidate(
                7,
                listOf(HabitDatabase.migrate_5_6, HabitDatabase.migrate_6_7),
            )
            .close()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun migration6to7_convertsHabitTimeToUtc() = runBlocking {
        val localNow = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val oldTime = localNow.toInstant(TimeZone.currentSystemDefault()).epochSeconds

        helper
            .createDatabase(6)
            .apply {
                execSQL(
                    "INSERT INTO habit_index (title, description, [index], days, time, reminder) " +
                        "VALUES ('Habit TZ', '', 0, 'MONDAY', $oldTime, 1)"
                )
            }
            .close()

        val db = helper.runMigrationsAndValidate(7, listOf(HabitDatabase.migrate_6_7))

        db.prepare("SELECT time FROM habit_index").use { stmt ->
            assertThat(stmt.step()).isTrue()
            val newTime = stmt.getLong(0)
            val expected = localNow.toInstant(TimeZone.UTC).epochSeconds
            assertThat(newTime).isEqualTo(expected)
        }
        db.close()
    }
}
