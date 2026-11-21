package com.hecker.islam;

import org.junit.Test;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import static org.junit.Assert.*;

public class WisdomCycleTest {
    @Test public void fullCycleHasNoRepeatEvenWithPersistedState() {
        String order = "";
        int cursor = 0;
        int last = -1;
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 13; i++) {
            // Simulate reconstructing an Activity from SharedPreferences each time.
            WisdomCycle.Pick pick = WisdomCycle.next(13, order, cursor, last, new Random(100 + i));
            assertTrue(seen.add(pick.index));
            assertTrue(pick.index >= 0 && pick.index < 13);
            order = pick.order;
            cursor = pick.cursor;
            last = pick.index;
        }
        assertEquals(13, seen.size());
        WisdomCycle.Pick next = WisdomCycle.next(13, order, cursor, last, new Random(200));
        assertNotEquals(last, next.index);
    }

    @Test public void malformedSavedStateRestartsSafely() {
        WisdomCycle.Pick pick = WisdomCycle.next(3, "0,0,9", 2, 0, new Random(1));
        assertTrue(pick.index >= 0 && pick.index < 3);
        assertNotEquals(0, pick.index);
        assertEquals(1, pick.cursor);
    }

    @Test public void singleQuoteCanRepeatAfterCycle() {
        WisdomCycle.Pick first = WisdomCycle.next(1, "", 0, -1, new Random(1));
        WisdomCycle.Pick again = WisdomCycle.next(1, first.order, first.cursor, first.index, new Random(2));
        assertEquals(0, first.index);
        assertEquals(0, again.index);
    }
}
