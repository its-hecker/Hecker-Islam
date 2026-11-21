package com.hecker.islam;

import android.content.Context;
import org.brotli.dec.BrotliInputStream;
import org.json.JSONArray;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * All reference data is compiled into the standalone APK's assets/reference/.
 * No import, account, download, file picker or network connection is needed.
 */
public final class ReferenceLibrary {
    private static final long MAX_JSON_BYTES = 8L * 1024 * 1024;
    private ReferenceLibrary() {}
    public static boolean ready(Context context) {
        return available(context, "quran/quran_uthmani.json")
            && available(context, "quran/chapters.json")
            && available(context, "azkar/items.json")
            && available(context, "hadith/hadith.ndjson.br")
            && available(context, "location/city_index.json");
    }
    private static boolean valid(String relative) {
        if (relative == null || relative.contains("..") || relative.contains("\\")
            || relative.startsWith("/") || relative.indexOf(':') >= 0) return false;
        return relative.startsWith("quran/") || relative.startsWith("hadith/")
            || relative.startsWith("azkar/") || relative.startsWith("location/")
            || relative.startsWith("adhan/") || relative.startsWith("adhan_background/");
    }
    public static boolean available(Context c, String relative) {
        if (!valid(relative)) return false;
        try (InputStream s = c.getAssets().open("reference/" + relative)) { return s.read() >= 0; }
        catch (IOException e) { return false; }
    }
    public static InputStream open(Context c, String relative) throws IOException {
        if (!valid(relative)) throw new IOException("Invalid catalog path");
        return c.getAssets().open("reference/" + relative);
    }
    public static InputStream hadithStream(Context c) throws IOException {
        return new BrotliInputStream(open(c, "hadith/hadith.ndjson.br"));
    }
    public static JSONArray array(Context c, String relative) throws Exception {
        try (InputStream in = open(c, relative); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[16384]; int n;
            while ((n = in.read(buf)) != -1) {
                if ((long) out.size() + n > MAX_JSON_BYTES) throw new IOException("Catalog exceeds limit");
                out.write(buf, 0, n);
            }
            return new JSONArray(out.toString(StandardCharsets.UTF_8.name()));
        }
    }
}
