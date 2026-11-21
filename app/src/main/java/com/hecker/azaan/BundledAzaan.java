package com.hecker.azaan;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import java.io.IOException;

/** Full-length Arabic azaan recorded by Nizar Al-Qatari, packaged inside the standalone APK. */
final class BundledAzaan {
    static final String ASSET = "azaan/nizar_al_qatari.mp3";
    /*
     * Skip 42.55 seconds of the pinned bundled recording, as requested:
     * 17 seconds more than the previous 25.55-second offset.
     * Do not change the start of user-imported recordings.
     */
    static final long START_AFTER_INTRO_MS = 42_550L;
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
