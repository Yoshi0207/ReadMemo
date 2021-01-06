package com.example.readmemo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.widget.AbsListView
import kotlinx.android.synthetic.main.activity_show_books.*
import okhttp3.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.yesButton
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class BookSearchActivity : AppCompatActivity() {
    private val handlerThread=HandlerThread("other")
    val handler=Handler()
    private lateinit var connMgr: ConnectivityManager

    // 本の検索結果をリストで管理
    val listOfTitle:MutableList<String> = mutableListOf()
    val listOfAuthor:MutableList<String> = mutableListOf()
    val listOfPictureUrl:MutableList<String> = mutableListOf()
    val listOfPublisher:MutableList<String> = mutableListOf()
    val listOfOutline:MutableList<String> = mutableListOf()
    val listOfAffiliate:MutableList<String> = mutableListOf()
    val listOfIsbn:MutableList<String> = mutableListOf()
    var pageNumberOfApi = 0 // apiの何ページ目か？
    var isLastPageOfApiBeDisplayed = false // 最後のデータかどうか
    var isApiAccessFinished=false // apiから新しいデータを受け取り終わったか
    private var categoryBeSearched = 0L // 検索カテゴリ 1:タイトル 2:著者 3:シリーズ 4:出版社
    private var searchString = "" // 検索文字列

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_show_books)
        connMgr = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        handlerThread.start()

        // actionbarの設定
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.title = getString(R.string.toggleMenuOfMainView_SearchingBooks) // 本を探す

        // 前の画面から送られる検索する文字列を取得
        searchString = intent.getStringExtra("string")

        // 前の画面から送られるどの項目で検索するのかの情報を取得
        // To-Do:シリーズによる検索を追加
        categoryBeSearched = intent.getStringExtra("id").toLong()

        // apiと通信して検索を実行し、書籍一覧に反映する
        getBooksInfoByCurrentStateAndUpdateBooksList()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
    @SuppressLint("ResourceAsColor", "SetTextI18n")

    // apiと通信し、その結果を書籍リストに反映する
    fun getBooksInfoByCurrentStateAndUpdateBooksList(){
        val networkInfo = connMgr.activeNetworkInfo

        // ネットワーク接続がない場合エラーメッセージを出力して処理を終了
        if ( networkInfo == null || !networkInfo.isConnected ) {
            // pageNumberOfApiを0としているのは、1以上の時はCallbackSetterOfInitBooksListでこの処理を行うため
            // ↑ この関数でメッセージを出す場合、リストがスクロールされるたびにネットワーク接続確認のアラートが出力される
            if (pageNumberOfApi==0){
                alert(getString(R.string.AlertMessage_PleaseCheckNetworkConnection)) { // ネットワーク接続を確認してください
                    yesButton {  }
                }.show()
            }
            return
        }

        pageNumberOfApi+=1 // apiページ番号を+1

        // 楽天ブックスapiの仕様上100ページ以降は存在しない
        // → 100ページを超えたら一覧の更新を終了
        if (pageNumberOfApi>100){
            isLastPageOfApiBeDisplayed=true
        }

        // 最後のページに到達したらそれ以降はAPIにアクセスしない
        if (isLastPageOfApiBeDisplayed) return

        // 何で検索するのかの情報を下に、検索する条件を分岐
        // To-Do:シリーズが未実装
        if (categoryBeSearched==1L){ // タイトル
            LabelOfSearchedCategory.text = "”${searchString}”" + getString(R.string.SearchResultLabel_Title)
            val client = OkHttpClient()
            val request: Request = Request.Builder().let {
                it.url(getString(R.string.ReadMemoApiServer_Domain) + "/book-info-getter/title?title=${searchString}&page=${pageNumberOfApi}")
                it.get()
                it.build()
            }
            client.newCall(request).enqueue(CallbackAdapter())
        }
        if (categoryBeSearched==2L){ // 著者
            LabelOfSearchedCategory.text = "”${searchString}”" + getString(R.string.SearchResultLabel_Author)
            val client = OkHttpClient()
            val request: Request = Request.Builder().let {
                it.url(getString(R.string.ReadMemoApiServer_Domain)+"/book-info-getter/author?author=${searchString}&page=${pageNumberOfApi}")
                it.get()
                it.build()
            }
            Log.d("test", request.toString())
            client.newCall(request).enqueue(CallbackAdapter())
        }
        if (categoryBeSearched==4L){ // 出版社
            LabelOfSearchedCategory.text = "”${searchString}”" + getString(R.string.SearchResultLabel_Author)
            val client = OkHttpClient()
            val request: Request = Request.Builder().let {
                it.url(getString(R.string.ReadMemoApiServer_Domain)+"/book-info-getter/publisher?publisher=${searchString}&page=${pageNumberOfApi}")
                it.get()
                it.build()
            }
            client.newCall(request).enqueue(CallbackAdapter())
        }
    }

    // apiから通信が返ってきたときに動作する
    // 返ってきた結果を、配列に格納する
    inner class CallbackAdapter: Callback {
        private val handler=Handler(handlerThread.looper)
        override fun onResponse(call: Call, response: Response) {
            val responseText: String? = response.body()?.string()
            val parentJsonObj = JSONObject(responseText)

            // apiから返されたJSONがItems属性を持っていれば通常の処理
            // 持っていなければ、エラー処理
            if (parentJsonObj.has("Items")){
                val parentJsonArray = parentJsonObj.getJSONArray("Items")
                Log.d("Success","APIから取得したデータの件数:${parentJsonArray.length()}")

                // 楽天ブックスapiの仕様上1ページごとに返却されるデータは30が上限である
                // → 30未満だった場合、最後のページであると判断する
                if (parentJsonArray.length()<30){
                    isLastPageOfApiBeDisplayed = true
                }

                // JSONデータをパースする
                // JSONデータの形式は次のようになっている
                //
                // Item(書籍): {
                //   title: "",
                //   author: "",
                //   ...
                // }
                //
                // しかし、このアプリケーションでは表示の処理として、タイトルなどの各情報がバラバラの配列に格納されている必要がある
                // よって、書籍ごとに各情報を取り出し、存在しなかった場合、配列に空文字を追加する
                try {
                    var i = 0
                    while (i < parentJsonArray.length()) {
                        val detailJsonObj = parentJsonArray.getJSONObject(i)
                        val volumeInfo = detailJsonObj.getJSONObject("Item")
                        Log.d("Response Item Title","${i+1}番目のデータタイトル${volumeInfo.getString("title")}")
                        if (volumeInfo.has("author")) {
                            listOfAuthor.add(volumeInfo.getString("author"))
                        } else {
                            listOfAuthor.add("")
                        }
                        if(volumeInfo.has("largeImageUrl")){
                            listOfPictureUrl.add(volumeInfo.getString("largeImageUrl"))
                        } else {
                            listOfPictureUrl.add("")
                        }
                        if (volumeInfo.has("title")){
                            listOfTitle.add(volumeInfo.getString("title"))
                        }
                        if (volumeInfo.has("publisherName")){
                            listOfPublisher.add(volumeInfo.getString("publisherName"))
                        } else {
                            listOfPublisher.add("")
                        }
                        if (volumeInfo.has("itemCaption")){
                            listOfOutline.add(volumeInfo.getString("itemCaption"))
                        } else {
                            listOfOutline.add("")
                        }
                        if(volumeInfo.has("affiliateUrl")){
                            listOfAffiliate.add(volumeInfo.getString("affiliateUrl"))
                        } else {
                            listOfAffiliate.add("")
                        }
                        if (volumeInfo.has("isbn")){
                            listOfIsbn.add(volumeInfo.getString("isbn"))
                        } else {
                            listOfIsbn.add("")
                        }
                        i += 1
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

                if (pageNumberOfApi==1) {
                    handler.post(ResultSearch())
                }
            } else {
                this@BookSearchActivity.handler.post(ApiDisableInSearchingByBookTitle())
            }

            // Apiとの通信が終了したことを表す
            // このフラグを用いて、リストがスクロールされたときに多重にリクエストを送らないようにしている
            isApiAccessFinished=true
        }

        override fun onFailure(call: Call, e: IOException) {
            Log.v("failure API Response",e.localizedMessage)
            isApiAccessFinished=true
            this@BookSearchActivity.handler.post(FailureAPIResponse())
        }
    }

    // 取得した情報をフィールドに反映する
    // 結果が格納された配列から、書籍一覧を更新
    inner class ResultSearch : Runnable{
        // Apiにアクセス中かどうか
        var isAccessingApi = false

        override fun run() {
            // 書籍リストの内容を更新
            val booksListAdapterOfSearchView = BooksListAdapterOfSearchView(this@BookSearchActivity,listOfTitle,listOfAuthor,listOfPictureUrl)

            runOnUiThread {
                searchedBooksListInFilterView.adapter = booksListAdapterOfSearchView
            }
            var alertState=false // 通知一回目falseそれ以降true

            // 書籍リストの各アイテムについて、クリックされた時の処理
            // 本の詳細画面へ遷移する
            searchedBooksListInFilterView.setOnItemClickListener { _, _, _, id ->
                val intent=Intent(this@BookSearchActivity,BookDetailActivity::class.java)
                intent.putExtra("title",listOfTitle[id.toInt()])
                intent.putExtra("author",listOfAuthor[id.toInt()])
                intent.putExtra("pictureURL",listOfPictureUrl[id.toInt()])
                intent.putExtra("publisher",listOfPublisher[id.toInt()])
                intent.putExtra("outline",listOfOutline[id.toInt()])
                intent.putExtra("affiliateURL",listOfAffiliate[id.toInt()])
                intent.putExtra("isbn",listOfIsbn[id.toInt()])
                startActivity(intent)
            }

            // 書籍リストがクリックされた時の処理
            // 現在表示しているリストの最後尾まで到達した時、show_listメソッドを呼び出し、内容を更新する
            searchedBooksListInFilterView.setOnScrollListener(
                object: AbsListView.OnScrollListener{
                    override fun onScroll(absListView: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                        //firstVisibleItem : 画面上での一番上のリストの番号\visibleItemCount : 画面内に表示されているリストの個数\totalItemCount : ListViewが持つリストの総数
                        //firstVisibleItem : 画面上での一番上のリストの番号\visibleItemCount : 画面内に表示されているリストの個数\totalItemCount : ListViewが持つリストの総数
                        if(totalItemCount != 0 && totalItemCount == firstVisibleItem + visibleItemCount) {
                            // Apiにアクセス中の時、スクロールされてもApiをコールしない
                            // → これがないと勢いよくスクロールしたときに恐ろしい数のApiリクエストが送信される
                            if (isAccessingApi) return

                            // ネットワーク接続を確認する
                            val networkInfo = connMgr.activeNetworkInfo
                            if (networkInfo == null || !networkInfo.isConnected ){
                                // 変数alertStateを用いることで、エラーメッセージが一度しか表示されないようにしている
                                if (!alertState){
                                    alertState=true
                                    alert(getString(R.string.AlertMessage_PleaseCheckNetworkConnection)) { // ネットワーク接続を確認してください
                                        yesButton {  }
                                    }.show()
                                }
                                isAccessingApi=false
                                return
                            }

                            // Apiにアクセスし、書籍情報を取得する
                            isAccessingApi = true // ApiにアクセスしているフラグをTrueに変更
                            isApiAccessFinished=false

                            getBooksInfoByCurrentStateAndUpdateBooksList()

                            if (!isLastPageOfApiBeDisplayed) {
                                var i=0
                                while (true){
                                    Log.d("tag","doing_roop")
                                    Thread.sleep(50)
                                    i+=1
                                    if (isApiAccessFinished || i>100){
                                        Log.d("tag", "finish_roop")
                                        break
                                    }
                                }
                                booksListAdapterOfSearchView.notifyDataSetChanged()
                                Log.d("tag", "system_off")
                                alertState=false
                            }
                            isAccessingApi=false
                        }
                    }

                    override fun onScrollStateChanged(p0: AbsListView?, p1: Int) {
                    }
                }
            )
        }
    }

    // 楽天APIと通信はできたが、リクエスト上限などの関係でエラーが返された場合の処理
    inner class ApiDisableInSearchingByBookTitle:Runnable{
        override fun run() {
            alert(getString(R.string.AlertMessage_ApiDisableInSearchingByBookTitle)) { // リクエストが混み合っております。しばらくお待ちください
                yesButton {  }
            }.show()
        }
    }

    inner class FailureAPIResponse:Runnable{
        override fun run() {
            alert(getString(R.string.AlertOfEditView_FailureAPIResponse)) { // エラーが発生しました
                finish()
            }.show()
        }
    }
}
