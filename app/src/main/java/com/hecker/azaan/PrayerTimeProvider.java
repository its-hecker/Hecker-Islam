package com.hecker.azaan;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AlAdhan calendar API with an offline astronomical fallback.
 *
 * The Jafari/Shia Ithna-Ashari method is 0 (Leva, Qom).
 * Method 1 is the University of Islamic Sciences Karachi, NOT the Shia method.
 * Tehran's separately supported method is 7.
 *
 * Refresh happens on a worker. The UI, alarm receiver and scheduler NEVER make
 * network calls on the main thread. Cache is keyed to location, time zone and
 * calculation method so changing the city cannot reuse another city's times.
 */
public final class PrayerTimeProvider {
    private static final String TAG = "HeckerIslamTimes";
    private static final String STORE = "azaan_prayer_provider";
    private static final long REFRESH_MS = 12 * 60 * 60 * 1000L;
    private static final long RETRY_MS = 20 * 60 * 1000L;
    private static final ExecutorService NETWORK = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean FETCHING = new AtomicBoolean(false);
    private static final java.util.List<Runnable> CALLBACKS = new java.util.ArrayList<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private PrayerTimeProvider() { }

    /** Auto-select API in Pakistan; elsewhere keep the existing offline behaviour. */
    public static boolean usesRemote(AppSettings s) {
        return s.configured() && s.providerEnabled();
    }

    public static boolean isPakistan(AppSettings s) {
        double lat = s.latitude(), lon = s.longitude();
        return s.configured() && lat >= 23 && lat <= 38 && lon >= 60 && lon <= 78
                && "Asia/Karachi".equals(s.zone().getId());
    }

    private static String signature(AppSettings s) {
        return String.format(Locale.US, "%.5f:%.5f:%s:%s",
                s.latitude(), s.longitude(), s.zone().getId(), s.method().name());
    }

    private static String monthKey(YearMonth month) { return "m_" + month; }

    private static SharedPreferences store(Context c) {
        return c.getSharedPreferences(STORE, Context.MODE_PRIVATE);
    }

    private static void ensureSignature(Context c, AppSettings s) {
        SharedPreferences p = store(c);
        String signature = signature(s);
        if (!signature.equals(p.getString("signature", ""))) {
            p.edit().clear().putString("signature", signature).commit();
        }
    }

    /** Returns provider data when available; otherwise clearly labelled offline calculations. */
    public static Map<Prayer, Instant> calculate(Context c, LocalDate date, AppSettings s) {
        Map<Prayer, Instant> cached = remoteTimes(c, date, s);
        if (cached != null) {
            int[] offsets = s.offsets();
            Map<Prayer, Instant> shifted = new EnumMap<>(Prayer.class);
            for (Prayer prayer : Prayer.values()) {
                Instant event = cached.get(prayer);
                if (event != null) shifted.put(prayer, event.plusSeconds(60L * offsets[prayer.ordinal()]));
            }
            return shifted;
        }
        return PrayerCalculator.calculate(date, s.latitude(), s.longitude(), s.method(), s.offsets());
    }

    public static boolean hasProviderTimes(Context c, LocalDate date, AppSettings s) {
        return remoteTimes(c, date, s) != null;
    }

    public static String status(Context c, LocalDate date, AppSettings s) {
        if (!s.configured()) return "Choose your location first";
        if (!usesRemote(s)) return "On-device " + s.method().title + " calculation";
        return hasProviderTimes(c, date, s)
                ? "AlAdhan • " + s.method().title + " • synced"
                : "Offline estimate • " + s.method().title + " • provider not synced";
    }

    private static Map<Prayer, Instant> remoteTimes(Context c, LocalDate date, AppSettings s) {
        if (!usesRemote(s)) return null;
        SharedPreferences p = store(c);
        if (!signature(s).equals(p.getString("signature", ""))) return null;
        String json = p.getString(monthKey(YearMonth.from(date)), "");
        if (json.isEmpty()) return null;
        try {
            JSONObject entries = new JSONObject(json);
            JSONArray row = entries.optJSONArray(date.toString());
            if (row == null || row.length() != 3) return null;
            Map<Prayer, Instant> result = new EnumMap<>(Prayer.class);
            for (Prayer prayer : Prayer.values()) {
                long epochMs = row.getLong(prayer.ordinal());
                Instant instant = Instant.ofEpochMilli(epochMs);
                // Defensive guard against a malformed or stale provider payload.
                if (!date.equals(instant.atZone(s.zone()).toLocalDate())) return null;
                result.put(prayer, instant);
            }
            if (!result.get(Prayer.FAJR).isBefore(result.get(Prayer.DHUHR))
                    || !result.get(Prayer.DHUHR).isBefore(result.get(Prayer.MAGHRIB))) return null;
            return result;
        } catch (Exception ignored) { return null; }
    }

    /** Request this month and next month so alarm planning works around month end. */
    public static void refreshAsync(Context context, boolean force, Runnable onFinish) {
        Context app = context.getApplicationContext();
        AppSettings settings = new AppSettings(app);
        if (!usesRemote(settings)) return;
        if (FETCHING.get()) {
            if (onFinish != null) synchronized (CALLBACKS) { CALLBACKS.add(onFinish); }
            return;
        }
        long now = System.currentTimeMillis();
        SharedPreferences prefs = store(app);
        String key = signature(settings);
        boolean mismatched = !key.equals(prefs.getString("signature", ""));
        if (!force && !mismatched
                && now - prefs.getLong("last_success", 0) < REFRESH_MS
                && hasProviderTimes(app, LocalDate.now(settings.zone()), settings)
                && hasProviderTimes(app, LocalDate.now(settings.zone()).plusMonths(1).withDayOfMonth(1), settings))
            return;
        if (!force && !mismatched && now - prefs.getLong("last_attempt", 0) < RETRY_MS) return;
        if (!FETCHING.compareAndSet(false, true)) {
            if (onFinish != null) synchronized (CALLBACKS) { CALLBACKS.add(onFinish); }
            return;
        }
        if (onFinish != null) synchronized (CALLBACKS) { CALLBACKS.add(onFinish); }
        NETWORK.execute(() -> {
            boolean changed = false;
            try {
                AppSettings current = new AppSettings(app);
                if (!key.equals(signature(current)) || !current.providerEnabled()) return;
                ensureSignature(app, current);
                store(app).edit().putLong("last_attempt", System.currentTimeMillis()).commit();
                YearMonth month = YearMonth.from(LocalDate.now(current.zone()));
                changed |= downloadMonth(app, month, current, key);
                changed |= downloadMonth(app, month.plusMonths(1), current, key);
                if (changed) store(app).edit().putLong("last_success", System.currentTimeMillis()).apply();
            } catch (Exception e) {
                Log.w(TAG, "AlAdhan calendar sync failed; offline times remain available", e);
            } finally {
                FETCHING.set(false);
                if (changed) AlarmScheduler.schedule(app);
                java.util.List<Runnable> callbacks;
                synchronized (CALLBACKS) {
                    callbacks = new java.util.ArrayList<>(CALLBACKS);
                    CALLBACKS.clear();
                }
                for (Runnable callback : callbacks) MAIN.post(callback);
            }
        });
    }

    private static boolean downloadMonth(Context app, YearMonth month,
                                         AppSettings original, String signature) throws Exception {
        int method = original.method() == PrayerCalculator.Method.TEHRAN ? 7 : 0;
        String endpoint = String.format(Locale.US,
                "https://api.aladhan.com/v1/calendar/%04d/%d?latitude=%.6f&longitude=%.6f&method=%d",
                month.getYear(), month.getMonthValue(), original.latitude(), original.longitude(), method);
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "HeckerIslam/0.3 (prayer time calendar)");
        JSONObject calendar;
        try {
            if (conn.getResponseCode() != 200) throw new java.io.IOException("HTTP " + conn.getResponseCode());
            try (InputStream input = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                    if (response.length() > 750_000) throw new java.io.IOException("Response too large");
                }
                calendar = new JSONObject(response.toString());
            }
        } finally { conn.disconnect(); }
        if (calendar.optInt("code") != 200) throw new java.io.IOException("Invalid provider response");
        JSONArray data = calendar.getJSONArray("data");
        if (data.length() != month.lengthOfMonth()) throw new java.io.IOException("Incomplete provider month");
        JSONObject entries = new JSONObject();
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.getJSONObject(i);
            JSONObject meta = item.getJSONObject("meta");
            ZoneId providerZone = ZoneId.of(meta.getString("timezone"));
            // A wrong time zone must never silently reschedule an azaan.
            if (!providerZone.equals(original.zone()))
                throw new java.io.IOException("Provider time zone differs: " + providerZone);
            String providerDate = item.getJSONObject("date").getJSONObject("gregorian").getString("date");
            LocalDate expected = month.atDay(i + 1);
            if (!expected.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-uuuu")).equals(providerDate))
                throw new java.io.IOException("Provider day mismatch");
            JSONObject timings = item.getJSONObject("timings");
            JSONArray times = new JSONArray();
            Instant prior = null;
            for (String field : new String[]{"Fajr", "Dhuhr", "Maghrib"}) {
                String raw = timings.getString(field);
                java.util.regex.Matcher match = java.util.regex.Pattern.compile("^(\\d{1,2}):(\\d{2})").matcher(raw);
                if (!match.find()) throw new java.io.IOException("Unrecognized prayer time");
                int hour = Integer.parseInt(match.group(1)), minute = Integer.parseInt(match.group(2));
                if (hour < 0 || hour > 23 || minute < 0 || minute > 59)
                    throw new java.io.IOException("Invalid prayer hour");
                Instant value = expected.atTime(LocalTime.of(hour, minute)).atZone(providerZone).toInstant();
                if (prior != null && !value.isAfter(prior)) throw new java.io.IOException("Unordered prayer times");
                prior = value;
                times.put(value.toEpochMilli());
            }
            entries.put(expected.toString(), times);
        }
        AppSettings latest = new AppSettings(app);
        if (!signature.equals(signature(latest)) || !latest.providerEnabled()) return false;
        store(app).edit().putString(monthKey(month), entries.toString()).commit();
        return true;
    }
}
