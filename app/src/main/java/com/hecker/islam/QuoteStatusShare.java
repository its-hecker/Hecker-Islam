package com.hecker.islam;

import android.app.Activity;
import android.app.AlertDialog;
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
    private QuoteStatusShare() { }

    static void share(Activity activity, NahjWisdom.Quote quote) {
        if (quote == null || quote.urdu == null || quote.urdu.trim().isEmpty()) return;
        final Bitmap bitmap;
        try {
            bitmap = renderStatus(quote.urdu);
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
        preview.setImageBitmap(bitmap);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        GradientDrawable frame = new GradientDrawable();
        frame.setColor(0xff0a1118);
        frame.setCornerRadius(dp(activity, 18));
        preview.setBackground(frame);
        preview.setClipToOutline(true);
        root.addView(preview, new LinearLayout.LayoutParams(-1, dp(activity, 380)));

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
            if (!bitmap.isRecycled()) bitmap.recycle();
        });
        other.setOnClickListener(v -> {
            if (send(activity, bitmap, false)) dialog.dismiss();
        });
        whatsapp.setOnClickListener(v -> {
            if (send(activity, bitmap, true)) dialog.dismiss();
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

    /**
     * The on-device renderer matches the approved 9:16 preview:
     * title and quotation inside the frame, with the Imam Ali (A.S.)
     * attribution entirely BELOW it. Neither app branding nor wisdom
     * numbering is drawn into the export.
     */
    private static Bitmap renderStatus(String quote) {
        final int panelLeft = 90, panelTop = 310, panelRight = 990, panelBottom = 1310;
        final int quoteWidth = 780, quoteTop = 625, quoteBottom = 1210;
        final int attributionTop = 1400;
        final int ivory = 0xfff6f1e7;
        Bitmap image = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Deep teal, understated emerald glow, and a gentle vignette.
        paint.setShader(new LinearGradient(0, 0, WIDTH, HEIGHT,
                new int[]{0xff091e26, 0xff103a37, 0xff091f28},
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, WIDTH, HEIGHT, paint);
        paint.setShader(new RadialGradient(WIDTH / 2f, 830, 780,
                new int[]{0x4026b59d, 0x16208579, 0x0014262b},
                new float[]{0f, .56f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, WIDTH, HEIGHT, paint);
        paint.setShader(null);

        // Symmetrical top motif, kept OUTSIDE the bordered quote.
        paint.setColor(0xbbe4c88e);
        paint.setStrokeWidth(2.3f);
        canvas.drawLine(350, 218, 490, 218, paint);
        canvas.drawLine(590, 218, 730, 218, paint);
        paint.setColor(GOLD);
        canvas.drawCircle(517, 218, 4.5f, paint);
        canvas.drawCircle(563, 218, 4.5f, paint);
        drawDiamond(canvas, 540, 218, 12f, paint);

        // Framed emerald panel: inset and softly illuminated.
        paint.setShader(new LinearGradient(panelLeft, panelTop, panelRight, panelBottom,
                new int[]{0xff103a38, 0xff0f4740, 0xff103a38},
                null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(panelLeft, panelTop, panelRight, panelBottom,
                62, 62, paint);
        paint.setShader(null);
        paint.setColor(0xffe6cc92);
        paint.setStrokeWidth(2.7f);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(panelLeft, panelTop, panelRight, panelBottom,
                62, 62, paint);
        paint.setStyle(Paint.Style.FILL);

        // Title and rosette have their own fixed zone above the quote.
        drawUrdu(canvas, "نہج البلاغہ", 61, GOLD, 820, 380, 105, false);
        paint.setColor(0x99e4c88e);
        paint.setStrokeWidth(2.4f);
        canvas.drawLine(365, 547, 484, 547, paint);
        canvas.drawLine(596, 547, 715, 547, paint);
        drawRosette(canvas, 540, 547, GOLD, 1.0f);

        // Fit the full Urdu quote, always centered inside a safe rectangle.
        // The attribution is not part of this layout and cannot cross its border.
        TextPaint words = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        words.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        words.setColor(ivory);
        StaticLayout layout = null;
        final int allowedHeight = quoteBottom - quoteTop;
        for (int size = 82; size >= 24; size -= 2) {
            words.setTextSize(size);
            layout = makeRtlLayout(quote, words, quoteWidth, 15, 1.16f);
            if (layout.getHeight() <= allowedHeight) break;
        }
        if (layout != null) {
            float y = quoteTop + (allowedHeight - layout.getHeight()) / 2f;
            // Clip the quotation to its own area as a final layout guard.
            canvas.save();
            canvas.clipRect(130, quoteTop, 950, quoteBottom);
            canvas.translate((WIDTH - quoteWidth) / 2f, y);
            layout.draw(canvas);
            canvas.restore();
        }

        // Crucial layout fix: the author sits well BELOW panelBottom=1310,
        // not at its lower border. Keep plenty of vertical breathing room.
        drawUrdu(canvas, "امام علی علیہ السلام", 55, GOLD, 900,
                attributionTop, 95, true);
        paint.setColor(0x88e4c88e);
        paint.setStrokeWidth(2.2f);
        canvas.drawLine(325, 1555, 490, 1555, paint);
        canvas.drawLine(590, 1555, 755, 1555, paint);
        drawRosette(canvas, 540, 1555, GOLD, .74f);
        paint.setColor(0x77e4c88e);
        drawDiamond(canvas, 540, 1650, 9f, paint);
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
