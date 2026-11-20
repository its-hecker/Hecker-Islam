package com.hecker.islam;

import android.app.Activity;
import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
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
    private LinearLayout content, navigation;
    private int page = 0;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        page = state == null ? getIntent().getIntExtra("tab", 0) : state.getInt("tab", 0);
        render();
    }
    @Override protected void onResume() { super.onResume(); render(); }
    @Override protected void onSaveInstanceState(Bundle b) { super.onSaveInstanceState(b); b.putInt("tab", page); }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent); page = intent.getIntExtra("tab", 0); render();
    }
    private void render() {
        Ui.applyTheme(this);
        getWindow().setStatusBarColor(Ui.BACKGROUND);
        getWindow().setNavigationBarColor(Ui.BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(
                !getSharedPreferences("islam_ui", MODE_PRIVATE).getBoolean("dark", true)
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
        GradientDrawable navSurface = new GradientDrawable();
        navSurface.setColor(Ui.CARD);
        navSurface.setCornerRadius(Ui.dp(this, 35));
        navSurface.setStroke(Ui.dp(this, 1), Ui.OUTLINE);
        navigation.setBackground(navSurface);
        navigation.setElevation(Ui.dp(this, 10));
        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 72));
        navParams.setMargins(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 12));
        root.addView(navigation, navParams);
        setContentView(root);
        header();
        if (page == 0) today();
        else if (page == 4) more();
        else page = 0;
        tabs();
        if (ValueAnimator.areAnimatorsEnabled()) {
            for (int i = 0; i < content.getChildCount(); i++) {
                View item = content.getChildAt(i);
                item.setAlpha(0f);
                item.setTranslationY(Ui.dp(this, 12));
                item.animate().alpha(1f).translationY(0f)
                        .setStartDelay(Math.min(180, i * 20L))
                        .setDuration(360).setInterpolator(new DecelerateInterpolator()).start();
            }
            navigation.setScaleX(.96f); navigation.setScaleY(.96f);
            navigation.animate().scaleX(1f).scaleY(1f).setDuration(370)
                    .setInterpolator(new OvershootInterpolator(.6f)).start();
        }
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
        tile("The Holy Qur'an", "114 Surahs with Shia Urdu translations • Jawadi & Najafi",
                () -> library("quran"));
        tile("Shia Duas & Ziyarat", "Kumayl • Tawassul • Nudba • Faraj • Ziyarat Ashura",
                () -> launch(ShiaDuasActivity.class));
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
            rowTitle("Offline library unavailable");
            content.addView(Ui.text(this,
                    "This development build is missing its content. Only release APKs with embedded offline catalogs should be distributed.",
                    14, Ui.MUTED, false));
        }
    }
    private void more() {
        title("More", "Your worship tools and app settings");
        tile("Azaan settings", "Method • City • Notifications • Recitation",
                () -> launch(com.hecker.azaan.MainActivity.class));
        tile("Full Islamic calendar", "Months • Events • Calendar customization",
                () -> launch(com.sabber.taqwim.MainActivity.class));
        boolean originalCalendar = getSharedPreferences("islam_ui", MODE_PRIVATE)
                .getBoolean("calendar_original", false);
        tile(originalCalendar ? "Calendar appearance: Original" : "Calendar appearance: Hecker Islam",
                "Hecker Islam is the default • Switch to the preserved original Calendar design", () -> {
                    getSharedPreferences("islam_ui", MODE_PRIVATE).edit()
                            .putBoolean("calendar_original", !originalCalendar).apply();
                    render();
                });
        tile("Nearby mosques", "Explore mosques with your chosen maps app", this::mosques);
        tile("Qibla compass", "Compass calibrated with device sensors", () -> launch(QiblaActivity.class));
        tile("Prayer tracker", "Your local prayer history", () -> launch(PrayerTrackerActivity.class));
        tile("Tasbeeh counter", "Remembrance and saved counts", () -> launch(TasbeehActivity.class));
        tile("Shia Duas & Ziyarat", "Ahlulbayt supplications, bookmarks and source texts",
                () -> launch(ShiaDuasActivity.class));
        tile("Offline library", ReferenceLibrary.ready(this)
                ? "Qur'an, Hadith, Azkar and city catalogs included, ready offline"
                : "Developer build incomplete: embedded content missing",
                () -> new AlertDialog.Builder(this)
                .setMessage(ReferenceLibrary.ready(this)
                        ? "Your complete offline library is already included with Hecker Islam."
                        : "This build is missing assets. Rebuild with the complete offline reference pack.")
                .setPositiveButton("OK", null).show());
        boolean dark = getSharedPreferences("islam_ui", MODE_PRIVATE).getBoolean("dark", true);
        tile(dark ? "Appearance: Dark mode" : "Appearance: Light mode",
                "Tap to switch between light and dark Material-inspired palettes", () -> {
                    getSharedPreferences("islam_ui", MODE_PRIVATE).edit().putBoolean("dark", !dark).apply();
                    render();
                });
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            boolean dynamic = getSharedPreferences("islam_ui", MODE_PRIVATE).getBoolean("dynamic", true);
            tile(dynamic ? "Material You colors: On" : "Material You colors: Off",
                    "Match the accent colors from your device, as in Material 3",
                    () -> {
                        getSharedPreferences("islam_ui", MODE_PRIVATE).edit()
                                .putBoolean("dynamic", !dynamic).apply();
                        render();
                    });
        }
        rowTitle("Content status");
        content.addView(Ui.text(this,
                ReferenceLibrary.ready(this)
                ? "✓ Offline Qur'an, Azkar and Hadith catalogs installed"
                : "Offline data pack not yet imported", 14, Ui.MUTED, false));
        Ui.space(this, content, 16);
        content.addView(Ui.text(this, "Hecker Islam • v0.2 alpha", 12, Ui.MUTED, false));
    }
    private void tabs() {
        Ui.fillNavigation(this, navigation, page, index -> {
            if (index == 1) library("quran");
            else if (index == 2) library("azkar");
            else if (index == 3) library("hadith");
            else { page = index; render(); }
        });
    }
    private void library(String section) {
        Intent intent = new Intent(this, ReferenceReadingActivity.class);
        intent.putExtra("section", section); startActivity(intent);
        overridePendingTransition(0, 0);
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
}
