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

    private static Bitmap renderStatus(String quote) {
        Bitmap image = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setShader(new LinearGradient(0, 0, WIDTH, HEIGHT,
                new int[]{0xff0b1720, 0xff15312f, 0xff09151e}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, WIDTH, HEIGHT, paint);
        paint.setShader(null);

        // Warm, subtle light behind the quotation; no app branding or numbering.
        paint.setShader(new RadialGradient(WIDTH / 2f, HEIGHT * 0.42f, 730,
                new int[]{0x2639b59e, 0x0e24575b, 0x001a2932},
                new float[]{0f, .53f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, WIDTH, HEIGHT, paint);
        paint.setShader(null);

        paint.setColor(0x2626a28b);
        canvas.drawRoundRect(92, 310, 988, 1540, 64, 64, paint);
        paint.setColor(GOLD);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        canvas.drawRoundRect(92, 310, 988, 1540, 64, 64, paint);
        paint.setStyle(Paint.Style.FILL);

        // Symmetrical ornaments instead of text-heavy headers or watermarks.
        paint.setColor(GOLD);
        canvas.drawCircle(540, 220, 9, paint);
        canvas.drawCircle(518, 220, 3.5f, paint);
        canvas.drawCircle(562, 220, 3.5f, paint);
        paint.setStrokeWidth(2);
        paint.setColor(0x88e4c88e);
        canvas.drawRect(365, 226, 486, 228, paint);
        canvas.drawRect(594, 226, 715, 228, paint);
        canvas.drawRect(365, 1646, 715, 1648, paint);

        drawUrdu(canvas, "نہج البلاغہ", 57, GOLD, 870, 385, 170, false);
        drawUrdu(canvas, "❈", 44, GOLD, 850, 525, 90, false);

        // Dynamically size RTL content to keep long quotations inside the frame.
        TextPaint words = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        words.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        words.setColor(0xfff7f1e5);
        StaticLayout layout = null;
        int maxTextHeight = 760;
        for (int point = 76; point >= 40; point -= 3) {
            words.setTextSize(point);
            layout = makeRtlLayout(quote, words, 770, 17, 1.18f);
            if (layout.getHeight() <= maxTextHeight) break;
        }
        int top = 925 - layout.getHeight() / 2;
        canvas.save();
        canvas.translate((WIDTH - 770) / 2f, top);
        layout.draw(canvas);
        canvas.restore();

        drawUrdu(canvas, "امام علی علیہ السلام", 53, GOLD, 870, 1505, 110, true);
        drawUrdu(canvas, "✦", 36, 0xffddc9a1, 870, 1696, 88, false);
        return image;
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
