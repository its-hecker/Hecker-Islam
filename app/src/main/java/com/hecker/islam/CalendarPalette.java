package com.hecker.islam;

import android.content.Context;
import android.graphics.Color;
import org.json.JSONObject;

/** Shared palette for the bundled Calendar WebView, matching the rest of Hecker Islam. */
public final class CalendarPalette {
    private CalendarPalette() { }

    private static String hex(int color) {
        return String.format(java.util.Locale.US, "#%06X", 0xFFFFFF & color);
    }

    private static int mix(int a, int b, float portion) {
        float inverse = 1f - portion;
        return Color.rgb(Math.round(Color.red(a) * inverse + Color.red(b) * portion),
                Math.round(Color.green(a) * inverse + Color.green(b) * portion),
                Math.round(Color.blue(a) * inverse + Color.blue(b) * portion));
    }

    public static String json(Context context) {
        Ui.applyTheme(context);
        JSONObject colors = new JSONObject();
        try {
            boolean dark = context.getSharedPreferences("islam_ui", Context.MODE_PRIVATE)
                    .getBoolean("dark", true);
            colors.put("dark", dark);
            colors.put("bg", hex(Ui.BACKGROUND));
            colors.put("card", hex(Ui.CARD));
            colors.put("ink", hex(Ui.WHITE));
            colors.put("muted", hex(Ui.MUTED));
            colors.put("line", hex(Ui.OUTLINE));
            colors.put("accent", hex(Ui.GOLD));
            colors.put("accent-text", hex(Ui.GOLD));
            colors.put("on-accent", hex(Ui.BACKGROUND));
            colors.put("hero-start", hex(Ui.CARD));
            colors.put("hero-end", hex(mix(Ui.CARD, Ui.GOLD, dark ? .10f : .05f)));
            colors.put("soft", hex(mix(Ui.CARD, Ui.GOLD, dark ? .14f : .10f)));
            colors.put("hijri", hex(Ui.GOLD));
            colors.put("desi", hex(Ui.MUTED));
            colors.put("event", hex(Ui.GOLD));
            colors.put("nav", hex(Ui.CARD));
        } catch (org.json.JSONException ignored) { }
        return colors.toString();
    }
}
