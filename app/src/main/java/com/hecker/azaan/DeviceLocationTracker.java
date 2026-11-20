package com.hecker.azaan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import java.time.ZoneId;

/**
 * Battery-conscious automatic location for visible screens only. The chosen
 * neighborhood is refreshed when the device moves significantly, and again
 * on returning to the dashboard. No background location permission is used.
 */
public final class DeviceLocationTracker {
    private static final long MAX_CACHED_AGE_MS = 15 * 60 * 1000L;
    private static final float MIN_MOVE_METERS = 300f;
    private LocationManager manager;
    private LocationListener listener;
    private Context app;
    private Runnable onChange;
    private boolean tracking;

    public void start(Context context, Runnable callback) {
        stop();
        app = context.getApplicationContext();
        onChange = callback;
        AppSettings cfg = new AppSettings(app);
        if (!cfg.configured() || !cfg.autoLocation()) return;
        boolean coarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!coarse && !fine) return;
        manager = app.getSystemService(LocationManager.class);
        if (manager == null) return;
        boolean network = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean gps = fine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!network && !gps) return;
        tracking = true;
        try {
            Location newest = null;
            for (String provider : new String[]{LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER}) {
                if (provider.equals(LocationManager.NETWORK_PROVIDER) && !network) continue;
                if (provider.equals(LocationManager.GPS_PROVIDER) && !gps) continue;
                Location cached = manager.getLastKnownLocation(provider);
                if (cached != null && (newest == null || cached.getTime() > newest.getTime()))
                    newest = cached;
            }
            if (newest != null && Math.abs(System.currentTimeMillis() - newest.getTime()) <= MAX_CACHED_AGE_MS)
                accept(newest);
            listener = new LocationListener() {
                @Override public void onLocationChanged(Location location) { accept(location); }
            };
            // Network location saves GPS battery; GPS is used only when network is absent.
            if (network) manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    120_000L, MIN_MOVE_METERS, listener, Looper.getMainLooper());
            else manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    120_000L, MIN_MOVE_METERS, listener, Looper.getMainLooper());
        } catch (SecurityException | IllegalArgumentException ignored) {
            stop(); // Permissions or providers may change while the screen is opening.
        }
    }

    private void accept(Location fix) {
        if (!tracking || app == null || fix == null
                || !Double.isFinite(fix.getLatitude()) || !Double.isFinite(fix.getLongitude())
                || (fix.hasAccuracy() && fix.getAccuracy() > 1500f)) return;
        AppSettings cfg = new AppSettings(app);
        if (!cfg.autoLocation() || !cfg.configured()) { stop(); return; }
        Location previous = new Location("saved");
        previous.setLatitude(cfg.latitude());
        previous.setLongitude(cfg.longitude());
        if (previous.distanceTo(fix) < MIN_MOVE_METERS) return;
        ZoneId zone = fix.getLatitude() >= 23 && fix.getLatitude() <= 38
                && fix.getLongitude() >= 60 && fix.getLongitude() <= 78
                ? ZoneId.of("Asia/Karachi") : ZoneId.systemDefault();
        cfg.locationFromDevice("Current location", fix.getLatitude(), fix.getLongitude(), zone);
        AlarmScheduler.schedule(app);
        if (onChange != null) onChange.run();
        LocationLabeler.resolve(app, () -> {
            if (tracking && onChange != null) onChange.run();
        }, true);
        if (cfg.providerEnabled()) PrayerTimeProvider.refreshAsync(app, true, () -> {
            if (tracking && onChange != null) onChange.run();
        });
    }

    public void stop() {
        tracking = false;
        if (manager != null && listener != null) {
            try { manager.removeUpdates(listener); } catch (SecurityException ignored) { }
        }
        listener = null;
        manager = null;
    }
}
