package com.hecker.islam;

import android.content.Context;
import android.net.Uri;
import org.brotli.dec.BrotliInputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Installs only the content catalogs explicitly authorized for Hecker Islam.
 * Accepts the original reference APK or the exported reference-content ZIP.
 * No executable code, native libraries, arbitrary paths or app permissions are imported.
 */
public final class ReferenceLibrary {
    private static final String DIR = "islam_content";
    private static final long MAX_ENTRY = 8L * 1024 * 1024;
    private static final long MAX_TOTAL = 26L * 1024 * 1024;
    private ReferenceLibrary() {}

    public static boolean ready(Context c) {
        return available(c, "quran/quran_uthmani.json")
            && available(c, "quran/chapters.json")
            && available(c, "azkar/items.json")
            && available(c, "hadith/hadith.ndjson.br");
    }
    public static boolean available(Context c, String relative) {
        if (!valid(relative)) return false;
        if (new File(new File(c.getFilesDir(), DIR), relative).isFile()) return true;
        try (InputStream in = c.getAssets().open("reference/" + relative)) { return in.read() != -1; }
        catch (IOException ex) { return false; }
    }
    public static InputStream open(Context c, String relative) throws IOException {
        if (!valid(relative)) throw new IOException("Invalid content path");
        File f = new File(new File(c.getFilesDir(), DIR), relative);
        if (f.isFile()) return new FileInputStream(f);
        return c.getAssets().open("reference/" + relative);
    }
    public static InputStream hadithStream(Context c) throws IOException {
        return new BrotliInputStream(open(c, "hadith/hadith.ndjson.br"));
    }
    public static JSONArray array(Context c, String relative) throws Exception {
        try (InputStream stream = open(c, relative); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[16384]; int n;
            while ((n = stream.read(buf)) != -1) {
                if (out.size() + n > MAX_ENTRY) throw new IOException("Catalog too large");
                out.write(buf, 0, n);
            }
            return new JSONArray(out.toString(StandardCharsets.UTF_8.name()));
        }
    }
    public static String importArchive(Context c, Uri uri) throws Exception {
        File base = new File(c.getFilesDir(), DIR);
        File temp = new File(c.getFilesDir(), DIR + "_staging");
        erase(temp);
        if (!temp.mkdirs()) throw new IOException("Cannot prepare the local content directory");
        int imported = 0;
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(c.getContentResolver().openInputStream(uri))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith("assets/")) name = name.substring(7);
                if (!valid(name) || entry.isDirectory()) { zip.closeEntry(); continue; }
                File dest = new File(temp, name);
                File parent = dest.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Could not create data directory");
                long len = 0;
                try (OutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[16384]; int n;
                    while ((n = zip.read(buf)) != -1) {
                        len += n; total += n;
                        if (len > MAX_ENTRY || total > MAX_TOTAL) throw new IOException("Data pack exceeds size limit");
                        out.write(buf, 0, n);
                    }
                }
                imported++;
                zip.closeEntry();
            }
        } catch (Exception e) {
            erase(temp); throw e;
        }
        File verses = new File(temp, "quran/quran_uthmani.json");
        File chapters = new File(temp, "quran/chapters.json");
        File azkar = new File(temp, "azkar/items.json");
        File hadith = new File(temp, "hadith/hadith.ndjson.br");
        if (!verses.isFile() || !chapters.isFile() || !azkar.isFile() || !hadith.isFile()) {
            erase(temp); throw new IOException("The archive is missing required Qur'an, Azkar or Hadith catalogs");
        }
        File old = new File(c.getFilesDir(), DIR + "_previous");
        erase(old);
        if (base.exists() && !base.renameTo(old)) {
            erase(temp); throw new IOException("Could not replace previous content");
        }
        if (!temp.renameTo(base)) {
            old.renameTo(base); throw new IOException("Could not activate the data pack");
        }
        erase(old);
        return String.format(Locale.ENGLISH, "Imported %d offline catalogs and location files", imported);
    }
    private static boolean valid(String value) {
        if (value == null || value.contains("..") || value.contains("\\")
                || value.startsWith("/") || value.indexOf(':') >= 0) return false;
        return value.startsWith("quran/") || value.startsWith("azkar/")
                || value.startsWith("hadith/") || value.startsWith("location/")
                || value.startsWith("adhan/") || value.startsWith("adhan_background/");
    }
    private static void erase(File f) {
        if (!f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File child : children) erase(child);
        }
        f.delete();
    }
}
