package com.hecker.islam;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Numbered index of all 480 maxims; selected sourced sayings read offline.
 * Full original and Urdu reference collections are accessible via links.
 */
public final class NahjAlBalaghaActivity extends Activity {
    private LinearLayout content;
    private int page, selected = -1;
    private static final int PAGE_SIZE = 30;

    @Override public void onCreate(Bundle state) { super.onCreate(state); render(); }
    @Override public void onBackPressed() {
        if (selected != -1) { selected = -1; render(); } else super.onBackPressed();
    }
    private void source(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception error) {
            new AlertDialog.Builder(this).setMessage("No browser was found to open this source.")
                .setPositiveButton("OK", null).show();
        }
    }
    private void button(String label, Runnable action) {
        LinearLayout panel = Ui.panel(this);
        TextView text = Ui.text(this, label, 16, Ui.GOLD, true);
        text.setGravity(Gravity.CENTER);
        panel.addView(text);
        panel.setOnClickListener(v -> action.run());
        content.addView(panel); Ui.space(this, content, 11);
    }
    private void render() {
        Ui.applyTheme(this);
        getWindow().setStatusBarColor(Ui.BACKGROUND);
        getWindow().setNavigationBarColor(Ui.BACKGROUND);
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BACKGROUND); Ui.rootInsets(root);
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1,-1));
        content = Ui.column(this);
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 22), Ui.dp(this, 20), Ui.dp(this, 30));
        scroll.addView(content);
        setContentView(root);
        TextView back = Ui.text(this, selected < 0 ? "‹  Hecker Islam" : "‹  Nahj al-Balagha", 16, Ui.GOLD, true);
        back.setOnClickListener(v -> onBackPressed());
        content.addView(back); Ui.space(this, content, 18);
        content.addView(Ui.text(this, "Nahj al-Balagha", 29, Ui.WHITE, true));
        Ui.space(this, content, 6);
        content.addView(Ui.text(this, "نہج البلاغہ • کلماتِ قصار • Imam Ali (a.s.)", 17, Ui.GOLD, false));
        Ui.space(this, content, 16);
        if (selected > 0) { detail(); return; }
        content.addView(Ui.text(this, "Index of 480 numbered wisdom sayings. Selected sayings include readable Arabic and original Urdu meanings offline; the remaining entries link to the complete source.", 14, Ui.MUTED, false));
        Ui.space(this, content, 15);
        button("Read complete Arabic and English source", () -> source(NahjWisdom.COLLECTION));
        button("Find complete Urdu book / translation", () -> source(NahjWisdom.URDU_BOOK));
        EditText find = new EditText(this);
        find.setSingleLine(true);
        find.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        find.setTextColor(Ui.WHITE); find.setHintTextColor(Ui.MUTED);
        find.setHint("Jump to saying number 1–480");
        LinearLayout search = Ui.panel(this);
        search.addView(find); content.addView(search); Ui.space(this, content, 10);
        button("Open number", () -> {
            try {
                int number = Integer.parseInt(find.getText().toString().trim());
                if (number < 1 || number > 480) throw new NumberFormatException();
                selected = number; render();
            } catch (NumberFormatException ex) {
                new AlertDialog.Builder(this).setMessage("Enter a saying number from 1 to 480.")
                    .setPositiveButton("OK", null).show();
            }
        });
        content.addView(Ui.text(this, "Sayings " + (page * PAGE_SIZE + 1) + "–" +
            Math.min(480, (page + 1) * PAGE_SIZE), 19, Ui.WHITE, true));
        Ui.space(this, content, 12);
        for (int i=page*PAGE_SIZE+1; i<=Math.min(480,(page+1)*PAGE_SIZE); i++) {
            final int number=i;
            NahjWisdom.Quote quote = NahjWisdom.find(number);
            LinearLayout panel=Ui.panel(this);
            panel.addView(Ui.text(this, "Wisdom " + number +
                (quote == null ? "  •  Read at source  ›" : "  •  Urdu available  ›"),
                16, Ui.GOLD, true));
            if (quote != null) {
                Ui.space(this,panel,8);
                TextView urdu=Ui.text(this,quote.urdu,17,Ui.WHITE,false);
                urdu.setTextDirection(View.TEXT_DIRECTION_RTL);
                urdu.setGravity(Gravity.RIGHT);
                panel.addView(urdu);
            }
            panel.setOnClickListener(v -> { selected=number; render(); });
            content.addView(panel); Ui.space(this,content,9);
        }
        if(page>0) button("‹ Previous 30 sayings", () -> { page--; render(); });
        if((page+1)*PAGE_SIZE<480) button("Next 30 sayings ›", () -> { page++; render(); });
        content.addView(Ui.text(this, "Source numbering follows the 480 sayings of Nahj al-Balagha; editions may differ slightly.", 12, Ui.MUTED, false));
    }
    private void detail() {
        NahjWisdom.Quote quote=NahjWisdom.find(selected);
        content.addView(Ui.text(this, "Wisdom • " + selected, 23, Ui.GOLD, true));
        Ui.space(this,content,16);
        if(quote!=null) {
            LinearLayout panel=Ui.panel(this);
            TextView ar=Ui.text(this,quote.arabic,24,Ui.WHITE,false);
            ar.setTextDirection(View.TEXT_DIRECTION_RTL);
            ar.setGravity(Gravity.RIGHT);
            ar.setLineSpacing(Ui.dp(this,8),1.14f);
            panel.addView(ar); content.addView(panel); Ui.space(this,content,11);
            LinearLayout translation=Ui.panel(this);
            translation.addView(Ui.text(this,"اردو مفہوم",14,Ui.GOLD,true));
            Ui.space(this,translation,10);
            TextView urdu=Ui.text(this,quote.urdu,20,Ui.WHITE,false);
            urdu.setTextDirection(View.TEXT_DIRECTION_RTL);
            urdu.setGravity(Gravity.RIGHT);
            urdu.setLineSpacing(Ui.dp(this,7),1.12f);
            translation.addView(urdu); content.addView(translation);
        } else {
            content.addView(Ui.text(this, "This numbered saying is part of the complete source collection. An offline Urdu rendering has not yet been verified for this entry; open the linked original or Urdu edition below.", 16, Ui.MUTED, false));
        }
        Ui.space(this,content,16);
        button("Read in full source • Al-Islam.org", () -> source(NahjWisdom.COLLECTION));
        button("Urdu reference • Nahj al-Balagha", () -> source(NahjWisdom.URDU_BOOK));
    }
}
