# OpenNLP を用いた固有表現抽出
[第21回 Lucene/Solr勉強会 #SolrJP @Yahoo! JAPAN][SolrJP21] にて、[Apache OpenNLP][OpenNLP] が日本語に対応したという話がありました。
[Google Cloud Natural Language API][GCP-NLP] の[`analyzeEntity`][GCP-API] のようなことができそうだったので、試してみました。

コードは[Github][repo]にあります。

## 本記事の流れ
1. 事前準備
1. 学習データの作成
1. OpenNLP を用いてモデルの作成
1. モデルを用いて固有表現抽出

## 事前準備
[ここ][OpenNLP] からバイナリデータをダウンロードして、適当な場所に解凍します。

## 学習データの作成
[公式ドキュメント][OpenNLP-NERecognition]

ラベル付き文章を作成します。
**OpenNLP は形態素ごとにスペースで区切られていることを前提としているため、予め形態素解析を行いスペースで区切っておく必要があります。**
また、学習データ間 (文と文の間) には空行をはさみます。
ラベルは自由に付与できます。

```ラベル付き文章
『 <START:作品名> 注文 の 多い 料理 店 <END> 』 （ ち ゅうもんのおおいりょうりてん ） は 、 <START:人名> 宮沢 賢治 <END> の 児童 文学 の 短 編集 で あり 、 また その 中 に 収録 さ れ た 表題 作 で ある 。
```

公式ドキュメントによると、

> The training data should contain at least 15000 sentences to create a model which performs well.

とのことなので、15,000文以上用意すると十分な精度がでるとのことです。

今回はこれだけのデータを作成するのは大変だったため、 [kuromoji][kuromoji] を用いて形態素解析を行い、品詞をラベルとして付与することにしました。
品詞は名詞のみに限定しています。

```kotlin
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

    // 形態素解析 + ラベル (品詞) の付与
    private fun String.putTags(): String = TOKENIZER.tokenize(this).map { token ->
        when (token.partOfSpeechLevel1) {
            "名詞" -> token.toStrWithTag()
            else -> token.surface
        }
    }
            // スペースで区切る
            .joinToString(" ")

    // ラベル付き文の作成
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
```

```text:学習データ例
『 <START:名詞-サ変接続> 注文 <END> の 多い <START:名詞-サ変接続> 料理 <END> <START:名詞-接尾-一般> 店 <END> 』 （ ち <START:名詞-一般> ゅうもんのおおいりょうりてん <END> ） は 、 <START:名詞-固有名詞-人名-姓> 宮沢 <END> <START:名詞-固有名詞-人名-名> 賢治 <END> の <START:名詞-一般> 児童 <END> <START:名詞-一般> 文学 <END> の 短 <START:名詞-サ変接続> 編集 <END> で あり 、 また その <START:名詞-非自立-副詞可能> 中 <END> に <START:名詞-サ変接続> 収録 <END> さ れ た <START:名詞-一般> 表題 <END> <START:名詞-接尾-一般> 作 <END> で ある 。
```

([実際のコード][ModelGenerator])

## OpenNLP を用いてモデルの作成
作成した学習用のデータをOpenNLPに食わせて、学習モデルを作成します。
Javaプログラムでの作成を試しましたが、うまくいきませんでした。 ([Training API][OpenNLP-TrainingAPI])
そのため、Training Tool を用いて学習モデルを作成します。
[こちらのページ][Rondhuit-training]が詳しいです。


```bash:最大エントロピー法での学習モデルの作成
// Usage : opennlp TokenNameFinderTrainer -model ${出力するモデルファイル名} -lang ${言語} -data ${先に作成した学習用データ} -encoding ${文字コード}
${path-to-downloaded-opennlp}/bin/opennlp TokenNameFinderTrainer -model ja-ner-hotel-detail.bin -lang ja -data tokenized_data.txt -encoding UTF-8
```

## モデルを用いて固有表現抽出
いよいよ、分類をしてみます。
先に述べたとおり、OpenNLPは **形態素ごとに分割済みの文** のみ扱えるため、予め形態素解析してから食わせる必要があります。

```kotlin
class SampleNameFinder {
    fun analyze(sentence: String) {
        // 形態素ごとに区切った配列の生成が必要
        val surfaces: Array<String> = tokenizer.tokenize(sentence)
                .mapNotNull { token -> token.surface }
                .toTypedArray()
        
        // -model に指定したパスを指定
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
}
```

[実際のコード][NameFinder]

## 実際に動かしてみた
学習用データは、 [宮沢賢治の「注文の多い料理店」][restaurant]を用いました。
学習用データ名は `kenji-miyazawa.txt` としました。

```text:ModelGenerator
<START:名詞-数> 二 <END> <START:名詞-接尾-助数詞> 人 <END> の 若い <START:名詞-一般> 紳士 <END> が 、 すっかり <START:名詞-固有名詞-地域-国> イギリス <END> の <START:名詞-一般> 兵隊 <END> の <START:名詞-一般> かたち <END> を し て 、 ぴかぴか する <START:名詞-一般> 鉄砲 <END> を かつい で 、 <START:名詞-一般> 白熊 <END> の <START:名詞-非自立-助動詞語幹> よう <END> な <START:名詞-一般> 犬 <END> を <START:名詞-数> 二 <END> <START:名詞-一般> 疋 <END> つれ て 、 だいぶ <START:名詞-一般> 山奥 <END> の 、 <START:名詞-一般> 木の葉 <END> の かさかさ し た <START:名詞-一般> とこ <END> を 、 こんな <START:名詞-非自立-一般> こと <END> を 云い ながら 、 ある い て おり まし た 。
```

生成した学習用データを、OpenNLPに食わせます。
モデルデータ名は `kenji-miyazawa.bin` にしています。

```bash:モデルの作成
$ ~/bin/apache-opennlp-1.9.0/bin/opennlp TokenNameFinderTrainer -model kenji-miyazawa.bin -lang ja -data kenji-miyazawa.train -encoding UTF-8
Indexing events with TwoPass using cutoff of 0

        Computing event counts...  done. 3375 events
        Indexing...  done.
Collecting events... Done indexing in 0.54 s.
Incorporating indexed data for training...
done.
        Number of Event Tokens: 3375
            Number of Outcomes: 22
          Number of Predicates: 12352
Computing model parameters...
Performing 300 iterations.
  1:  . (2822/3375) 0.8361481481481482
  2:  . (3175/3375) 0.9407407407407408
  3:  . (3269/3375) 0.9685925925925926
  4:  . (3314/3375) 0.981925925925926
  5:  . (3335/3375) 0.9881481481481481
  6:  . (3344/3375) 0.9908148148148148
  7:  . (3357/3375) 0.9946666666666667
  8:  . (3350/3375) 0.9925925925925926
  9:  . (3351/3375) 0.9928888888888889
 10:  . (3352/3375) 0.9931851851851852
 20:  . (3370/3375) 0.9985185185185185
Stopping: change in training set accuracy less than 1.0E-5
Stats: (3375/3375) 1.0
...done.

Training data summary:
#Sentences: 153
#Tokens: 3375
#名詞-ナイ形容詞語幹 entities: 2
#名詞-固有名詞-地域-国 entities: 2
#名詞-固有名詞-人名-姓 entities: 2
#名詞-数 entities: 48
#名詞-固有名詞-地域-一般 entities: 5
#名詞-固有名詞-組織 entities: 3
#名詞-副詞可能 entities: 21
#名詞-サ変接続 entities: 45
#名詞-形容動詞語幹 entities: 22
#名詞-接尾-助動詞語幹 entities: 2
#名詞-接尾-サ変接続 entities: 1
#名詞-一般 entities: 398
#名詞-非自立-一般 entities: 63
#名詞-代名詞-一般 entities: 74
#名詞-接尾-一般 entities: 31
#名詞-接尾-人名 entities: 3
#名詞-固有名詞-一般 entities: 1
#名詞-接尾-助数詞 entities: 37
#名詞-非自立-副詞可能 entities: 27
#名詞-非自立-助動詞語幹 entities: 9
#名詞-接尾-特殊 entities: 2

Writing name finder model ... Compressed 12352 parameters to 5383
375 outcome patterns
done (0.303s)
```

この学習データを用いて、[宮沢賢治の「セロ弾きのゴーシュ」][gauche]を分類してみました。

```text
ゴーシュは町の活動写真館でセロを弾く係りでした。けれどもあんまり上手でないという評判でした。上手でないどころではなく実は仲間の楽手のなかではいちばん下手でしたから、いつでも楽長にいじめられるのでした。
Span(0,1,名詞-一般) = ゴーシュ
Span(2,3,名詞-一般) = 町
Span(4,5,名詞-一般) = 活動
Span(6,7,名詞-一般) = 館
Span(8,9,名詞-一般) = セロ
Span(11,12,名詞-一般) = 係り
Span(17,18,名詞-一般) = 上手
Span(21,22,名詞-一般) = 評判
Span(25,26,名詞-一般) = 上手
Span(33,34,名詞-一般) = 仲間
Span(35,36,名詞-一般) = 楽
Span(36,37,名詞-一般) = 手
Span(38,39,名詞-非自立-副詞可能) = なか
Span(42,43,名詞-一般) = 下手
Span(47,48,名詞-代名詞-一般) = いつ
Span(49,50,名詞-一般) = 楽長
Span(53,54,名詞-非自立-一般) = の
```

この出力から分かる通り、「注文の多い料理店」には出現しない単語「ゴーシュ」も名詞として分類できています。

## まとめ
OpenNLP を用いて学習データの作成、及びそれを利用した文章の自動ラベル付を行いました。
品詞ではあまり有用なラベル付はできませんでしたが、利用方法を理解できました。

## 参考
* [Apache OpenNLP Developer Documentaion][OpenNLP-Document]
* [最新 Apache OpenNLP 1.9.0 で日本語固有表現抽出を試す](https://www.rondhuit.com/apache-opennlp-1-9-0-ja-ner.html)


[restaurant]: https://www.aozora.gr.jp/cards/000081/files/43754_17659.html
[gauche]: https://www.aozora.gr.jp/cards/000081/files/470_15407.html
[kuromoji]: https://www.atilika.com/ja/kuromoji/
[ModelGenerator]: https://github.com/lasta/sample-opennlp/blob/master/src/main/java/generator/ModelGenerator.kt
[NameFinder]: https://github.com/lasta/sample-opennlp/blob/master/src/main/java/namefinder/SampleNameFinder.kt
[OpenNLP-TrainingAPI]: https://opennlp.apache.org/docs/1.9.0/manual/opennlp.html#tools.langdetect.training.api
[Rondhuit-training]: https://www.rondhuit.com/opennlp-%E6%9C%80%E5%A4%A7%E3%82%A8%E3%83%B3%E3%83%88%E3%83%AD%E3%83%94%E3%83%BC%E6%B3%95%E3%81%A8%E3%83%91%E3%83%BC%E3%82%BB%E3%83%97%E3%83%88%E3%83%AD%E3%83%B3%E3%81%AE%E5%88%86%E9%A1%9E%E5%99%A8.html
[SolrJP21]: https://solr.doorkeeper.jp/events/75586
[OpenNLP]: https://opennlp.apache.org/
[OpenNLP-NERecognition]: https://opennlp.apache.org/docs/1.9.0/manual/opennlp.html#tools.namefind.recognition
[GCP-NLP]: https://cloud.google.com/natural-language/
[GCP-API]: https://cloud.google.com/natural-language/docs/reference/rest/
[repo]: https://github.com/lasta/sample-opennlp
[OpenNLP-Document]: https://opennlp.apache.org/docs/1.9.0/manual/opennlp.html
