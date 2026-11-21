package com.hecker.islam;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
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
        new AlertDialog.Builder(this)
                .setTitle("Welcome • خوش آمدید")
                .setMessage("Assalamualaikum Ya Ali (A.S Madad Syed)")
                .setCancelable(false)
                .setPositiveButton("Continue", (dialog, which) -> enterDashboard())
                .show();
    }

    private void showDeclined() {
        new AlertDialog.Builder(this)
                .setTitle("Hecker Islam")
                .setMessage("This app is designed around the Shia devotional tradition selected by its creator. You may leave or reconsider your choice.")
                .setCancelable(false)
                .setPositiveButton("Choose again", (dialog, which) -> render())
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .show();
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
                "Are you Qayal of ‘Aliyyun Waliullah’ in your devotional practice of Namaz and Azaan?",
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
