package com.hecker.islam;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.hecker.azaan.AppSettings;
import com.hecker.azaan.Prayer;
import com.hecker.azaan.PrayerCalculator;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public final class HomeActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView location, next, nextDetails;
    private final Runnable ticker = new Runnable() {
        @Override public void run() { updatePrayer(); handler.postDelayed(this, 30000); }
    };
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BACKGROUND);
        Ui.rootInsets(root);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout content = Ui.column(this);
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 28), Ui.dp(this, 20), Ui.dp(this, 32));
        scroll.addView(content);
        content.addView(Ui.text(this, "HECKER ISLAM", 13, Ui.GOLD, true));
        Ui.space(this, content, 8);
        content.addView(Ui.text(this, "Faith. Time. Reflection.", 30, Ui.WHITE, true));
        Ui.space(this, content, 6);
        content.addView(Ui.text(this, LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)), 14, Ui.MUTED, false));
        Ui.space(this, content, 25);
        LinearLayout featured = Ui.panel(this);
        featured.addView(Ui.text(this, "UPCOMING PRAYER", 12, Ui.GOLD, true));
        Ui.space(this, featured, 12);
        next = Ui.text(this, "Set up prayer times", 25, Ui.WHITE, true);
        featured.addView(next);
        Ui.space(this, featured, 7);
        nextDetails = Ui.text(this, "", 15, Ui.MUTED, false);
        featured.addView(nextDetails);
        Ui.space(this, featured, 7);
        location = Ui.text(this, "", 13, Ui.MUTED, false);
        featured.addView(location);
        featured.setOnClickListener(v -> open(com.hecker.azaan.MainActivity.class));
        content.addView(featured);
        Ui.space(this, content, 28);
        content.addView(Ui.text(this, "Explore", 21, Ui.WHITE, true));
        Ui.space(this, content, 12);
        entry(content, "Azaan & Prayer Times", "Three daily announcements, voices, countdown and settings",
                com.hecker.azaan.MainActivity.class);
        entry(content, "Islamic Calendar", "Gregorian • Hijri • Desi and religious occasions",
                com.sabber.taqwim.MainActivity.class);
        entry(content, "Qibla Compass", "Find the direction to the Kaaba",
                QiblaActivity.class);
        entry(content, "Tasbeeh", "Track your dhikr with a saved counter",
                TasbeehActivity.class);
        Ui.space(this, content, 19);
        content.addView(Ui.text(this, "More to come", 19, Ui.WHITE, true));
        Ui.space(this, content, 8);
        TextView upcoming = Ui.text(this,
                "Qur'an reader • Hadith • Azkar • Prayer tracker • Widgets • Backup & restore",
                14, Ui.MUTED, false);
        upcoming.setLineSpacing(Ui.dp(this, 3), 1f);
        content.addView(upcoming);
        Ui.space(this, content, 14);
        TextView notice = Ui.text(this, "Early integration build — your original calendar and Azaan features are preserved.",
                12, Ui.MUTED, false);
        content.addView(notice);
        setContentView(root);
    }
    private void entry(LinearLayout parent, String title, String detail, Class<?> screen) {
        LinearLayout card = Ui.panel(this);
        TextView t = Ui.text(this, title + "   ›", 19, Ui.WHITE, true);
        card.addView(t);
        Ui.space(this, card, 5);
        card.addView(Ui.text(this, detail, 13, Ui.MUTED, false));
        card.setClickable(true);
        card.setOnClickListener(v -> open(screen));
        parent.addView(card);
        Ui.space(this, parent, 12);
    }
    private void open(Class<?> screen) { startActivity(new Intent(this, screen)); }
    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        ticker.run();
    }
    @Override protected void onPause() { handler.removeCallbacks(ticker); super.onPause(); }
    private void updatePrayer() {
        if (next == null) return;
        AppSettings settings = new AppSettings(this);
        if (!settings.configured()) {
            next.setText("Set up prayer times");
            nextDetails.setText("Tap to choose your city and calculation method.");
            location.setText("Azaan is not configured yet");
            return;
        }
        location.setText(settings.city() + " · " + settings.method().title);
        Instant now = Instant.now(), first = null;
        Prayer prayer = null;
        LocalDate date = LocalDate.now(settings.zone());
        try {
            for (int d = 0; d < 3; d++) {
                Map<Prayer, Instant> times = PrayerCalculator.calculate(date.plusDays(d),
                        settings.latitude(), settings.longitude(), settings.method(), settings.offsets());
                for (Prayer p : Prayer.values()) {
                    Instant t = times.get(p);
                    if (t != null && t.isAfter(now) && (first == null || t.isBefore(first))) {
                        first = t; prayer = p;
                    }
                }
            }
            if (first == null) {
                next.setText("No time available");
                nextDetails.setText("Check location and local prayer calculation.");
            } else {
                next.setText(prayer.title);
                long seconds = Math.max(0, Duration.between(now, first).getSeconds());
                long hours = seconds / 3600, minutes = (seconds % 3600 + 59) / 60;
                String when = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
                        .withZone(settings.zone()).format(first);
                nextDetails.setText(when + "  ·  in " + hours + "h " + minutes + "m"
                        + (settings.active() ? "  ·  Azaan enabled" : "  ·  Azaan off"));
            }
        } catch (RuntimeException ex) {
            next.setText("Prayer times unavailable");
            nextDetails.setText("Review your location in Azaan settings.");
        }
    }
}
