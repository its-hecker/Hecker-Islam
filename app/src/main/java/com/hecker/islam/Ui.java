package com.hecker.islam;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

/** Shared Material 3 Expressive-inspired colors and rounded surfaces. */
final class Ui {
    static int BACKGROUND, CARD, GOLD, WHITE, MUTED, OUTLINE;
    private Ui() {}
    static void applyTheme(Context c) {
        boolean night = c.getSharedPreferences("islam_ui", Context.MODE_PRIVATE).getBoolean("dark", false);
        BACKGROUND = Color.parseColor(night ? "#101916" : "#F5F8F2");
        CARD = Color.parseColor(night ? "#1E312A" : "#FFFFFF");
        GOLD = Color.parseColor(night ? "#BDE9C4" : "#286D54");
        WHITE = Color.parseColor(night ? "#F0F7EE" : "#173D2E");
        MUTED = Color.parseColor(night ? "#A4BCAF" : "#62766C");
        OUTLINE = Color.parseColor(night ? "#3B5748" : "#DDE8DE");
    }
    static int dp(Context c, int px) { return (int) (c.getResources().getDisplayMetrics().density * px + .5f); }
    static LinearLayout column(Context c) { LinearLayout v = new LinearLayout(c); v.setOrientation(LinearLayout.VERTICAL); return v; }
    static TextView text(Context c, String value, float size, int color, boolean bold) {
        TextView v = new TextView(c);
        v.setText(value); v.setTextColor(color);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }
    static LinearLayout panel(Context c) {
        LinearLayout v = column(c);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD); bg.setCornerRadius(dp(c, 24)); bg.setStroke(dp(c, 1), OUTLINE);
        v.setBackground(bg);
        v.setPadding(dp(c, 18), dp(c, 18), dp(c, 18), dp(c, 18));
        return v;
    }
    static void space(Context c, LinearLayout parent, int height) {
        Space v = new Space(c);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(c, height)));
    }
    static void rootInsets(View v) {
        v.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        v.requestApplyInsets();
    }
}
