package com.sabber.taqwim;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.hecker.islam.CalendarPalette;
import com.sabber.offline.OfflineActivity;

/**
 * Keep the original Calendar engine and all its saved settings intact.
 * The integrated Hecker Islam skin is the default; the original design is opt-in.
 */
public class MainActivity extends OfflineActivity {
    @Override protected void configureWebView(WebView view) {
        super.configureWebView(view);
        // Only the bundled file:///android_asset/index.html can use this bridge.
        view.addJavascriptInterface(new CalendarStyleBridge(), "HeckerCalendar");
    }

    public final class CalendarStyleBridge {
        @JavascriptInterface public boolean unified() {
            return !getSharedPreferences("islam_ui", MODE_PRIVATE)
                    .getBoolean("calendar_original", false);
        }

        @JavascriptInterface public String palette() {
            return CalendarPalette.json(MainActivity.this);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        // Refresh today's dates after returning from another app or after midnight.
        evaluatePage("if (typeof render === 'function') render();");
    }
}
