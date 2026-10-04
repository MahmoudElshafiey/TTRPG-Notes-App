package com.example.dndnotes.data.backup

import com.example.dndnotes.data.prefs.BackupFrequency
import com.example.dndnotes.data.prefs.BackupSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Covers the schedule arithmetic behind [BackupScheduler.computeInitialDelayMillis].
 *
 * The defaults are asserted explicitly because they are the schedule a user gets on a
 * fresh install: weekly, Sunday, 03:00.
 */
class BackupSchedulerTest {

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis

    private fun settings(
        frequency: BackupFrequency = BackupFrequency.WEEKLY,
        hour: Int = 3,
        minute: Int = 0,
        dayOfWeek: Int = Calendar.SUNDAY
    ) = BackupSettings(frequency = frequency, minuteOfDay = hour * 60 + minute, dayOfWeek = dayOfWeek)

    private fun assertDelayLandsOn(
        settings: BackupSettings,
        now: Long,
        expected: Long
    ) {
        val delay = BackupScheduler.computeInitialDelayMillis(settings, now)
        assertEquals("delay should be exactly the gap to the next slot", expected, delay)
    }

    @Test
    fun `defaults are weekly on sunday at 3am`() {
        val defaults = BackupSettings()
        assertTrue(defaults.enabled)
        assertEquals(BackupFrequency.WEEKLY, defaults.frequency)
        assertEquals(Calendar.SUNDAY, defaults.dayOfWeek)
        assertEquals(3, defaults.hour)
        assertEquals(0, defaults.minute)
        assertEquals(12, defaults.keepCount)
    }

    @Test
    fun `daily slot later today is used as-is`() {
        // Wednesday 10:00, backup at 15:00 the same day.
        val now = millis(2026, Calendar.OCTOBER, 7, 10, 0)
        val delay = BackupScheduler.computeInitialDelayMillis(settings(BackupFrequency.DAILY, hour = 15), now)
        assertEquals(5 * 60 * 60 * 1000L, delay)
    }

    @Test
    fun `daily slot already passed today rolls to tomorrow`() {
        // Wednesday 10:00, backup at 09:00 -> next run is tomorrow 09:00.
        val now = millis(2026, Calendar.OCTOBER, 7, 10, 0)
        val delay = BackupScheduler.computeInitialDelayMillis(settings(BackupFrequency.DAILY, hour = 9), now)
        assertEquals(23 * 60 * 60 * 1000L, delay)
    }

    @Test
    fun `weekly slot later the same day is used as-is`() {
        // Sunday 01:00, backup Sunday 03:00 -> 2 hours away, no full week added.
        val now = millis(2026, Calendar.OCTOBER, 4, 1, 0)
        val delay = BackupScheduler.computeInitialDelayMillis(
            settings(BackupFrequency.WEEKLY, hour = 3, dayOfWeek = Calendar.SUNDAY),
            now
        )
        assertEquals(2 * 60 * 60 * 1000L, delay)
    }

    @Test
    fun `weekly slot later today but already passed rolls a full week`() {
        // Sunday 10:00, backup Sunday 03:00 -> next Sunday 03:00, which is 6d17h away.
        val now = millis(2026, Calendar.OCTOBER, 4, 10, 0)
        val delay = BackupScheduler.computeInitialDelayMillis(
            settings(BackupFrequency.WEEKLY, hour = 3, dayOfWeek = Calendar.SUNDAY),
            now
        )
        assertEquals((6 * 24 + 17) * 60 * 60 * 1000L, delay)
    }

    @Test
    fun `weekly slot on a later weekday counts days ahead`() {
        // Wednesday 10:00, backup Saturday 03:00 -> 2d17h away.
        val now = millis(2026, Calendar.OCTOBER, 7, 10, 0)
        val delay = BackupScheduler.computeInitialDelayMillis(
            settings(BackupFrequency.WEEKLY, hour = 3, dayOfWeek = Calendar.SATURDAY),
            now
        )
        assertEquals((2 * 24 + 17) * 60 * 60 * 1000L, delay)
    }

    @Test
    fun `weekly slot on an earlier weekday wraps into next week`() {
        // Wednesday 10:00, backup Monday 03:00 -> the Monday 5 days later at 03:00,
        // i.e. 4d17h away.
        val now = millis(2026, Calendar.OCTOBER, 7, 10, 0)
        val delay = BackupScheduler.computeInitialDelayMillis(
            settings(BackupFrequency.WEEKLY, hour = 3, dayOfWeek = Calendar.MONDAY),
            now
        )
        assertEquals((4 * 24 + 17) * 60 * 60 * 1000L, delay)
    }

    @Test
    fun `a slot later today in a weekly schedule does not skip to next week`() {
        // The regression that matters: "days ahead" is 0 when the weekday matches, so the
        // only thing standing between the user and a 7-day wait is the same-day check.
        val now = millis(2026, Calendar.OCTOBER, 7, 1, 0) // Wednesday 01:00
        val delay = BackupScheduler.computeInitialDelayMillis(
            settings(BackupFrequency.WEEKLY, hour = 2, dayOfWeek = Calendar.WEDNESDAY),
            now
        )
        assertEquals(60 * 60 * 1000L, delay)
    }

    @Test
    fun `delay is always positive even when the slot is exactly now`() {
        val now = millis(2026, Calendar.OCTOBER, 7, 3, 0)
        listOf(BackupFrequency.DAILY, BackupFrequency.WEEKLY).forEach { frequency ->
            for (day in 1..7) {
                val delay = BackupScheduler.computeInitialDelayMillis(
                    settings(frequency, hour = 3, dayOfWeek = day),
                    now
                )
                assertTrue("$frequency day $day produced $delay", delay > 0)
            }
        }
    }

    @Test
    fun `every day of the week lands on the requested day`() {
        val now = millis(2026, Calendar.OCTOBER, 7, 12, 0) // Wednesday midday
        for (day in 1..7) {
            val delay = BackupScheduler.computeInitialDelayMillis(
                settings(BackupFrequency.WEEKLY, hour = 3, dayOfWeek = day),
                now
            )
            val lands = Calendar.getInstance().apply {
                timeInMillis = now + delay
            }
            assertEquals("day $day", day, lands.get(Calendar.DAY_OF_WEEK))
            assertEquals("hour for day $day", 3, lands.get(Calendar.HOUR_OF_DAY))
        }
    }

    @Test
    fun `minute of day is respected`() {
        val now = millis(2026, Calendar.OCTOBER, 7, 12, 0)
        val delay = BackupScheduler.computeInitialDelayMillis(
            settings(BackupFrequency.DAILY, hour = 12, minute = 45),
            now
        )
        assertEquals(45 * 60 * 1000L, delay)
    }
}