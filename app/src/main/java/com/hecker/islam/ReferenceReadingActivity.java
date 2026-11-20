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
import android.animation.ValueAnimator;
import android.transition.Fade;
import android.transition.TransitionManager;
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
    private LinearLayout content, navigation;
    private ScrollView scroll;
    private String section, query = "";
    private int stage = 0, category = -1, chapter = -1;
    private JSONObject selectedHadith;
    private TextView recitationControl, quranSpeedControl;
    private final android.content.BroadcastReceiver recitationUpdates =
            new android.content.BroadcastReceiver() {
                @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
                    updateRecitationControl();
                }
            };
    private long generation = 0;
    private JSONArray chapters, verses, azkarCategories, azkarChapters, azkarItems, hadithCategories;
    private java.util.Map<Integer, String> urduTranslations;
    private String loadedUrduEdition, loadingUrduEdition;
    private boolean urduMissing;


    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        section = getIntent().getStringExtra("section");
        if ("hadith".equals(section)) { startActivity(new android.content.Intent(this, ShiaHadithActivity.class)); finish(); return; }
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
        navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 11));
        android.graphics.drawable.GradientDrawable surface = new android.graphics.drawable.GradientDrawable();
        surface.setColor(Ui.CARD);
        surface.setCornerRadius(Ui.dp(this, 35));
        surface.setStroke(Ui.dp(this, 1), Ui.OUTLINE);
        navigation.setBackground(surface);
        navigation.setElevation(Ui.dp(this, 10));
        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 72));
        navParams.setMargins(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 12));
        root.addView(navigation, navParams);
        setContentView(root);
        showNavigation();
        if (!ReferenceLibrary.ready(this)) {
            content.addView(Ui.text(this, "Offline content missing from build", 25, Ui.WHITE, true));
            Ui.space(this, content, 15);
            content.addView(Ui.text(this, "This APK was compiled without its mandatory bundled content. Install a complete standalone release build.", 16, Ui.MUTED, false));
            return;
        }
        loadSection();
    }
    private void showNavigation() {
        int selected = "quran".equals(section) ? 1 : "azkar".equals(section) ? 2 : 3;
        Ui.fillNavigation(this, navigation, selected, index -> {
            if (index >= 1 && index <= 3) {
                if (index == 3) { startActivity(new android.content.Intent(this, ShiaHadithActivity.class)); finish(); return; }
                String next = index == 1 ? "quran" : index == 2 ? "azkar" : "hadith";
                if (!next.equals(section)) {
                    section = next; stage = 0; chapter = -1; category = -1; query = "";
                    selectedHadith = null;
                    stopAudio();
                    showNavigation();
                    loadSection();
                }
            } else {
                android.content.Intent home = new android.content.Intent(this, IslamDashboardActivity.class);
                home.putExtra("tab", index);
                home.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP |
                              android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(home);
                overridePendingTransition(0, 0);
                finish();
            }
        });
    }
    private void loadSection() {
        try {
            if ("quran".equals(section)) {
                if (chapters == null) chapters = ReferenceLibrary.array(this, "quran/chapters.json");
                if (verses == null) verses = ReferenceLibrary.array(this, "quran/quran_uthmani.json");
            } else if ("azkar".equals(section)) {
                if (azkarCategories == null) azkarCategories = ReferenceLibrary.array(this, "azkar/categories.json");
                if (azkarChapters == null) azkarChapters = ReferenceLibrary.array(this, "azkar/chapters.json");
                if (azkarItems == null) azkarItems = ReferenceLibrary.array(this, "azkar/items.json");
            } else {
                if (hadithCategories == null) hadithCategories = ReferenceLibrary.array(this, "hadith/categories.json");
            }
            draw();
            if ("quran".equals(section)) loadUrduTranslation();
        } catch (Exception ex) {
            content.removeAllViews();
            content.addView(Ui.text(this, "Cannot load library: " + ex.getMessage(), 16, Ui.MUTED, false));
        }
    }
    @Override public void onBackPressed() {
        stopAudio();
        if (stage > 0) {
            if ("azkar".equals(section) && stage == 2) stage = 1;
            else stage = 0;
            selectedHadith = null; query = ""; draw();
        } else super.onBackPressed();
    }
    @Override protected void onStart() {
        super.onStart();
        android.content.IntentFilter filter = new android.content.IntentFilter(RecitationService.STATE_CHANGED);
        if (android.os.Build.VERSION.SDK_INT >= 33)
            registerReceiver(recitationUpdates, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(recitationUpdates, filter);
        updateRecitationControl();
    }
    @Override protected void onStop() {
        unregisterReceiver(recitationUpdates);
        super.onStop();
    }
    @Override protected void onDestroy() {
        // Deliberately leave playback running if the screen locks or Activity is destroyed.
        generation++;
        worker.shutdownNow();
        super.onDestroy();
    }
    private void draw() {
        if (content != null && ValueAnimator.areAnimatorsEnabled()) {
            Fade fade = new Fade();
            fade.setDuration(260);
            TransitionManager.beginDelayedTransition(content, fade);
        }
        generation++;
        recitationControl = null;
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
    private TextView button(String text, Runnable action) {
        LinearLayout p = Ui.panel(this);
        TextView t = Ui.text(this, text, 15, Ui.GOLD, true);
        t.setGravity(Gravity.CENTER); p.addView(t);
        p.setOnClickListener(v -> action.run());
        content.addView(p); Ui.space(this, content, 12);
        return t;
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

    /** The player lives in RecitationService, not in this Activity. */
    private void updateRecitationControl() {
        if (recitationControl == null) return;
        if (RecitationService.matchesQuran(this, chapter)) {
            recitationControl.setText(RecitationService.isLoading(this)
                    ? "◌ Connecting to reciter…"
                    : RecitationService.isPlaying(this)
                    ? "Ⅱ Pause • Mishary Alafasy" : "▶ Resume • Mishary Alafasy");
        } else {
            recitationControl.setText("▶ Listen • Mishary Alafasy");
        }
    }
    private void stopAudio() {
        RecitationService.stop(this);
        updateRecitationControl();
    }
    private void playSurah() {
        if (chapter < 1 || chapter > 114) return;
        RecitationService.toggleQuran(this, chapter, quranSpeed());
    }
    private static final float[] QURAN_SPEEDS = {0.75f, 1.00f, 1.25f, 1.50f, 2.00f};
    private float quranSpeed() {
        float value = saved.getFloat("quran_playback_speed", 1.0f);
        return value >= 0.75f && value <= 2.0f ? value : 1.0f;
    }
    private String quranSpeedLabel() {
        return String.format(Locale.ENGLISH, "%.2fx", quranSpeed());
    }
    private void chooseQuranSpeed() {
        final String[] options = {"0.75x • Slower", "1.00x • Normal", "1.25x • Faster", "1.50x • Fast", "2.00x • Very fast"};
        int selected = 1;
        for (int i = 0; i < QURAN_SPEEDS.length; i++) {
            if (Math.abs(QURAN_SPEEDS[i] - quranSpeed()) < 0.01f) { selected = i; break; }
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle("Qur'an recitation speed")
                .setSingleChoiceItems(options, selected, (dialog, choice) -> {
                    float newRate = QURAN_SPEEDS[choice];
                    saved.edit().putFloat("quran_playback_speed", newRate).apply();
                    RecitationService.speed(this, newRate);
                    if (quranSpeedControl != null)
                        quranSpeedControl.setText("Playback speed • " + quranSpeedLabel() + "  ›");
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null).show();
    }
    private void speakDua(String arabic) {
        if (arabic == null || arabic.trim().isEmpty()) return;
        RecitationService.toggleText(this, arabic, Locale.forLanguageTag("ar-SA"), 0.80f);
    }
    private String urduEdition() {
        return "najafi".equals(saved.getString("urduEdition", "jawadi")) ? "najafi" : "jawadi";
    }
    private String urduTranslator() {
        return "najafi".equals(urduEdition()) ? "Allama Muhammad Hussain Najafi" : "Allama Zeeshan Haider Jawadi";
    }
    private boolean showUrdu() { return saved.getBoolean("showUrdu", true); }

    /** Read pre-bundled text off the UI thread. No account, import, or internet is required. */
    private void loadUrduTranslation() {
        if (!"quran".equals(section) || !showUrdu()) return;
        final String edition = urduEdition();
        if (edition.equals(loadedUrduEdition) || edition.equals(loadingUrduEdition)) return;
        loadingUrduEdition = edition;
        urduMissing = false;
        worker.execute(() -> {
            java.util.Map<Integer, String> results = new java.util.HashMap<>();
            String failure = null;
            try {
                JSONArray data = ReferenceLibrary.array(this, "quran/ur_" + edition + ".json");
                if (data.length() != 6236) throw new java.io.IOException("Incorrect translation verse count");
                for (int i = 0; i < data.length(); i++) {
                    JSONArray item = data.getJSONArray(i);
                    int surah = item.getInt(0), ayah = item.getInt(1);
                    String translated = item.getString(2);
                    if (surah < 1 || surah > 114 || ayah < 1 || ayah > 286 || translated.trim().isEmpty())
                        throw new java.io.IOException("Invalid translated verse");
                    if (results.put(surah * 1000 + ayah, translated) != null)
                        throw new java.io.IOException("Duplicate translated verse");
                }
            } catch (Exception ex) { failure = ex.getMessage(); }
            final String message = failure;
            main.post(() -> {
                if (isFinishing() || isDestroyed() || !edition.equals(loadingUrduEdition)) return;
                loadingUrduEdition = null;
                if (message == null) {
                    urduTranslations = results;
                    loadedUrduEdition = edition;
                    urduMissing = false;
                } else {
                    urduTranslations = null;
                    loadedUrduEdition = null;
                    urduMissing = true;
                    android.util.Log.w("HeckerIslamQuran", "Urdu translation unavailable: " + message);
                }
                if ("quran".equals(section) && stage == 1 && showUrdu()) draw();
            });
        });
    }

    private void selectUrduTranslator() {
        final String[] choices = {"علامہ ذیشان حیدر جوادی", "علامہ محمد حسین نجفی"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Urdu Qur'an translation")
                .setSingleChoiceItems(choices, "najafi".equals(urduEdition()) ? 1 : 0,
                        (dialog, chosen) -> {
                            saved.edit().putString("urduEdition", chosen == 1 ? "najafi" : "jawadi").apply();
                            dialog.dismiss();
                            urduTranslations = null; loadedUrduEdition = null; urduMissing = false;
                            loadUrduTranslation(); draw();
                        })
                .setNegativeButton("Cancel", null).show();
    }

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
                    stopAudio(); chapter = id; stage = 1; query = ""; draw();
                });
            }
        } else if (stage == 1) {
            JSONObject c = chapters.optJSONObject(chapter - 1);
            if (c != null) {
                content.addView(Ui.text(this, c.optString("name_simple") + " • " + c.optString("name_arabic"), 23, Ui.GOLD, true));
                Ui.space(this, content, 14);
            }
            button("Search verses", () -> { stage = 2; query = ""; stopAudio(); draw(); });
            button(showUrdu() ? "اردو ترجمہ ✓ • Hide translation" : "اردو ترجمہ • Show translation", () -> {
                saved.edit().putBoolean("showUrdu", !showUrdu()).apply();
                if (showUrdu()) loadUrduTranslation();
                draw();
            });
            if (showUrdu()) {
                button("Translator: " + urduTranslator() + " • Change", this::selectUrduTranslator);
                content.addView(Ui.text(this,
                        "Shia Urdu translation • AlQuran Cloud • Translator credited. Arabic remains the original text.",
                        12, Ui.MUTED, false));
                Ui.space(this, content, 12);
                if (urduTranslations == null) {
                    content.addView(Ui.text(this,
                            urduMissing ? "Urdu translation missing from this APK. Install the complete build."
                                    : "Loading included Urdu translation…",
                            13, Ui.MUTED, false));
                    Ui.space(this, content, 12);
                }
            }
            recitationControl = button("▶ Listen • Mishary Alafasy", this::playSurah);
            updateRecitationControl();
            quranSpeedControl = button("Playback speed • " + quranSpeedLabel() + "  ›",
                    this::chooseQuranSpeed);
            content.addView(Ui.text(this, "Recorded reciter playback requires internet. Qur’an text remains available offline.", 12, Ui.MUTED, false));
            Ui.space(this, content, 16);
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
                if (showUrdu() && urduTranslations != null && urduEdition().equals(loadedUrduEdition)) {
                    String translated = urduTranslations.get(chapter * 1000 + num);
                    if (translated != null) {
                        Ui.space(this, p, 12);
                        TextView urdu = Ui.text(this, translated, 18, Ui.MUTED, false);
                        urdu.setTextDirection(View.TEXT_DIRECTION_RTL);
                        urdu.setGravity(Gravity.RIGHT);
                        urdu.setLineSpacing(Ui.dp(this, 6), 1.12f);
                        p.addView(urdu);
                    }
                }
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
                    chapter = id; stage = 2; stopAudio(); draw();
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
                TextView audio = Ui.text(this, "▶ Listen to Dua • Arabic voice", 14, Ui.GOLD, true);
                Ui.space(this, p, 12); p.addView(audio);
                audio.setOnClickListener(v -> speakDua(o.optString("ar")));
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
            java.util.Set<Integer> included = hadithDescendants(cat);
            worker.execute(() -> {
                List<JSONObject> results = new ArrayList<>();
                String error = null;
                try (BufferedReader in = new BufferedReader(new InputStreamReader(
                        ReferenceLibrary.hadithStream(this), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null && results.size() < 60 && !Thread.currentThread().isInterrupted()) {
                        JSONObject hadith = new JSONObject(line);
                        JSONArray categories = hadith.optJSONArray("c");
                        if (cat != -1 && (categories == null || !containsAny(categories, included))) continue;
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
    /** Parent topics include all nested Hadith leaf-category IDs. */
    private java.util.Set<Integer> hadithDescendants(int parentId) {
        java.util.Set<Integer> matches = new java.util.HashSet<>();
        if (parentId < 0 || hadithCategories == null) return matches;
        java.util.Map<Integer, Integer> parents = new java.util.HashMap<>();
        for (int i = 0; i < hadithCategories.length(); i++) {
            JSONObject c = hadithCategories.optJSONObject(i);
            if (c != null) parents.put(c.optInt("id"), c.optInt("p", -1));
        }
        matches.add(parentId);
        for (Integer id : parents.keySet()) {
            int cursor = id;
            java.util.Set<Integer> seen = new java.util.HashSet<>();
            while (cursor > 0 && seen.add(cursor)) {
                if (cursor == parentId) { matches.add(id); break; }
                Integer next = parents.get(cursor);
                if (next == null || next == cursor) break;
                cursor = next;
            }
        }
        return matches;
    }
    private static boolean containsAny(JSONArray data, java.util.Set<Integer> allowed) {
        for (int i = 0; i < data.length(); i++)
            if (allowed.contains(data.optInt(i))) return true;
        return false;
    }
}
