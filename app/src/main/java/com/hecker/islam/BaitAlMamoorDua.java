package com.hecker.islam;

/**
 * Arabic prayer and accompanying Urdu meanings transcribed, in printed order,
 * from the two pages supplied by the reader. Each array entry is one printed
 * Arabic line and its immediately following Urdu line. Page two starts at 7.
 *
 * This is the supplied two-page text, not an independently authenticated
 * edition or a claim that every traditional variant has the same length.
 */
final class BaitAlMamoorDua {
    private BaitAlMamoorDua() { }

    static final int SECOND_PAGE_START = 7;

    static final String[] ARABIC = {
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        "يَا مَنْ أَظْهَرَ الْجَمِيلَ وَسَتَرَ الْقَبِيحَ يَا",
        "مَنْ لَمْ يُؤَاخِذْ بِالْجَرِيرَةِ وَلَمْ يَهْتِكِ",
        "السِّتْرَ يَا عَظِيمَ الْعَفْوِ يَا حَسَنَ التَّجَاوُزِ",
        "يَا وَاسِعَ الْمَغْفِرَةِ يَا بَاسِطَ الْيَدَيْنِ",
        "يَا ذَا الرَّحْمَةِ يَا صَاحِبَ كُلِّ نَجْوَى يَا مُنْتَهَى",
        "كُلِّ شَكْوَى يَا كَرِيمَ الصَّفْحِ يَا عَظِيمَ الْمَنِّ",
        "يَا مُبْتَدِئًا بِالنِّعَمِ قَبْلَ اسْتِحْقَاقِهَا يَا",
        "رَبَّنَا يَا سَيِّدَنَا يَا مَوْلَانَا يَا غَايَةَ رَغْبَتِنَا",
        "أَسْأَلُكَ يَا اللَّهُ يَا اللَّهُ يَا اللَّهُ أَنْ لَا تَشَوَّهَ",
        "خَلْقِي بِالنَّارِ بِرَحْمَتِكَ يَا أَرْحَمَ الرَّاحِمِينَ"
    };

    static final String[] URDU = {
        "شروع کرتا ہوں اللہ کے نام سے جو بڑا مہربان نہایت رحم والا ہے۔",
        "اے وہ جس نے اچھائی کو نمایاں کیا اور برائی کو چھپایا، اے",
        "گناہوں کا بدلہ نہ لینے والے، اے پردہ دری نہ کرنے والے،",
        "اے سب سے بڑے معافی دینے والے، اے حسن و خوبی سے درگزر کرنے والے،",
        "اے بڑی بخشش والے، اے دستِ کرم بڑھانے والے،",
        "اے ہر بھید کے جاننے والے، اے ہر شکایت کی آماجگاہ،",
        "اے درگزر کرنے والے، اے سب سے بڑے احسان کرنے والے،",
        "اے استحقاق سے قبل نعمتوں سے نوازنے والے، اے",
        "ہمارے پالنے والے، اے ہمارے سردار اور ہمارے آقا، اے ہماری منتہائے آرزو!",
        "میں تجھ ہی سے سوال کرتا ہوں۔ اے اللہ، اے اللہ، اے اللہ! کہ تو ہمارے حلیے",
        "کو آگ سے نہ خراب کر، تیری رحمتوں کا واسطہ، اے سب سے بڑے رحم کرنے والے۔"
    };

    static String arabic() {
        return String.join("\n", ARABIC);
    }

    static String urdu() {
        return String.join("\n", URDU);
    }
}
