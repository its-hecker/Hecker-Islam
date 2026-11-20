package com.hecker.islam;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.text.TextWatcher;
import android.text.Editable;
import java.util.Locale;

/**
 * Dedicated Ahlulbayt duas catalogue. Short liturgies have an offline reader;
 * complete long supplications open their attributed original at Duas.org.
 * Do not label snippets as the full traditional texts.
 */
public final class ShiaDuasActivity extends Activity {
    private static final class Dua {
        final String id, name, urduName, occasion, arabic, instructions, source, urduTranslation;
        Dua(String id, String name, String urduName, String occasion,
            String arabic, String instructions, String source) {
            this.id = id; this.name = name; this.urduName = urduName;
            this.occasion = occasion; this.arabic = arabic;
            this.instructions = instructions; this.source = source;
            this.urduTranslation = translations(id);
        }
        private static String translations(String id) {
            switch (id) {
                case "faraj": return "اے اللہ! اپنے ولی، حجت ابن الحسنؑ کے لیے اس گھڑی اور ہر گھڑی سرپرست، محافظ، رہنما، مددگار اور نگہبان بن، یہاں تک کہ تو انہیں اپنی زمین پر بااختیار طور پر بسائے اور انہیں طویل عرصے تک اس سے بہرہ مند فرمائے۔";
                case "tasbih": return "اللہ سب سے بڑا ہے (۳۴ مرتبہ)۔ تمام تعریفیں اللہ ہی کے لیے ہیں (۳۳ مرتبہ)۔ اللہ ہر نقص سے پاک ہے (۳۳ مرتبہ)۔";
                case "salawat": return "اے اللہ! محمدؐ اور آلِ محمدؑ پر رحمت نازل فرما۔";
                case "ahad": return "دعائے عہد: امام مہدیؑ سے اپنے عہد کی تجدید کے لیے پڑھی جانے والی دعا۔ مکمل عربی متن اور اردو ترجمہ ماخذ پر دستیاب ہے۔";
                case "kumayl": return "دعائے کمیل: اللہ سے مغفرت، رحمت اور بندگی کی درخواست۔ یہ مکمل دعا کا ترجمہ نہیں بلکہ تعارف ہے۔";
                case "tawassul": return "دعائے توسل: اللہ سے اہلِ بیتؑ کے وسیلے سے دعا۔ یہ مکمل دعا کا ترجمہ نہیں بلکہ تعارف ہے۔";
                case "nudba": return "دعائے ندبہ: امام زمانہؑ کی یاد اور انتظار سے متعلق مناجات؛ یہ مکمل متن کا ترجمہ نہیں۔";
                case "iftitah": return "دعائے افتتاح: رمضان المبارک میں دعا، حمد اور رحمت کی طلب؛ یہ مکمل متن کا ترجمہ نہیں۔";
                case "simaat": return "دعائے سمات: اللہ کی حمد اور اس کے اسمائے مبارکہ کے وسیلے سے دعا؛ یہ خلاصہ ہے۔";
                case "sabah": return "دعائے صباح: اللہ کی عظمت، ہدایت اور رحمت کی التجا؛ یہ تعارف ہے، مکمل ترجمہ نہیں۔";
                case "ashura": return "زیارت عاشورا: امام حسینؑ کی یاد، سلام اور وفاداری کی زیارت؛ یہ مکمل ترجمہ نہیں۔";
                default: return "";
            }
        }
        boolean offline() { return !arabic.isEmpty(); }
    }

    private static final Dua[] DUAS = {
        new Dua("faraj", "Dua Imam-e-Zamana (Dua al-Faraj)", "دعائے امامِ زمانہؑ / دعائے فرج",
            "Supplication for Imam al-Mahdi (a.j.)",
            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\n"
            + "اللَّهُمَّ كُنْ لِوَلِيِّكَ الْحُجَّةِ بْنِ الْحَسَنِ، صَلَوَاتُكَ عَلَيْهِ وَعَلَىٰ آبَائِهِ، "
            + "فِي هَٰذِهِ السَّاعَةِ وَفِي كُلِّ سَاعَةٍ، وَلِيًّا وَحَافِظًا وَقَائِدًا وَنَاصِرًا "
            + "وَدَلِيلًا وَعَيْنًا، حَتَّىٰ تُسْكِنَهُ أَرْضَكَ طَوْعًا، وَتُمَتِّعَهُ فِيهَا طَوِيلًا.",
            "Short Arabic supplication, available offline.",
            "https://www.duas.org/Allahumma.htm"),
        new Dua("ahad", "Dua-e-Ahad • Imam-e-Zamana", "دعائے عہد / امامِ زمانہؑ",
            "Morning covenant supplication • full Arabic, English and Urdu from FZIC",
            "", "Complete Arabic and Urdu text are available at the linked reference.",
            "https://fzic.org/library/dua/dua-e-ahad-1232"),
        new Dua("tasbih", "Tasbih of Sayyida Fatima al-Zahra", "تسبیحِ حضرت فاطمہ زہراؑ",
            "Remembrance after prayers",
            "اللَّهُ أَكْبَرُ × 34\nالْحَمْدُ لِلَّهِ × 33\nسُبْحَانَ اللَّهِ × 33",
            "Recite in this order. The counts are part of the practice.",
            "https://www.duas.org/regularsd.desktop.htm"),
        new Dua("salawat", "Salawat on Muhammad and Ahlulbayt", "صلوات",
            "Daily remembrance",
            "اللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ وَآلِ مُحَمَّدٍ",
            "A short form of salawat, available offline.",
            "https://www.duas.org/Allahumma.htm"),
        new Dua("kumayl", "Dua Kumayl", "دعائے کمیل", "Commonly recited on Thursday nights",
            "", "Full prayer: opens the attributed text at Duas.org.",
            "https://fzic.org/library/dua/kumail-1285"),
        new Dua("tawassul", "Dua Tawassul", "دعائے توسل",
            "Seeking nearness to Allah through the Ahlulbayt",
            "", "Full prayer: opens the attributed text at Duas.org.",
            "https://fzic.org/library/dua/tawassul-1417"),
        new Dua("nudba", "Dua al-Nudba", "دعائے ندبہ",
            "Commonly recited on Friday mornings",
            "", "Full prayer: opens the attributed text at Duas.org.",
            "https://fzic.org/library/dua/nudba-1313"),
        new Dua("iftitah", "Dua Iftitah", "دعائے افتتاح",
            "Recited during the nights of Ramadan",
            "", "Full prayer: opens the attributed text at Duas.org.",
            "https://fzic.org/library/dua/iftetah-1276"),
        new Dua("simaat", "Dua al-Simat", "دعائے سمات",
            "Commonly recited on Friday evenings",
            "", "Full prayer: opens the attributed text at Duas.org.",
            "https://www.duas.org/simaat-desktop.htm"),
        new Dua("sabah", "Dua al-Sabah", "دعائے صباح",
            "Morning supplication attributed to Imam Ali (a.s.)",
            "", "Full prayer: opens the attributed text at Duas.org.",
            "https://www.duas.org/mobile/dua-sabah.html"),
        new Dua("ashura", "Ziyarat Ashura", "زیارت عاشورا",
            "Ziyarat of Imam Husayn (a.s.)",
            "", "Full ziyarat: opens the attributed text at Duas.org.",
            "https://fzic.org/library/ziyarat/ashura-1435")
    };

    private LinearLayout content;
    private String search = "";
    private Dua selected;
    private boolean favoritesOnly;
    private ReaderSpeech reciter;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        reciter = new ReaderSpeech(this);
        render();
    }
    @Override protected void onDestroy() {
        if (reciter != null) reciter.destroy();
        super.onDestroy();
    }

    private boolean favorite(String id) {
        return getSharedPreferences("shia_duas", MODE_PRIVATE).getBoolean(id, false);
    }
    private void toggleFavorite(Dua dua) {
        getSharedPreferences("shia_duas", MODE_PRIVATE).edit()
                .putBoolean(dua.id, !favorite(dua.id)).apply();
        render();
    }
    private void openSource(Dua dua) {
        try {
            Intent intent = new Intent(this, SourceWebActivity.class);
            intent.putExtra("title", dua.name);
            intent.putExtra("url", dua.source);
            startActivity(intent);
        }
        catch (ActivityNotFoundException error) {
            new android.app.AlertDialog.Builder(this)
                    .setMessage("No browser is available to open this source.")
                    .setPositiveButton("OK", null).show();
        }
    }
    @Override public void onBackPressed() {
        if (selected != null) { reciter.stop(); selected = null; render(); }
        else super.onBackPressed();
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
        ScrollView scrolling = new ScrollView(this);
        scrolling.setFillViewport(true);
        root.addView(scrolling, new LinearLayout.LayoutParams(-1, -1));
        content = Ui.column(this);
        content.setPadding(Ui.dp(this, 19), Ui.dp(this, 24), Ui.dp(this, 19), Ui.dp(this, 45));
        scrolling.addView(content);
        setContentView(root);
        TextView back = Ui.text(this, "‹  " + (selected == null ? "Hecker Islam" : "Shia Duas"), 16, Ui.GOLD, true);
        back.setOnClickListener(v -> onBackPressed());
        content.addView(back);
        Ui.space(this, content, 21);
        content.addView(Ui.text(this, selected == null ? "Shia Duas & Ziyarat" : selected.name, 28, Ui.WHITE, true));
        Ui.space(this, content, 7);
        content.addView(Ui.text(this, selected == null
                ? "Supplications from the Ahlulbayt tradition" : selected.urduName,
                15, Ui.MUTED, false));
        Ui.space(this, content, 23);
        if (selected == null) showIndex(); else showDua(selected);
    }
    private void button(String label, Runnable action) {
        LinearLayout panel = Ui.panel(this);
        TextView text = Ui.text(this, label, 16, Ui.GOLD, true);
        text.setGravity(Gravity.CENTER);
        panel.addView(text);
        panel.setOnClickListener(v -> action.run());
        content.addView(panel);
        Ui.space(this, content, 11);
    }
    private void showIndex() {
        EditText filter = new EditText(this);
        filter.setSingleLine(true);
        filter.setHint("Search Kumayl, Tawassul, ندبہ…");
        filter.setText(search);
        filter.setTextColor(Ui.WHITE);
        filter.setHintTextColor(Ui.MUTED);
        filter.setTextSize(15);
        LinearLayout p = Ui.panel(this);
        p.addView(filter);
        content.addView(p);
        Ui.space(this, content, 9);
        filter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                search = s.toString().trim();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        button(favoritesOnly ? "★ Showing saved • Show all" : "☆ Show saved duas", () -> {
            favoritesOnly = !favoritesOnly; render();
        });
        content.addView(Ui.text(this,
                "Short duas include Arabic and Urdu offline. For longer duas, the Urdu introduction is not the full translation; open the linked source.",
                13, Ui.MUTED, false));
        Ui.space(this, content, 16);
        int shown = 0;
        for (Dua dua : DUAS) {
            if (favoritesOnly && !favorite(dua.id)) continue;
            String compare = (dua.name + " " + dua.urduName + " " + dua.occasion).toLowerCase(Locale.ROOT);
            if (!compare.contains(search.toLowerCase(Locale.ROOT))) continue;
            shown++;
            LinearLayout card = Ui.panel(this);
            card.addView(Ui.text(this, dua.name + "   ›", 19, Ui.WHITE, true));
            Ui.space(this, card, 6);
            card.addView(Ui.text(this, dua.urduName, 17, Ui.GOLD, false));
            Ui.space(this, card, 8);
            card.addView(Ui.text(this, dua.occasion, 13, Ui.MUTED, false));
            Ui.space(this, card, 8);
            card.addView(Ui.text(this, dua.offline() ? "Available offline" : "Read full text online",
                    12, Ui.MUTED, true));
            card.setOnClickListener(v -> { reciter.stop(); selected = dua; render(); });
            content.addView(card);
            Ui.space(this, content, 11);
        }
        if (shown == 0) content.addView(Ui.text(this, "No matching duas", 15, Ui.MUTED, false));
        Ui.space(this, content, 12);
        content.addView(Ui.text(this,
                "Sources: Duas.org and FZIC. Original text and any full translation must be checked at the cited source; attribution is not a grading claim.",
                12, Ui.MUTED, false));
    }
    private void showDua(Dua dua) {
        content.addView(Ui.text(this, dua.occasion, 15, Ui.MUTED, false));
        Ui.space(this, content, 16);
        if (dua.offline()) {
            LinearLayout panel = Ui.panel(this);
            TextView original = Ui.text(this, dua.arabic, 25, Ui.WHITE, false);
            original.setGravity(Gravity.RIGHT);
            original.setTextDirection(View.TEXT_DIRECTION_RTL);
            original.setLineSpacing(Ui.dp(this, 11), 1.18f);
            panel.addView(original);
            content.addView(panel);
            Ui.space(this, content, 12);
        } else {
            LinearLayout panel = Ui.panel(this);
            panel.addView(Ui.text(this,
                    "The complete text is available from the original source. An incomplete excerpt is intentionally not presented as the full prayer.",
                    15, Ui.WHITE, false));
            content.addView(panel);
            Ui.space(this, content, 14);
        }
        if (!dua.urduTranslation.isEmpty()) {
            Ui.space(this, content, 8);
            LinearLayout translation = Ui.panel(this);
            translation.addView(Ui.text(this,
                    dua.offline() ? "اردو ترجمہ" : "اردو تعارف • مکمل ترجمہ نہیں",
                    14, Ui.GOLD, true));
            Ui.space(this, translation, 10);
            TextView text = Ui.text(this, dua.urduTranslation, 19, Ui.WHITE, false);
            text.setTextDirection(View.TEXT_DIRECTION_RTL);
            text.setGravity(Gravity.RIGHT);
            text.setLineSpacing(Ui.dp(this, 5), 1.15f);
            translation.addView(text);
            content.addView(translation);
            Ui.space(this, content, 12);
        }
        if (dua.offline()) {
            LinearLayout audioPanel = Ui.panel(this);
            audioPanel.addView(Ui.text(this, "RECITATION", 12, Ui.GOLD, true));
            Ui.space(this, audioPanel, 10);
            TextView arabicRecite = Ui.text(this, "▶ Recite Arabic", 16, Ui.WHITE, true);
            arabicRecite.setPadding(0, Ui.dp(this, 9), 0, Ui.dp(this, 9));
            arabicRecite.setOnClickListener(v -> reciter.playOrStop(dua.arabic, new Locale("ar")));
            reciter.attach(arabicRecite);
            audioPanel.addView(arabicRecite);
            if (!dua.urduTranslation.isEmpty()) {
                Ui.space(this, audioPanel, 9);
                TextView urduRecite = Ui.text(this, "▶ Recite Urdu translation", 16, Ui.GOLD, true);
                urduRecite.setPadding(0, Ui.dp(this, 9), 0, Ui.dp(this, 9));
                urduRecite.setOnClickListener(v ->
                    reciter.speak(dua.urduTranslation, new Locale("ur", "PK")));
                audioPanel.addView(urduRecite);
            }
            Ui.space(this, audioPanel, 8);
            audioPanel.addView(Ui.text(this,
                "Uses Android speech voices; it is not a recorded human reciter.",
                12, Ui.MUTED, false));
            content.addView(audioPanel);
            Ui.space(this, content, 12);
        } else {
            button("▶ Open complete dua & recite", () -> openSource(dua));
        }
        content.addView(Ui.text(this, dua.instructions, 13, Ui.MUTED, false));
        Ui.space(this, content, 15);
        button(favorite(dua.id) ? "★ Remove bookmark" : "☆ Save dua",
                () -> toggleFavorite(dua));
        button(dua.source.contains("fzic.org")
                ? "Read complete Arabic & Urdu translation ›"
                : "Read full source (online) ›", () -> openSource(dua));
    }
}
