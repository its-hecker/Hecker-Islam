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
        TextView back=Ui.text(this,"‹  Shia Duas",17,Ui.GOLD,true);
        back.setOnClickListener(v -> onBackPressed());
        heading.addView(back); Ui.space(this,heading,10);
        heading.addView(Ui.text(this,getIntent().getStringExtra("title"),18,Ui.WHITE,true));
        root.addView(heading);
        web=new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setDisplayZoomControls(false);
        web.setWebViewClient(new WebViewClient() {
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
        return "fzic.org".equals(host) || "www.fzic.org".equals(host)
            || "duas.org".equals(host) || "www.duas.org".equals(host);
    }
    @Override public void onBackPressed() {
        if(web!=null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if(web!=null) { web.destroy(); web=null; }
        super.onDestroy();
    }
}
