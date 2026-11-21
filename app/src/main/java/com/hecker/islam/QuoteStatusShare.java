package com.hecker.islam;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;

/** Generates a temporary vertical story card that can be posted to WhatsApp My Status. */
final class QuoteStatusShare {
    private QuoteStatusShare() { }

    static void share(Activity activity, NahjWisdom.Quote quote) {
        try {
            final int width = 1080, height = 1920;
            final int gold = Color.rgb(222, 184, 105);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new LinearGradient(0, 0, width, height,
                    new int[]{0xff111620, 0xff192333, 0xff0c1119}, null, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, width, height, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            paint.setColor(gold);
            canvas.drawRoundRect(65, 100, width - 65, height - 100, 44, 44, paint);
            paint.setStyle(Paint.Style.FILL);

            TextPaint heading = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            heading.setColor(gold);
            heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            heading.setTextSize(46f);
            heading.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("HECKER ISLAM", width / 2f, 305, heading);
            heading.setColor(Color.rgb(205, 209, 221));
            heading.setTextSize(38f);
            canvas.drawText("NAHJ AL-BALAGHA", width / 2f, 370, heading);

            TextPaint urdu = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            urdu.setColor(Color.WHITE);
            urdu.setTextSize(64f);
            urdu.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            int maxWidth = width - 230;
            StaticLayout layout = null;
            for (int point = 64; point >= 44; point -= 4) {
                urdu.setTextSize(point);
                layout = StaticLayout.Builder.obtain(quote.urdu, 0, quote.urdu.length(), urdu, maxWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setTextDirection(TextDirectionHeuristics.RTL)
                        .setIncludePad(false)
                        .setLineSpacing(24f, 1.18f)
                        .build();
                if (layout.getHeight() <= 900) break;
            }
            int top = (height - layout.getHeight()) / 2;
            canvas.save();
            canvas.translate((width - maxWidth) / 2f, top);
            layout.draw(canvas);
            canvas.restore();

            heading.setColor(gold);
            heading.setTextSize(38f);
            canvas.drawText("Imam Ali (A.S.)  •  Wisdom " + quote.number,
                    width / 2f, height - 315, heading);
            heading.setTextSize(28f);
            heading.setColor(0xffc6c9d1);
            canvas.drawText("Urdu meaning  •  Hecker Islam", width / 2f, height - 255, heading);

            File folder = new File(activity.getCacheDir(), "status_share");
            if (!folder.exists() && !folder.mkdirs()) throw new java.io.IOException("Cannot create share cache");
            File file = new File(folder, "nahj_status.jpg");
            try (FileOutputStream out = new FileOutputStream(file)) {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 93, out))
                    throw new java.io.IOException("Could not encode status card");
            } finally {
                bitmap.recycle();
            }
            Uri uri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".statusfiles", file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("image/jpeg");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.setClipData(ClipData.newRawUri("Nahj al-Balagha status card", uri));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                Intent whatsapp = new Intent(send);
                whatsapp.setPackage("com.whatsapp");
                activity.startActivity(whatsapp);
            } catch (android.content.ActivityNotFoundException missing) {
                activity.startActivity(Intent.createChooser(send,
                        "Share quote image • choose WhatsApp My Status"));
            }
        } catch (Exception ex) {
            android.util.Log.e("HeckerIslamShare", "Could not share wisdom status", ex);
            Toast.makeText(activity, "Could not prepare the quote image for sharing.",
                    Toast.LENGTH_LONG).show();
        }
    }
}
