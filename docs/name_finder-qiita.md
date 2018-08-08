# OpenNLP を用いた固有表現抽出
[第21回 Lucene/Solr勉強会 #SolrJP @Yahoo! JAPAN][SolrJP21] にて、[Apache OpenNLP][OpenNLP] が日本語に対応したと聞きました。
これまで OpenNLP というプロダクト自体を知りませんでしたが、[Google Cloud Natural Language API][GCP-NLP] の
[`analyzeEntity`][GCP-API] のようなことができそうだったので、試してみました。

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

下記のようなラベル付き文章を作成します。
ラベルは自由に付与できます。

```
『 <START:作品名> 注文 の 多い 料理 店 <END> 』 （ ち ゅうもんのおおいりょうりてん ） は 、 <START:人名> 宮沢 賢治 <END> の 児童 文学 の 短 編集 で あり 、 また その 中 に 収録 さ れ た 表題 作 で ある 。
```

公式ドキュメントによると、

> The training data should contain at least 15000 sentences to create a model which performs well.

とのことなので、15,000文以上用意すると十分な精度がでるとのことです。

今回はこれだけのデータを作成するのは大変だったため、 [kuromoji][kuromoji] を用いて形態素解析を行い、品詞をラベルとして付与することにしました。
品詞は名詞のみに限定しています。

```
『 <START:名詞-サ変接続> 注文 <END> の 多い <START:名詞-サ変接続> 料理 <END> <START:名詞-接尾-一般> 店 <END> 』 （ ち <START:名詞-一般> ゅうもんのおおいりょうりてん <END> ） は 、 <START:名詞-固有名詞-人名-姓> 宮沢 <END> <START:名詞-固有名詞-人名-名> 賢治 <END> の <START:名詞-一般> 児童 <END> <START:名詞-一般> 文学 <END> の 短 <START:名詞-サ変接続> 編集 <END> で あり 、 また その <START:名詞-非自立-副詞可能> 中 <END> に <START:名詞-サ変接続> 収録 <END> さ れ た <START:名詞-一般> 表題 <END> <START:名詞-接尾-一般> 作 <END> で ある 。
```


# 参考
* [Apache OpenNLP Developer Documentaion][OpenNLP-Document]
* [最新 Apache OpenNLP 1.9.0 で日本語固有表現抽出を試す]: https://www.rondhuit.com/apache-opennlp-1-9-0-ja-ner.html


[SolrJP21]: https://solr.doorkeeper.jp/events/75586
[OpenNLP]: https://opennlp.apache.org/
[OpenNLP-NERecognition]: https://opennlp.apache.org/docs/1.9.0/manual/opennlp.html#tools.namefind.recognition
[GCP-NLP]: https://cloud.google.com/natural-language/
[GCP-API]: https://cloud.google.com/natural-language/docs/reference/rest/
[repo]: https://github.com/lasta/sample-opennlp
[OpenNLP-Document]: https://opennlp.apache.org/docs/1.9.0/manual/opennlp.html