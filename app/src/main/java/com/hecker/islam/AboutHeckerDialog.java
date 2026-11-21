package com.hecker.islam;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
        Ui.space(activity, surface, 14);
        githubProfile(activity, surface);
        Ui.space(activity, surface, 21);
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

    /** GitHub brand icon and accessible profile link inside the About sheet. */
    private static void githubProfile(Activity activity, LinearLayout parent) {
        LinearLayout link = new LinearLayout(activity);
        link.setOrientation(LinearLayout.HORIZONTAL);
        link.setGravity(Gravity.CENTER_VERTICAL);
        link.setPadding(Ui.dp(activity, 18), Ui.dp(activity, 13),
                Ui.dp(activity, 18), Ui.dp(activity, 13));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Ui.BACKGROUND);
        background.setCornerRadius(Ui.dp(activity, 20));
        background.setStroke(Ui.dp(activity, 1), Ui.OUTLINE);
        link.setBackground(background);

        ImageView mark = new ImageView(activity);
        mark.setImageResource(R.drawable.ic_github_mark);
        mark.setImageTintList(ColorStateList.valueOf(Ui.GOLD));
        mark.setContentDescription(null); // The surrounding link has the accessible label.
        LinearLayout.LayoutParams iconSize = new LinearLayout.LayoutParams(
                Ui.dp(activity, 26), Ui.dp(activity, 26));
        iconSize.setMargins(0, 0, Ui.dp(activity, 14), 0);
        link.addView(mark, iconSize);

        LinearLayout caption = Ui.column(activity);
        TextView heading = Ui.text(activity, "GITHUB", 11, Ui.MUTED, true);
        heading.setLetterSpacing(.10f);
        caption.addView(heading);
        Ui.space(activity, caption, 3);
        caption.addView(Ui.text(activity, "@its-hecker", 17, Ui.WHITE, true));
        link.addView(caption, new LinearLayout.LayoutParams(0, -2, 1));

        TextView chevron = Ui.text(activity, "↗", 23, Ui.GOLD, true);
        chevron.setGravity(Gravity.CENTER);
        link.addView(chevron);
        link.setContentDescription("Open GitHub profile its-hecker");
        link.setClickable(true);
        link.setFocusable(true);
        link.setOnClickListener(v -> {
            try {
                Intent browse = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/its-hecker"));
                activity.startActivity(browse);
            } catch (android.content.ActivityNotFoundException error) {
                Toast.makeText(activity,
                        "No browser available to open GitHub.", Toast.LENGTH_LONG).show();
            }
        });
        parent.addView(link);
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
