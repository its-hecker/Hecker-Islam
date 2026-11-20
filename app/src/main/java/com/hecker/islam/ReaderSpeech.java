package com.hecker.islam;

import android.app.Activity;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONTokener;
import java.util.Locale;

/** Speech controls shared by Shia dua and hadith readers. Uses the device TTS
 * engine (not a human reciter recording) and never ships page content to another service.
 */
final class ReaderSpeech {
    private final Activity activity;
    private TextToSpeech speech;
    private TextView control;
    private String pendingText;
    private Locale pendingLanguage;
    private boolean ready, failed, speaking, disposed;
    private int requestId;
    private volatile String lastUtteranceId;

    ReaderSpeech(Activity activity) {
        this.activity = activity;
        speech = new TextToSpeech(activity.getApplicationContext(), status -> {
            if (disposed) return;
            ready = status == TextToSpeech.SUCCESS;
            failed = !ready;
            if (ready && speech != null) {
                speech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) { }
                    @Override public void onDone(String utteranceId) { complete(utteranceId); }
                    @Override public void onError(String utteranceId) { complete(utteranceId); }
                });
            }
            if (ready && pendingText != null) {
                String text = pendingText;
                Locale language = pendingLanguage;
                pendingText = null;
                speakNow(text, language);
            } else if (failed) {
                pendingText = null;
                toast("Speech engine unavailable. Install or enable Android text-to-speech.");
                updateControl();
            }
        });
    }

    void attach(TextView view) {
        control = view;
        updateControl();
    }

    void playOrStop(String text, Locale locale) {
        if (speaking) { stop(); return; }
        speak(text, locale);
    }

    void speak(String text, Locale locale) {
        if (disposed || text == null || text.trim().isEmpty()) {
            toast("No recitable text is available here.");
            return;
        }
        stop();
        String clean = text.trim().replaceAll("[ \\t]{2,}", " ");
        if (failed) {
            toast("Android text-to-speech is not available on this device.");
            return;
        }
        if (!ready) {
            pendingText = clean;
            pendingLanguage = locale;
            if (control != null) control.setText("Preparing voice…");
            return;
        }
        speakNow(clean, locale);
    }

    private void speakNow(String text, Locale locale) {
        if (disposed || speech == null) return;
        int result = speech.setLanguage(locale);
        if (result == TextToSpeech.LANG_NOT_SUPPORTED
                || result == TextToSpeech.LANG_MISSING_DATA) {
            toast("Install an " + locale.getDisplayLanguage(Locale.ENGLISH)
                    + " speech voice in your phone's text-to-speech settings.");
            return;
        }
        speech.setSpeechRate(0.90f);
        // Avoid Android's per-utterance text limit. Prefer complete lines/sentences.
        int chunkSize = Math.min(2800, TextToSpeech.getMaxSpeechInputLength() - 100);
        int pos = 0, part = 0;
        while (pos < text.length()) {
            int end = Math.min(text.length(), pos + chunkSize);
            if (end < text.length()) {
                int boundary = Math.max(text.lastIndexOf('۔', end - 1),
                    Math.max(text.lastIndexOf('.', end - 1), text.lastIndexOf((char) 10, end - 1)));
                if (boundary > pos + chunkSize / 2) end = boundary + 1;
            }
            String piece = text.substring(pos, end).trim();
            if (!piece.isEmpty()) {
                String id = "hecker-reader-" + (++requestId);
                lastUtteranceId = id;
                speech.speak(piece, part == 0 ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD,
                        null, id);
            }
            pos = end;
            part++;
        }
        speaking = part > 0;
        updateControl();
    }

    /**
     * Read highlighted text first. Otherwise read the main article / reading
     * area, avoiding headers and navigation; selected text lets a user target
     * a single hadith or paragraph in a large source collection.
     */
    void reciteFromWeb(WebView web) {
        if (speaking) { stop(); return; }
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
                if (!(parsed instanceof String)) { toast("Page text is not ready. Try selecting a passage."); return; }
                String text = ((String) parsed).trim();
                if (text.length() < 4) {
                    toast("Highlight the text you want to recite and try again.");
                    return;
                }
                Locale language = detect(text);
                speak(text, language);
            } catch (Exception e) {
                toast("Unable to read the web page. Try selecting a passage.");
            }
        });
    }

    static Locale detect(String text) {
        int urdu = 0, arabic = 0, latin = 0;
        for (int i = 0; i < Math.min(text.length(), 5000); i++) {
            char c = text.charAt(i);
            if (c == 'ٹ' || c == 'ڈ' || c == 'ڑ' || c == 'ں' || c == 'ے' || c == 'ھ') urdu += 5;
            else if (c >= 0x0600 && c <= 0x06ff) arabic++;
            else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) latin++;
        }
        if (urdu > 2) return new Locale("ur", "PK");
        if (arabic > latin / 2) return new Locale("ar");
        return Locale.ENGLISH;
    }

    private void complete(String id) {
        if (disposed || !id.equals(lastUtteranceId)) return;
        activity.runOnUiThread(() -> {
            if (disposed || !id.equals(lastUtteranceId)) return;
            speaking = false;
            updateControl();
        });
    }

    void stop() {
        lastUtteranceId = null;
        pendingText = null;
        speaking = false;
        if (speech != null) speech.stop();
        updateControl();
    }
    void destroy() {
        disposed = true;
        if (speech != null) { speech.stop(); speech.shutdown(); speech = null; }
    }
    private void updateControl() {
        if (control != null) control.setText(speaking ? "■ Stop recitation" : "▶ Recite • device voice");
    }
    private void toast(String value) {
        Toast.makeText(activity, value, Toast.LENGTH_LONG).show();
    }
}
