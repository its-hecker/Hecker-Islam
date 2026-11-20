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
    private android.media.MediaPlayer surahPlayer;
    private android.speech.tts.TextToSpeech arabicSpeech;
    private TextView recitationControl;
    private Runnable recitationTimeout;
    private boolean audioLoading, audioPlaying;
    private int recitationSource;
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
    @Override protected void onDestroy() {
        stopAudio();
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

    private void clearRecitationTimeout() {
        if (recitationTimeout != null) {
            main.removeCallbacks(recitationTimeout);
            recitationTimeout = null;
        }
    }

    private void releaseSurahPlayer() {
        clearRecitationTimeout();
        android.media.MediaPlayer old = surahPlayer;
        surahPlayer = null;
        audioLoading = false;
        audioPlaying = false;
        if (old != null) {
            try { old.release(); }
            catch (RuntimeException ignored) { }
        }
    }

    private void updateRecitationControl() {
        if (recitationControl == null) return;
        if (audioLoading) {
            recitationControl.setText(recitationSource > 1
                    ? "◌ Trying backup recitation source…" : "◌ Connecting to reciter…");
        } else if (audioPlaying) {
            recitationControl.setText("■ Stop • Mishary Alafasy");
        } else {
            recitationControl.setText("▶ Listen • Mishary Alafasy");
        }
    }

    private void stopAudio() {
        releaseSurahPlayer();
        if (arabicSpeech != null) {
            arabicSpeech.stop();
            arabicSpeech.shutdown();
            arabicSpeech = null;
        }
        updateRecitationControl();
    }

    /** Independent HTTPS streams: avoid depending on one occasionally broken CDN. */
    private static String recitationUrl(int number, int source) {
        String surah = String.format(Locale.US, "%03d.mp3", number);
        if (source == 0) return "https://server8.mp3quran.net/afs/" + surah;
        if (source == 1) return "https://cdn.mp3quran.net/audio/mishary-alafasy/r1/" + surah;
        return "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/" + number + ".mp3";
    }

    private void connectRecitation(int number, int source) {
        releaseSurahPlayer();
        if (isFinishing() || isDestroyed() || !"quran".equals(section) || stage != 1) return;
        if (source >= 3) {
            updateRecitationControl();
            android.widget.Toast.makeText(this,
                    "Unable to reach a recitation server. Check your connection and try again.",
                    android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        recitationSource = source + 1;
        audioLoading = true;
        updateRecitationControl();
        try {
            final android.media.MediaPlayer player = new android.media.MediaPlayer();
            surahPlayer = player;
            player.setAudioAttributes(new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build());
            // String data source allows MediaPlayer's HTTP/HTTPS streaming implementation
            // to follow the remote MP3 URL directly.
            player.setDataSource(recitationUrl(number, source));
            player.setOnPreparedListener(prepared -> {
                if (surahPlayer != prepared || isFinishing() || isDestroyed()) return;
                clearRecitationTimeout();
                try {
                    prepared.start();
                    audioLoading = false;
                    audioPlaying = true;
                    updateRecitationControl();
                } catch (RuntimeException ex) {
                    android.util.Log.w("HeckerIslamQuran", "Unable to start stream", ex);
                    connectRecitation(number, source + 1);
                }
            });
            player.setOnCompletionListener(ended -> {
                if (surahPlayer == ended) stopAudio();
            });
            player.setOnErrorListener((failed, what, extra) -> {
                if (surahPlayer == failed) {
                    android.util.Log.w("HeckerIslamQuran",
                            "Audio source " + (source + 1) + " failed (" + what + "/" + extra + ")");
                    connectRecitation(number, source + 1);
                }
                return true;
            });
            player.prepareAsync();
            recitationTimeout = () -> {
                if (surahPlayer == player && audioLoading) {
                    android.util.Log.w("HeckerIslamQuran", "Audio source timed out: " + (source + 1));
                    connectRecitation(number, source + 1);
                }
            };
            main.postDelayed(recitationTimeout, 12000);
        } catch (Exception ex) {
            android.util.Log.w("HeckerIslamQuran", "Cannot prepare audio source", ex);
            connectRecitation(number, source + 1);
        }
    }

    /** Start or stop a recorded Qur'an Surah. The written library remains offline. */
    private void playSurah() {
        if (surahPlayer != null || audioLoading) {
            stopAudio();
            return;
        }
        if (chapter < 1 || chapter > 114) return;
        stopAudio();
        connectRecitation(chapter, 0);
    }

    /** Duas have no licensed studio recordings bundled; use the available Arabic speech voice. */
    private void speakDua(String arabic) {
        if (arabic == null || arabic.trim().isEmpty()) return;
        stopAudio();
        arabicSpeech = new android.speech.tts.TextToSpeech(this, status -> {
            if (isFinishing() || arabicSpeech == null) return;
            if (status != android.speech.tts.TextToSpeech.SUCCESS) {
                android.widget.Toast.makeText(this, "Arabic voice is not available on this device.",
                        android.widget.Toast.LENGTH_LONG).show();
                stopAudio(); return;
            }
            int supported = arabicSpeech.setLanguage(java.util.Locale.forLanguageTag("ar-SA"));
            if (supported == android.speech.tts.TextToSpeech.LANG_MISSING_DATA ||
                supported == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                android.widget.Toast.makeText(this,
                        "Install an Arabic speech voice in Android's Text-to-speech settings.",
                        android.widget.Toast.LENGTH_LONG).show();
                stopAudio(); return;
            }
            arabicSpeech.setSpeechRate(.80f);
            arabicSpeech.speak(arabic, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "dua-" + System.nanoTime());
        });
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
            recitationControl = button("▶ Listen • Mishary Alafasy", this::playSurah);
            updateRecitationControl();
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
