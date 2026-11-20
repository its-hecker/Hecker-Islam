package com.hecker.islam;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;

/** Shia-only books, with a native selection screen and embedded sourced reader.
 * The former general reference hadith dataset is intentionally not used here.
 */
public final class ShiaHadithActivity extends Activity {
    private static final String[] NAMES = {
        "Al-Kafi • الکافی", "Man La Yahduruhu al-Faqih • من لا يحضره الفقيه",
        "Ghurar al-Hikam • غرر الحکم", "Kitab al-Ghayba • کتاب الغیبة",
        "Al-Amali • الأمالي", "Thawab al-A'mal • ثواب الأعمال"
    };
    private static final String[] DESCRIPTIONS = {
        "Al-Kulayni • Shia hadith collection",
        "Shaykh al-Saduq • narrations and jurisprudence",
        "Short sayings attributed to Imam Ali (a.s.)",
        "Reports concerning the occultation",
        "Scholarly sessions and narrations",
        "Narrations concerning deeds and their consequences"
    };
    private static final String[] URLS = {
        "https://rewayaat.info/books/al-kafi",
        "https://rewayaat.info/books/man-la-yahduruh-al-faqih",
        "https://rewayaat.info/books/ghurar-al-hikam",
        "https://rewayaat.info/books/kitab-al-ghayba",
        "https://rewayaat.info/books/al-amali",
        "https://rewayaat.info/books/thawab-al-a-mal-wa-iqab-al-a-mal"
    };
    private WebView web;
    private ReaderSpeech reciter;
    private String selectedName;

    @Override public void onCreate(Bundle state) { super.onCreate(state); showBooks(); }
    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) { web.goBack(); return; }
        if (web != null) {
            if (reciter != null) reciter.stop();
            web.destroy(); web=null; selectedName=null; showBooks(); return;
        }
        super.onBackPressed();
    }
    private LinearLayout scaffold() {
        Ui.applyTheme(this);
        getWindow().setStatusBarColor(Ui.BACKGROUND);
        getWindow().setNavigationBarColor(Ui.BACKGROUND);
        LinearLayout root=Ui.column(this);
        root.setBackgroundColor(Ui.BACKGROUND);
        Ui.rootInsets(root);
        return root;
    }
    private void bottom(LinearLayout root) {
        LinearLayout nav=new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(Ui.dp(this,10),Ui.dp(this,8),Ui.dp(this,10),Ui.dp(this,11));
        android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable();
        background.setColor(Ui.CARD);
        background.setCornerRadius(Ui.dp(this,35));
        background.setStroke(Ui.dp(this,1),Ui.OUTLINE);
        nav.setBackground(background);
        Ui.fillNavigation(this,nav,3,index -> {
            if(index==3) { if(web!=null) onBackPressed(); return; }
            Intent intent;
            if(index==0 || index==4) {
                intent=new Intent(this,IslamDashboardActivity.class);
                intent.putExtra("tab", index==4 ? 4 : 0);
            } else {
                intent=new Intent(this,ReferenceReadingActivity.class);
                intent.putExtra("section", index==1 ? "quran" : "azkar");
            }
            startActivity(intent); finish();
            overridePendingTransition(0,0);
        });
        LinearLayout.LayoutParams navLayout=new LinearLayout.LayoutParams(-1,Ui.dp(this,72));
        navLayout.setMargins(Ui.dp(this,16),Ui.dp(this,4),Ui.dp(this,16),Ui.dp(this,12));
        root.addView(nav,navLayout);
    }
    private void showBooks() {
        LinearLayout root=scaffold();
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout content=Ui.column(this);
        content.setPadding(Ui.dp(this,20),Ui.dp(this,23),Ui.dp(this,20),Ui.dp(this,25));
        scroll.addView(content);
        TextView back=Ui.text(this,"‹  Hecker Islam",16,Ui.GOLD,true);
        back.setOnClickListener(v -> finish()); content.addView(back);
        Ui.space(this,content,19);
        content.addView(Ui.text(this,"Shia Hadith Library",29,Ui.WHITE,true));
        Ui.space(this,content,8);
        content.addView(Ui.text(this,"اہلِ بیتؑ کی روایات • Sourced Shia collections",16,Ui.GOLD,false));
        Ui.space(this,content,12);
        content.addView(Ui.text(this,
            "Read Arabic and available translations from Shia books. Books open inside the app and need an internet connection. Historical inclusion does not imply every narration is authenticated.",
            14,Ui.MUTED,false));
        Ui.space(this,content,18);
        for(int i=0;i<NAMES.length;i++) {
            final int index=i;
            LinearLayout card=Ui.panel(this);
            card.addView(Ui.text(this,NAMES[i]+"   ›",18,Ui.WHITE,true));
            Ui.space(this,card,6);
            card.addView(Ui.text(this,DESCRIPTIONS[i],13,Ui.MUTED,false));
            card.setOnClickListener(v -> openBook(index));
            content.addView(card); Ui.space(this,content,11);
        }
        content.addView(Ui.text(this,"Source: Rewayaat.info • Shia Arabic/English hadith collections.",12,Ui.MUTED,false));
        bottom(root); setContentView(root);
    }
    private void openBook(int index) {
        selectedName=NAMES[index];
        LinearLayout root=scaffold();
        LinearLayout head=Ui.column(this);
        head.setPadding(Ui.dp(this,17),Ui.dp(this,12),Ui.dp(this,17),Ui.dp(this,10));
        TextView back=Ui.text(this,"‹  Shia Hadith",17,Ui.GOLD,true);
        back.setOnClickListener(v -> {
            if (reciter != null) reciter.stop();
            if(web!=null) { web.destroy(); web=null; }
            showBooks();
        });
        head.addView(back); Ui.space(this,head,8);
        head.addView(Ui.text(this,selectedName,16,Ui.WHITE,true));
        root.addView(head);
        if (reciter == null) reciter = new ReaderSpeech(this);
        TextView listen=Ui.text(this,"▶ Recite selected Hadith / page",15,Ui.GOLD,true);
        listen.setGravity(Gravity.CENTER);
        listen.setContentDescription("Recite selected Hadith or page text with device speech");
        listen.setPadding(0,Ui.dp(this,13),0,Ui.dp(this,13));
        reciter.attach(listen);
        listen.setOnClickListener(v -> reciter.reciteFromWeb(web));
        root.addView(listen);
        TextView speed = Ui.text(this, "Recitation speed • " + reciter.speedLabel() + "  ›", 14, Ui.GOLD, true);
        speed.setGravity(Gravity.CENTER);
        speed.setPadding(0, Ui.dp(this, 9), 0, Ui.dp(this, 9));
        speed.setOnClickListener(v -> reciter.chooseSpeed(speed));
        root.addView(speed);
        TextView hint=Ui.text(this,
            "Highlight one narration for accurate playback • Android speech voice",
            12,Ui.MUTED,false);
        hint.setGravity(Gravity.CENTER);
        root.addView(hint);
        web=new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                if (reciter != null) reciter.stop();
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri link=request.getUrl();
                if("https".equals(link.getScheme()) && "rewayaat.info".equals(link.getHost())) return false;
                if("https".equals(link.getScheme())) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW,link)); } catch(Exception ignored) { }
                }
                return true;
            }
        });
        root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        bottom(root);
        setContentView(root);
        web.loadUrl(URLS[index]);
    }
    @Override protected void onDestroy() {
        if(web!=null) { web.destroy(); web=null; }
        if (reciter != null) { reciter.destroy(); reciter=null; }
        super.onDestroy();
    }
}
