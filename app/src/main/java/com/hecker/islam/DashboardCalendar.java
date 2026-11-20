package com.hecker.islam;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lightweight native view of the original Calendar 2.3 data.
 *
 * All occasion titles, summaries, Hijri month names and Desi month starts
 * are loaded from the SAME bundled calendar-data.js used by Calendar itself.
 * The Islamic-civil algorithm is tabular, matching the calendar's
 * Intl.DateTimeFormat('en-u-ca-islamic-civil') implementation.
 */
public final class DashboardCalendar {
    private static volatile JSONObject catalog;
    private static volatile boolean loadAttempted;
    private static final int ISLAMIC_EPOCH_JDN = 1948440;
    private DashboardCalendar() { }

    public static final class Occasion {
        public final String title;
        public final String summary;
        public final String kind;
        public Occasion(String title, String summary, String kind) {
            this.title = title;
            this.summary = summary;
            this.kind = kind;
        }
    }

    public static final class Day {
        public final String hijriDate;
        public final String desiDate;
        public final List<Occasion> today;
        public final Occasion upcoming;
        public final int daysUntilNext;
        public final boolean calendarLoaded;
        Day(String hijriDate, String desiDate, List<Occasion> today,
            Occasion upcoming, int daysUntilNext, boolean calendarLoaded) {
            this.hijriDate = hijriDate;
            this.desiDate = desiDate;
            this.today = today;
            this.upcoming = upcoming;
            this.daysUntilNext = daysUntilNext;
            this.calendarLoaded = calendarLoaded;
        }
    }

    private static JSONObject data(Context context) {
        if (loadAttempted) return catalog;
        synchronized (DashboardCalendar.class) {
            if (loadAttempted) return catalog;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    context.getAssets().open("calendar-data.js"), StandardCharsets.UTF_8))) {
                StringBuilder js = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    js.append(line);
                    if (js.length() > 512_000) throw new java.io.IOException("Calendar data too large");
                }
                String data = js.toString().trim();
                int equals = data.indexOf('=');
                if (!data.startsWith("window.CalData=") || equals < 0)
                    throw new java.io.IOException("Unexpected calendar data format");
                data = data.substring(equals + 1).trim();
                if (data.endsWith(";")) data = data.substring(0, data.length() - 1);
                catalog = new JSONObject(data);
            } catch (Exception e) {
                Log.w("HeckerIslamCalendar", "Cannot read bundled calendar occasions", e);
            } finally {
                loadAttempted = true;
            }
            return catalog;
        }
    }

    private static long hijriMonthStart(int year, int month) {
        return ISLAMIC_EPOCH_JDN + 354L * (year - 1)
                + Math.floorDiv(3 + 11L * year, 30)
                + (59L * (month - 1) + 1) / 2;
    }

    /** Returns [year, month, day] of the Islamic civil calendar. */
    public static int[] hijri(LocalDate gregorian) {
        long jdn = gregorian.toEpochDay() + 2440588L;
        int year = Math.toIntExact(Math.floorDiv(30 * (jdn - ISLAMIC_EPOCH_JDN) + 10646, 10631));
        int month = 1;
        while (month < 12 && hijriMonthStart(year, month + 1) <= jdn) month++;
        return new int[]{year, month, Math.toIntExact(jdn - hijriMonthStart(year, month) + 1)};
    }

    private static List<Occasion> events(JSONObject data, int month, int day) {
        JSONArray list = data.optJSONObject("EVENTS") == null ? null
                : data.optJSONObject("EVENTS").optJSONArray(month + "-" + day);
        if (list == null || list.length() == 0) return Collections.emptyList();
        List<Occasion> results = new ArrayList<>();
        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.optJSONObject(i);
            if (item != null) results.add(new Occasion(
                    item.optString("title"), item.optString("summary"), item.optString("kind")));
        }
        return results;
    }

    private static String desi(JSONObject data, LocalDate date, int adjustment) {
        JSONArray months = data.optJSONArray("DM");
        if (months == null || months.length() != 12) return "Desi date unavailable";
        int year = date.getYear();
        int chosen = -1;
        LocalDate start = null;
        for (int i = 0; i < months.length(); i++) {
            JSONArray entry = months.optJSONArray(i);
            if (entry == null) continue;
            LocalDate candidate = LocalDate.of(year, entry.optInt(1) + 1, entry.optInt(2))
                    .plusDays(adjustment);
            if (!candidate.isAfter(date)) { chosen = i; start = candidate; }
        }
        if (chosen < 0) {
            chosen = 11;
            JSONArray entry = months.optJSONArray(chosen);
            start = LocalDate.of(year - 1, entry.optInt(1) + 1, entry.optInt(2))
                    .plusDays(adjustment);
        }
        long number = java.time.temporal.ChronoUnit.DAYS.between(start, date) + 1;
        return number + " " + months.optJSONArray(chosen).optString(0);
    }

    public static Day today(Context context, LocalDate date) {
        JSONObject source = data(context);
        if (source == null) return new Day("Hijri date unavailable", "Desi date unavailable",
                Collections.emptyList(), null, 0, false);
        int hijriAdjustment = context.getSharedPreferences("islam_ui", Context.MODE_PRIVATE)
                .getInt("calendar_hijri_adjustment", 0);
        int[] h = hijri(date.plusDays(hijriAdjustment));
        JSONArray names = source.optJSONArray("HM");
        String month = names == null ? "Hijri" : names.optString(h[1] - 1, "Hijri");
        String label = h[2] + " " + month + " " + h[0] + " AH";
        int desiAdjustment = context.getSharedPreferences("islam_ui", Context.MODE_PRIVATE)
                .getInt("calendar_desi_adjustment", 0);
        List<Occasion> now = events(source, h[1], h[2]);
        Occasion next = null;
        int until = 0;
        for (int delta = 1; delta <= 400; delta++) {
            int[] follow = hijri(date.plusDays(delta + hijriAdjustment));
            List<Occasion> found = events(source, follow[1], follow[2]);
            if (!found.isEmpty()) { next = found.get(0); until = delta; break; }
        }
        return new Day(label, desi(source, date, desiAdjustment),
                Collections.unmodifiableList(now), next, until, true);
    }
}
