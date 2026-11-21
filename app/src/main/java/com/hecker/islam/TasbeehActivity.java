package com.hecker.islam;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class TasbeehActivity extends Activity {
    private static final String[] PHRASES = {
            "SubhanAllah", "Alhamdulillah", "Allahu Akbar", "Allahumma salli ala Muhammad wa aali Muhammad"};
    private static final String[] MEANINGS = {
            "Glory be to Allah", "All praise is due to Allah", "Allah is the Greatest",
            "Blessings on Muhammad and the family of Muhammad"};
    private int selection;
    private SharedPreferences prefs;
    private TextView countView, phraseView, meaningView;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("tasbeeh", MODE_PRIVATE);
        selection = prefs.getInt("selection", 0);
        if (selection < 0 || selection >= PHRASES.length) selection = 0;
        LinearLayout root = Ui.column(this); root.setBackgroundColor(Ui.BACKGROUND);
        Ui.rootInsets(root);
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout content = Ui.column(this);
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 28), Ui.dp(this, 20), Ui.dp(this, 28));
        scroll.addView(content);
        TextView back = Ui.text(this, "‹  Hecker Islam", 15, Ui.GOLD, true);
        back.setOnClickListener(v -> finish()); content.addView(back);
        Ui.space(this, content, 25);
        content.addView(Ui.text(this, "Tasbeeh", 32, Ui.WHITE, true));
        Ui.space(this, content, 5);
        content.addView(Ui.text(this, "A quiet space for remembrance", 14, Ui.MUTED, false));
        Ui.space(this, content, 30);
        LinearLayout card = Ui.panel(this);
        phraseView = Ui.text(this, "", 21, Ui.GOLD, true);
        phraseView.setGravity(Gravity.CENTER); card.addView(phraseView);
        Ui.space(this, card, 7);
        meaningView = Ui.text(this, "", 14, Ui.MUTED, false);
        meaningView.setGravity(Gravity.CENTER); card.addView(meaningView);
        Ui.space(this, card, 35);
        countView = Ui.text(this, "0", 72, Ui.WHITE, true);
        countView.setGravity(Gravity.CENTER);
        countView.setMinimumHeight(Ui.dp(this, 156));
        countView.setClickable(true);
        countView.setContentDescription("Tap to count one remembrance");
        countView.setOnClickListener(v -> {
            int value = prefs.getInt(key(), 0);
            if (value < Integer.MAX_VALUE) prefs.edit().putInt(key(), value + 1).apply();
            countView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            refresh();
            if (android.animation.ValueAnimator.areAnimatorsEnabled()) {
                countView.animate().scaleX(1.13f).scaleY(1.13f).setDuration(110)
                    .withEndAction(() -> countView.animate().scaleX(1f).scaleY(1f)
                        .setDuration(230)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(.9f)).start())
                    .start();
            }
        });
        card.addView(countView);
        TextView tap = Ui.text(this, "TAP THE NUMBER TO COUNT", 12, Ui.MUTED, true);
        tap.setGravity(Gravity.CENTER); card.addView(tap);
        Ui.space(this, card, 22);
        TextView change = Ui.text(this, "Change dhikr   ›", 17, Ui.GOLD, true);
        change.setGravity(Gravity.CENTER);
        change.setOnClickListener(v -> { selection = (selection + 1) % PHRASES.length;
            prefs.edit().putInt("selection", selection).apply(); refresh(); });
        card.addView(change);
        content.addView(card);
        Ui.space(this, content, 17);
        LinearLayout reset = Ui.panel(this);
        TextView clear = Ui.text(this, "Reset current counter", 16, Ui.WHITE, true);
        clear.setGravity(Gravity.CENTER);
        reset.addView(clear); reset.setOnClickListener(v ->
                new AlertDialog.Builder(this).setTitle("Reset count?")
                        .setMessage("Only the selected dhikr counter will be reset.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Reset", (d, w) -> { prefs.edit().putInt(key(), 0).apply(); refresh(); })
                        .show());
        content.addView(reset);
        Ui.space(this, content, 15);
        content.addView(Ui.text(this, "Counts are saved only on this device.", 13, Ui.MUTED, false));
        setContentView(root);
        refresh();
    }
    private String key() { return "count_" + selection; }
    private void refresh() {
        phraseView.setText(PHRASES[selection]);
        meaningView.setText(MEANINGS[selection]);
        countView.setText(String.valueOf(prefs.getInt(key(), 0)));
    }
}
