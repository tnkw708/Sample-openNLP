import com.atilika.kuromoji.TokenizerBase
import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer

object TokenizerSample {
    private val TOKENIZER: Tokenizer = Tokenizer.Builder().mode(TokenizerBase.Mode.NORMAL).build()

    @JvmStatic
    fun main(args: Array<String>) {
        val sentence = "『注文の多い料理店』（ちゅうもんのおおいりょうりてん）は、宮沢賢治の児童文学の短編集であり、またその中に収録された表題作である。"
        val surfaces: List<String> = TOKENIZER.tokenize(sentence).map { token -> token.surface }

        val splitSentence = surfaces.joinToString(" ")
        println(splitSentence)

        println(sentence.putTags())
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