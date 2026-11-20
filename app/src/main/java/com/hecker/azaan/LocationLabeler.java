package com.hecker.azaan;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import java.util.List;
import java.util.Locale;

/** Resolve human-readable neighborhoods on demand, after foreground location
 * selection. No background tracking. Uses Android's geocoder, not a hardcoded city.
 */
public final class LocationLabeler {
    private static final long RETRY_DELAY_MS = 10 * 60 * 1000L;
    private LocationLabeler() { }

    public static void resolveIfNeeded(Context context, Runnable onResolved) {
        resolve(context, onResolved, false);
    }

    public static void resolve(Context context, Runnable onResolved, boolean force) {
        Context app = context.getApplicationContext();
        AppSettings settings = new AppSettings(app);
        if (!settings.configured()) return;
        String stored = settings.city();
        if (!force && !"Current location".equalsIgnoreCase(stored)
                && !"My location".equalsIgnoreCase(stored)) return;
        SharedPreferences prefs = app.getSharedPreferences("azaan", Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (!force && now - prefs.getLong("last_reverse_geocode", 0) < RETRY_DELAY_MS) return;
        if (!Geocoder.isPresent()) return;
        prefs.edit().putLong("last_reverse_geocode", now).apply();
        double lat = settings.latitude(), lon = settings.longitude();
        new Thread(() -> {
            try {
                @SuppressWarnings("deprecation")
                List<Address> results = new Geocoder(app, Locale.ENGLISH).getFromLocation(lat, lon, 1);
                if (results == null || results.isEmpty()) return;
                String label = displayName(results.get(0));
                if (label.isEmpty()) return;
                AppSettings latest = new AppSettings(app);
                // Don't overwrite a location selected after this request began.
                if (Math.abs(lat - latest.latitude()) > .00001
                        || Math.abs(lon - latest.longitude()) > .00001
                        || (!"Current location".equalsIgnoreCase(latest.city())
                        && !"My location".equalsIgnoreCase(latest.city()))) return;
                latest.location(label, lat, lon, latest.zone());
                if (onResolved != null) new Handler(Looper.getMainLooper()).post(onResolved);
            } catch (Exception ignored) {
                // Device geocoding can be unavailable. Coordinates remain authoritative.
            }
        }, "hecker-location-label").start();
    }

    private static String displayName(Address address) {
        String city = first(address.getLocality(), address.getSubAdminArea());
        String area = first(address.getSubLocality(), address.getFeatureName());
        if (area != null && !area.isEmpty() && (!area.matches(".*[0-9]{5,}.*"))
                && (city == null || !area.equalsIgnoreCase(city))
                && area.length() <= 65) {
            return city == null ? area : area + ", " + city;
        }
        if (city != null) return city;
        String fallback = first(address.getAdminArea(), address.getCountryName());
        return fallback == null ? "" : fallback;
    }

    private static String first(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return null;
    }
}
