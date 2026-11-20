package com.hecker.islam;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

/** Foreground, screen-off-safe playback shared by Quran streams and all text readers.
 * Stores the active item and a playback checkpoint, including when the app is closed.
 */
public final class RecitationService extends Service {
    public static final String STATE_CHANGED = "com.hecker.islam.RECITATION_STATE_CHANGED";
    private static final String PLAY_QURAN = "recitation.PLAY_QURAN";
    private static final String PLAY_TEXT = "recitation.PLAY_TEXT";
    private static final String RESUME = "recitation.RESUME";
    private static final String PAUSE = "recitation.PAUSE";
    private static final String TOGGLE = "recitation.TOGGLE";
    private static final String STOP = "recitation.STOP";
    private static final String SET_SPEED = "recitation.SET_SPEED";
    private static final String PREFERENCES = "hecker_recitation";
    private static final String CHANNEL = "hecker_recitation_media";
    private static final int NOTIFICATION_ID = 4207;
    private static final String TAG = "HeckerRecitation";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences state;
    private String key = "", kind = "", text = "", localeTag = "ar", title = "";
    private int surah, positionMs, textOffset, segmentEnd, source, serial;
    private float rate = 1.0f;
    private boolean playing, loading, foreground, speechReady;
    private MediaPlayer player;
    private TextToSpeech speech;
    private AudioManager audio;
    private AudioFocusRequest focus;
    private PowerManager.WakeLock speechWake;
    private Runnable timeout;
    private final Runnable checkpoint = new Runnable() {
        @Override public void run() {
            savePosition();
            if (playing && "quran".equals(kind)) handler.postDelayed(this, 3000);
        }
    };

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
    private static String textKey(String words, Locale language) {
        return "text:" + language.toLanguageTag() + ":" + words.length() + ":" + words.hashCode();
    }
    public static boolean matchesQuran(Context context, int number) {
        return ("quran:" + number).equals(prefs(context).getString("key", ""));
    }
    public static boolean matchesText(Context context, String words, Locale language) {
        return words != null && textKey(words.trim(), language).equals(prefs(context).getString("key", ""));
    }
    public static boolean isPlaying(Context context) {
        return prefs(context).getBoolean("playing", false);
    }
    public static boolean isLoading(Context context) {
        return prefs(context).getBoolean("loading", false);
    }
    public static boolean hasPlayback(Context context) {
        return !prefs(context).getString("key", "").isEmpty();
    }
    private static void dispatch(Context context, Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
        else context.startService(intent);
    }
    public static void toggleQuran(Context context, int number, float speed) {
        if (number < 1 || number > 114) return;
        Intent intent = new Intent(context, RecitationService.class);
        if (matchesQuran(context, number)) {
            intent.setAction(isPlaying(context) || isLoading(context) ? PAUSE : RESUME);
        } else {
            intent.setAction(PLAY_QURAN);
            intent.putExtra("surah", number);
            intent.putExtra("rate", speed);
        }
        dispatch(context, intent);
    }
    public static void toggleText(Context context, String words, Locale language, float speed) {
        if (words == null || words.trim().isEmpty()) return;
        String clean = words.trim().replaceAll("[ \\t]{2,}", " ");
        Intent intent = new Intent(context, RecitationService.class);
        if (matchesText(context, clean, language)) {
            intent.setAction(isPlaying(context) || isLoading(context) ? PAUSE : RESUME);
        } else {
            intent.setAction(PLAY_TEXT);
            intent.putExtra("words", clean);
            intent.putExtra("locale", language.toLanguageTag());
            intent.putExtra("rate", speed);
        }
        dispatch(context, intent);
    }
    public static void stop(Context context) {
        if (hasPlayback(context)) dispatch(context,
                new Intent(context, RecitationService.class).setAction(STOP));
    }
    public static void speed(Context context, float rate) {
        if (!hasPlayback(context)) return;
        dispatch(context, new Intent(context, RecitationService.class)
                .setAction(SET_SPEED).putExtra("rate", rate));
    }

    @Override public void onCreate() {
        super.onCreate();
        state = prefs(this);
        audio = getSystemService(AudioManager.class);
        PowerManager manager = getSystemService(PowerManager.class);
        speechWake = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HeckerIslam:recitation");
        speechWake.setReferenceCounted(false);
        NotificationManager notifications = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Recitation playback",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Background Quran and reading playback controls");
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notifications.createNotificationChannel(channel);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) { stopSelf(); return START_NOT_STICKY; }
        String action = intent.getAction();
        if (STOP.equals(action)) { finishPlayback(); return START_NOT_STICKY; }
        if (PLAY_QURAN.equals(action) || PLAY_TEXT.equals(action)) {
            releaseResources();
            key = ""; text = ""; textOffset = 0; positionMs = 0;
            if (PLAY_QURAN.equals(action)) {
                kind = "quran";
                surah = intent.getIntExtra("surah", 0);
                if (surah < 1 || surah > 114) { stopSelf(); return START_NOT_STICKY; }
                key = "quran:" + surah;
                title = "Qur'an • Surah " + surah + " • Mishary Alafasy";
            } else {
                kind = "text";
                text = intent.getStringExtra("words");
                localeTag = intent.getStringExtra("locale");
                if (text == null || text.isEmpty() || localeTag == null) {
                    stopSelf(); return START_NOT_STICKY;
                }
                key = textKey(text, Locale.forLanguageTag(localeTag));
                title = "Recitation • device speech";
            }
            rate = intent.getFloatExtra("rate", 1f);
            source = 0;
            playing = true; loading = true;
            saveState();
            ensureForeground();
            begin();
        } else {
            if (key.isEmpty()) restoreState();
            if (key.isEmpty()) { stopSelf(); return START_NOT_STICKY; }
            ensureForeground();
            if (SET_SPEED.equals(action)) {
                rate = intent.getFloatExtra("rate", 1f);
                if (player != null && playing) applyPlaybackRate();
                if (speech != null && speechReady) speech.setSpeechRate(rate);
                saveState(); refreshNotification(); return START_NOT_STICKY;
            }
            boolean shouldPause = PAUSE.equals(action)
                    || (TOGGLE.equals(action) && (playing || loading));
            if (shouldPause) pausePlayback();
            else if (!playing) resumePlayback();
        }
        return START_NOT_STICKY;
    }

    private void restoreState() {
        key = state.getString("key", "");
        kind = state.getString("kind", "");
        surah = state.getInt("surah", 0);
        title = state.getString("title", "Recitation");
        text = state.getString("text", "");
        localeTag = state.getString("locale", "ar");
        textOffset = state.getInt("offset", 0);
        positionMs = state.getInt("position", 0);
        rate = state.getFloat("rate", 1f);
        // A new service has no player, regardless of the previous process state.
        playing = false; loading = false;
        if ((!kind.equals("quran") && !kind.equals("text"))
                || (kind.equals("text") && text.isEmpty())) key = "";
    }

    private void savePosition() {
        if (player != null && "quran".equals(kind)) {
            try { positionMs = Math.max(0, player.getCurrentPosition()); }
            catch (IllegalStateException ignored) { }
        }
        if (!key.isEmpty()) state.edit()
                .putInt("position", positionMs).putInt("offset", textOffset).apply();
    }
    private void saveState() {
        state.edit().putString("key", key).putString("kind", kind)
                .putInt("surah", surah).putString("title", title)
                .putString("text", text).putString("locale", localeTag)
                .putInt("position", positionMs).putInt("offset", textOffset)
                .putFloat("rate", rate).putBoolean("playing", playing)
                .putBoolean("loading", loading).apply();
        sendBroadcast(new Intent(STATE_CHANGED).setPackage(getPackageName()));
        refreshNotification();
    }
    private void ensureForeground() {
        Notification notification = notification();
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        foreground = true;
    }
    private PendingIntent action(String which, int requestCode) {
        Intent intent = new Intent(this, RecitationService.class).setAction(which);
        return PendingIntent.getService(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private Notification notification() {
        Intent open = new Intent(this, "quran".equals(kind)
                ? ReferenceReadingActivity.class : IslamDashboardActivity.class);
        if ("quran".equals(kind)) open.putExtra("section", "quran");
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openIntent = PendingIntent.getActivity(this, 3, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        boolean active = playing || loading;
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(com.hecker.azaan.R.drawable.ic_notification)
                .setContentTitle(title.isEmpty() ? "Hecker Islam" : title)
                .setContentText(loading ? "Preparing recitation…" : playing
                        ? "Playing in background" : "Paused • tap to resume")
                .setContentIntent(openIntent).setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true).setOngoing(active)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause,
                        active ? "Pause" : "Resume", action(active ? PAUSE : RESUME, 1)).build())
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Stop", action(STOP, 2)).build())
                .build();
    }
    private void refreshNotification() {
        if (foreground) getSystemService(NotificationManager.class)
                .notify(NOTIFICATION_ID, notification());
    }

    private boolean requestFocus() {
        if (focus != null) return true;
        AudioAttributes attr = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
        focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attr)
                .setOnAudioFocusChangeListener(change -> {
                    if (change == AudioManager.AUDIOFOCUS_LOSS
                            || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
                        handler.post(this::pausePlayback);
                }, handler).build();
        if (audio.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            return true;
        focus = null;
        return false;
    }
    private void begin() {
        if (!requestFocus()) { pausePlayback(); return; }
        if ("quran".equals(kind)) connectSource(0);
        else prepareSpeech();
    }
    private static String recitationUrl(int number, int source) {
        String surah = String.format(Locale.US, "%03d.mp3", number);
        if (source == 0) return "https://server8.mp3quran.net/afs/" + surah;
        if (source == 1) return "https://cdn.mp3quran.net/audio/mishary-alafasy/r1/" + surah;
        return "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/" + number + ".mp3";
    }
    private void connectSource(int nextSource) {
        releasePlayer();
        source = nextSource;
        if (!playing) return;
        if (source >= 3) {
            Log.w(TAG, "All Quran audio sources unavailable");
            pausePlayback();
            return;
        }
        loading = true; saveState();
        try {
            MediaPlayer pending = new MediaPlayer();
            player = pending;
            pending.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            pending.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            pending.setDataSource(recitationUrl(surah, source));
            pending.setOnPreparedListener(p -> {
                if (player != p || !playing) return;
                clearTimeout();
                if (positionMs > 0) {
                    p.setOnSeekCompleteListener(seek -> { if (seek == player && playing) startMedia(); });
                    p.seekTo(positionMs);
                } else startMedia();
            });
            pending.setOnCompletionListener(p -> {
                if (p == player) finishPlayback();
            });
            pending.setOnErrorListener((p, what, extra) -> {
                if (p == player && playing) {
                    Log.w(TAG, "Quran source " + source + " failed: " + what + "/" + extra);
                    savePosition();
                    connectSource(source + 1);
                }
                return true;
            });
            pending.prepareAsync();
            timeout = () -> {
                if (pending == player && loading && playing) connectSource(source + 1);
            };
            handler.postDelayed(timeout, 14000);
        } catch (Exception ex) {
            Log.w(TAG, "Cannot open Quran audio source", ex);
            connectSource(source + 1);
        }
    }
    private void startMedia() {
        if (player == null || !playing) return;
        try {
            player.start();
            applyPlaybackRate();
            loading = false;
            saveState();
            handler.removeCallbacks(checkpoint);
            handler.postDelayed(checkpoint, 3000);
        } catch (RuntimeException ex) {
            Log.w(TAG, "Cannot start Quran stream", ex);
            connectSource(source + 1);
        }
    }
    private void applyPlaybackRate() {
        try { player.setPlaybackParams(new PlaybackParams().setSpeed(rate)); }
        catch (RuntimeException ex) { Log.w(TAG, "Playback speed not supported", ex); }
    }

    private void prepareSpeech() {
        if (speech != null && speechReady) {
            speakNext(); return;
        }
        if (speech != null) { speech.shutdown(); speech = null; }
        loading = true; saveState();
        final int token = ++serial;
        speech = new TextToSpeech(getApplicationContext(), status -> handler.post(() -> {
            if (token != serial || !playing || speech == null) return;
            if (status != TextToSpeech.SUCCESS) { pausePlayback(); return; }
            int supported = speech.setLanguage(Locale.forLanguageTag(localeTag));
            if (supported == TextToSpeech.LANG_MISSING_DATA
                    || supported == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Missing speech voice for " + localeTag);
                pausePlayback(); return;
            }
            speech.setSpeechRate(rate);
            speech.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            speech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) { }
                @Override public void onRangeStart(String id, int start, int end, int frame) {
                    handler.post(() -> {
                        if (id.equals(activeUtterance) && playing) {
                            textOffset = Math.min(text.length(), segmentStart + start);
                            savePosition();
                        }
                    });
                }
                @Override public void onDone(String id) { handler.post(() -> {
                    if (id.equals(activeUtterance) && playing) {
                        textOffset = segmentEnd;
                        savePosition();
                        speakNext();
                    }
                }); }
                @Override public void onError(String id) { handler.post(() -> {
                    if (id.equals(activeUtterance)) pausePlayback();
                }); }
            });
            speechReady = true;
            speakNext();
        }));
    }
    private int segmentStart;
    private String activeUtterance;
    private void speakNext() {
        if (!playing || speech == null || !speechReady) return;
        if (textOffset >= text.length()) { finishPlayback(); return; }
        int end = Math.min(text.length(), textOffset + 240);
        if (end < text.length()) {
            int boundary = Math.max(text.lastIndexOf('۔', end - 1),
                    Math.max(text.lastIndexOf('.', end - 1),
                            text.lastIndexOf('\n', end - 1)));
            if (boundary > textOffset + 80) end = boundary + 1;
            else {
                int space = text.lastIndexOf(' ', end - 1);
                if (space > textOffset + 80) end = space + 1;
            }
        }
        segmentStart = textOffset;
        segmentEnd = end;
        activeUtterance = "segment-" + serial + "-" + segmentStart;
        loading = false;
        saveState();
        if (speechWake != null) speechWake.acquire(90000L);
        if (speech.speak(text.substring(segmentStart, segmentEnd),
                TextToSpeech.QUEUE_FLUSH, null, activeUtterance) == TextToSpeech.ERROR)
            pausePlayback();
    }
    private void pausePlayback() {
        if (key.isEmpty()) return;
        savePosition();
        boolean wasLoading = loading;
        playing = false; loading = false; serial++;
        activeUtterance = null;
        handler.removeCallbacks(checkpoint);
        clearTimeout();
        if (player != null) {
            if (wasLoading) releasePlayer();
            else try { if (player.isPlaying()) player.pause(); }
                 catch (RuntimeException ignored) { }
        }
        if (speech != null) speech.stop();
        if (speechWake != null && speechWake.isHeld()) speechWake.release();
        if (focus != null) { audio.abandonAudioFocusRequest(focus); focus = null; }
        saveState();
    }
    private void resumePlayback() {
        if (!requestFocus()) return;
        playing = true;
        if ("quran".equals(kind)) {
            if (player != null && !loading) {
                startMedia(); return;
            }
            connectSource(0);
        } else if (speech != null && speechReady) speakNext();
        else prepareSpeech();
        saveState();
    }
    private void clearTimeout() {
        if (timeout != null) handler.removeCallbacks(timeout);
        timeout = null;
    }
    private void releasePlayer() {
        clearTimeout();
        if (player != null) {
            try { player.release(); } catch (RuntimeException ignored) { }
            player = null;
        }
    }
    private void releaseResources() {
        handler.removeCallbacks(checkpoint);
        clearTimeout();
        releasePlayer();
        serial++;
        if (speech != null) { speech.stop(); speech.shutdown(); speech = null; }
        speechReady = false;
        if (speechWake != null && speechWake.isHeld()) speechWake.release();
        if (focus != null) { audio.abandonAudioFocusRequest(focus); focus = null; }
    }
    private void finishPlayback() {
        savePosition();
        releaseResources();
        key = ""; kind = ""; text = ""; playing = false; loading = false;
        positionMs = 0; textOffset = 0;
        saveState();
        stopForeground(STOP_FOREGROUND_REMOVE);
        foreground = false;
        stopSelf();
    }
    @Override public void onDestroy() {
        savePosition();
        // Do not erase the checkpoint if Android stops the service or process.
        if (!key.isEmpty()) {
            playing = false; loading = false;
            saveState();
        }
        releaseResources();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
