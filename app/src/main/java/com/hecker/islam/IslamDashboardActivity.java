package com.hecker.islam;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.hecker.azaan.AppSettings;
import com.hecker.azaan.Prayer;
import com.hecker.azaan.PrayerCalculator;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Unified entry point, inspired by the reference app's expressive cards and floating tab bar.
 * The original Calendar and Azaan activities are preserved verbatim.
 */
public final class IslamDashboardActivity extends Activity {
    private static final int IMPORT_ARCHIVE = 4821;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout content, navigation;
    private int page = 0;
    private boolean importing = false;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); render();
    }
    @Override protected void onResume() { super.onResume(); render(); }
    @Override protected void onDestroy() { worker.shutdownNow(); super.onDestroy(); }
    private void render() {
        Ui.applyTheme(this);
        getWindow().setStatusBarColor(Ui.BACKGROUND);
        getWindow().setNavigationBarColor(Ui.BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(
                Ui.BACKGROUND == android.graphics.Color.parseColor("#F5F8F2")
                ? android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                : 0);
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BACKGROUND);
        Ui.rootInsets(root);
        ScrollView scrolling = new ScrollView(this);
        scrolling.setFillViewport(true);
        scrolling.setVerticalScrollBarEnabled(false);
        root.addView(scrolling, new LinearLayout.LayoutParams(-1, 0, 1));
        content = Ui.column(this);
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 19), Ui.dp(this, 20), Ui.dp(this, 24));
        scrolling.addView(content);
        navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 11));
        navigation.setBackgroundColor(Ui.BACKGROUND);
        root.addView(navigation, new LinearLayout.LayoutParams(-1, Ui.dp(this, 80)));
        setContentView(root);
        header();
        if (page == 0) today();
        else if (page == 4) more();
        else page = 0;
        tabs();
    }
    private void header() {
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = Ui.text(this, "HECKER ISLAM", 14, Ui.GOLD, true);
        line.addView(heading, new LinearLayout.LayoutParams(0, Ui.dp(this, 40), 1));
        TextView settings = Ui.text(this, "⚙", 26, Ui.WHITE, false);
        settings.setGravity(Gravity.CENTER); settings.setContentDescription("Open settings");
        settings.setOnClickListener(v -> { page = 4; render(); });
        line.addView(settings, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        content.addView(line);
        Ui.space(this, content, 9);
    }
    private void title(String s, String subtitle) {
        content.addView(Ui.text(this, s, 30, Ui.WHITE, true));
        Ui.space(this, content, 5);
        content.addView(Ui.text(this, subtitle, 14, Ui.MUTED, false));
        Ui.space(this, content, 20);
    }
    private void tile(String text, String subtitle, Runnable action) {
        LinearLayout card = Ui.panel(this);
        card.addView(Ui.text(this, text + "   ›", 19, Ui.WHITE, true));
        if (subtitle != null) {
            Ui.space(this, card, 6);
            card.addView(Ui.text(this, subtitle, 13, Ui.MUTED, false));
        }
        card.setOnClickListener(v -> action.run());
        content.addView(card);
        Ui.space(this, content, 11);
    }
    private void rowTitle(String title) {
        Ui.space(this, content, 19);
        content.addView(Ui.text(this, title, 21, Ui.WHITE, true));
        Ui.space(this, content, 13);
    }
    private void today() {
        AppSettings cfg = new AppSettings(this);
        LocalDate date = LocalDate.now(cfg.zone());
        title("Assalamu Alaikum", date.format(DateTimeFormatter.ofPattern("EEEE • d MMMM yyyy", Locale.ENGLISH)));
        LinearLayout featured = Ui.panel(this);
        featured.addView(Ui.text(this, cfg.configured() ? cfg.city() : "Choose your city", 14, Ui.GOLD, true));
        Ui.space(this, featured, 12);
        featured.addView(Ui.text(this, "Next prayer", 14, Ui.MUTED, false));
        Ui.space(this, featured, 3);
        String prayerTitle = "Configure prayer times";
        String upcoming = "Tap to select your location and calculation method.";
        try {
            if (cfg.configured()) {
                Instant now = Instant.now(), nextAt = null; Prayer nextPrayer = null;
                for (int d = 0; d < 3; d++) {
                    Map<Prayer, Instant> times = PrayerCalculator.calculate(date.plusDays(d),
                            cfg.latitude(), cfg.longitude(), cfg.method(), cfg.offsets());
                    for (Prayer p : Prayer.values()) {
                        Instant t = times.get(p);
                        if (t != null && t.isAfter(now) && (nextAt == null || t.isBefore(nextAt))) {
                            nextAt = t; nextPrayer = p;
                        }
                    }
                }
                if (nextAt != null) {
                    prayerTitle = nextPrayer.title;
                    long minutes = Math.max(0, Duration.between(now, nextAt).toMinutes());
                    upcoming = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH).withZone(cfg.zone()).format(nextAt)
                            + " • in " + minutes / 60 + "h " + minutes % 60 + "m";
                } else upcoming = "No calculated time. Check your settings.";
            }
        } catch (Exception ignored) { upcoming = "Check your coordinates and calculation settings."; }
        featured.addView(Ui.text(this, prayerTitle, 31, Ui.WHITE, true));
        Ui.space(this, featured, 7);
        featured.addView(Ui.text(this, upcoming, 15, Ui.MUTED, false));
        Ui.space(this, featured, 10);
        featured.addView(Ui.text(this, cfg.active() ? "● Azaan enabled" : "○ Configure Azaan", 13, Ui.GOLD, true));
        featured.setOnClickListener(v -> launch(com.hecker.azaan.MainActivity.class));
        content.addView(featured);
        rowTitle("Prayer & calendar");
        tile("Prayer times & Azaan", "Full prayer setup, voice announcements and countdown",
                () -> launch(com.hecker.azaan.MainActivity.class));
        tile("Islamic calendar", "Gregorian • Hijri • Desi • Shia religious occasions",
                () -> launch(com.sabber.taqwim.MainActivity.class));
        tile("Prayer tracker", "Mark your five daily prayers",
                () -> launch(PrayerTrackerActivity.class));
        rowTitle("Read & reflect");
        tile("The Holy Qur'an", "Offline 114 Surahs, Arabic Uthmani text",
                () -> library("quran"));
        tile("Azkar and Duas", "Daily supplications with translations and repeat counts",
                () -> library("azkar"));
        tile("Hadith", "Search 3,574 Hadith, read and bookmark",
                () -> library("hadith"));
        rowTitle("Explore");
        tile("Qibla direction", "Live compass toward the Kaaba",
                () -> launch(QiblaActivity.class));
        tile("Tasbeeh", "Your personal dhikr counter",
                () -> launch(TasbeehActivity.class));
        if (!ReferenceLibrary.ready(this)) {
            rowTitle("Offline content");
            tile(importing ? "Importing the content pack..." : "Import reference content",
                    "Select your friend's original APK or the Hecker Islam content ZIP",
                    this::chooseDataPack);
        }
    }
    private void more() {
        title("More", "Your worship tools and app settings");
        tile("Azaan settings", "Method • City • Notifications • Recitation",
                () -> launch(com.hecker.azaan.MainActivity.class));
        tile("Full Islamic calendar", "Months • Events • Calendar customization",
                () -> launch(com.sabber.taqwim.MainActivity.class));
        tile("Nearby mosques", "Explore mosques with your chosen maps app", this::mosques);
        tile("Qibla compass", "Compass calibrated with device sensors", () -> launch(QiblaActivity.class));
        tile("Prayer tracker", "Your local prayer history", () -> launch(PrayerTrackerActivity.class));
        tile("Tasbeeh counter", "Remembrance and saved counts", () -> launch(TasbeehActivity.class));
        tile("Import offline library", "Import Qur'an, Azkar, Hadith and city catalogs from the reference APK or ZIP", this::chooseDataPack);
        boolean dark = getSharedPreferences("islam_ui", MODE_PRIVATE).getBoolean("dark", false);
        tile(dark ? "Appearance: Dark mode" : "Appearance: Light mode",
                "Tap to switch between light and dark Material-inspired palettes", () -> {
                    getSharedPreferences("islam_ui", MODE_PRIVATE).edit().putBoolean("dark", !dark).apply();
                    render();
                });
        rowTitle("Content status");
        content.addView(Ui.text(this,
                ReferenceLibrary.ready(this)
                ? "✓ Offline Qur'an, Azkar and Hadith catalogs installed"
                : "Offline data pack not yet imported", 14, Ui.MUTED, false));
        Ui.space(this, content, 16);
        content.addView(Ui.text(this, "Hecker Islam • v0.2 alpha", 12, Ui.MUTED, false));
    }
    private void tabs() {
        String[] labels = {"Today", "Qur'an", "Azkar", "Hadith", "More"};
        String[] icons = {"⌂", "☷", "✺", "▤", "☰"};
        for (int i = 0; i < labels.length; i++) {
            final int position = i;
            LinearLayout pill = Ui.column(this);
            pill.setGravity(Gravity.CENTER);
            pill.setBackgroundColor(i == page ? Ui.CARD : Ui.BACKGROUND);
            TextView icon = Ui.text(this, icons[i], 22, i == page ? Ui.GOLD : Ui.MUTED, true);
            icon.setGravity(Gravity.CENTER);
            pill.addView(icon);
            TextView label = Ui.text(this, labels[i], 10, i == page ? Ui.WHITE : Ui.MUTED, i == page);
            label.setGravity(Gravity.CENTER);
            pill.addView(label);
            pill.setOnClickListener(v -> {
                if (position == 1) library("quran");
                else if (position == 2) library("azkar");
                else if (position == 3) library("hadith");
                else { page = position; render(); }
            });
            LinearLayout.LayoutParams item = new LinearLayout.LayoutParams(0, -1, 1);
            item.setMargins(Ui.dp(this, 2), 0, Ui.dp(this, 2), 0);
            navigation.addView(pill, item);
        }
    }
    private void library(String section) {
        Intent intent = new Intent(this, ReferenceReadingActivity.class);
        intent.putExtra("section", section); startActivity(intent);
    }
    private void launch(Class<?> cls) { startActivity(new Intent(this, cls)); }
    private void mosques() {
        AppSettings cfg = new AppSettings(this);
        if (!cfg.configured()) {
            new AlertDialog.Builder(this).setMessage("Choose a location in Azaan settings first.")
                    .setPositiveButton("Open Azaan", (d, w) -> launch(com.hecker.azaan.MainActivity.class))
                    .setNegativeButton("Cancel", null).show(); return;
        }
        Uri uri = Uri.parse(String.format(Locale.US,
                "geo:%.6f,%.6f?q=mosque", cfg.latitude(), cfg.longitude()));
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (Exception ex) {
            new AlertDialog.Builder(this).setMessage("Install a maps app to search nearby mosques.")
                    .setPositiveButton("OK", null).show();
        }
    }
    private void chooseDataPack() {
        if (importing) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, IMPORT_ARCHIVE);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != IMPORT_ARCHIVE || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri file = data.getData();
        importing = true; render();
        worker.execute(() -> {
            String message;
            try { message = ReferenceLibrary.importArchive(this, file); }
            catch (Exception ex) { message = "Import failed: " + ex.getMessage(); }
            String report = message;
            handler.post(() -> {
                if (isFinishing()) return;
                importing = false; render();
                new AlertDialog.Builder(this).setTitle("Offline library")
                        .setMessage(report).setPositiveButton("OK", null).show();
            });
        });
    }
}
