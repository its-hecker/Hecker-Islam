package com.hecker.islam;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONTokener;
import java.util.Locale;

/** UI controls for the foreground recitation service.
 * Playback deliberately outlives an Activity, tab switch or screen lock.
 */
final class ReaderSpeech {
    private final Activity activity;
    private TextView control;
    private String idleLabel = "▶ Recite • device voice";
    private String lastWebText;
    private Locale lastWebLanguage;
    private boolean disposed;
    private static final float[] SPEEDS = {0.65f, 0.80f, 1.00f, 1.20f, 1.40f, 1.60f};
    private static final String[] SPEED_LABELS = {"0.65x • Very slow", "0.80x • Slow", "1.00x • Normal", "1.20x • Fast", "1.40x • Faster", "1.60x • Very fast"};
    private float speed;

    private final BroadcastReceiver stateChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { updateControl(); }
    };

    ReaderSpeech(Activity activity) {
        this.activity = activity;
        speed = activity.getSharedPreferences("islam_reader", Activity.MODE_PRIVATE)
                .getFloat("tts_speed", 1.00f);
        if (speed < 0.65f || speed > 1.60f) speed = 1.00f;
        IntentFilter filter = new IntentFilter(RecitationService.STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33)
            activity.registerReceiver(stateChanged, filter, Context.RECEIVER_NOT_EXPORTED);
        else activity.registerReceiver(stateChanged, filter);
    }

    String speedLabel() { return String.format(Locale.ENGLISH, "%.2fx", speed); }

    void chooseSpeed(TextView indicator) {
        int selected = 2;
        for (int i = 0; i < SPEEDS.length; i++) {
            if (Math.abs(SPEEDS[i] - speed) < 0.011f) { selected = i; break; }
        }
        new android.app.AlertDialog.Builder(activity)
                .setTitle("Recitation speed")
                .setSingleChoiceItems(SPEED_LABELS, selected, (dialog, which) -> {
                    speed = SPEEDS[which];
                    activity.getSharedPreferences("islam_reader", Activity.MODE_PRIVATE)
                            .edit().putFloat("tts_speed", speed).apply();
                    RecitationService.speed(activity, speed);
                    if (indicator != null)
                        indicator.setText("Recitation speed • " + speedLabel() + "  ›");
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null).show();
    }

    void attach(TextView view) { attach(view, "▶ Recite • device voice"); }
    void attach(TextView view, String label) {
        control = view;
        idleLabel = label;
        updateControl();
    }
    void playOrStop(String words, Locale language) { speak(words, language); }
    void speak(String words, Locale language) {
        if (disposed || words == null || words.trim().isEmpty()) {
            toast("No recitable text is available here."); return;
        }
        RecitationService.toggleText(activity, words, language, speed);
        updateControl();
    }
    void stop() {
        RecitationService.stop(activity);
        updateControl();
    }
    /**
     * Read selected text first, or article content. Repeated tapping pauses or
     * resumes the same selection instead of resetting to its first sentence.
     */
    void reciteFromWeb(WebView web) {
        if (lastWebText != null && lastWebLanguage != null
                && RecitationService.matchesText(activity, lastWebText, lastWebLanguage)) {
            speak(lastWebText, lastWebLanguage);
            return;
        }
        if (web == null) { toast("Open a dua or hadith first."); return; }
        final String javascript =
            "(function() { try {"
            + "var selection=window.getSelection?String(window.getSelection()):'';"
            + "if(selection.trim().length>3)return selection.slice(0,12000);"
            + "var main=document.querySelector('[itemprop=articleBody],article,.entry-content,.post-content,.content-body,main,[role=main]')||document.body;"
            + "var copy=main.cloneNode(true);"
            + "copy.querySelectorAll('script,style,nav,header,footer,aside,button,form,[role=navigation]').forEach(function(e){e.remove()});"
            + "return String(copy.innerText||copy.textContent||'').trim().slice(0,12000);"
            + "}catch(e){return '';}})()";
        web.evaluateJavascript(javascript, raw -> {
            if (disposed || activity.isFinishing() || activity.isDestroyed()) return;
            try {
                Object parsed = new JSONTokener(raw).nextValue();
                if (!(parsed instanceof String)) {
                    toast("Page text is not ready. Try selecting a passage."); return;
                }
                String words = ((String) parsed).trim();
                if (words.length() < 4) {
                    toast("Highlight the text you want to recite and try again."); return;
                }
                lastWebText = words;
                lastWebLanguage = detect(words);
                speak(words, lastWebLanguage);
            } catch (Exception e) {
                toast("Unable to read the web page. Try selecting a passage.");
            }
        });
    }

    static Locale detect(String words) {
        int urdu = 0, arabic = 0, latin = 0;
        for (int i = 0; i < Math.min(words.length(), 5000); i++) {
            char c = words.charAt(i);
            if (c == 'ٹ' || c == 'ڈ' || c == 'ڑ' || c == 'ں' || c == 'ے' || c == 'ھ') urdu += 5;
            else if (c >= 0x0600 && c <= 0x06ff) arabic++;
            else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) latin++;
        }
        if (urdu > 2) return new Locale("ur", "PK");
        if (arabic > latin / 2) return new Locale("ar");
        return Locale.ENGLISH;
    }
    private void updateControl() {
        if (control == null) return;
        boolean active = RecitationService.hasPlayback(activity);
        control.setText(active && RecitationService.isLoading(activity) ? "◌ Preparing recitation…"
                : active && RecitationService.isPlaying(activity) ? "Ⅱ Pause recitation"
                : active ? "▶ Resume recitation" : idleLabel);
    }
    void destroy() {
        if (disposed) return;
        disposed = true;
        activity.unregisterReceiver(stateChanged);
        control = null;
    }
    private void toast(String value) { Toast.makeText(activity, value, Toast.LENGTH_LONG).show(); }
}
