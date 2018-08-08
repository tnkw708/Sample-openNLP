package generator

import com.atilika.kuromoji.TokenizerBase
import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import kotlin.system.exitProcess

object ModelGenerator {
    private val TOKENIZER: Tokenizer = Tokenizer.Builder().mode(TokenizerBase.Mode.NORMAL).build()
    private val INPUT_FILE by lazy { ClassLoader.getSystemResourceAsStream("restaurant-with-a-lot-of-orders.txt") }
    private const val OUTPUT_FILE_PATH = "kenji-miyazawa.train"

    @JvmStatic
    fun main(args: Array<String>) {
        val sentences: List<String> = try {
            InputStreamReader(INPUT_FILE).use { reader -> reader.readLines() }
        } catch (e: IOException) {
            e.printStackTrace()
            exitProcess(1)
        }

        val sentencesWithTags: List<String> = sentences.map { sentence -> sentence.putTags() }

        File(OUTPUT_FILE_PATH).printWriter().use { out ->
            sentencesWithTags.forEach {
                println(it) // debug
                out.println(it)
                out.println()
            }
        }
    }

    private fun String.putTags(): String = TOKENIZER.tokenize(this).map { token ->
        when (token.partOfSpeechLevel1) {
            "名詞" -> token.toStrWithTag()
            else -> token.surface
        }
    }
            .joinToString(" ")

    private fun Token.toStrWithTag(): String {
        val emptySymbol = "*"
        val sb = StringBuilder()
        sb.append("<START:")
        sb.append(this.partOfSpeechLevel1)
        if (this.partOfSpeechLevel2 != emptySymbol) sb.append("-").append(partOfSpeechLevel2)
        if (this.partOfSpeechLevel3 != emptySymbol) sb.append("-").append(partOfSpeechLevel3)
        if (this.partOfSpeechLevel4 != emptySymbol) sb.append("-").append(partOfSpeechLevel4)
        sb.append("> ")
        sb.append(this.surface)
        sb.append(" <END>")
        return sb.toString()
    }
}
