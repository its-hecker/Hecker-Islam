package com.hecker.islam;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
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
 * Offline Urdu Shia mojzat and karamat reader. Stories are catalogued with
 * explicit source / historical-status notes, and not treated as authenticated
 * Hadith. Extend app/src/main/assets/mojzat/catalog.json with reviewed texts.
 */
public final class MojzatActivity extends Activity {
    private static final String[] FILTERS = {"all", "ali", "abbas", "fatima", "jaffar", "saved"};
    private static final String[] LABELS = {"سب", "امام علیؑ", "غازی عباسؑ", "بی بی فاطمہؑ",
            "امام جعفر صادقؑ", "★ محفوظ"};
    private JSONArray stories = new JSONArray(), books = new JSONArray();
    private String catalogNote = "", selectedId, filter = "all", keyword = "";
    private LinearLayout cards, content;
    private TextView storyText;
    private ReaderSpeech reciter;
    private int fontSize = 20;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        readCatalog();
        reciter = new ReaderSpeech(this);
        if (state != null) {
            selectedId = state.getString("mojzat_story");
            filter = state.getString("mojzat_filter", "all");
            keyword = state.getString("mojzat_query", "");
            fontSize = state.getInt("mojzat_font_size", 20);
        }
        render();
    }
    @Override protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putString("mojzat_story", selectedId);
        b.putString("mojzat_filter", filter);
        b.putString("mojzat_query", keyword);
        b.putInt("mojzat_font_size", fontSize);
    }
    @Override protected void onPause() {
        if (reciter != null) reciter.stop();
        super.onPause();
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
            books = root.optJSONArray("books");
            if (stories == null) stories = new JSONArray();
            if (books == null) books = new JSONArray();
            catalogNote = root.optString("note");
        } catch (Exception e) {
            stories = new JSONArray(); books = new JSONArray();
            catalogNote = "اردو مواد اس بلڈ میں دستیاب نہیں۔ براہِ کرم مکمل APK انسٹال کریں۔";
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
                selectedId == null ? "‹   Hecker Islam" : "‹   معجزات و کرامات",
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
        content.addView(Ui.text(this, "Shia Mojzat", 30, Ui.WHITE, true));
        Ui.space(this, content, 4);
        content.addView(urdu("معجزات و کراماتِ اہلِ بیتؑ", 23, Ui.GOLD, true));
        Ui.space(this, content, 17);
        LinearLayout introduction = Ui.panel(this);
        introduction.addView(urdu(
                "اردو میں مشہور واقعات، کرامات اور مجلسی حکایات — معتبر ماخذ کا لنک اور ہر داستان کی نوعیت کے ساتھ۔",
                17, Ui.WHITE, false));
        Ui.space(this, introduction, 9);
        introduction.addView(urdu(
                "توجہ: عام روایت یا خواب کا بیان، سند کے ساتھ ثابت شدہ حدیث نہیں۔",
                13, Ui.MUTED, false));
        content.addView(introduction);
        Ui.space(this, content, 18);
        content.addView(Ui.text(this, "مخصوص واقعات", 19, Ui.WHITE, true));
        Ui.space(this, content, 10);
        for (String id : new String[]{"ali-iran-woodcutter", "abbas-shrine-child", "fatima-sunaran"}) {
            JSONObject item = find(id);
            if (item != null) featured(item);
        }
        Ui.space(this, content, 13);
        content.addView(Ui.text(this, "تمام اردو حکایات", 21, Ui.WHITE, true));
        Ui.space(this, content, 10);
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(Ui.WHITE);
        search.setHintTextColor(Ui.MUTED);
        search.setTextSize(16);
        search.setHint("تلاش: لکڑہارا، سنارن، عباس...");
        search.setText(keyword);
        LinearLayout searchPanel = Ui.panel(this);
        searchPanel.addView(search);
        content.addView(searchPanel);
        Ui.space(this, content, 11);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                keyword = s.toString().trim();
                fillCards();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < FILTERS.length; i++) {
            final String chosen = FILTERS[i];
            boolean active = chosen.equals(filter);
            TextView chip = Ui.text(this, LABELS[i], 14, active ? Ui.BACKGROUND : Ui.GOLD, true);
            chip.setPadding(Ui.dp(this, 14), Ui.dp(this, 10),
                    Ui.dp(this, 14), Ui.dp(this, 10));
            chip.setGravity(Gravity.CENTER);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(active ? Ui.GOLD : Ui.CARD);
            bg.setCornerRadius(Ui.dp(this, 22));
            bg.setStroke(Ui.dp(this, 1), Ui.OUTLINE);
            chip.setBackground(bg);
            chip.setOnClickListener(v -> { filter = chosen; render(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(0, 0, Ui.dp(this, 8), 0);
            chips.addView(chip, lp);
        }
        horizontal.addView(chips);
        content.addView(horizontal);
        Ui.space(this, content, 14);
        cards = Ui.column(this);
        content.addView(cards);
        fillCards();

        Ui.space(this, content, 12);
        content.addView(Ui.text(this, "مزید اردو کتابیں اور مجموعے", 19, Ui.WHITE, true));
        Ui.space(this, content, 9);
        for (int i = 0; i < books.length(); i++) {
            JSONObject book = books.optJSONObject(i);
            if (book == null) continue;
            String url = book.optString("url");
            LinearLayout box = Ui.panel(this);
            box.addView(urdu(book.optString("title") + "  ›", 16, Ui.GOLD, true));
            box.setOnClickListener(v -> openSource(url));
            content.addView(box);
            Ui.space(this, content, 9);
        }
        Ui.space(this, content, 7);
        content.addView(urdu(catalogNote, 13, Ui.MUTED, false));
    }
    private void featured(JSONObject item) {
        LinearLayout card = Ui.panel(this);
        card.addView(urdu(item.optString("title"), 19, Ui.WHITE, true));
        Ui.space(this, card, 7);
        card.addView(urdu(item.optString("subtitle"), 14, Ui.MUTED, false));
        Ui.space(this, card, 9);
        card.addView(Ui.text(this, "مکمل اردو مطالعہ  ›", 14, Ui.GOLD, true));
        card.setOnClickListener(v -> select(item.optString("id")));
        content.addView(card);
        Ui.space(this, content, 10);
    }
    private void fillCards() {
        if (cards == null) return;
        cards.removeAllViews();
        int shown = 0;
        String q = keyword.toLowerCase(Locale.ROOT);
        for (int i = 0; i < stories.length(); i++) {
            JSONObject item = stories.optJSONObject(i);
            if (item == null) continue;
            String id = item.optString("id");
            if ("saved".equals(filter) ? !saved(id)
                    : !"all".equals(filter) && !filter.equals(item.optString("category"))) continue;
            String searchable = (item.optString("title") + " " + item.optString("subtitle")
                    + " " + item.optString("text")).toLowerCase(Locale.ROOT);
            if (!q.isEmpty() && !searchable.contains(q)) continue;
            shown++;
            LinearLayout card = Ui.panel(this);
            card.addView(urdu((saved(id) ? "★  " : "") + item.optString("title") + "  ›",
                    18, Ui.WHITE, true));
            Ui.space(this, card, 7);
            card.addView(urdu(item.optString("subtitle"), 14, Ui.MUTED, false));
            Ui.space(this, card, 7);
            card.addView(urdu(item.optString("label"), 12, Ui.GOLD, false));
            card.setOnClickListener(v -> select(id));
            cards.addView(card);
            Ui.space(this, cards, 10);
        }
        if (shown == 0) cards.addView(urdu("اس تلاش میں کوئی حکایت نہیں ملی۔", 16, Ui.MUTED, false));
    }
    private void select(String id) {
        selectedId = id;
        if (reciter != null) reciter.stop();
        render();
    }
    private void detail() {
        JSONObject item = find(selectedId);
        if (item == null) { selectedId = null; render(); return; }
        String text = item.optString("text");
        content.addView(urdu(item.optString("title"), 25, Ui.WHITE, true));
        Ui.space(this, content, 10);
        content.addView(urdu(item.optString("subtitle"), 16, Ui.GOLD, false));
        Ui.space(this, content, 18);
        LinearLayout label = Ui.panel(this);
        label.addView(urdu("نوعیتِ روایت", 13, Ui.GOLD, true));
        Ui.space(this, label, 7);
        label.addView(urdu(item.optString("label"), 15, Ui.WHITE, false));
        content.addView(label);
        Ui.space(this, content, 13);

        LinearLayout reader = Ui.panel(this);
        TextView title = Ui.text(this, "اردو متن", 13, Ui.GOLD, true);
        reader.addView(title);
        Ui.space(this, reader, 12);
        storyText = urdu(text, fontSize, Ui.WHITE, false);
        reader.addView(storyText);
        content.addView(reader);
        Ui.space(this, content, 12);

        LinearLayout controls = Ui.panel(this);
        controls.addView(Ui.text(this, "سماعت اور مطالعہ", 13, Ui.GOLD, true));
        Ui.space(this, controls, 9);
        TextView recite = Ui.text(this, "▶ Recite Urdu", 17, Ui.WHITE, true);
        recite.setGravity(Gravity.CENTER);
        recite.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        reciter.attach(recite);
        recite.setOnClickListener(v -> reciter.playOrStop(text, new Locale("ur", "PK")));
        controls.addView(recite);
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
        zoom.addView(Ui.text(this, "   فونٹ کا سائز   ", 13, Ui.MUTED, false));
        zoom.addView(large);
        controls.addView(zoom);
        content.addView(controls);
        Ui.space(this, content, 13);

        String id = item.optString("id");
        action(saved(id) ? "★ محفوظ سے ہٹائیں" : "☆ محفوظ کریں", () -> toggleBookmark(id));
        action("ماخذ / مکمل حوالہ کھولیں  ›", () -> openSource(item.optString("source")));
        action("حکایت شیئر کریں", () -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, item.optString("title") + "\n\n"
                    + text + "\n\n" + item.optString("label") + "\n"
                    + item.optString("source"));
            startActivity(Intent.createChooser(share, "Share Urdu mojza"));
        });
        content.addView(urdu("ماخذ: " + item.optString("sourceTitle"), 13, Ui.MUTED, false));
        Ui.space(this, content, 8);
        content.addView(urdu(
                "وضاحت: یہ آزادانہ اردو خلاصہ ہے، لفظ بہ لفظ نقل نہیں؛ سند کی جانچ کے لیے اصل ماخذ دیکھیے۔",
                13, Ui.MUTED, false));
    }
    private void size(int change) {
        fontSize = Math.max(16, Math.min(32, fontSize + change));
        if (storyText != null) storyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
    }
    private void openSource(String raw) {
        try {
            Uri uri = Uri.parse(raw);
            if (!"https".equalsIgnoreCase(uri.getScheme())) throw new IllegalArgumentException();
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ex) {
            new AlertDialog.Builder(this)
                    .setMessage("یہ ماخذ کھولنے کے لیے براؤزر درکار ہے۔")
                    .setPositiveButton("ٹھیک ہے", null).show();
        }
    }
}
