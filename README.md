# ReadMemoBackEndApplication
本アプリケーションは蔵書を管理するためのAndroid用読書管理アプリです。

## 本アプリケーションについて
◇気になったことをなんでもメモしよう！<br>
「読書メモ」では、登録した本それぞれにメモをつけることができます。気になったフレーズ・感想など、後から思い出すのに便利です。<br>
<br>
◇キーワードまたはISBNで本を楽々追加！<br>
書籍登録画面では、検索することで本を追加できるので、面倒な書籍情報入力は不要です。また、検索した本は表紙も登録されます。<br>
<br>
◇その月にどれくらいの本を読んだかがすぐわかる！<br>
読書記録画面で一か月に読んだ本の冊数とページ数が丸わかり。もちろん過去の記録も見ることができるので、後から振り返りたいときも便利です。<br>
<br>
◇本をカテゴリ別に管理可能！<br>
「読んだ本」、「読んでいる本」、「読みたい本」と分類することができます。どの本読もうとしてたんだっけと迷うこともありません。<br>
<br>
◇今人気の本が一目で分かる！<br>
「本を検索する」では、ジャンル別で今人気の本が順に表示されます。この本読んでみたいな！と思う本があれば、「読みたい本」にそのまま登録することが可能です。次何読もうかな…？と考えているときも、ぜひ参考にしてみてください。もちろん、タイトル・ISBNによる本の検索も可能です。<br>

## 使い方
### 1. [ReadMemoBackEndApplication](https://github.com/Yoshi0207/ReadMemoBackEndApplication)を準備する
本アプリケーションでは、アプリケーション内で書籍情報を取得する際、[ReadMemoBackEndApplication](https://github.com/Yoshi0207/ReadMemoBackEndApplication)へリクエストを送信する必要があります。

### 2. ReadMemoBackEndApplicationへのアドレスを指定する
`1`で準備した`ReadMemoBackEndApplication`が動作するサーバへのアドレスを設定します。ファイル`app/src/main/res/values/strings.xml`内、`<string name="ReadMemoApiServer_Domain">http://10.0.2.2:8080</string>`の`http://10.0.2.2:8080`の部分を、`ReadMemoBackEndApplication`が動作するサーバのアドレスに変更します。

### 3. アプリケーションをビルドし、Android端末にインストールする
