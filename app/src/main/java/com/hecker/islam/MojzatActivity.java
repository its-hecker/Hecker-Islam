package com.hecker.islam;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Devotional narrative of Mola Ali (A.S.) from the user's supplied printed Urdu pages.
 * A photographed book excerpt is not independently authenticated as a hadith.
 */
public final class MojzatActivity extends Activity {
    private JSONArray stories = new JSONArray();
    private String selectedId;
    private LinearLayout content;
    private TextView storyText;
    private ReaderSpeech reciter;
    private int fontSize = 20;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        readCatalog();
        reciter = new ReaderSpeech(this);
        if (state != null) {
            selectedId = state.getString("mojzat_story");
            fontSize = state.getInt("mojzat_font_size", 20);
        } else {
            // Dashboard shortcuts can open a specific story without skipping the catalog.
            String requested = getIntent().getStringExtra("mojzat_story");
            if (requested != null && find(requested) != null) selectedId = requested;
        }
        render();
    }
    @Override protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putString("mojzat_story", selectedId);
        b.putInt("mojzat_font_size", fontSize);
    }
    @Override protected void onDestroy() {
        if (reciter != null) reciter.destroy();
        super.onDestroy();
    }
    @Override public void onBackPressed() {
        if (selectedId != null) {
            reciter.stop();
            selectedId = null;
            render();
        } else super.onBackPressed();
    }

    private void readCatalog() {
        try (InputStream stream = getAssets().open("mojzat/catalog.json");
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[8192];
            int count;
            while ((count = stream.read(bytes)) != -1) buffer.write(bytes, 0, count);
            JSONObject root = new JSONObject(buffer.toString(StandardCharsets.UTF_8.name()));
            stories = root.optJSONArray("stories");
            if (stories == null) stories = new JSONArray();
        } catch (Exception e) {
            stories = new JSONArray();
        }
    }
    private JSONObject find(String id) {
        for (int i = 0; i < stories.length(); i++) {
            JSONObject item = stories.optJSONObject(i);
            if (item != null && id.equals(item.optString("id"))) return item;
        }
        return null;
    }
    private SharedPreferences bookmarks() {
        return getSharedPreferences("mojzat_bookmarks", MODE_PRIVATE);
    }
    private boolean saved(String id) { return bookmarks().getBoolean(id, false); }
    private void toggleBookmark(String id) {
        bookmarks().edit().putBoolean(id, !saved(id)).apply();
        render();
    }

    private void render() {
        Ui.applyTheme(this);
        getWindow().setStatusBarColor(Ui.BACKGROUND);
        getWindow().setNavigationBarColor(Ui.BACKGROUND);
        boolean dark = getSharedPreferences("islam_ui", MODE_PRIVATE).getBoolean("dark", true);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0 :
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BACKGROUND);
        Ui.rootInsets(root);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        content = Ui.column(this);
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 30));
        scroll.addView(content);
        bottom(root);
        setContentView(root);

        TextView back = Ui.text(this,
                selectedId == null ? "‹   Hecker Islam" : "‹   معجزہ کی تفصیل",
                16, Ui.GOLD, true);
        back.setContentDescription("Go back");
        back.setOnClickListener(v -> onBackPressed());
        content.addView(back);
        Ui.space(this, content, 18);
        if (selectedId == null) index(); else detail();
    }
    private void bottom(LinearLayout root) {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 11));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Ui.CARD);
        bg.setCornerRadius(Ui.dp(this, 35));
        bg.setStroke(Ui.dp(this, 1), Ui.OUTLINE);
        nav.setBackground(bg);
        nav.setElevation(Ui.dp(this, 8));
        Ui.fillNavigation(this, nav, -1, index -> {
            Intent intent;
            if (index == 0 || index == 4) {
                intent = new Intent(this, IslamDashboardActivity.class);
                intent.putExtra("tab", index == 4 ? 4 : 0);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            } else if (index == 3) {
                intent = new Intent(this, ShiaHadithActivity.class);
            } else {
                intent = new Intent(this, ReferenceReadingActivity.class);
                intent.putExtra("section", index == 1 ? "quran" : "azkar");
            }
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        });
        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 72));
        navParams.setMargins(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 12));
        root.addView(nav, navParams);
    }
    private TextView urdu(String value, float size, int color, boolean bold) {
        TextView t = Ui.text(this, value, size, color, bold);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setGravity(Gravity.RIGHT);
        t.setLineSpacing(Ui.dp(this, 6), 1.15f);
        return t;
    }
    private TextView roman(String value, float size, int color, boolean bold) {
        TextView t = Ui.text(this, value, size, color, bold);
        t.setTextDirection(View.TEXT_DIRECTION_LTR);
        t.setGravity(Gravity.LEFT);
        t.setLineSpacing(Ui.dp(this, 6), 1.15f);
        return t;
    }

    private void action(String title, Runnable runner) {
        LinearLayout box = Ui.panel(this);
        TextView text = Ui.text(this, title, 15, Ui.GOLD, true);
        text.setGravity(Gravity.CENTER);
        box.addView(text);
        box.setOnClickListener(v -> runner.run());
        content.addView(box);
        Ui.space(this, content, 10);
    }
    private void index() {
        content.addView(Ui.text(this, "Mola Ali Mojza", 30, Ui.WHITE, true));
        Ui.space(this, content, 5);
        content.addView(urdu("معجزۂ مولا علی علیہ السلام", 23, Ui.GOLD, true));
        Ui.space(this, content, 9);
        Ui.space(this, content, 17);
        for (int i = 0; i < stories.length(); i++) {
            JSONObject item = stories.optJSONObject(i);
            if (item != null) featured(item);
        }
        if (stories.length() == 0)
            content.addView(urdu("حکایات اس بلڈ میں موجود نہیں۔", 16, Ui.MUTED, false));

    }
    private void featured(JSONObject item) {
        LinearLayout card = Ui.panel(this);
        boolean urduOnly = "urdu".equals(item.optString("script"));
        card.addView(urduOnly ? urdu(item.optString("title"), 19, Ui.WHITE, true)
                : roman(item.optString("title"), 19, Ui.WHITE, true));
        Ui.space(this, card, 7);
        card.addView(urduOnly ? urdu(item.optString("subtitle"), 14, Ui.MUTED, false)
                : roman(item.optString("subtitle"), 14, Ui.MUTED, false));
        Ui.space(this, card, 9);
        card.addView(Ui.text(this, urduOnly ? "اردو میں پڑھیں  ›"
                : "Read in Urdu / Roman Urdu  ›", 14, Ui.GOLD, true));
        card.setOnClickListener(v -> select(item.optString("id")));
        content.addView(card);
        Ui.space(this, content, 10);
    }
    private void select(String id) {
        selectedId = id;
        if (reciter != null) reciter.stop();
        render();
    }
    private void detail() {
        JSONObject item = find(selectedId);
        if (item == null) { selectedId = null; render(); return; }
        boolean showUrdu = getSharedPreferences("mojzat_reader", MODE_PRIVATE)
                .getBoolean("urdu_script", true);
        boolean urduOnly = "urdu".equals(item.optString("script"));
        if (urduOnly) showUrdu = true;
        String urduText = item.optString("urduText", "");
        String text = showUrdu && !urduText.isEmpty() ? urduText : item.optString("text");
        String recitationText = urduText.isEmpty() ? item.optString("text") : urduText;
        content.addView(urduOnly ? urdu(item.optString("title"), 25, Ui.WHITE, true)
                : roman(item.optString("title"), 25, Ui.WHITE, true));
        Ui.space(this, content, 10);
        content.addView(urduOnly ? urdu(item.optString("subtitle"), 16, Ui.GOLD, false)
                : roman(item.optString("subtitle"), 16, Ui.GOLD, false));
        Ui.space(this, content, 18);
        if (!urduOnly) {
        LinearLayout switcher = Ui.panel(this);
        switcher.addView(Ui.text(this, "READING LANGUAGE", 12, Ui.GOLD, true));
        Ui.space(this, switcher, 10);
        LinearLayout options = new LinearLayout(this);
        for (boolean urduChoice : new boolean[]{true, false}) {
            boolean active = urduChoice == showUrdu;
            TextView option = Ui.text(this, urduChoice ? "اردو" : "Roman Urdu", 16,
                    active ? Ui.BACKGROUND : Ui.GOLD, true);
            option.setGravity(Gravity.CENTER);
            option.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));
            GradientDrawable background = new GradientDrawable();
            background.setColor(active ? Ui.GOLD : Ui.CARD);
            background.setCornerRadius(Ui.dp(this, 18));
            background.setStroke(Ui.dp(this, 1), Ui.OUTLINE);
            option.setBackground(background);
            option.setOnClickListener(v -> {
                if (reciter != null) reciter.stop();
                getSharedPreferences("mojzat_reader", MODE_PRIVATE).edit()
                        .putBoolean("urdu_script", urduChoice).apply();
                render();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
            params.setMargins(0, 0, Ui.dp(this, 8), 0);
            options.addView(option, params);
        }
        switcher.addView(options);
        content.addView(switcher);
        Ui.space(this, content, 13);
        }

        LinearLayout controls = Ui.panel(this);
        controls.addView(Ui.text(this, "سماعت اور مطالعہ", 13, Ui.GOLD, true));
        Ui.space(this, controls, 9);
        TextView recite = Ui.text(this, "▶ Recite in Urdu", 17, Ui.WHITE, true);
        recite.setGravity(Gravity.CENTER);
        recite.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        reciter.attach(recite, "▶ Recite in Urdu");
        recite.setOnClickListener(v -> reciter.playOrStop(recitationText, new Locale("ur", "PK")));
        controls.addView(recite);
        Ui.space(this, controls, 10);
        TextView speed = Ui.text(this, "Recitation speed • " + reciter.speedLabel() + "  ›",
                15, Ui.GOLD, true);
        speed.setGravity(Gravity.CENTER);
        speed.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        speed.setOnClickListener(v -> reciter.chooseSpeed(speed));
        controls.addView(speed);
        Ui.space(this, controls, 5);
        controls.addView(roman(
                "Recitation always uses Urdu script with an installed Urdu text-to-speech voice, even when reading Roman Urdu. If unavailable, install an Urdu voice in Android speech settings.",
                12, Ui.MUTED, false));
        Ui.space(this, controls, 12);
        LinearLayout zoom = new LinearLayout(this);
        zoom.setGravity(Gravity.CENTER);
        TextView small = Ui.text(this, "A−", 18, Ui.GOLD, true);
        small.setGravity(Gravity.CENTER);
        small.setPadding(Ui.dp(this, 20), Ui.dp(this, 6), Ui.dp(this, 20), Ui.dp(this, 6));
        small.setOnClickListener(v -> size(-2));
        TextView large = Ui.text(this, "A+", 18, Ui.GOLD, true);
        large.setGravity(Gravity.CENTER);
        large.setPadding(Ui.dp(this, 20), Ui.dp(this, 6), Ui.dp(this, 20), Ui.dp(this, 6));
        large.setOnClickListener(v -> size(2));
        zoom.addView(small);
        zoom.addView(Ui.text(this, "   Font size   ", 13, Ui.MUTED, false));
        zoom.addView(large);
        controls.addView(zoom);
        content.addView(controls);
        Ui.space(this, content, 13);
        // Keep reading, speech, speed and font controls visible before the story.
        LinearLayout reader = Ui.panel(this);
        TextView title = Ui.text(this, showUrdu ? "اردو متن" : "Roman Urdu text",
                13, Ui.GOLD, true);
        reader.addView(title);
        Ui.space(this, reader, 12);
        storyText = showUrdu ? urdu(text, fontSize, Ui.WHITE, false)
                : roman(text, fontSize, Ui.WHITE, false);
        storyText.setLineSpacing(Ui.dp(this, 5), 1.12f);
        reader.addView(storyText);
        content.addView(reader);
        Ui.space(this, content, 12);


        String id = item.optString("id");
        action(saved(id) ? "★ محفوظ سے ہٹائیں" : "☆ محفوظ کریں", () -> toggleBookmark(id));
        action("حکایت شیئر کریں", () -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, item.optString("title") + "\n\n" + text);
            startActivity(Intent.createChooser(share, "Share devotional story"));
        });
    }
    private void size(int change) {
        fontSize = Math.max(16, Math.min(32, fontSize + change));
        if (storyText != null) storyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
    }
}
