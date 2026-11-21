package com.hecker.islam;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;

/** A WhatsApp-friendly 9:16 wisdom card, with an in-app preview before sharing. */
final class QuoteStatusShare {
    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1920;
    private static final int GOLD = 0xffe4c88e;
    private static final int STYLE_COUNT = 8;
    private static final int[] STYLE_ORDER = {0, 5, 2, 7, 1, 4, 6, 3};
    private static final String[] STYLE_NAMES = {
        "Emerald Classic", "Midnight Minimal", "Ivory Manuscript",
        "Royal Garnet", "Ocean Split", "Garden Glow",
        "Bronze Heritage", "Moonlit Paper"
    };
    private QuoteStatusShare() { }

    /**
     * Cycle all eight artwork treatments across launches, with a varied initial
     * entry. The preview is freshly drawn each time the user starts sharing.
     */
    private static int nextStyle(Activity activity) {
        SharedPreferences preferences =
                activity.getSharedPreferences("wisdom_status_posters", Activity.MODE_PRIVATE);
        int cursor = preferences.getInt("style_cursor", -1);
        if (cursor < 0) cursor = new java.util.Random().nextInt(STYLE_COUNT);
        int style = STYLE_ORDER[Math.floorMod(cursor, STYLE_COUNT)];
        preferences.edit().putInt("style_cursor", (cursor + 1) % STYLE_COUNT).apply();
        return style;
    }

    static void share(Activity activity, NahjWisdom.Quote quote) {
        if (quote == null || quote.displayText().trim().isEmpty()) return;
        final Bitmap[] bitmap = new Bitmap[1];
        final int[] currentStyle = {nextStyle(activity)};
        try {
            bitmap[0] = renderStatus(posterText(quote.displayText()), currentStyle[0]);
        } catch (Exception ex) {
            android.util.Log.e("WisdomStatus", "Could not draw wisdom image", ex);
            Toast.makeText(activity, "Unable to prepare status preview.", Toast.LENGTH_LONG).show();
            return;
        }

        // Keep the preview inside the app, with actions that never post without confirmation.
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 17), dp(activity, 17), dp(activity, 17), dp(activity, 17));
        GradientDrawable sheet = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{0xff222e35, 0xff111b22});
        sheet.setCornerRadius(dp(activity, 26));
        sheet.setStroke(dp(activity, 1), 0xff526365);
        root.setBackground(sheet);

        TextView heading = new TextView(activity);
        heading.setText("Share as WhatsApp Status");
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(20);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(heading);

        TextView helper = new TextView(activity);
        helper.setText("Preview your quote, then choose My Status in WhatsApp.");
        helper.setTextColor(0xffc0c9ca);
        helper.setTextSize(13);
        helper.setPadding(0, dp(activity, 4), 0, dp(activity, 12));
        root.addView(helper);

        ImageView preview = new ImageView(activity);
        preview.setContentDescription("Urdu wisdom quote status preview");
        preview.setImageBitmap(bitmap[0]);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        GradientDrawable frame = new GradientDrawable();
        frame.setColor(0xff0a1118);
        frame.setCornerRadius(dp(activity, 18));
        preview.setBackground(frame);
        preview.setClipToOutline(true);
        root.addView(preview, new LinearLayout.LayoutParams(-1, dp(activity, 350)));

        LinearLayout designRow = new LinearLayout(activity);
        designRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams designParams = new LinearLayout.LayoutParams(
                -1, dp(activity, 44));
        designParams.topMargin = dp(activity, 9);
        root.addView(designRow, designParams);
        TextView styleName = new TextView(activity);
        styleName.setText(STYLE_NAMES[currentStyle[0]] + "  •  "
                + (currentStyle[0] + 1) + "/" + STYLE_COUNT);
        styleName.setTextColor(0xffc0c9ca);
        styleName.setTextSize(12);
        designRow.addView(styleName, new LinearLayout.LayoutParams(0, -2, 1));
        TextView another = actionButton(activity, "Change design ↻", false);
        another.setTextSize(13);
        designRow.addView(another, new LinearLayout.LayoutParams(dp(activity, 155), -1));
        another.setOnClickListener(v -> {
            try {
                int next = nextStyle(activity);
                Bitmap replacement = renderStatus(posterText(quote.displayText()), next);
                Bitmap previous = bitmap[0];
                bitmap[0] = replacement;
                currentStyle[0] = next;
                preview.setImageBitmap(replacement);
                styleName.setText(STYLE_NAMES[next] + "  •  "
                        + (next + 1) + "/" + STYLE_COUNT);
                if (previous != null && !previous.isRecycled()) previous.recycle();
            } catch (Exception error) {
                android.util.Log.e("WisdomStatus", "Unable to change poster design", error);
                Toast.makeText(activity, "Could not change the design.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionRow = new LinearLayout.LayoutParams(-1, dp(activity, 58));
        actionRow.topMargin = dp(activity, 13);
        root.addView(actions, actionRow);

        TextView other = actionButton(activity, "Share…", false);
        TextView whatsapp = actionButton(activity, "WhatsApp  ↗", true);
        LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, -1, 1);
        first.rightMargin = dp(activity, 9);
        actions.addView(other, first);
        actions.addView(whatsapp, new LinearLayout.LayoutParams(0, -1, 1.5f));

        AlertDialog dialog = new AlertDialog.Builder(activity).setView(root).create();
        dialog.setOnDismissListener(ignored -> {
            preview.setImageDrawable(null);
            if (bitmap[0] != null && !bitmap[0].isRecycled()) bitmap[0].recycle();
        });
        other.setOnClickListener(v -> {
            if (send(activity, bitmap[0], false)) dialog.dismiss();
        });
        whatsapp.setOnClickListener(v -> {
            if (send(activity, bitmap[0], true)) dialog.dismiss();
        });
        dialog.show();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    private static TextView actionButton(Activity activity, String label, boolean featured) {
        TextView button = new TextView(activity);
        button.setText(label);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(featured ? 0xff182522 : Color.WHITE);
        GradientDrawable surface = new GradientDrawable();
        surface.setColor(featured ? GOLD : 0xff36454a);
        surface.setCornerRadius(dp(activity, 17));
        button.setBackground(surface);
        return button;
    }

    private static int dp(Activity activity, int dip) {
        return (int) (activity.getResources().getDisplayMetrics().density * dip + 0.5f);
    }

    /** Keep long classical sayings readable in a WhatsApp-sized image.
     * The complete saying remains available on the Nahj reading page.
     */
    private static String posterText(String source) {
        if (source == null) return "";
        String text = source.trim().replaceAll("\\s+", " ");
        int length = text.codePointCount(0, text.length());
        if (length <= 320) return text;
        int cut = text.offsetByCodePoints(0, 317);
        return text.substring(0, cut).trim() + "…";
    }

    /**
     * Eight distinct 9:16 poster compositions. The exported image always uses
     * the same selected preview bitmap: no branding and no wisdom number.
     * Reading text is kept inside the safe quote rectangle; attribution is
     * drawn below the main panel so it never overlaps the quotation.
     */
    private static Bitmap renderStatus(String quote, int style) {
        if (style < 0 || style >= STYLE_COUNT)
            throw new IllegalArgumentException("Unknown poster design");

        // Background start/middle/end, panel start/end, accent, quotation ink.
        final int[][] palettes = {
            {0xff091e26, 0xff103a37, 0xff091f28, 0xff103a38, 0xff0f4740, 0xffe4c88e, 0xfff6f1e7},
            {0xff0b1524, 0xff1c3040, 0xff080f1c, 0xff1a2938, 0xff10202f, 0xffc7dfda, 0xfff8f5ed},
            {0xfff6eddb, 0xffe8d7ba, 0xfffaf2e7, 0xfffffcf3, 0xffefe5cf, 0xff986b3f, 0xff244035},
            {0xff240f21, 0xff54263c, 0xff170d1a, 0xff432338, 0xff281726, 0xffedc4a0, 0xfffff1e8},
            {0xff071e37, 0xff164969, 0xff071523, 0xff164763, 0xff0a2b43, 0xffa9dfe5, 0xfff7faff},
            {0xff09271f, 0xff235b48, 0xff071c18, 0xff205540, 0xff0d3a2e, 0xffd2eab4, 0xfff6f6e9},
            {0xff2c1b17, 0xff623b2b, 0xff1b1312, 0xff58382e, 0xff301d1b, 0xffffcc8c, 0xfffff5e6},
            {0xfffaf9f1, 0xffe2eae3, 0xfff8f4e9, 0xfffafcf7, 0xffecefe8, 0xff4b7769, 0xff233e34}
        };
        int[] colors = palettes[style];
        final int accent = colors[5], ink = colors[6];
        final boolean light = style == 2 || style == 7;
        int left = 90, top = 320, right = 990, bottom = 1335;
        int titleTop = 400, dividerY = 555, quoteTop = 640;
        int quoteBottom = 1235, quoteWidth = 780, quoteLeft = 150;
        int authorTop = 1455;

        switch (style) {
            case 0:
                top = 310; bottom = 1310; titleTop = 380; dividerY = 547;
                quoteTop = 625; quoteBottom = 1210; authorTop = 1400;
                break;
            case 1: // Airier typography, subtle rules and no solid panel.
                left = 112; top = 385; right = 968; bottom = 1350;
                titleTop = 460; dividerY = 616; quoteTop = 690;
                quoteBottom = 1270; quoteWidth = 820; quoteLeft = 130;
                break;
            case 2: // Cream paper with double-lined border.
                left = 84; top = 325; right = 996; bottom = 1350;
                titleTop = 420; dividerY = 578; quoteTop = 652;
                quoteBottom = 1265; quoteWidth = 790; quoteLeft = 145;
                break;
            case 3: // Tall, gently arched royal frame.
                left = 96; top = 280; right = 984; bottom = 1350;
                titleTop = 395; dividerY = 570; quoteTop = 665;
                quoteBottom = 1230; authorTop = 1450;
                break;
            case 4: // Left accent stripe, right-aligned manuscript quotation.
                left = 85; top = 360; right = 985; bottom = 1380;
                titleTop = 440; dividerY = 605; quoteTop = 690;
                quoteBottom = 1290; quoteWidth = 730; quoteLeft = 190;
                authorTop = 1485;
                break;
            case 5: // Layered emerald-green cards and corner rosettes.
                left = 108; top = 337; right = 971; bottom = 1370;
                titleTop = 430; dividerY = 586; quoteTop = 675;
                quoteBottom = 1275; authorTop = 1480;
                break;
            case 6: // Copper double edge, lower decorative corners.
                left = 83; top = 348; right = 997; bottom = 1350;
                titleTop = 430; dividerY = 580; quoteTop = 660;
                quoteBottom = 1260; authorTop = 1460;
                break;
            case 7: // Light editorial card with minimal framing.
                left = 100; top = 366; right = 980; bottom = 1360;
                titleTop = 445; dividerY = 600; quoteTop = 690;
                quoteBottom = 1260; quoteWidth = 810; quoteLeft = 135;
                break;
            default: break;
        }

        Bitmap image = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(0, 0, WIDTH, HEIGHT,
                new int[]{colors[0], colors[1], colors[2]},
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, WIDTH, HEIGHT, paint);
        paint.setShader(new RadialGradient(
                style == 4 ? 850 : 540, 900, 830,
                new int[]{light ? 0x33ffffff : 0x302ad9ac,
                        light ? 0x10ffffff : 0x0a1ac19b, 0x00000000},
                new float[]{0, .55f, 1}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, WIDTH, HEIGHT, paint);
        paint.setShader(null);

        // Background adornment visibly changes by design.
        paint.setColor(accent);
        paint.setStrokeWidth(2.5f);
        switch (style) {
            case 0:
                canvas.drawLine(350, 218, 490, 218, paint);
                canvas.drawLine(590, 218, 730, 218, paint);
                drawDiamond(canvas, 540, 218, 12, paint);
                break;
            case 1:
                canvas.drawLine(140, 240, 940, 240, paint);
                canvas.drawCircle(540, 240, 7, paint);
                break;
            case 2:
                drawRosette(canvas, 540, 210, accent, 1.3f);
                drawDiamond(canvas, 160, 210, 10, paint);
                drawDiamond(canvas, 920, 210, 10, paint);
                break;
            case 3:
                drawRosette(canvas, 540, 190, accent, 1.5f);
                canvas.drawLine(345, 220, 480, 220, paint);
                canvas.drawLine(600, 220, 735, 220, paint);
                break;
            case 4:
                canvas.drawLine(155, 220, 925, 220, paint);
                drawDiamond(canvas, 155, 220, 12, paint);
                break;
            case 5:
                drawRosette(canvas, 265, 219, accent, .8f);
                drawRosette(canvas, 815, 219, accent, .8f);
                canvas.drawLine(335, 219, 745, 219, paint);
                break;
            case 6:
                drawDiamond(canvas, 540, 215, 18, paint);
                canvas.drawLine(290, 215, 495, 215, paint);
                canvas.drawLine(585, 215, 790, 215, paint);
                break;
            case 7:
                canvas.drawLine(120, 210, 960, 210, paint);
                drawRosette(canvas, 540, 210, accent, .85f);
                break;
            default: break;
        }

        // Framing and positioning are intentionally different across styles.
        if (style == 5) {
            paint.setColor(light ? 0x33887766 : 0x4431a887);
            canvas.drawRoundRect(left - 24, top + 28, right + 24, bottom + 28,
                    58, 58, paint);
        }
        if (style == 1) {
            paint.setColor(0x2819a89c);
            canvas.drawRoundRect(left, top, right, bottom, 20, 20, paint);
            paint.setColor(accent);
            paint.setStrokeWidth(3f);
            canvas.drawLine(left + 45, top, right - 45, top, paint);
            canvas.drawLine(left + 45, bottom, right - 45, bottom, paint);
        } else {
            paint.setShader(new LinearGradient(left, top, right, bottom,
                    new int[]{colors[3], colors[4]}, null, Shader.TileMode.CLAMP));
            float radius = style == 3 ? 175f : (style == 7 ? 26f : 60f);
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint);
            paint.setShader(null);
            paint.setColor(accent);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(style == 2 || style == 6 ? 3.9f : 2.5f);
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint);
            if (style == 2 || style == 6) {
                canvas.drawRoundRect(left + 17, top + 17, right - 17, bottom - 17,
                        Math.max(16, radius - 10), Math.max(16, radius - 10), paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }
        if (style == 4) {
            paint.setColor(accent);
            canvas.drawRoundRect(left + 29, top + 75, left + 37,
                    bottom - 75, 6, 6, paint);
        } else if (style == 5 || style == 3) {
            drawRosette(canvas, left + 82, top + 100, accent, .56f);
            drawRosette(canvas, right - 82, top + 100, accent, .56f);
        } else if (style == 7) {
            paint.setColor(accent);
            canvas.drawRect(left + 35, top + 35, left + 170, top + 42, paint);
            canvas.drawRect(right - 170, bottom - 42, right - 35, bottom - 35, paint);
        }

        drawUrdu(canvas, "نہج البلاغہ", style == 1 ? 56 : 61,
                accent, 790, titleTop, 100, false);
        paint.setColor(accent);
        paint.setStrokeWidth(2.4f);
        canvas.drawLine(style == 4 ? 425 : 345, dividerY,
                style == 4 ? 492 : 477, dividerY, paint);
        canvas.drawLine(603, dividerY, style == 4 ? 690 : 735, dividerY, paint);
        drawRosette(canvas, 540, dividerY, accent, style == 7 ? .6f : .9f);

        // Strict safe area for quotations of varying lengths and font sizes.
        TextPaint words = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        words.setTypeface(Typeface.create("serif",
                style == 3 || style == 6 ? Typeface.BOLD : Typeface.NORMAL));
        words.setColor(ink);
        StaticLayout layout = null;
        final int safeHeight = quoteBottom - quoteTop;
        for (int fontSize = style == 4 ? 76 : 84; fontSize >= 24; fontSize -= 2) {
            words.setTextSize(fontSize);
            layout = StaticLayout.Builder.obtain(quote, 0, quote.length(),
                    words, quoteWidth)
                    .setAlignment(style == 4
                            ? Layout.Alignment.ALIGN_NORMAL : Layout.Alignment.ALIGN_CENTER)
                    .setTextDirection(TextDirectionHeuristics.RTL)
                    .setIncludePad(false)
                    .setLineSpacing(15, 1.16f)
                    .build();
            if (layout.getHeight() <= safeHeight) break;
        }
        if (layout != null) {
            int safeLeft = style == 4 ? 180 : 125;
            int safeRight = style == 4 ? 940 : 955;
            float y = quoteTop + Math.max(0, safeHeight - layout.getHeight()) / 2f;
            canvas.save();
            canvas.clipRect(safeLeft, quoteTop, safeRight, quoteBottom);
            canvas.translate(quoteLeft, y);
            layout.draw(canvas);
            canvas.restore();
        }

        // The attribution is never painted inside the quote card.
        drawUrdu(canvas, "امام علی علیہ السلام", style == 7 ? 52 : 55,
                accent, 890, authorTop, 100, true);
        int footerY = 1640;
        paint.setColor(accent);
        paint.setStrokeWidth(2.2f);
        if (style == 4) {
            canvas.drawLine(330, footerY, 750, footerY, paint);
            drawDiamond(canvas, 540, footerY, 11, paint);
        } else if (style == 1 || style == 7) {
            canvas.drawLine(420, footerY, 660, footerY, paint);
        } else {
            canvas.drawLine(325, footerY, 482, footerY, paint);
            canvas.drawLine(598, footerY, 755, footerY, paint);
            drawRosette(canvas, 540, footerY, accent, .74f);
        }
        return image;
    }

    private static void drawDiamond(Canvas canvas, float cx, float cy,
                                    float radius, Paint paint) {
        android.graphics.Path diamond = new android.graphics.Path();
        diamond.moveTo(cx, cy - radius);
        diamond.lineTo(cx + radius * .72f, cy);
        diamond.lineTo(cx, cy + radius);
        diamond.lineTo(cx - radius * .72f, cy);
        diamond.close();
        canvas.drawPath(diamond, paint);
    }

    private static void drawRosette(Canvas canvas, float cx, float cy,
                                    int color, float scale) {
        Paint petal = new Paint(Paint.ANTI_ALIAS_FLAG);
        petal.setColor(color);
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);
        for (int i = 0; i < 8; i++) {
            canvas.save();
            canvas.rotate(i * 45f);
            canvas.drawOval(-4f, -29f, 4f, -10f, petal);
            canvas.restore();
        }
        canvas.drawCircle(0, 0, 6, petal);
        canvas.restore();
    }

    private static StaticLayout makeRtlLayout(String value, TextPaint paint,
                                              int width, int spacing, float multiplier) {
        return StaticLayout.Builder.obtain(value, 0, value.length(), paint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setIncludePad(false)
                .setLineSpacing(spacing, multiplier)
                .build();
    }

    private static void drawUrdu(Canvas canvas, String value, int size, int color,
                                 int width, int top, int maxHeight, boolean bold) {
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.create("serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        paint.setColor(color);
        paint.setTextSize(size);
        StaticLayout layout = makeRtlLayout(value, paint, width, 0, 1f);
        canvas.save();
        canvas.translate((WIDTH - width) / 2f, top + (maxHeight - layout.getHeight()) / 2f);
        layout.draw(canvas);
        canvas.restore();
    }

    private static boolean send(Activity activity, Bitmap image, boolean preferWhatsApp) {
        try {
            File folder = new File(activity.getCacheDir(), "status_share");
            if (!folder.isDirectory() && !folder.mkdirs())
                throw new java.io.IOException("Cannot create sharing directory");
            File file = new File(folder, "nahj_quote_status.jpg");
            try (FileOutputStream stream = new FileOutputStream(file)) {
                if (!image.compress(Bitmap.CompressFormat.JPEG, 94, stream))
                    throw new java.io.IOException("Failed to encode image");
            }
            Uri uri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".statusfiles", file);
            Intent outgoing = new Intent(Intent.ACTION_SEND);
            outgoing.setType("image/jpeg");
            outgoing.putExtra(Intent.EXTRA_STREAM, uri);
            outgoing.setClipData(ClipData.newRawUri("Nahj al-Balagha quote image", uri));
            outgoing.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (preferWhatsApp) {
                Intent direct = new Intent(outgoing);
                direct.setPackage("com.whatsapp");
                if (direct.resolveActivity(activity.getPackageManager()) != null) {
                    activity.startActivity(direct);
                    return true;
                }
            }
            activity.startActivity(Intent.createChooser(outgoing, "Share quote image"));
            return true;
        } catch (Exception ex) {
            android.util.Log.e("WisdomStatus", "Share intent failed", ex);
            Toast.makeText(activity, "Could not share the quote image.", Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
