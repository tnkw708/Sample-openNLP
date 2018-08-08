事前準備
========
1. [ここ][OpenNLP] からOpenNLPのバイナリをDL
1. 適当に解凍

[OpenNLP]: https://opennlp.apache.org/

Named Entity Recognition の使い方
------
名前付きエンティティの推定を行う。
文章内から、特徴的な単語をラベル付きで抽出できる。 (固有表現抽出)


1. 下記のようなモデルを作成する (15,000件以上推奨)
    * 各モデルごとに、空行を挟む
    * 例示したモデルはあまり良いものではないことに注意
    * 公式のモデル例は下記
    ```
    <START:person> Pierre Vinken <END> , 61 years old , will join the board as a nonexecutive director Nov. 29 .
    Mr . <START:person> Vinken <END> is chairman of Elsevier N.V. , the Dutch publishing group .
    ```
1. OpenNLP を用いてモデル (バイナリデータ) を生成
    ```bash
    ${path-to-downloaded-opennlp}/bin/opennlp TokenNameFinderTrainer -model ja-ner-hotel-detail.bin -lang ja -data tokenized_data.txt -encoding UTF-8
    ```
    * `-model` : モデルファイルの出力先
    * `-data` : 1で作成したモデル (テキストデータ) 
1. 2.で学習したモデルをもとに、Nameの推定
    ```kotlin
	import com.atilika.kuromoji.TokenizerBase
    import com.atilika.kuromoji.ipadic.Token
    import com.atilika.kuromoji.ipadic.Tokenizer
    import opennlp.tools.namefind.NameFinderME
    import opennlp.tools.namefind.TokenNameFinderModel
    import opennlp.tools.util.Span

    fun analyze(sentence: String) {
        // 形態素ごとに区切った配列の生成が必要
        val surfaces: Array<String> = tokenizer.tokenize(sentence)
                .mapNotNull { token -> token.surface }
                .toTypedArray()
        
        // 2. の -model に指定したパスを指定
        val nameFinder = NameFinderME(TokenNameFinderModel(File("path/to/model")))
        // 固有表現抽出
        val spans: Array<Span> = nameFinder.find(surfaces)
        
        // 標準出力
        surfaces.analyze().forEach { span ->
            println("Span(${span.start},${span.end},${span.type}) = ${span.toStr(surfaces)}")
        }
    }
    
    // 標準出力用のサポート関数
    fun Span.toStr(tokens: Array<String>): String {
        val sb = StringBuilder()
        for (i in this.start..(this.end - 1)) sb.append(tokens[i])
        return sb.toString()
    }
    ```

参考 : [最新 Apache OpenNLP 1.9.0 で日本語固有表現抽出を試す](https://www.rondhuit.com/apache-opennlp-1-9-0-ja-ner.html)