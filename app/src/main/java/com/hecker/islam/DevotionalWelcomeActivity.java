package com.hecker.islam;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Optional remembered choice for the app's devotional identity.
 * The preference is local to the device and not uploaded or logged.
 * It is a user preference, not a pronouncement about legal validity of prayer.
 */
public final class DevotionalWelcomeActivity extends Activity {
    private static final String SETTINGS = "hecker_devotional_welcome";
    private static final String REMEMBERED = "accepted_dont_ask";
    private CheckBox dontAsk;

    @Override public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (getSharedPreferences(SETTINGS, MODE_PRIVATE).getBoolean(REMEMBERED, false)) {
            enterDashboard();
            return;
        }
        render();
    }

    private void enterDashboard() {
        Intent intent = new Intent(this, IslamDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void showAccepted() {
        if (dontAsk != null && dontAsk.isChecked()) {
            getSharedPreferences(SETTINGS, MODE_PRIVATE)
                    .edit().putBoolean(REMEMBERED, true).apply();
        }
        showDevotionalDialog(true);
    }

    private void showDeclined() {
        showDevotionalDialog(false);
    }

    /** Rounded, dynamic-color popup matching the app's cards and action surfaces. */
    private void showDevotionalDialog(boolean accepted) {
        Ui.applyTheme(this);
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        LinearLayout frame = Ui.column(this);
        frame.setPadding(Ui.dp(this, 6), Ui.dp(this, 8),
                Ui.dp(this, 6), Ui.dp(this, 8));
        LinearLayout card = Ui.panel(this);
        card.setPadding(Ui.dp(this, 23), Ui.dp(this, 25),
                Ui.dp(this, 23), Ui.dp(this, 23));
        frame.addView(card);

        TextView emblem = Ui.text(this, accepted ? "☪" : "☾", 38, Ui.GOLD, true);
        emblem.setGravity(Gravity.CENTER);
        card.addView(emblem);
        Ui.space(this, card, 9);

        if (accepted) {
            TextView greeting = Ui.text(this,
                    "Assalamualaikum Ya Ali (A.S) Madad", 22, Ui.WHITE, true);
            greeting.setGravity(Gravity.CENTER);
            greeting.setLineSpacing(Ui.dp(this, 5), 1.12f);
            card.addView(greeting);
            Ui.space(this, card, 21);
            card.addView(dialogAction(dialog, "Continue  ›", true,
                    this::enterDashboard));
        } else {
            TextView heading = Ui.text(this, "HECKER ISLAM", 12, Ui.GOLD, true);
            heading.setGravity(Gravity.CENTER);
            heading.setLetterSpacing(.13f);
            card.addView(heading);
            Ui.space(this, card, 15);

            TextView notice = Ui.text(this,
                    "This app is for Momineen Only.", 21, Ui.WHITE, true);
            notice.setGravity(Gravity.CENTER);
            notice.setLineSpacing(Ui.dp(this, 3), 1.08f);
            card.addView(notice);
            Ui.space(this, card, 10);

            TextView guidance = Ui.text(this,
                    "If this reflects your devotional practice, you can choose Yes. Otherwise, you can exit the app.",
                    14, Ui.MUTED, false);
            guidance.setGravity(Gravity.CENTER);
            guidance.setLineSpacing(Ui.dp(this, 3), 1.08f);
            card.addView(guidance);
            Ui.space(this, card, 23);

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            TextView exit = dialogAction(dialog, "Exit", false, this::finish);
            LinearLayout.LayoutParams exitParams =
                    new LinearLayout.LayoutParams(0, -2, 1);
            actions.addView(exit, exitParams);
            TextView retry = dialogAction(dialog, "Choose again", true, this::render);
            LinearLayout.LayoutParams retryParams =
                    new LinearLayout.LayoutParams(0, -2, 1.55f);
            retryParams.setMargins(Ui.dp(this, 10), 0, 0, 0);
            actions.addView(retry, retryParams);
            card.addView(actions);
        }

        dialog.setContentView(frame);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(.62f);
        }
        dialog.show();
        if (window != null) {
            int usable = getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 28);
            window.setLayout(Math.min(usable, Ui.dp(this, 425)),
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
    }

    private TextView dialogAction(Dialog dialog, String label, boolean primary,
                                  Runnable action) {
        TextView button = Ui.text(this, label, 15,
                primary ? Ui.BACKGROUND : Ui.WHITE, true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(Ui.dp(this, 10), Ui.dp(this, 15),
                Ui.dp(this, 10), Ui.dp(this, 15));
        GradientDrawable background = new GradientDrawable();
        background.setColor(primary ? Ui.GOLD : Ui.BACKGROUND);
        background.setCornerRadius(Ui.dp(this, 18));
        background.setStroke(Ui.dp(this, 1), primary ? Ui.GOLD : Ui.OUTLINE);
        button.setBackground(background);
        button.setOnClickListener(v -> {
            dialog.dismiss();
            action.run();
        });
        return button;
    }

    @Override public void onBackPressed() {
        finish();
    }

    private void render() {
        Ui.applyTheme(this);
        getWindow().setStatusBarColor(Ui.BACKGROUND);
        getWindow().setNavigationBarColor(Ui.BACKGROUND);
        boolean dark = getSharedPreferences("islam_ui", MODE_PRIVATE).getBoolean("dark", true);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0
                : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BACKGROUND);
        Ui.rootInsets(root);
        root.setPadding(Ui.dp(this, 23), Ui.dp(this, 40),
                Ui.dp(this, 23), Ui.dp(this, 24));

        TextView mark = Ui.text(this, "☪", 45, Ui.GOLD, true);
        mark.setGravity(Gravity.CENTER);
        root.addView(mark);
        Ui.space(this, root, 16);
        TextView title = Ui.text(this, "Hecker Islam", 28, Ui.WHITE, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        Ui.space(this, root, 5);
        TextView subtitle = Ui.text(this, "Ahlulbayt devotional welcome", 15, Ui.MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);
        Ui.space(this, root, 24);

        LinearLayout panel = Ui.panel(this);
        TextView question = Ui.text(this,
                "Do you believe in the testimony ‘Aliyyun Waliullah’ in your devotional practice of Namaz and Azaan?",
                20, Ui.WHITE, true);
        question.setGravity(Gravity.CENTER);
        panel.addView(question);
        Ui.space(this, panel, 13);
        TextView urdu = Ui.text(this,
                "کیا آپ نماز اور اذان کے حوالے سے علیٌ ولیُّ اللہ کی گواہی کے قائل ہیں؟",
                20, Ui.GOLD, false);
        urdu.setGravity(Gravity.CENTER);
        urdu.setTextDirection(View.TEXT_DIRECTION_RTL);
        panel.addView(urdu);
        Ui.space(this, panel, 14);
        panel.addView(Ui.text(this,
                "This asks about your personal devotional belief. It does not make a ruling about whether the testimony is a required part of Namaz or Adhan.",
                12, Ui.MUTED, false));
        Ui.space(this, panel, 17);
        dontAsk = new CheckBox(this);
        dontAsk.setText("Don't remind me again");
        dontAsk.setTextColor(Ui.WHITE);
        dontAsk.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.GOLD));
        panel.addView(dontAsk);
        root.addView(panel);
        Ui.space(this, root, 18);

        LinearLayout yes = Ui.panel(this);
        TextView yesText = Ui.text(this, "Yes · جی ہاں", 18, Ui.GOLD, true);
        yesText.setGravity(Gravity.CENTER);
        yes.addView(yesText);
        yes.setOnClickListener(v -> showAccepted());
        root.addView(yes);
        Ui.space(this, root, 10);

        LinearLayout no = Ui.panel(this);
        TextView noText = Ui.text(this, "No · نہیں", 18, Ui.WHITE, true);
        noText.setGravity(Gravity.CENTER);
        no.addView(noText);
        no.setOnClickListener(v -> showDeclined());
        root.addView(no);
        setContentView(root);
    }
}
