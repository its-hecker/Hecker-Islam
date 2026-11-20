package com.hecker.islam;

import android.content.Context;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
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
        boolean night = c.getSharedPreferences("islam_ui", Context.MODE_PRIVATE).getBoolean("dark", true);
        if (Build.VERSION.SDK_INT >= 31 && c.getSharedPreferences("islam_ui", Context.MODE_PRIVATE).getBoolean("dynamic", true)) {
            BACKGROUND = c.getColor(night ? android.R.color.system_neutral1_900 : android.R.color.system_neutral1_10);
            CARD = c.getColor(night ? android.R.color.system_neutral1_800 : android.R.color.system_neutral1_50);
            GOLD = c.getColor(night ? android.R.color.system_accent1_200 : android.R.color.system_accent1_600);
            WHITE = c.getColor(night ? android.R.color.system_neutral1_50 : android.R.color.system_neutral1_900);
            MUTED = c.getColor(night ? android.R.color.system_neutral2_300 : android.R.color.system_neutral2_600);
            OUTLINE = c.getColor(night ? android.R.color.system_neutral2_700 : android.R.color.system_neutral2_200);
            return;
        }
        BACKGROUND = Color.parseColor(night ? "#101916" : "#F5F8F2");
        CARD = Color.parseColor(night ? "#1E312A" : "#FFFFFF");
        GOLD = Color.parseColor(night ? "#BDE9C4" : "#286D54");
        WHITE = Color.parseColor(night ? "#F0F7EE" : "#173D2E");
        MUTED = Color.parseColor(night ? "#A4BCAF" : "#62766C");
        OUTLINE = Color.parseColor(night ? "#3B5748" : "#DDE8DE");
    }
    /** Keep the same floating navigation surface across all library screens. */
    static void fillNavigation(android.app.Activity activity, LinearLayout nav, int selected,
                               java.util.function.IntConsumer onSelect) {
        nav.removeAllViews();
        String[] labels = {"Today", "Qur'an", "Azkar", "Hadith", "More"};
        int[] icons = {android.R.drawable.ic_menu_today, android.R.drawable.ic_menu_agenda,
                android.R.drawable.ic_menu_compass, android.R.drawable.ic_menu_info_details,
                android.R.drawable.ic_menu_more};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            LinearLayout pill = column(activity);
            pill.setGravity(Gravity.CENTER);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(i == selected ? BACKGROUND : CARD);
            bg.setCornerRadius(dp(activity, 32));
            pill.setBackground(bg);
            android.widget.ImageView image = new android.widget.ImageView(activity);
            image.setImageResource(icons[i]);
            image.setColorFilter(i == selected ? GOLD : MUTED);
            pill.addView(image, new LinearLayout.LayoutParams(dp(activity, 23), dp(activity, 23)));
            space(activity, pill, 4);
            TextView label = text(activity, labels[i], 10, i == selected ? WHITE : MUTED, i == selected);
            label.setGravity(Gravity.CENTER);
            pill.addView(label);
            pill.setContentDescription("Open " + labels[i]);
            pill.setOnClickListener(v -> onSelect.accept(index));
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(0, -1, 1);
            size.setMargins(dp(activity, 2), 0, dp(activity, 2), 0);
            nav.addView(pill, size);
        }
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
        bg.setColor(CARD); bg.setCornerRadius(dp(c, 28)); bg.setStroke(dp(c, 1), OUTLINE);
        v.setBackground(bg);
        v.setPadding(dp(c, 18), dp(c, 19), dp(c, 18), dp(c, 19));
        // Material expressive pressed feedback: scale without swallowing Click listeners.
        v.setOnTouchListener((view, ev) -> {
            if (!view.isClickable() || !ValueAnimator.areAnimatorsEnabled()) return false;
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(.975f).scaleY(.975f).setDuration(130)
                        .setInterpolator(new DecelerateInterpolator()).start(); break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(250)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(.8f)).start(); break;
                default: break;
            }
            return false;
        });
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
