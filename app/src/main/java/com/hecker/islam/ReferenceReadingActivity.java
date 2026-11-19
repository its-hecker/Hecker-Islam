package com.hecker.islam;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ReferenceReadingActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private SharedPreferences saved;
    private LinearLayout content;
    private ScrollView scroll;
    private String section, query = "";
    private int stage = 0, category = -1, chapter = -1;
    private JSONObject selectedHadith;
    private long generation = 0;
    private JSONArray chapters, verses, azkarCategories, azkarChapters, azkarItems, hadithCategories;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        section = getIntent().getStringExtra("section");
        if (!"quran".equals(section) && !"azkar".equals(section) && !"hadith".equals(section))
            section = "quran";
        saved = getSharedPreferences("islam_library", MODE_PRIVATE);
        Ui.applyTheme(this);
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BACKGROUND); Ui.rootInsets(root);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        content = Ui.column(this);
        content.setPadding(Ui.dp(this, 19), Ui.dp(this, 22), Ui.dp(this, 19), Ui.dp(this, 35));
        scroll.addView(content);
        setContentView(root);
        if (!ReferenceLibrary.ready(this)) {
            content.addView(Ui.text(this, "Offline library not installed", 25, Ui.WHITE, true));
            Ui.space(this, content, 15);
            content.addView(Ui.text(this, "Import the authorized Salat Times APK or Hecker Islam content pack from the Home screen. Once imported, all reading works offline.", 16, Ui.MUTED, false));
            return;
        }
        try {
            if ("quran".equals(section)) {
                chapters = ReferenceLibrary.array(this, "quran/chapters.json");
                verses = ReferenceLibrary.array(this, "quran/quran_uthmani.json");
            } else if ("azkar".equals(section)) {
                azkarCategories = ReferenceLibrary.array(this, "azkar/categories.json");
                azkarChapters = ReferenceLibrary.array(this, "azkar/chapters.json");
                azkarItems = ReferenceLibrary.array(this, "azkar/items.json");
            } else hadithCategories = ReferenceLibrary.array(this, "hadith/categories.json");
            draw();
        } catch (Exception ex) {
            content.addView(Ui.text(this, "Cannot load library: " + ex.getMessage(), 16, Ui.MUTED, false));
        }
    }
    @Override public void onBackPressed() {
        if (stage > 0) {
            if ("azkar".equals(section) && stage == 2) stage = 1;
            else stage = 0;
            selectedHadith = null; query = ""; draw();
        } else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        generation++;
        worker.shutdownNow();
        super.onDestroy();
    }
    private void draw() {
        generation++;
        content.removeAllViews();
        TextView back = Ui.text(this, "‹  " + (stage == 0 ? "Hecker Islam" : "Back"), 15, Ui.GOLD, true);
        back.setOnClickListener(v -> onBackPressed());
        content.addView(back);
        Ui.space(this, content, 20);
        String title = "quran".equals(section) ? "The Holy Qur'an"
                : "azkar".equals(section) ? "Azkar & Duas" : "Hadith Library";
        content.addView(Ui.text(this, title, 29, Ui.WHITE, true));
        Ui.space(this, content, 5);
        content.addView(Ui.text(this, "quran".equals(section) ? "114 Surahs • 6,236 verses"
                : "azkar".equals(section) ? "Daily remembrance, by topic"
                : "3,574 Hadith • 493 topics", 13, Ui.MUTED, false));
        Ui.space(this, content, 20);
        if ("quran".equals(section)) quran();
        if ("azkar".equals(section)) azkar();
        if ("hadith".equals(section)) hadith();
        scroll.smoothScrollTo(0, 0);
    }
    private void search(String hint, Runnable onSearch) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setTextColor(Ui.WHITE); field.setHintTextColor(Ui.MUTED);
        field.setTextSize(16); field.setHint(hint);
        field.setText(query);
        field.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), 0);
        LinearLayout holder = Ui.panel(this);
        holder.addView(field, new LinearLayout.LayoutParams(-1, Ui.dp(this, 54)));
        content.addView(holder);
        Ui.space(this, content, 12);
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { query = s.toString().trim(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        field.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        field.setOnEditorActionListener((v, action, event) -> { onSearch.run(); return true; });
        button("Search", onSearch);
    }
    private void card(String title, String subtitle, Runnable click) {
        LinearLayout p = Ui.panel(this);
        TextView t = Ui.text(this, title, 18, Ui.WHITE, true);
        p.addView(t);
        if (subtitle != null && !subtitle.isEmpty()) {
            Ui.space(this, p, 6);
            TextView sub = Ui.text(this, subtitle, 13, Ui.MUTED, false);
            sub.setMaxLines(3); p.addView(sub);
        }
        if (click != null) p.setOnClickListener(v -> click.run());
        content.addView(p); Ui.space(this, content, 10);
    }
    private void button(String text, Runnable action) {
        LinearLayout p = Ui.panel(this);
        TextView t = Ui.text(this, text, 15, Ui.GOLD, true);
        t.setGravity(Gravity.CENTER); p.addView(t);
        p.setOnClickListener(v -> action.run());
        content.addView(p); Ui.space(this, content, 12);
    }
    private String arabicText(String s, int size) {
        TextView t = Ui.text(this, s, size, Ui.WHITE, false);
        t.setGravity(Gravity.RIGHT); t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setLineSpacing(Ui.dp(this, 7), 1.12f);
        LinearLayout panel = Ui.panel(this);
        panel.addView(t);
        content.addView(panel); Ui.space(this, content, 12);
        return s;
    }
    private boolean bookmarked(String key) { return saved.getBoolean(key, false); }
    private void bookmark(String key) { saved.edit().putBoolean(key, !bookmarked(key)).apply(); draw(); }

    private void quran() {
        if (stage == 0) {
            search("Search Surah name", () -> { stage = 3; draw(); });
            if (stage != 0) return;
            for (int i = 0; i < chapters.length(); i++) {
                JSONObject c = chapters.optJSONObject(i);
                if (c == null) continue;
                int id = c.optInt("id");
                String title = c.optString("name_simple") + "  •  " + c.optString("name_arabic");
                String subtitle = c.optInt("verses_count") + " verses";
                card(String.format(Locale.ENGLISH, "%d. %s", id, title), subtitle, () -> {
                    chapter = id; stage = 1; query = ""; draw();
                });
            }
        } else if (stage == 1) {
            JSONObject c = chapters.optJSONObject(chapter - 1);
            if (c != null) {
                content.addView(Ui.text(this, c.optString("name_simple") + " • " + c.optString("name_arabic"), 23, Ui.GOLD, true));
                Ui.space(this, content, 14);
            }
            button("Search verses", () -> { stage = 2; query = ""; draw(); });
            int selected = 0;
            for (int i = 0; i < verses.length(); i++) {
                JSONObject v = verses.optJSONObject(i);
                if (v == null || v.optInt("ch") != chapter) continue;
                int num = v.optInt("vn"); selected++;
                LinearLayout p = Ui.panel(this);
                p.addView(Ui.text(this, String.valueOf(num), 13, Ui.GOLD, true));
                Ui.space(this, p, 10);
                TextView text = Ui.text(this, v.optString("t"), 24, Ui.WHITE, false);
                text.setTextDirection(View.TEXT_DIRECTION_RTL);
                text.setGravity(Gravity.RIGHT);
                text.setLineSpacing(Ui.dp(this, 9), 1.17f);
                p.addView(text);
                Ui.space(this, p, 10);
                String key = "verse_" + chapter + "_" + num;
                TextView star = Ui.text(this, bookmarked(key) ? "★ Saved" : "☆ Bookmark", 14, Ui.GOLD, true);
                p.addView(star); star.setOnClickListener(w -> bookmark(key));
                content.addView(p); Ui.space(this, content, 10);
            }
            if (selected == 0) card("No verses found", null, null);
        } else {
            search("Arabic verse or Surah name", () -> {
                String q = query.toLowerCase(Locale.ROOT);
                content.removeViews(4, Math.max(0, content.getChildCount() - 4));
                draw();
            });
            String q = query.toLowerCase(Locale.ROOT);
            if (q.isEmpty()) {
                card("Enter a phrase to search verses", "Search is performed offline on the original Arabic text.", null);
            } else {
                int shown = 0;
                for (int i = 0; i < verses.length() && shown < 70; i++) {
                    JSONObject v = verses.optJSONObject(i);
                    if (v == null || !v.optString("t").toLowerCase(Locale.ROOT).contains(q)) continue;
                    int ch = v.optInt("ch"), n = v.optInt("vn");
                    card(ch + ":" + n, v.optString("t"), () -> { chapter = ch; stage = 1; query = ""; draw(); });
                    shown++;
                }
                if (shown == 0) card("No matching verses", null, null);
            }
        }
    }
    private void azkar() {
        if (stage == 0) {
            for (int i = 0; i < azkarCategories.length(); i++) {
                JSONObject o = azkarCategories.optJSONObject(i);
                if (o == null) continue;
                int id = o.optInt("id");
                card(o.optString("en"), o.optString("ar"), () -> {
                    category = id; stage = 1; draw();
                });
            }
        } else if (stage == 1) {
            int shown = 0;
            for (int i = 0; i < azkarChapters.length(); i++) {
                JSONObject o = azkarChapters.optJSONObject(i);
                if (o == null || o.optInt("categoryId") != category) continue;
                int id = o.optInt("id");
                card(o.optString("en"), o.optString("ar"), () -> {
                    chapter = id; stage = 2; draw();
                }); shown++;
            }
            if (shown == 0) card("No chapters in this category", null, null);
        } else {
            int shown = 0;
            for (int i = 0; i < azkarItems.length(); i++) {
                JSONObject o = azkarItems.optJSONObject(i);
                if (o == null || o.optInt("chapterId") != chapter) continue;
                shown++;
                LinearLayout p = Ui.panel(this);
                TextView arabic = Ui.text(this, o.optString("ar"), 22, Ui.WHITE, false);
                arabic.setTextDirection(View.TEXT_DIRECTION_RTL); arabic.setGravity(Gravity.RIGHT);
                arabic.setLineSpacing(Ui.dp(this, 7), 1.15f);
                p.addView(arabic); Ui.space(this, p, 14);
                p.addView(Ui.text(this, o.optString("en"), 15, Ui.MUTED, false));
                Ui.space(this, p, 13);
                p.addView(Ui.text(this, "Repeat " + o.optInt("repeat", 1) + " time(s)", 13, Ui.GOLD, true));
                String ref = o.optJSONObject("ref") == null ? "" : o.optJSONObject("ref").optString("en");
                if (!ref.isEmpty()) { Ui.space(this, p, 8); p.addView(Ui.text(this, ref, 11, Ui.MUTED, false)); }
                String key = "dhikr_" + o.optInt("id");
                TextView mark = Ui.text(this, bookmarked(key) ? "★ Saved" : "☆ Save", 14, Ui.GOLD, true);
                Ui.space(this, p, 12); p.addView(mark);
                mark.setOnClickListener(v -> bookmark(key));
                content.addView(p); Ui.space(this, content, 12);
            }
            if (shown == 0) card("No entries found", null, null);
        }
    }
    private void hadith() {
        if (stage == 0) {
            search("Search Hadith text", () -> { stage = 1; category = -1; draw(); });
            for (int i = 0; i < hadithCategories.length(); i++) {
                JSONObject o = hadithCategories.optJSONObject(i);
                if (o == null || o.optInt("l") != 0) continue;
                int id = o.optInt("id");
                card(o.optString("en") + " ›", o.optString("ar"), () -> {
                    category = id; stage = 1; query = ""; draw();
                });
            }
        } else if (stage == 1) {
            search("Search within Hadith", () -> draw());
            content.addView(Ui.text(this, "Results (first 60)", 15, Ui.MUTED, false));
            Ui.space(this, content, 8);
            final long stamp = generation;
            String q = query.toLowerCase(Locale.ROOT);
            int cat = category;
            worker.execute(() -> {
                List<JSONObject> results = new ArrayList<>();
                String error = null;
                try (BufferedReader in = new BufferedReader(new InputStreamReader(
                        ReferenceLibrary.hadithStream(this), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null && results.size() < 60 && !Thread.currentThread().isInterrupted()) {
                        JSONObject hadith = new JSONObject(line);
                        JSONArray categories = hadith.optJSONArray("c");
                        if (cat != -1 && (categories == null || !contains(categories, cat))) continue;
                        JSONObject english = hadith.optJSONObject("en");
                        JSONObject arabic = hadith.optJSONObject("ar");
                        String value = (english == null ? "" : english.optString("t") + " " + english.optString("i"))
                                + " " + (arabic == null ? "" : arabic.optString("t"));
                        if (!q.isEmpty() && !value.toLowerCase(Locale.ROOT).contains(q)) continue;
                        results.add(hadith);
                    }
                } catch (Exception ex) { error = ex.getMessage(); }
                String finalError = error;
                main.post(() -> {
                    if (isFinishing() || stamp != generation) return;
                    if (finalError != null) { card("Could not read Hadith", finalError, null); return; }
                    if (results.isEmpty()) { card("No matching Hadith", "Try another phrase or category.", null); return; }
                    for (JSONObject h : results) {
                        JSONObject en = h.optJSONObject("en"), ar = h.optJSONObject("ar");
                        String title = "Hadith #" + h.optInt("id");
                        String detail = en == null ? (ar == null ? "" : ar.optString("t")) : en.optString("t");
                        card(title, detail, () -> {
                            selectedHadith = h; stage = 2; draw();
                        });
                    }
                });
            });
        } else {
            if (selectedHadith == null) { stage = 1; draw(); return; }
            int id = selectedHadith.optInt("id");
            content.addView(Ui.text(this, "Hadith #" + id, 23, Ui.GOLD, true));
            Ui.space(this, content, 14);
            JSONObject ar = selectedHadith.optJSONObject("ar");
            JSONObject en = selectedHadith.optJSONObject("en");
            if (ar != null) arabicText(ar.optString("h", ar.optString("t")), 23);
            if (en != null) {
                card("English translation", en.optString("h", en.optString("t")), null);
                if (!en.optString("e").isEmpty()) card("Explanation", en.optString("e"), null);
                if (!en.optString("r").isEmpty()) card("References", en.optString("r"), null);
            }
            String key = "hadith_" + id;
            button(bookmarked(key) ? "★ Remove bookmark" : "☆ Bookmark Hadith", () -> bookmark(key));
        }
    }
    private static boolean contains(JSONArray data, int value) {
        for (int i = 0; i < data.length(); i++) if (data.optInt(i) == value) return true;
        return false;
    }
}
