package generator

import com.atilika.kuromoji.TokenizerBase
import com.atilika.kuromoji.ipadic.Tokenizer
import com.kennycason.kumo.CollisionMode
import com.kennycason.kumo.WordCloud
import com.kennycason.kumo.WordFrequency
import com.kennycason.kumo.bg.RectangleBackground
import com.kennycason.kumo.font.scale.LinearFontScalar
import com.kennycason.kumo.nlp.FrequencyAnalyzer
import com.kennycason.kumo.palette.ColorPalette
import java.awt.Color
import java.awt.Dimension
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
    private const val OUTPUT_FILE_PATH = "src/main/java/wakatigaki_nva_all_novel.txt"


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

        val allNovels: List<List<String>> = listOf(novel1, novel2, novel3, novel4, novel5)

        val sentencesWithSurface: List<String> = allNovels.flatMap { novel -> novel.map { sentence -> sentence.putSurface()}}

//        File(OUTPUT_FILE_PATH).printWriter().use { out ->
//            sentencesWithSurface.forEach {
//                println(it) // debug
//                out.println(it)
//                out.println()
//            }
//        }
        val frequencyAnalyzer: FrequencyAnalyzer = FrequencyAnalyzer()
        frequencyAnalyzer.setWordFrequenciesToReturn(300)
        frequencyAnalyzer.setMinWordLength(4)
//        frequencyAnalyzer.setStopWords(loadStopWords())
        val wordFrequencies: List<WordFrequency> = frequencyAnalyzer.load(sentencesWithSurface)
        val dimension: Dimension = Dimension(500, 312)
        val wordCloud: WordCloud = WordCloud(dimension, CollisionMode.PIXEL_PERFECT)
        wordCloud.setPadding(0)
        wordCloud.setBackground(RectangleBackground(dimension))
        wordCloud.setColorPalette(ColorPalette(Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE))
        wordCloud.setFontScalar(LinearFontScalar(10, 40))
        wordCloud.build(wordFrequencies)
        wordCloud.writeToFile("output/wordcloud_rectangle.png")
    }

    private fun String.putSurface(): String = TOKENIZER.tokenize(this).map {token ->
        when(token.partOfSpeechLevel1) {
            "名詞" -> token.surface
            "動詞" -> token.surface
            "形容詞" -> token.surface
            else -> null
        }
    }.filterNotNull().joinToString("\n")
}
