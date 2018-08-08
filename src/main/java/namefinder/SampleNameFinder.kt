package namefinder

import com.atilika.kuromoji.TokenizerBase
import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer
import opennlp.tools.namefind.NameFinderME
import opennlp.tools.namefind.TokenNameFinderModel
import opennlp.tools.util.Span
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import kotlin.system.exitProcess

object SampleNameFinder {
    private val MODEL = "kenji-miyazawa.bin"
    private val TOKENIZER: Tokenizer = Tokenizer.Builder().mode(TokenizerBase.Mode.NORMAL).build()
    // https://www.aozora.gr.jp/cards/000081/files/470_15407.html
    private val GAUCHE = ClassLoader.getSystemResourceAsStream("gauche-the-cellist.txt")

    @JvmStatic
    fun main(args: Array<String>) {
        val sentences: List<String> = InputStreamReader(GAUCHE).use {
            try {
                it.readLines()
            } catch (e: IOException) {
                e.printStackTrace()
                exitProcess(1)
            }
        }

        sentences.forEach { sentence ->
            val tokens: List<Token> = sentence.tokenize()
            val surfaces: List<String> = tokens.map { token -> token.surface }
            val spans: List<Span> = tokens.analyze()

            println(sentence)
            spans.forEach { span ->
                println("Span(${span.start},${span.end},${span.type}) = ${span.toStr(surfaces)}")
            }

            println()
        }
    }

    private fun String.tokenize(): List<Token> =
            TOKENIZER.tokenize(this).filterNotNull()

    private fun List<Token>.analyze(): List<Span> = NameFinderME(TokenNameFinderModel(File(MODEL)))
            .find(this.map { token -> token.surface }.toTypedArray())
            .filter { span -> span is Span }

    private fun Span.toStr(surfaces: List<String>): String {
        val sb = StringBuilder()
        for (i in this.start..(this.end - 1)) sb.append(surfaces[i])
        return sb.toString()
    }

}