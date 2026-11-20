package com.hecker.islam;

import org.junit.Test;
import java.time.LocalDate;
import static org.junit.Assert.assertArrayEquals;

/** Expected tabular Hijri dates independently checked against Calendar's
 * Intl.DateTimeFormat('en-u-ca-islamic-civil-nu-latn') output. */
public class DashboardCalendarTest {
    @Test public void hijriMatchesEmbeddedCalendarAtKnownDates() {
        assertArrayEquals(new int[]{1448, 6, 9},
                DashboardCalendar.hijri(LocalDate.parse("2026-11-20")));
        assertArrayEquals(new int[]{1447, 7, 12},
                DashboardCalendar.hijri(LocalDate.parse("2026-01-01")));
        assertArrayEquals(new int[]{1447, 10, 27},
                DashboardCalendar.hijri(LocalDate.parse("2026-04-15")));
        assertArrayEquals(new int[]{1448, 7, 22},
                DashboardCalendar.hijri(LocalDate.parse("2027-01-01")));
        assertArrayEquals(new int[]{1451, 10, 8},
                DashboardCalendar.hijri(LocalDate.parse("2030-02-12")));
    }
}
