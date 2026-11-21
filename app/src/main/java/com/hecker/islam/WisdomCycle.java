package com.hecker.islam;

import java.util.Random;

/**
 * A persisted shuffle bag for dashboard wisdom quotations.
 * Every quote appears once before another cycle begins, including across app restarts.
 */
final class WisdomCycle {
    private WisdomCycle() { }

    static final class Pick {
        final int index;
        final String order;
        final int cursor;

        Pick(int index, String order, int cursor) {
            this.index = index;
            this.order = order;
            this.cursor = cursor;
        }
    }

    static Pick next(int count, String persistedOrder, int cursor, int previous, Random rng) {
        if (count <= 0) throw new IllegalArgumentException("At least one quote is required");
        int[] bag = decode(persistedOrder, count);
        if (bag == null || cursor < 0 || cursor >= count) {
            bag = new int[count];
            for (int i = 0; i < count; i++) bag[i] = i;
            for (int i = count - 1; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                int t = bag[i]; bag[i] = bag[j]; bag[j] = t;
            }
            // Prevent the last quote of the old cycle from immediately returning.
            if (count > 1 && bag[0] == previous) {
                int swap = 1 + rng.nextInt(count - 1);
                int t = bag[0]; bag[0] = bag[swap]; bag[swap] = t;
            }
            cursor = 0;
        }
        return new Pick(bag[cursor], encode(bag), cursor + 1);
    }

    private static int[] decode(String stored, int count) {
        if (stored == null || stored.isEmpty()) return null;
        String[] pieces = stored.split(",", -1);
        if (pieces.length != count) return null;
        int[] result = new int[count];
        boolean[] seen = new boolean[count];
        for (int i = 0; i < count; i++) {
            try {
                int value = Integer.parseInt(pieces[i]);
                if (value < 0 || value >= count || seen[value]) return null;
                seen[value] = true;
                result[i] = value;
            } catch (NumberFormatException invalid) {
                return null;
            }
        }
        return result;
    }

    private static String encode(int[] bag) {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < bag.length; i++) {
            if (i != 0) value.append(',');
            value.append(bag[i]);
        }
        return value.toString();
    }
}
