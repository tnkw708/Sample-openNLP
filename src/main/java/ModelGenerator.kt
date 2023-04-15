import com.atilika.kuromoji.TokenizerBase
import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer
import com.kennycason.kumo.CollisionMode
import com.kennycason.kumo.WordCloud
import com.kennycason.kumo.WordFrequency
import com.kennycason.kumo.bg.CircleBackground
import com.kennycason.kumo.bg.RectangleBackground
import com.kennycason.kumo.font.scale.LinearFontScalar
import com.kennycason.kumo.font.scale.SqrtFontScalar
import com.kennycason.kumo.nlp.FrequencyAnalyzer
import com.kennycason.kumo.palette.ColorPalette
import com.kennycason.kumo.palette.LinearGradientColorPalette
import java.awt.Color
import java.awt.Dimension
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import kotlin.system.exitProcess


object ModelGenerator {
    private val TOKENIZER: Tokenizer = Tokenizer.Builder().mode(TokenizerBase.Mode.NORMAL).build()
    private val INPUT_FILE1 by lazy {ClassLoader.getSystemResourceAsStream("dendenmushinokanashimi.txt")}
    private val INPUT_FILE2 by lazy {ClassLoader.getSystemResourceAsStream("gongitsune.txt")}
    private val INPUT_FILE3 by lazy {ClassLoader.getSystemResourceAsStream("ojisannolamp.txt")}
    private val INPUT_FILE4 by lazy {ClassLoader.getSystemResourceAsStream("tebukurookaini.txt")}
    private val INPUT_FILE5 by lazy {ClassLoader.getSystemResourceAsStream("ushiotsunaidatsubaki.txt")}
    private const val OUTPUT_FILE_PATH = "output/word_separated_all_novel.txt"


    @JvmStatic
    fun main(args: Array<String>) {
        val novel1: List<String> = try {
            InputStreamReader(INPUT_FILE1).use { reader -> reader.readLines() }
        } catch (e: IOException) {
            e.printStackTrace()
            exitProcess(1)
        }
        val novel2: List<String> = try {
            InputStreamReader(INPUT_FILE2).use { reader -> reader.readLines() }
        } catch (e: IOException) {
            e.printStackTrace()
            exitProcess(1)
        }
        val novel3: List<String> = try {
            InputStreamReader(INPUT_FILE3).use { reader -> reader.readLines() }
        } catch (e: IOException) {
            e.printStackTrace()
            exitProcess(1)
        }
        val novel4: List<String> = try {
            InputStreamReader(INPUT_FILE4).use { reader -> reader.readLines() }
        } catch (e: IOException) {
            e.printStackTrace()
            exitProcess(1)
        }
        val novel5: List<String> = try {
            InputStreamReader(INPUT_FILE5).use { reader -> reader.readLines() }
        } catch (e: IOException) {
            e.printStackTrace()
            exitProcess(1)
        }

        val allNovels: List<List<String>> =
            listOf(novel1, novel2, novel3, novel4, novel5)

        val wordSeparatedWithSurface: List<String> = allNovels.flatMap { novel ->
                novel.map { sentence ->
                    sentence.translateSurface()}}

        val wordseparatedWithTags = allNovels.flatMap { novel ->
            novel.map { sentence -> sentence.putTags() }
        }

        wordseparatedWithTags.forEach { println(it) }

        // ファイルに分かち書きした単語を出力
        File(OUTPUT_FILE_PATH).printWriter().use { out ->
            wordSeparatedWithSurface.forEach {
                println(it) // debug
                out.println(it)
                out.println()
            }
        }

        // ファイル形態素解析した単語を出力
        File(OUTPUT_FILE_PATH).printWriter().use { out ->
            wordseparatedWithTags.forEach {
                println(it) // debug
                out.println(it)
                out.println()
            }
        }

        // 長方形のワードクラウドを生成
        val frequencyAnalyzerRec = FrequencyAnalyzer()
        frequencyAnalyzerRec.setWordFrequenciesToReturn(300)
        frequencyAnalyzerRec.setMinWordLength(4)
        val wordFrequenciesRec: List<WordFrequency> =
            frequencyAnalyzerRec.load(wordSeparatedWithSurface)
        val dimensionRec = Dimension(500, 312)
        val wordCloudRec = WordCloud(dimensionRec, CollisionMode.PIXEL_PERFECT)
        wordCloudRec.setPadding(0)
        wordCloudRec.setBackground(RectangleBackground(dimensionRec))
        wordCloudRec.setColorPalette(ColorPalette(Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE))
        wordCloudRec.setFontScalar(LinearFontScalar(10, 40))
        wordCloudRec.build(wordFrequenciesRec)
        wordCloudRec.writeToFile("output/wordcloud_n_rectangle.png")

        // 円形のワードクラウドを生成
        val frequencyAnalyzerCir = FrequencyAnalyzer()
        val wordFrequenciesCir = frequencyAnalyzerCir.load(wordSeparatedWithSurface)
        val dimensionCir = Dimension(600, 600)
        val wordCloudCir = WordCloud(dimensionCir, CollisionMode.PIXEL_PERFECT)
        wordCloudCir.setPadding(2)
        wordCloudCir.setBackground(CircleBackground(300))
        wordCloudCir.setColorPalette(
            ColorPalette(
                Color(0x4055F1),
                Color(0x408DF1),
                Color(0x40AAF1),
                Color(0x40C5F1),
                Color(0x40D3F1),
                Color(0xFFFFFF)
            )
        )
        wordCloudCir.setFontScalar(SqrtFontScalar(10, 40))
        wordCloudCir.build(wordFrequenciesCir)
        wordCloudCir.writeToFile("output/wordcloud_n_circle.png")

        // 線形カラーグラデーションのワードクラウドを生成
        val frequencyAnalyzerGra = FrequencyAnalyzer()
        frequencyAnalyzerGra.setWordFrequenciesToReturn(500)
        frequencyAnalyzerGra.setMinWordLength(4)
        val wordFrequencies = frequencyAnalyzerGra.load(wordSeparatedWithSurface)
        val dimensionGra = Dimension(600, 600)
        val wordCloudGra = WordCloud(dimensionGra, CollisionMode.PIXEL_PERFECT)
        wordCloudGra.setPadding(2)
        wordCloudGra.setBackground(CircleBackground(300))
        wordCloudGra.setColorPalette(LinearGradientColorPalette(Color.RED, Color.BLUE, Color.GREEN, 30, 30))
        wordCloudGra.setFontScalar(SqrtFontScalar(10, 40))
        wordCloudGra.build(wordFrequencies)
        wordCloudGra.writeToFile("output/wordcloud_n_gradient.png")

    }

    private fun String.translateSurface(): String =
        TOKENIZER.tokenize(this).map { token ->
            when(token.partOfSpeechLevel1) {
                "名詞" -> token.surface
                "動詞" -> null
                "形容詞" -> null
                else -> null
            }
        }.filterNotNull().joinToString("\t")

    private fun String.putTags(): String =
        TOKENIZER.tokenize(this).map { token ->
            token.surface+ "：" + token.allFeatures
        }.joinToString(" ")
}
