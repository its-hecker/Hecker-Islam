package com.hecker.islam;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** A private, date-indexed record of the five daily Salah. */
public final class PrayerTrackerActivity extends Activity {
    private static final String[] PRAYERS = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
    private SharedPreferences data;
    private LocalDate day;
    private LinearLayout content;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        data = getSharedPreferences("prayer_tracker", MODE_PRIVATE);
        day = LocalDate.now();
        Ui.applyTheme(this);
        LinearLayout root = Ui.column(this); root.setBackgroundColor(Ui.BACKGROUND);
        Ui.rootInsets(root);
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        content = Ui.column(this);
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 25), Ui.dp(this, 20), Ui.dp(this, 30));
        scroll.addView(content);
        setContentView(root); render();
    }
    private String key(String prayer) { return day.toString() + "_" + prayer; }
    private void render() {
        content.removeAllViews();
        TextView back = Ui.text(this, "‹  Hecker Islam", 15, Ui.GOLD, true);
        back.setOnClickListener(v -> finish()); content.addView(back);
        Ui.space(this, content, 24);
        content.addView(Ui.text(this, "Prayer tracker", 31, Ui.WHITE, true));
        Ui.space(this, content, 6);
        content.addView(Ui.text(this, "Track each Salah, privately on your phone", 14, Ui.MUTED, false));
        Ui.space(this, content, 24);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView prev = Ui.text(this, "‹", 28, Ui.GOLD, true);
        prev.setGravity(Gravity.CENTER);
        row.addView(prev, new LinearLayout.LayoutParams(Ui.dp(this, 47), Ui.dp(this, 56)));
        prev.setOnClickListener(v -> { day = day.minusDays(1); render(); });
        TextView date = Ui.text(this, day.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)),
                18, Ui.WHITE, true);
        date.setGravity(Gravity.CENTER);
        row.addView(date, new LinearLayout.LayoutParams(0, Ui.dp(this, 55), 1));
        TextView next = Ui.text(this, "›", 28, Ui.GOLD, true);
        next.setGravity(Gravity.CENTER);
        row.addView(next, new LinearLayout.LayoutParams(Ui.dp(this, 47), Ui.dp(this, 56)));
        next.setOnClickListener(v -> {
            if (day.isBefore(LocalDate.now())) { day = day.plusDays(1); render(); }
        });
        content.addView(row);
        Ui.space(this, content, 15);
        int count = 0;
        for (String prayer : PRAYERS) if (data.getBoolean(key(prayer), false)) count++;
        LinearLayout summary = Ui.panel(this);
        summary.addView(Ui.text(this, count + " of 5 prayers recorded", 21, Ui.GOLD, true));
        Ui.space(this, summary, 6);
        summary.addView(Ui.text(this, "Record each prayer when completed.", 13, Ui.MUTED, false));
        content.addView(summary);
        Ui.space(this, content, 14);
        for (String prayer : PRAYERS) {
            LinearLayout card = Ui.panel(this);
            CheckBox box = new CheckBox(this);
            box.setText(prayer);
            box.setTextColor(Ui.WHITE);
            box.setTextSize(19);
            box.setChecked(data.getBoolean(key(prayer), false));
            box.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.GOLD));
            box.setOnCheckedChangeListener((button, checked) -> {
                data.edit().putBoolean(key(prayer), checked).apply();
                // Refresh counters without interfering with the user's tap.
                content.post(this::render);
            });
            card.addView(box);
            content.addView(card);
            Ui.space(this, content, 11);
        }
        Ui.space(this, content, 14);
        content.addView(Ui.text(this,
                "Your prayer history is stored on this device. No accounts or tracking services are used.",
                13, Ui.MUTED, false));
    }
}
