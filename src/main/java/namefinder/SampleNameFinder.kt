package namefinder

import com.atilika.kuromoji.TokenizerBase
import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer
import opennlp.tools.namefind.NameFinderME
import opennlp.tools.namefind.TokenNameFinderModel
import opennlp.tools.util.Span
import java.io.File

object SampleNameFinder {
    private val MODEL = "my-model.bin"
    private val TOKENIZER: Tokenizer = Tokenizer.Builder().mode(TokenizerBase.Mode.NORMAL).build()

    @JvmStatic
    fun main(args: Array<String>) {
        val sentence = "東京では雨が降っている。"
        val tokens: List<Token> = sentence.tokenize()
        val surfaces: List<String> = tokens.map { token -> token.surface }
        val spans: List<Span> = tokens.analyze()

        spans.forEach { span ->
            println("Span(${span.start},${span.end},${span.type}) = ${span.toStr(surfaces)}")
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