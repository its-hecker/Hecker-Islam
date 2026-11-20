package com.hecker.islam;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Material You-style About panel, matching the Hecker Islam card palette. */
final class AboutHeckerDialog {
    private AboutHeckerDialog() { }

    static void show(Activity activity) {
        Ui.applyTheme(activity);
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout frame = Ui.column(activity);
        frame.setPadding(Ui.dp(activity, 14), Ui.dp(activity, 10),
                Ui.dp(activity, 14), Ui.dp(activity, 10));
        LinearLayout surface = Ui.panel(activity);
        surface.setPadding(Ui.dp(activity, 24), Ui.dp(activity, 25),
                Ui.dp(activity, 24), Ui.dp(activity, 24));
        frame.addView(surface);

        TextView icon = Ui.text(activity, "☾", 45, Ui.GOLD, true);
        icon.setGravity(Gravity.CENTER);
        surface.addView(icon);
        Ui.space(activity, surface, 5);
        TextView title = Ui.text(activity, "Hecker Islam", 27, Ui.WHITE, true);
        title.setGravity(Gravity.CENTER);
        surface.addView(title);
        Ui.space(activity, surface, 6);
        TextView subtitle = Ui.text(activity, "ABOUT THE APP", 11, Ui.GOLD, true);
        subtitle.setLetterSpacing(.19f);
        subtitle.setGravity(Gravity.CENTER);
        surface.addView(subtitle);
        Ui.space(activity, surface, 22);

        credit(activity, surface, "DEVELOPED BY", "Syed Ali Raza Kazmi");
        Ui.space(activity, surface, 12);
        credit(activity, surface, "SPECIAL CREDITS TO", "Sabber Kazmi");
        Ui.space(activity, surface, 23);
        TextView close = Ui.text(activity, "Close", 16, Ui.WHITE, true);
        close.setGravity(Gravity.CENTER);
        close.setPadding(0, Ui.dp(activity, 14), 0, Ui.dp(activity, 14));
        GradientDrawable action = new GradientDrawable();
        action.setColor(Ui.BACKGROUND);
        action.setCornerRadius(Ui.dp(activity, 22));
        action.setStroke(Ui.dp(activity, 1), Ui.OUTLINE);
        close.setBackground(action);
        close.setOnClickListener(v -> dialog.dismiss());
        surface.addView(close);
        dialog.setContentView(frame);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(.62f);
        }
        dialog.show();
        if (window != null) {
            int width = activity.getResources().getDisplayMetrics().widthPixels - Ui.dp(activity, 28);
            window.setLayout(Math.min(width, Ui.dp(activity, 425)), WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
    }

    private static void credit(Activity activity, LinearLayout parent, String label, String name) {
        LinearLayout box = Ui.column(activity);
        box.setPadding(Ui.dp(activity, 18), Ui.dp(activity, 14),
                Ui.dp(activity, 18), Ui.dp(activity, 14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Ui.BACKGROUND);
        bg.setCornerRadius(Ui.dp(activity, 20));
        bg.setStroke(Ui.dp(activity, 1), Ui.OUTLINE);
        box.setBackground(bg);
        TextView heading = Ui.text(activity, label, 11, Ui.MUTED, true);
        heading.setLetterSpacing(.11f);
        heading.setGravity(Gravity.CENTER);
        box.addView(heading);
        Ui.space(activity, box, 7);
        TextView value = Ui.text(activity, name, 18, Ui.WHITE, true);
        value.setGravity(Gravity.CENTER);
        box.addView(value);
        parent.addView(box);
    }
}
