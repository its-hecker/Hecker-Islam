package com.hecker.islam;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;

/**
 * Dedicated offline daily ziyarat reader: seven weekdays, nine traditional
 * readings, Arabic originals and newly written Urdu translations.
 * Editorial content is loaded from assets/ziaraat/daily.json.
 */
public final class DailyZiaraatActivity extends Activity {
    public static final String EXTRA_OPEN_TODAY = "open_today_ziyarat";

    /** Android Calendar indexes Sunday as 1 and Saturday as 7. */
    public static String labelForDate(java.time.LocalDate date) {
        return DAY_LABELS[(date.getDayOfWeek().getValue() % 7) + 1];
    }
    private static final String[] EN_DAYS = {"", "Sunday", "Monday", "Tuesday",
            "Wednesday", "Thursday", "Friday", "Saturday"};
    private static final String[] UR_DAYS = {"", "اتوار", "پیر", "منگل",
            "بدھ", "جمعرات", "جمعہ", "ہفتہ"};
    private static final String[] DAY_LABELS = {"", "Imam Ali & Sayyida Fatima",
            "Imam Hasan & Imam Hussain", "Imams Sajjad, Baqir & Sadiq",
            "Imams Kazim, Reza, Jawad & Hadi", "Imam Hasan al-Askari",
            "Imam Mahdi (a.j.)", "Prophet Muhammad (s.a.w.w.)"};
    private final JSONArray[] byDay = new JSONArray[8];
    private JSONObject selected;
    private boolean focusToday;
    private ReaderSpeech reciter;
    private LinearLayout content;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        reciter = new ReaderSpeech(this);
        readCatalog();
        focusToday = getIntent().getBooleanExtra(EXTRA_OPEN_TODAY, false);
        if (focusToday) {
            int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
            // Open the reading immediately if only one Ziyarat exists for this day.
            // For days with multiple readings, present today's choices instead.
            if (byDay[today].length() == 1) {
                selected = byDay[today].optJSONObject(0);
            }
        }
        render();
    }

    private void readCatalog() {
        for (int d = 1; d <= 7; d++) byDay[d] = new JSONArray();
        try (InputStream stream = getAssets().open("ziaraat/daily.json");
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = stream.read(buffer)) > 0) out.write(buffer, 0, n);
            JSONObject catalog = new JSONObject(out.toString("UTF-8"));
            JSONArray readings = catalog.getJSONArray("ziaraat");
            for (int i = 0; i < readings.length(); i++) {
                JSONObject obj = readings.getJSONObject(i);
                int day = obj.getInt("day");
                if (day < 1 || day > 7 || obj.optString("arabic").isEmpty()
                        || obj.optString("urdu").isEmpty()) continue;
                byDay[day].put(obj);
            }
        } catch (Exception e) {
            Log.e("DailyZiaraat", "Unable to load weekday ziyarats", e);
        }
    }

    private TextView text(String value, int size, int color, boolean bold) {
        return Ui.text(this, value, size, color, bold);
    }

    private void addButton(LinearLayout parent, String label, Runnable action) {
        LinearLayout panel = Ui.panel(this);
        TextView t = text(label, 16, Ui.GOLD, true);
        t.setGravity(Gravity.CENTER);
        panel.addView(t);
        panel.setOnClickListener(v -> action.run());
        parent.addView(panel);
        Ui.space(this, parent, 10);
    }

    private void rtlPanel(String title, String body, int textSize) {
        LinearLayout panel = Ui.panel(this);
        TextView heading = text(title, 15, Ui.GOLD, true);
        heading.setGravity(Gravity.RIGHT);
        panel.addView(heading);
        Ui.space(this, panel, 15);
        TextView contentText = text(body, textSize, Ui.WHITE, false);
        contentText.setGravity(Gravity.RIGHT);
        contentText.setTextDirection(View.TEXT_DIRECTION_RTL);
        contentText.setLineSpacing(Ui.dp(this, 9), 1.2f);
        panel.addView(contentText);
        content.addView(panel);
        Ui.space(this, content, 15);
    }

    private void openSource(String url, String title) {
        if (url == null || !url.startsWith("https://")) return;
        Intent intent = new Intent(this, SourceWebActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("url", url);
        startActivity(intent);
    }

    private boolean starred(String id) {
        return getSharedPreferences("daily_ziaraat", MODE_PRIVATE)
                .getBoolean(id, false);
    }

    private void toggleStar(String id) {
        SharedPreferences prefs = getSharedPreferences("daily_ziaraat", MODE_PRIVATE);
        prefs.edit().putBoolean(id, !prefs.getBoolean(id, false)).apply();
        render();
    }

    private void choose(JSONObject obj) {
        reciter.stop();
        selected = obj;
        render();
    }

    @Override public void onBackPressed() {
        if (selected != null) {
            reciter.stop();
            selected = null;
            render();
        } else super.onBackPressed();
    }

    private void render() {
        Ui.applyTheme(this);
        getWindow().setStatusBarColor(Ui.BACKGROUND);
        getWindow().setNavigationBarColor(Ui.BACKGROUND);
        boolean dark = getSharedPreferences("islam_ui", MODE_PRIVATE).getBoolean("dark", true);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0
                : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BACKGROUND);
        Ui.rootInsets(root);
        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        root.addView(scroller, new LinearLayout.LayoutParams(-1, -1));
        content = Ui.column(this);
        content.setPadding(Ui.dp(this, 19), Ui.dp(this, 24), Ui.dp(this, 19), Ui.dp(this, 45));
        scroller.addView(content);
        setContentView(root);

        TextView back = text(selected == null ? "‹  Shia Duas" : "‹  Daily Ziaraat",
                16, Ui.GOLD, true);
        back.setOnClickListener(v -> onBackPressed());
        content.addView(back);
        Ui.space(this, content, 19);
        content.addView(text(selected == null ? "Daily Ziaraat" :
                selected.optString("name"), 27, Ui.WHITE, true));
        Ui.space(this, content, 7);
        content.addView(text(selected == null ? "ہفتے کے سات دن کی زیارات" :
                selected.optString("urduName"), 17, Ui.GOLD, false));
        Ui.space(this, content, 20);
        if (selected == null) renderIndex();
        else renderReading();
    }

    private void renderIndex() {
        int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        LinearLayout todayCard = Ui.panel(this);
        todayCard.addView(text("TODAY · " + EN_DAYS[today] + " / " + UR_DAYS[today],
                14, Ui.GOLD, true));
        Ui.space(this, todayCard, 7);
        todayCard.addView(text(DAY_LABELS[today], 18, Ui.WHITE, true));
        Ui.space(this, todayCard, 9);
        todayCard.addView(text("Read today's ziyarat below. You can open any other day too.",
                13, Ui.MUTED, false));
        content.addView(todayCard);
        Ui.space(this, content, 14);

        int daysToShow = focusToday ? 1 : 7;
        for (int offset = 0; offset < daysToShow; offset++) {
            int day = ((today - 1 + offset) % 7) + 1;
            LinearLayout heading = Ui.panel(this);
            heading.addView(text((day == today ? "●  " : "") +
                    EN_DAYS[day] + " · " + UR_DAYS[day], 19, Ui.GOLD, true));
            Ui.space(this, heading, 4);
            heading.addView(text(DAY_LABELS[day], 13, Ui.MUTED, false));
            content.addView(heading);
            Ui.space(this, content, 9);
            for (int j = 0; j < byDay[day].length(); j++) {
                JSONObject entry = byDay[day].optJSONObject(j);
                if (entry == null) continue;
                LinearLayout card = Ui.panel(this);
                card.addView(text(entry.optString("name") + "   ›", 17, Ui.WHITE, true));
                Ui.space(this, card, 4);
                card.addView(text(entry.optString("urduName"), 16, Ui.GOLD, false));
                Ui.space(this, card, 7);
                card.addView(text("Arabic & Urdu offline" +
                        (starred(entry.optString("id")) ? " · ★ Saved" : ""), 12, Ui.MUTED, false));
                card.setOnClickListener(v -> choose(entry));
                content.addView(card);
                Ui.space(this, content, 9);
            }
            Ui.space(this, content, 8);
        }
        if (focusToday) {
            addButton(content, "Browse all seven days  ›", () -> {
                focusToday = false;
                render();
            });
        }
        content.addView(text("Source: Mafatih al-Jinan daily ziaraat; Arabic checked against " +
                "Duas.org and Ahkaam.org. Urdu meanings are prepared for this app. " +
                "Ziyarat is recommended devotion, not an obligatory part of namaz.",
                12, Ui.MUTED, false));
    }

    private void renderReading() {
        String arabic = selected.optString("arabic");
        String urdu = selected.optString("urdu");
        String id = selected.optString("id");
        String name = selected.optString("name");
        content.addView(text("Arabic original · Original Urdu translation", 14, Ui.MUTED, false));
        Ui.space(this, content, 14);
        LinearLayout audio = Ui.panel(this);
        audio.addView(text("RECITATION · DEVICE VOICES", 12, Ui.GOLD, true));
        Ui.space(this, audio, 12);
        TextView arabicPlay = text("▶ Recite Arabic", 17, Ui.WHITE, true);
        arabicPlay.setPadding(0, Ui.dp(this, 9), 0, Ui.dp(this, 9));
        arabicPlay.setOnClickListener(v -> reciter.playOrStop(arabic, new Locale("ar")));
        reciter.attach(arabicPlay);
        audio.addView(arabicPlay);
        Ui.space(this, audio, 10);
        TextView urduPlay = text("▶ Recite Urdu translation", 17, Ui.GOLD, true);
        urduPlay.setPadding(0, Ui.dp(this, 9), 0, Ui.dp(this, 9));
        urduPlay.setOnClickListener(v -> reciter.speak(urdu, new Locale("ur", "PK")));
        audio.addView(urduPlay);
        Ui.space(this, audio, 12);
        TextView speed = text("Recitation speed · " + reciter.speedLabel() + "  ›",
                15, Ui.GOLD, true);
        speed.setOnClickListener(v -> reciter.chooseSpeed(speed));
        audio.addView(speed);
        Ui.space(this, audio, 9);
        audio.addView(text("Speech uses installed Android voices, not recorded reciters.",
                12, Ui.MUTED, false));
        content.addView(audio);
        Ui.space(this, content, 13);
        // Recitation and playback controls precede the long bilingual reading.
        rtlPanel("عربی متن", arabic, 24);
        rtlPanel("اردو ترجمہ", urdu, 20);


        addButton(content, starred(id) ? "★ Remove from saved" : "☆ Save ziyarat",
                () -> toggleStar(id));
        addButton(content, "Read the source online ›",
                () -> openSource(selected.optString("source"), name));
        content.addView(text("This Urdu translation is an original explanatory rendering. " +
                "For differences of wording or traditional variants, consult the linked source.",
                12, Ui.MUTED, false));
    }

    @Override protected void onDestroy() {
        if (reciter != null) reciter.destroy();
        super.onDestroy();
    }
}
