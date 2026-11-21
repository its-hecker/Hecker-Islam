package com.hecker.azaan;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import java.io.IOException;

/** Full-length Arabic azaan recorded by Nizar Al-Qatari, packaged inside the standalone APK. */
final class BundledAzaan {
    static final String ASSET = "azaan/nizar_al_qatari.mp3";
    /*
     * The bundled 316s recording contains an extended recited introduction.
     * The previous 25.55s seek skips only its first part; the remaining
     * introductory praises are still audible. The next long silent boundary
     * at 64.88s–67.10s precedes the Adhan proper. Begin after it so the first
     * phrase played is Allahu Akbar. This applies ONLY to the pinned bundled
     * recording (user-imported recordings must remain untouched).
     */
    static final long START_AFTER_INTRO_MS = 67_130L;
    private BundledAzaan() {}
    static boolean available(Context context) {
        try (AssetFileDescriptor file = context.getAssets().openFd(ASSET)) {
            return file.getLength() > 100_000;
        } catch (IOException exception) {
            return false;
        }
    }
    static void configure(Context context, MediaPlayer player) throws IOException {
        try (AssetFileDescriptor file = context.getAssets().openFd(ASSET)) {
            player.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
        }
    }
}
