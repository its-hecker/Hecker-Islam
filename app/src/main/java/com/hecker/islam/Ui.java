package com.hecker.islam;

import android.app.Activity;
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

final class Ui {
    static final int BACKGROUND = Color.rgb(9, 23, 21);
    static final int CARD = Color.rgb(21, 45, 39);
    static final int GOLD = Color.rgb(226, 192, 121);
    static final int WHITE = Color.rgb(245, 246, 239);
    static final int MUTED = Color.rgb(158, 182, 170);
    static final int OUTLINE = Color.rgb(50, 80, 66);
    private Ui() {}

    static int dp(Context c, int px) { return (int) (c.getResources().getDisplayMetrics().density * px + .5f); }
    static LinearLayout column(Context c) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }
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
        bg.setColor(CARD); bg.setCornerRadius(dp(c, 22));
        bg.setStroke(dp(c, 1), OUTLINE);
        v.setBackground(bg); v.setPadding(dp(c, 18), dp(c, 18), dp(c, 18), dp(c, 18));
        return v;
    }
    static void space(Context c, LinearLayout parent, int height) {
        Space v = new Space(c); parent.addView(v, new LinearLayout.LayoutParams(1, dp(c, height)));
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
