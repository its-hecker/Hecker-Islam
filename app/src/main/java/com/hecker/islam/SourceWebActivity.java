package com.hecker.islam;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;

/** Read attributed original Arabic and Urdu source pages without leaving Hecker Islam. */
public final class SourceWebActivity extends Activity {
    private WebView web;
    private ReaderSpeech reciter;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String url=getIntent().getStringExtra("url");
        Uri uri=url == null ? Uri.EMPTY : Uri.parse(url);
        if (!allowed(uri)) { finish(); return; }
        Ui.applyTheme(this);
        getWindow().setStatusBarColor(Ui.BACKGROUND);
        getWindow().setNavigationBarColor(Ui.BACKGROUND);
        LinearLayout root=Ui.column(this);
        root.setBackgroundColor(Ui.BACKGROUND);
        Ui.rootInsets(root);
        LinearLayout heading=Ui.column(this);
        heading.setPadding(Ui.dp(this,20),Ui.dp(this,14),Ui.dp(this,20),Ui.dp(this,12));
        TextView back=Ui.text(this,"‹  Hecker Islam",17,Ui.GOLD,true);
        back.setOnClickListener(v -> onBackPressed());
        heading.addView(back); Ui.space(this,heading,10);
        heading.addView(Ui.text(this,getIntent().getStringExtra("title"),18,Ui.WHITE,true));
        root.addView(heading);
        reciter = new ReaderSpeech(this);
        TextView recite = Ui.text(this, "▶ Recite selected text", 15, Ui.GOLD, true);
        recite.setGravity(android.view.Gravity.CENTER);
        recite.setContentDescription("Recite highlighted passage, or the main dua content, with device speech");
        recite.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        reciter.attach(recite);
        recite.setOnClickListener(v -> reciter.reciteFromWeb(web));
        root.addView(recite);
        TextView hint = Ui.text(this,
            "Select a passage for precise recitation • Uses Android speech voice",
            12, Ui.MUTED, false);
        hint.setGravity(android.view.Gravity.CENTER);
        hint.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), Ui.dp(this, 9));
        root.addView(hint);
        web=new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setDisplayZoomControls(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                if (reciter != null) reciter.stop();
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri link=request.getUrl();
                if(allowed(link)) return false;
                if("https".equals(link.getScheme())) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW,link)); } catch(Exception ignored) { }
                }
                return true;
            }
        });
        root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        web.loadUrl(url);
    }
    private static boolean allowed(Uri uri) {
        if(!"https".equals(uri.getScheme())) return false;
        String host=uri.getHost();
        return "nehjulbalagah.com".equals(host) || "www.nehjulbalagah.com".equals(host)
            || "fzic.org".equals(host) || "www.fzic.org".equals(host)
            || "duas.org".equals(host) || "www.duas.org".equals(host);
    }
    @Override public void onBackPressed() {
        if(web!=null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if(web!=null) { web.destroy(); web=null; }
        if (reciter != null) { reciter.destroy(); reciter=null; }
        super.onDestroy();
    }
}
