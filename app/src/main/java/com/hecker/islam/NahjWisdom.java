package com.hecker.islam;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import org.json.JSONArray;
import org.json.JSONObject;

/** Complete public-domain classical Arabic sayings with original Urdu paraphrases.
 * Numbering follows the 480-saying Nahj al-Balagha edition at Al-Islam.org.
 */
public final class NahjWisdom {
    private NahjWisdom() { }
    public static final String COLLECTION = "https://al-islam.org/nahjul-balagha-part-2-letters-and-sayings";
    public static final String URDU_BOOK = "https://nehjulbalagah.com/category/hikmat/";
    public static final class Quote {
        public final int number;
        public final String arabic, urdu;
        Quote(int number, String arabic, String urdu) {
            this.number = number; this.arabic = arabic; this.urdu = urdu;
        }
        public boolean hasUrdu() { return urdu != null && !urdu.trim().isEmpty(); }
        public String displayText() { return hasUrdu() ? urdu : arabic; }
    }
    // Fallback text is available even in a source checkout without generated assets.
    private static final Quote[] FALLBACK = {
        new Quote(4, "الصَّبْرُ شَجَاعَةٌ وَ الزُّهْدُ ثَرْوَةٌ وَ الْوَرَعُ جُنَّةٌ",
            "صبر بہادری ہے، دنیا سے بے رغبتی ایک دولت ہے اور پرہیزگاری گناہ سے حفاظت کرتی ہے۔"),
        new Quote(5, "الْعِلْمُ وِرَاثَةٌ كَرِيمَةٌ وَ الْآدَابُ حُلَلٌ مُجَدَّدَةٌ وَ الْفِكْرُ مِرْآةٌ صَافِيَةٌ",
            "علم گراں قدر میراث ہے، اچھے آداب انسان کا زیور ہیں اور غور و فکر صاف آئینے کی مانند ہے۔"),
        new Quote(10, "خَالِطُوا النَّاسَ مُخَالَطَةً إِنْ مِتُّمْ مَعَهَا بَكَوْا عَلَيْكُمْ وَ إِنْ عِشْتُمْ حَنُّوا إِلَيْكُمْ",
            "لوگوں سے ایسا حسنِ سلوک رکھو کہ تم نہ رہو تو وہ تمہیں یاد کریں اور تم موجود ہو تو تمہاری رفاقت چاہیں۔"),
        new Quote(11, "إِذَا قَدَرْتَ عَلَى عَدُوِّكَ فَاجْعَلِ الْعَفْوَ عَنْهُ شُكْراً لِلْقُدْرَةِ عَلَيْهِ",
            "جب کسی مخالف پر قدرت حاصل ہو تو معاف کر دینا اس قدرت کا شکر بناؤ۔"),
        new Quote(13, "إِذَا وَصَلَتْ إِلَيْكُمْ أَطْرَافُ النِّعَمِ فَلَا تُنَفِّرُوا أَقْصَاهَا بِقِلَّةِ الشُّكْرِ",
            "نعمتوں کی ابتدا ملے تو ناشکری سے باقی نعمتوں کو خود سے دور نہ کرو۔"),
        new Quote(21, "الْفُرْصَةُ تَمُرُّ مَرَّ السَّحَابِ، فَانْتَهِزُوا فُرَصَ الْخَيْرِ",
            "موقع بادل کی طرح گزرتا ہے، اس لیے نیکی کے مواقع سے فائدہ اٹھاؤ۔"),
        new Quote(23, "مَنْ أَبْطَأَ بِهِ عَمَلُهُ لَمْ يُسْرِعْ بِهِ حَسَبُهُ",
            "جس کے اعمال اسے پیچھے رکھیں، اس کا نسب اسے آگے نہیں بڑھا سکتا۔"),
        new Quote(24, "مِنْ كَفَّارَاتِ الذُّنُوبِ الْعِظَامِ إِغَاثَةُ الْمَلْهُوفِ وَالتَّنْفِيسُ عَنِ الْمَكْرُوبِ",
            "مصیبت زدہ کی مدد اور پریشان حال کو راحت پہنچانا بڑے گناہوں کے کفارے کے اسباب میں ہے۔"),
        new Quote(54, "لاَ غِنَى كَالْعَقْلِ، وَلاَ فَقْرَ كَالْجَهْلِ، وَلاَ ظَهِيرَ كَالْمُشَاوَرَةِ",
            "عقل جیسی دولت نہیں، جہالت جیسی محتاجی نہیں، اور مشورے جیسی مدد نہیں۔"),
        new Quote(55, "الصَّبْرُ صَبْرَانِ: صَبْرٌ عَلَى مَا تَكْرَهُ، وَصَبْرٌ عَمَّا تُحِبُّ",
            "صبر کی دو صورتیں ہیں: ناپسندیدہ مشکل کو برداشت کرنا، اور پسندیدہ مگر نامناسب چیز سے رکنا۔"),
        new Quote(57, "الْقَنَاعَةُ مَالٌ لاَ يَنْفَدُ",
            "قناعت ایسی دولت ہے جو ختم نہیں ہوتی۔"),
        new Quote(67, "لاَ تَسْتَحِ مِنْ إِعْطَاءِ الْقَلِيلِ، فَإِنَّ الْحِرْمَانَ أَقَلُّ مِنْهُ",
            "تھوڑی چیز دینے سے شرم نہ کرو؛ کچھ بھی نہ دینا اس سے بھی کم ہے۔"),
        new Quote(338, "الْعِلْمُ عِلْمَانِ: مَطْبُوعٌ وَمَسْمُوعٌ وَلاَ يَنْفَعُ الْمَسْمُوعُ إِذَا لَمْ يَكُنِ الْمَطْبُوعُ",
            "علم ایک وہ ہے جو سمجھ میں اترے اور دوسرا محض سنا ہوا؛ سمجھ کے بغیر سنا ہوا علم فائدہ نہیں دیتا۔")
    };
    public static Quote[] QUOTES = FALLBACK;
    private static volatile boolean loaded = false;

    /**
     * Load all 480 original Arabic sayings from the app's verified bundled file.
     * The 57 originally written Urdu summaries accompany their matching Arabic
     * passages, while other readings display original Arabic and link to the
     * online Urdu collection instead of inventing attributed translations.
     */
    public static synchronized void initialize(Context context) {
        if (loaded) return;
        try (BufferedReader stream = new BufferedReader(new InputStreamReader(
                context.getAssets().open("nahj/sayings_480.json"),
                StandardCharsets.UTF_8))) {
            StringBuilder data = new StringBuilder(190_000);
            char[] buffer = new char[8192];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                data.append(buffer, 0, read);
            }
            JSONArray records = new JSONObject(data.toString()).getJSONArray("sayings");
            if (records.length() != 480) throw new IllegalStateException(
                    "Expected all 480 Nahj sayings, found " + records.length());
            Quote[] complete = new Quote[480];
            for (int i = 0; i < complete.length; i++) {
                JSONObject entry = records.getJSONObject(i);
                int number = entry.getInt("number");
                String arabic = entry.getString("arabic").trim();
                if (number != i + 1 || arabic.length() < 12) {
                    throw new IllegalStateException("Invalid wisdom " + (i + 1));
                }
                complete[i] = new Quote(number, arabic,
                        entry.optString("urdu", "").trim());
            }
            QUOTES = complete;
            loaded = true;
        } catch (Exception problem) {
            Log.e("NahjWisdom", "Complete offline sayings could not be loaded", problem);
            // Do not suppress the error by reporting the fallback as full coverage.
            QUOTES = FALLBACK;
        }
    }

    public static int randomOtherThan(int prior) {
        int index = ThreadLocalRandom.current().nextInt(QUOTES.length);
        if (QUOTES.length > 1 && index == prior) index = (index + 1) % QUOTES.length;
        return index;
    }
    public static Quote find(int number) {
        for (Quote quote : QUOTES) if (quote.number == number) return quote;
        return null;
    }
}
