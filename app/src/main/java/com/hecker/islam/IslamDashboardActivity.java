package com.hecker.islam;

import android.app.Activity;
import android.animation.ValueAnimator;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
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
import com.hecker.azaan.AlarmPlanner;
import com.hecker.azaan.PrayerTimeProvider;
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
    private final com.hecker.azaan.DeviceLocationTracker automaticLocation = new com.hecker.azaan.DeviceLocationTracker();
    private TextView nextAzaanCountdown;
    private TextView wisdomText, wisdomReference;
    private int currentWisdom = -1;
    // Persist the last automatic/manual change so frequent location/Prayer refreshes
    // cannot indefinitely postpone the next wisdom.
    private static final long WISDOM_ROTATION_INTERVAL_MS = 30_000L;
    private static final String WISDOM_CHANGED_KEY = "changed_at_ms";
    private long wisdomChangedAt;
    private Instant nextAzaanInstant;
    private LocalDate renderedCalendarDay;
    private final Handler clock = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (page != 0 || isFinishing() || isDestroyed()) return;
            if (renderedCalendarDay != null && !renderedCalendarDay.equals(LocalDate.now())) {
                render(); return;
            }
            if (nextAzaanInstant != null && !nextAzaanInstant.isAfter(Instant.now())) {
                render(); return;
            }
            updateCountdown();
            rotateWisdomIfDue();
            clock.postDelayed(this, 5_000);
        }
    };
    private int page = 0;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        page = state == null ? getIntent().getIntExtra("tab", 0) : state.getInt("tab", 0);
        render();
    }
    @Override protected void onResume() {
        super.onResume();
        render();
        AppSettings times = new AppSettings(this);
        automaticLocation.start(this, () -> {
            if (!isFinishing() && !isDestroyed() && page == 0) render();
        });
        com.hecker.azaan.LocationLabeler.resolveIfNeeded(this, () -> {
            if (!isFinishing() && !isDestroyed() && page == 0) render();
        });
        if (times.configured() && times.providerEnabled()) {
            com.hecker.azaan.PrayerTimeProvider.refreshAsync(this, false, () -> {
                if (!isFinishing() && !isDestroyed()) render();
            });
        }
    }
    @Override protected void onPause() {
        automaticLocation.stop();
        clock.removeCallbacks(clockTick);
        super.onPause();
    }
    @Override protected void onDestroy() {
        automaticLocation.stop();
        clock.removeCallbacks(clockTick);
        super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle b) { super.onSaveInstanceState(b); b.putInt("tab", page); }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent); page = intent.getIntExtra("tab", 0); render();
    }
    private void render() {
        clock.removeCallbacks(clockTick);
        nextAzaanCountdown = null;
        nextAzaanInstant = null;
        wisdomText = null;
        wisdomReference = null;
        Ui.applyTheme(this);
        NahjWisdom.initialize(this);
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
        if (page == 0) {
            updateCountdown();
            // Catch up even when onResume/location/calendar renders have reset
            // the regular clock callback. Rotation never resets on a UI refresh.
            rotateWisdomIfDue();
            clock.postDelayed(clockTick, 5_000);
        }
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
        if (page == 0) {
            // A compact single-line greeting fills the available header column.
            // The colored pill makes the area beside the action icons deliberate.
            LinearLayout greetingPill = Ui.column(this);
            greetingPill.setGravity(Gravity.CENTER);
            greetingPill.setPadding(Ui.dp(this, 8), Ui.dp(this, 5),
                    Ui.dp(this, 8), Ui.dp(this, 5));
            GradientDrawable greetingBackground = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{mix(Ui.CARD, Ui.GOLD, .20f),
                              mix(Ui.CARD, Ui.GOLD, .06f)});
            greetingBackground.setCornerRadius(Ui.dp(this, 17));
            greetingBackground.setStroke(Ui.dp(this, 1),
                    mix(Ui.OUTLINE, Ui.GOLD, .48f));
            greetingPill.setBackground(greetingBackground);
            ShimmerGreeting heading = new ShimmerGreeting(this);
            heading.setText("السلام علیکم یا علیؑ مدد");
            heading.setSingleLine(true);
            heading.setGravity(Gravity.CENTER);
            // Android sizes the text to the available width rather than making
            // a ragged two-line header on Pixel 4-series-sized screens.
            heading.setAutoSizeTextTypeUniformWithConfiguration(
                    14, 22, 1, TypedValue.COMPLEX_UNIT_SP);
            greetingPill.addView(heading, new LinearLayout.LayoutParams(-1, -1));
            LinearLayout.LayoutParams greetingParams =
                    new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1);
            greetingParams.setMargins(0, 0, Ui.dp(this, 9), 0);
            line.addView(greetingPill, greetingParams);
        } else {
            TextView heading = Ui.text(this, "HECKER ISLAM", 14, Ui.GOLD, true);
            line.addView(heading, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1));
        }
        TextView info = Ui.text(this, "ⓘ", 25, Ui.GOLD, false);
        info.setGravity(Gravity.CENTER); info.setContentDescription("App information and credits");
        info.setOnClickListener(v -> AboutHeckerDialog.show(this));
        line.addView(info, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
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
    private static int mix(int a, int b, float amount) {
        float inverse = 1f - amount;
        return Color.rgb(
                Math.round(Color.red(a) * inverse + Color.red(b) * amount),
                Math.round(Color.green(a) * inverse + Color.green(b) * amount),
                Math.round(Color.blue(a) * inverse + Color.blue(b) * amount));
    }

    /** One surface palette for wisdom, calendar, Azaan and exploration tiles. */
    private LinearLayout dashboardCard(boolean featured) {
        LinearLayout card = Ui.panel(this);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 18),
                Ui.dp(this, 18), Ui.dp(this, 18));
        return card;
    }

    private TextView eyebrow(String value) {
        TextView t = Ui.text(this, value, 11, Ui.GOLD, true);
        t.setLetterSpacing(.12f);
        return t;
    }

    private void addCard(LinearLayout card) {
        content.addView(card, new LinearLayout.LayoutParams(-1, -2));
        Ui.space(this, content, 14);
    }

    private void separator(LinearLayout parent) {
        Ui.space(this, parent, 17);
        View line = new View(this);
        line.setBackgroundColor(Ui.OUTLINE);
        parent.addView(line, new LinearLayout.LayoutParams(-1, Ui.dp(this, 1)));
        Ui.space(this, parent, 15);
    }

    private void dateDashboard(LocalDate date) {
        DashboardCalendar.Day calendar = DashboardCalendar.today(this, date);
        LinearLayout card = dashboardCard(true);
        // Dense, readable calendar summary: the full Calendar remains one tap away.
        card.setPadding(Ui.dp(this, 16), Ui.dp(this, 15),
                Ui.dp(this, 16), Ui.dp(this, 15));
        card.addView(eyebrow("TODAY · YOUR CALENDAR"));
        Ui.space(this, card, 8);

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView number = Ui.text(this, String.valueOf(date.getDayOfMonth()),
                43, Ui.GOLD, true);
        number.setIncludeFontPadding(false);
        dateRow.addView(number);
        LinearLayout dateDetail = Ui.column(this);
        dateDetail.setPadding(Ui.dp(this, 12), 0, 0, 0);
        dateDetail.addView(Ui.text(this,
                date.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)),
                17, Ui.WHITE, true));
        dateDetail.addView(Ui.text(this,
                date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
                13, Ui.MUTED, false));
        dateRow.addView(dateDetail, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(dateRow);
        Ui.space(this, card, 10);

        TextView hijri = Ui.text(this, "☾  " + calendar.hijriDate,
                13, Ui.WHITE, true);
        hijri.setPadding(Ui.dp(this, 10), Ui.dp(this, 7),
                Ui.dp(this, 10), Ui.dp(this, 7));
        GradientDrawable hijriBg = new GradientDrawable();
        hijriBg.setColor(mix(Ui.CARD, Ui.GOLD, .13f));
        hijriBg.setCornerRadius(Ui.dp(this, 13));
        hijri.setBackground(hijriBg);
        card.addView(hijri);
        Ui.space(this, card, 5);
        TextView desi = Ui.text(this,
                "✦  " + calendar.desiDate + " · Desi calendar",
                12, Ui.MUTED, false);
        desi.setPadding(Ui.dp(this, 6), 0, Ui.dp(this, 6), 0);
        card.addView(desi);

        Ui.space(this, card, 10);
        View separator = new View(this);
        separator.setBackgroundColor(Ui.OUTLINE);
        card.addView(separator, new LinearLayout.LayoutParams(-1, Ui.dp(this, 1)));
        Ui.space(this, card, 10);

        if (!calendar.calendarLoaded) {
            card.addView(eyebrow("CALENDAR EVENTS"));
            Ui.space(this, card, 4);
            card.addView(Ui.text(this,
                    "Calendar details are unavailable in this build.",
                    12, Ui.MUTED, false));
        } else if (!calendar.today.isEmpty()) {
            card.addView(eyebrow("TODAY'S OCCASION"));
            Ui.space(this, card, 4);
            DashboardCalendar.Occasion event = calendar.today.get(0);
            TextView occasionTitle = Ui.text(this, event.title, 15, Ui.WHITE, true);
            occasionTitle.setMaxLines(2);
            card.addView(occasionTitle);
            if (!event.summary.isEmpty()) {
                Ui.space(this, card, 4);
                TextView summary = Ui.text(this, event.summary, 12, Ui.MUTED, false);
                summary.setMaxLines(2);
                card.addView(summary);
            }
            if (calendar.today.size() > 1) {
                Ui.space(this, card, 5);
                card.addView(Ui.text(this, "+" + (calendar.today.size() - 1)
                        + " more occasion(s) · View Calendar", 11, Ui.GOLD, true));
            }
        } else {
            card.addView(eyebrow("UPCOMING OCCASION"));
            Ui.space(this, card, 4);
            if (calendar.upcoming != null) {
                TextView title = Ui.text(this, calendar.upcoming.title,
                        15, Ui.WHITE, true);
                title.setMaxLines(2);
                card.addView(title);
                Ui.space(this, card, 3);
                card.addView(Ui.text(this, "In " + calendar.daysUntilNext
                        + (calendar.daysUntilNext == 1 ? " day" : " days"),
                        12, Ui.MUTED, false));
            } else {
                card.addView(Ui.text(this,
                        "No upcoming occasion listed.", 12, Ui.MUTED, false));
            }
        }

        // Nested tap target deliberately remains separate from the Calendar.
        Ui.space(this, card, 11);
        LinearLayout ziyaratShortcut = Ui.column(this);
        ziyaratShortcut.setPadding(Ui.dp(this, 11), Ui.dp(this, 9),
                Ui.dp(this, 11), Ui.dp(this, 9));
        GradientDrawable ziyaratBackground = new GradientDrawable();
        ziyaratBackground.setColor(mix(Ui.CARD, Ui.GOLD, .10f));
        ziyaratBackground.setCornerRadius(Ui.dp(this, 15));
        ziyaratBackground.setStroke(Ui.dp(this, 1),
                mix(Ui.OUTLINE, Ui.GOLD, .24f));
        ziyaratShortcut.setBackground(ziyaratBackground);
        ziyaratShortcut.addView(eyebrow("TODAY'S ZIYARAT"));
        Ui.space(this, ziyaratShortcut, 3);
        String ziyaratName = DailyZiaraatActivity.labelForDate(date);
        TextView ziyaratTitle = Ui.text(this, ziyaratName + "  ›",
                14, Ui.WHITE, true);
        ziyaratTitle.setMaxLines(2);
        ziyaratShortcut.addView(ziyaratTitle);
        ziyaratShortcut.setContentDescription("Read today's Ziyarat: " + ziyaratName);
        ziyaratShortcut.setOnClickListener(v -> {
            Intent intent = new Intent(this, DailyZiaraatActivity.class);
            intent.putExtra(DailyZiaraatActivity.EXTRA_OPEN_TODAY, true);
            startActivity(intent);
        });
        card.addView(ziyaratShortcut);
        Ui.space(this, card, 9);
        card.addView(Ui.text(this, "Open full calendar  ›", 12, Ui.GOLD, true));
        card.setContentDescription("Open Islamic calendar with today's dates and occasions");
        card.setOnClickListener(v -> launch(com.sabber.taqwim.MainActivity.class));
        addCard(card);
    }

    private void updateCountdown() {
        if (nextAzaanCountdown == null) return;
        if (nextAzaanInstant == null) {
            nextAzaanCountdown.setText("Set your city to see upcoming Azaan times");
            return;
        }
        long seconds = Math.max(0, Duration.between(Instant.now(), nextAzaanInstant).getSeconds());
        long hours = seconds / 3600, minutes = seconds % 3600 / 60;
        nextAzaanCountdown.setText("In " + hours + "h " + minutes + "m");
    }

    private void azaanDashboard(AppSettings cfg) {
        LinearLayout card = dashboardCard(false);
        LocalDate prayerDay = LocalDate.now(cfg.zone());
        card.addView(eyebrow("NEXT AZAAN"));
        Ui.space(this, card, 12);
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout copy = Ui.column(this);
        TextView name = Ui.text(this, "Set your location", 25, Ui.WHITE, true);
        copy.addView(name);
        Ui.space(this, copy, 3);
        nextAzaanCountdown = Ui.text(this, "", 14, Ui.GOLD, true);
        copy.addView(nextAzaanCountdown);
        top.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        TextView clockTime = Ui.text(this, "—", 23, Ui.GOLD, true);
        clockTime.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        top.addView(clockTime);
        card.addView(top);

        if (cfg.configured()) {
            try {
                java.util.List<AlarmPlanner.Event> upcoming = AlarmPlanner.upcoming(
                        this, Instant.now(), cfg, new boolean[]{true, true, true});
                if (!upcoming.isEmpty()) {
                    AlarmPlanner.Event next = upcoming.get(0);
                    nextAzaanInstant = next.time;
                    name.setText(next.prayer.title);
                    clockTime.setText(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
                            .withZone(cfg.zone()).format(next.time));
                } else {
                    name.setText("Check local timetable");
                    nextAzaanCountdown.setText("No upcoming calculated time available");
                }
            } catch (RuntimeException e) {
                name.setText("Check prayer settings");
                nextAzaanCountdown.setText("Prayer times could not be calculated");
            }
            Ui.space(this, card, 14);
            LinearLayout place = Ui.panel(this);
            place.setPadding(Ui.dp(this, 14), Ui.dp(this, 11),
                    Ui.dp(this, 14), Ui.dp(this, 11));
            place.addView(Ui.text(this, cfg.autoLocation()
                    ? "📍  AUTOMATIC PRAYER LOCATION" : "📍  SELECTED PRAYER LOCATION",
                    10, Ui.GOLD, true));
            Ui.space(this, place, 5);
            String resolved = cfg.city();
            if ("Current location".equalsIgnoreCase(resolved)
                    || "My location".equalsIgnoreCase(resolved)) {
                resolved = String.format(Locale.US, "GPS • %.4f, %.4f",
                        cfg.latitude(), cfg.longitude());
            }
            place.addView(Ui.text(this, resolved, 16, Ui.WHITE, true));
            card.addView(place);
            Ui.space(this, card, 9);
            card.addView(Ui.text(this, PrayerTimeProvider.status(this, prayerDay, cfg),
                    12, Ui.MUTED, false));
        } else {
            Ui.space(this, card, 12);
            card.addView(Ui.text(this, "Choose your city or coordinates in Azaan settings.",
                    13, Ui.MUTED, false));
        }
        separator(card);
        card.addView(eyebrow("TODAY'S THREE AZAAN TIMES"));
        Ui.space(this, card, 10);
        Map<Prayer, Instant> todayTimes = null;
        if (cfg.configured()) {
            try { todayTimes = PrayerTimeProvider.calculate(this, prayerDay, cfg); }
            catch (RuntimeException ignored) { }
        }
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        for (Prayer prayer : Prayer.values()) {
            LinearLayout mini = Ui.column(this);
            mini.setGravity(Gravity.CENTER);
            mini.setPadding(Ui.dp(this, 2), Ui.dp(this, 8), Ui.dp(this, 2), Ui.dp(this, 8));
            String label = prayer == Prayer.FAJR ? "FAJR" :
                    prayer == Prayer.DHUHR ? "DHUHR" : "MAGHRIB";
            TextView labelView = Ui.text(this, label, 10, Ui.MUTED, true);
            labelView.setGravity(Gravity.CENTER);
            mini.addView(labelView);
            Ui.space(this, mini, 7);
            Instant at = todayTimes == null ? null : todayTimes.get(prayer);
            String time = at == null ? "—" : DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
                    .withZone(cfg.zone()).format(at);
            TextView value = Ui.text(this, time, 13, Ui.WHITE, true);
            value.setGravity(Gravity.CENTER);
            mini.addView(value);
            GradientDrawable pill = new GradientDrawable();
            pill.setColor(mix(Ui.CARD, Ui.GOLD, .08f));
            pill.setCornerRadius(Ui.dp(this, 16));
            mini.setBackground(pill);
            LinearLayout.LayoutParams miniParams = new LinearLayout.LayoutParams(0, -2, 1);
            miniParams.setMargins(Ui.dp(this, 2), 0, Ui.dp(this, 2), 0);
            strip.addView(mini, miniParams);
        }
        card.addView(strip);
        Ui.space(this, card, 13);
        card.addView(Ui.text(this, "Prayer times & Azaan settings  ›", 13, Ui.GOLD, true));
        card.setContentDescription("Open Azaan and prayer times");
        card.setOnClickListener(v -> launch(com.hecker.azaan.MainActivity.class));
        addCard(card);
    }

    private LinearLayout shortcut(String symbol, String title, String description, Runnable action) {
        LinearLayout card = dashboardCard(false);
        card.setMinimumHeight(Ui.dp(this, 132));
        TextView mark = Ui.text(this, symbol, 24, Ui.GOLD, true);
        card.addView(mark);
        Ui.space(this, card, 10);
        card.addView(Ui.text(this, title, 17, Ui.WHITE, true));
        Ui.space(this, card, 5);
        TextView descriptionText = Ui.text(this, description, 12, Ui.MUTED, false);
        descriptionText.setMaxLines(3);
        card.addView(descriptionText);
        card.setContentDescription("Open " + title);
        card.setOnClickListener(v -> action.run());
        return card;
    }

    private void shortcutRow(String leftIcon, String leftTitle, String leftSubtitle, Runnable left,
                             String rightIcon, String rightTitle, String rightSubtitle, Runnable right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout leftCard = shortcut(leftIcon, leftTitle, leftSubtitle, left);
        LinearLayout rightCard = shortcut(rightIcon, rightTitle, rightSubtitle, right);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, -1, 1);
        lp1.setMargins(0, 0, Ui.dp(this, 6), 0);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, -1, 1);
        lp2.setMargins(Ui.dp(this, 6), 0, 0, 0);
        row.addView(leftCard, lp1);
        row.addView(rightCard, lp2);
        content.addView(row);
        Ui.space(this, content, 12);
    }

    /** Rotate after a real 30-second interval, even across repeated dashboard renders. */
    private void rotateWisdomIfDue() {
        if (page != 0 || wisdomText == null || wisdomReference == null) return;
        android.content.SharedPreferences prefs =
                getSharedPreferences("nahj_wisdom_cycle", MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (wisdomChangedAt <= 0L) {
            wisdomChangedAt = prefs.getLong(WISDOM_CHANGED_KEY, 0L);
        }
        if (wisdomChangedAt <= 0L || wisdomChangedAt > now) {
            wisdomChangedAt = now;
            prefs.edit().putLong(WISDOM_CHANGED_KEY, now).apply();
        }
        if (now - wisdomChangedAt >= WISDOM_ROTATION_INTERVAL_MS) {
            rotateWisdom();
        }
    }

    /** Pick the next unused wisdom and save both selection and rotation time. */
    private void rotateWisdom() {
        if (wisdomText == null || wisdomReference == null) return;
        android.content.SharedPreferences prefs =
                getSharedPreferences("nahj_wisdom_cycle", MODE_PRIVATE);
        if (currentWisdom < 0 || currentWisdom >= NahjWisdom.QUOTES.length)
            currentWisdom = prefs.getInt("current", -1);
        WisdomCycle.Pick next = WisdomCycle.next(NahjWisdom.QUOTES.length,
                prefs.getString("order", ""), prefs.getInt("cursor", 0),
                currentWisdom, new java.util.Random());
        currentWisdom = next.index;
        wisdomChangedAt = System.currentTimeMillis();
        prefs.edit().putInt("current", next.index).putString("order", next.order)
                .putInt("cursor", next.cursor)
                .putLong(WISDOM_CHANGED_KEY, wisdomChangedAt).apply();
        showCurrentWisdom();
        if (ValueAnimator.areAnimatorsEnabled() && wisdomText.isAttachedToWindow()) {
            // Subtle transition without moving or resizing the entire card.
            wisdomText.animate().cancel();
            wisdomText.setAlpha(.35f);
            wisdomText.setTranslationY(Ui.dp(this, 5));
            wisdomText.animate().alpha(1f).translationY(0f)
                    .setDuration(320).setInterpolator(new DecelerateInterpolator()).start();
            wisdomReference.animate().cancel();
            wisdomReference.setAlpha(.35f);
            wisdomReference.animate().alpha(1f).setDuration(320).start();
        }
    }

    /** Re-rendering the dashboard does not consume a quote or restart the timer. */
    private void showCurrentWisdom() {
        if (wisdomText == null || wisdomReference == null) return;
        android.content.SharedPreferences prefs =
                getSharedPreferences("nahj_wisdom_cycle", MODE_PRIVATE);
        if (currentWisdom < 0 || currentWisdom >= NahjWisdom.QUOTES.length)
            currentWisdom = prefs.getInt("current", -1);
        if (currentWisdom < 0 || currentWisdom >= NahjWisdom.QUOTES.length) {
            rotateWisdom();
            return;
        }
        NahjWisdom.Quote quote = NahjWisdom.QUOTES[currentWisdom];
        String display = quote.displayText();
        int quoteLength = display.codePointCount(0, display.length());
        wisdomText.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                quoteLength > 105 ? 15f : quoteLength > 73 ? 17f : 18f);
        wisdomText.setText(display);
        wisdomReference.setText(quote.hasUrdu()
                ? "نہج البلاغہ • اردو مفہوم"
                : "نہج البلاغہ • اصل عربی • مکمل مطالعہ کے لیے کھولیں");
        // First render of older installations: initialize once, then persist.
        // On later launches the saved epoch lets us rotate immediately if due.
        if (wisdomChangedAt <= 0L) {
            wisdomChangedAt = prefs.getLong(WISDOM_CHANGED_KEY, 0L);
            if (wisdomChangedAt <= 0L) {
                wisdomChangedAt = System.currentTimeMillis();
                prefs.edit().putLong(WISDOM_CHANGED_KEY, wisdomChangedAt).apply();
            }
        }
    }
    private void wisdomCard() {
        // Short dashboard preview: full text expands naturally for longer quotes,
        // but there is no redundant full-width bottom action row.
        LinearLayout card = dashboardCard(true);
        card.setPadding(Ui.dp(this, 15), Ui.dp(this, 13),
                Ui.dp(this, 15), Ui.dp(this, 13));
        LinearLayout wisdomHeader = new LinearLayout(this);
        wisdomHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView wisdomHeading =
                Ui.text(this, "WISDOM FROM IMAM ALI (A.S.)", 11, Ui.GOLD, true);
        wisdomHeading.setMaxLines(2);
        wisdomHeader.addView(wisdomHeading,
                new LinearLayout.LayoutParams(0, -2, 1));

        GradientDrawable buttonSurface = new GradientDrawable();
        buttonSurface.setColor(mix(Ui.CARD, Ui.GOLD, .11f));
        buttonSurface.setCornerRadius(Ui.dp(this, 22));
        buttonSurface.setStroke(Ui.dp(this, 1), Ui.OUTLINE);

        ImageView nextWisdom = new ImageView(this);
        nextWisdom.setImageResource(android.R.drawable.ic_media_next);
        nextWisdom.setColorFilter(Ui.GOLD);
        nextWisdom.setContentDescription("Show next wisdom quote");
        nextWisdom.setPadding(Ui.dp(this, 11), Ui.dp(this, 11),
                Ui.dp(this, 11), Ui.dp(this, 11));
        nextWisdom.setBackground(buttonSurface.getConstantState().newDrawable().mutate());
        nextWisdom.setOnClickListener(v -> rotateWisdom());
        wisdomHeader.addView(nextWisdom, new LinearLayout.LayoutParams(
                Ui.dp(this, 44), Ui.dp(this, 44)));

        ImageView shareStatus = new ImageView(this);
        shareStatus.setImageResource(android.R.drawable.ic_menu_share);
        shareStatus.setColorFilter(Ui.GOLD);
        shareStatus.setContentDescription("Share this wisdom to WhatsApp Status");
        shareStatus.setPadding(Ui.dp(this, 11), Ui.dp(this, 11),
                Ui.dp(this, 11), Ui.dp(this, 11));
        shareStatus.setBackground(buttonSurface.getConstantState().newDrawable().mutate());
        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(
                Ui.dp(this, 44), Ui.dp(this, 44));
        shareParams.setMargins(Ui.dp(this, 7), 0, 0, 0);
        wisdomHeader.addView(shareStatus, shareParams);
        shareStatus.setOnClickListener(v -> {
            if (currentWisdom >= 0 && currentWisdom < NahjWisdom.QUOTES.length)
                QuoteStatusShare.share(this, NahjWisdom.QUOTES[currentWisdom]);
        });
        card.addView(wisdomHeader);
        Ui.space(this, card, 7);

        wisdomText = Ui.text(this, "", 18, Ui.WHITE, false);
        wisdomText.setTextDirection(View.TEXT_DIRECTION_RTL);
        wisdomText.setGravity(Gravity.RIGHT);
        wisdomText.setLineSpacing(Ui.dp(this, 2), 1.06f);
        // A few maxims are long essays. Show their first lines on the dashboard;
        // tapping the card always opens the complete 480-saying Arabic reader.
        wisdomText.setMaxLines(5);
        wisdomText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.addView(wisdomText);
        Ui.space(this, card, 6);

        wisdomReference = Ui.text(this, "", 11, Ui.MUTED, false);
        wisdomReference.setTextDirection(View.TEXT_DIRECTION_RTL);
        wisdomReference.setGravity(Gravity.RIGHT);
        card.addView(wisdomReference);
        showCurrentWisdom();
        card.setContentDescription("Nahj al-Balagha wisdom. Tap to read, or use the next quote and share buttons.");
        card.setOnClickListener(v -> launch(NahjAlBalaghaActivity.class));
        content.addView(card);
        Ui.space(this, content, 12);
    }
    /**
     * Animated but readable theme-accent shimmer. Uses the actual TextView width
     * and cancels its animator whenever the dashboard is redrawn or backgrounded.
     * With system animations disabled, the text remains a static accent color.
     */
    private static final class ShimmerGreeting extends TextView {
        private LinearGradient gradient;
        private final Matrix matrix = new Matrix();
        private ValueAnimator shimmer;

        ShimmerGreeting(Activity activity) {
            super(activity);
            setText("السلام علیکم یا علیؑ مدد");
            setTextDirection(View.TEXT_DIRECTION_RTL);
            setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            setGravity(Gravity.CENTER);
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 27);
            setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            setTextColor(Ui.GOLD);
            setIncludeFontPadding(true);
            setLineSpacing(Ui.dp(activity, 5), 1.08f);
            setContentDescription("السلام علیکم یا علیؑ مدد");
        }

        @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
            super.onSizeChanged(w, h, oldW, oldH);
            if (w <= 0) return;
            int glow = mix(Ui.GOLD, Ui.WHITE, .76f);
            gradient = new LinearGradient(-w, 0, w, 0,
                    new int[]{Ui.GOLD, glow, Ui.GOLD},
                    new float[]{0f, .5f, 1f}, Shader.TileMode.CLAMP);
            getPaint().setShader(gradient);
            if (isAttachedToWindow()) startShimmer();
            invalidate();
        }

        private void startShimmer() {
            if (gradient == null || shimmer != null
                    || getWindowVisibility() != View.VISIBLE
                    || !ValueAnimator.areAnimatorsEnabled()) return;
            shimmer = ValueAnimator.ofFloat(0f, 1f);
            shimmer.setDuration(4200L);
            shimmer.setRepeatCount(ValueAnimator.INFINITE);
            shimmer.setRepeatMode(ValueAnimator.REVERSE);
            shimmer.addUpdateListener(animation -> {
                if (gradient == null) return;
                float fraction = (Float) animation.getAnimatedValue();
                matrix.setTranslate((fraction - .5f) * getWidth(), 0);
                gradient.setLocalMatrix(matrix);
                invalidate();
            });
            shimmer.start();
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            startShimmer();
        }

        @Override protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);
            if (visibility == View.VISIBLE) {
                startShimmer();
            } else {
                stopShimmer();
            }
        }

        private void stopShimmer() {
            if (shimmer != null) {
                shimmer.cancel();
                shimmer = null;
            }
        }

        @Override protected void onDetachedFromWindow() {
            stopShimmer();
            super.onDetachedFromWindow();
        }
    }

    private void today() {
        renderedCalendarDay = LocalDate.now();
        AppSettings cfg = new AppSettings(this);
        // One interactive wisdom card directly beneath the header,
        // followed by the calendar. No duplicate wisdom below the calendar.
        wisdomCard();
        dateDashboard(renderedCalendarDay);
        azaanDashboard(cfg);

        Ui.space(this, content, 10);
        content.addView(Ui.text(this, "Explore", 23, Ui.WHITE, true));
        Ui.space(this, content, 5);
        content.addView(Ui.text(this, "Everything you need, one tap away.", 13, Ui.MUTED, false));
        Ui.space(this, content, 15);

        shortcutRow("☪", "Qur'an", "Read, listen & Urdu translation",
                () -> library("quran"), "✧", "Shia Duas", "Duas, Ziyarat & bookmarks",
                () -> launch(ShiaDuasActivity.class));
        shortcutRow("▦", "Calendar", "Hijri, Desi & occasions",
                () -> launch(com.sabber.taqwim.MainActivity.class),
                "☾", "Azaan", "Times, voice & settings",
                () -> launch(com.hecker.azaan.MainActivity.class));
        shortcutRow("◎", "Qibla", "Direction of the Kaaba",
                () -> launch(QiblaActivity.class),
                "✿", "Tasbeeh", "Keep track of your dhikr",
                () -> launch(TasbeehActivity.class));

        Ui.space(this, content, 8);
        content.addView(Ui.text(this, "More to explore", 19, Ui.WHITE, true));
        Ui.space(this, content, 12);
        tile("Shia Hadith Library", "Al-Kafi, Al-Faqih and Ahlulbayt narrations", () -> launch(ShiaHadithActivity.class));
        tile("Nahj al-Balagha", "Imam Ali’s wisdom • Arabic & Urdu", () -> launch(NahjAlBalaghaActivity.class));
        tile("Shia Mojzaat • معجزات اہلِ بیتؑ", "Mola Ali and Sayyida Fatima Zahra • Urdu readings",
                () -> launch(MojzatActivity.class));
        tile("Azkar & daily remembrance", "Daily supplications and repeat counts", () -> library("azkar"));
        tile("Prayer tracker", "Keep a personal record of five daily prayers",
                () -> launch(PrayerTrackerActivity.class));
        if (!ReferenceLibrary.ready(this)) {
            Ui.space(this, content, 6);
            content.addView(Ui.text(this,
                    "This build is missing its offline library. Install a complete release.",
                    13, Ui.MUTED, false));
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
        tile("Nahj al-Balagha", "480 wisdom sayings and Urdu reference links", () -> launch(NahjAlBalaghaActivity.class));
        tile("Shia Hadith", "Read Shia source books", () -> launch(ShiaHadithActivity.class));
        tile("Shia Mojzaat • معجزات اہلِ بیتؑ", "Mola Ali and Sayyida Fatima Zahra • recitation and bookmarks",
                () -> launch(MojzatActivity.class));
        tile("Shia Duas & Ziyarat", "Ahlulbayt supplications, bookmarks and source texts",
                () -> launch(ShiaDuasActivity.class));
        tile("Offline library", ReferenceLibrary.ready(this)
                ? "Qur'an, Azkar and city catalogs are available offline"
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
                ? "✓ Qur'an and Azkar available offline • Shia Hadith uses online sources"
                : "Offline data pack not yet imported", 14, Ui.MUTED, false));
        Ui.space(this, content, 16);
        content.addView(Ui.text(this, "Hecker Islam • v0.2 alpha", 12, Ui.MUTED, false));
    }
    private void tabs() {
        Ui.fillNavigation(this, navigation, page, index -> {
            if (index == 1) library("quran");
            else if (index == 2) library("azkar");
            else if (index == 3) launch(ShiaHadithActivity.class);
            else { page = index; render(); }
        });
    }
    private void library(String section) {
        if ("hadith".equals(section)) { launch(ShiaHadithActivity.class); return; }
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
