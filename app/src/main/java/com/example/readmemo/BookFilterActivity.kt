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
import android.view.Menu
import android.widget.AbsListView
import android.widget.SearchView
import kotlinx.android.synthetic.main.activity_book_ranking.*
import okhttp3.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.yesButton
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class BookFilterActivity : AppCompatActivity(),SearchView.OnQueryTextListener {
    private lateinit var searchView: SearchView
    private val handlerThread = HandlerThread("other")
    private lateinit var connMgr: ConnectivityManager

    val handler=Handler()

    // 本の検索結果をリストで管理
    val listOfTitle:MutableList<String> = mutableListOf()
    val listOfAuthors:MutableList<String> = mutableListOf()
    val listOfPictureUrl:MutableList<String> = mutableListOf()
    val listOfPublisher:MutableList<String> = mutableListOf()
    val listOfOutline:MutableList<String> = mutableListOf()
    val listOfAffiliateUrl:MutableList<String> = mutableListOf()
    val listOfIsbn:MutableList<String> = mutableListOf()
    var pageNumberOfApi = 0 // apiの何ページ目か？
    private var displayedGenreId = "" // 現在選択されているジャンルのid(初期値All)
    var isLastPageOfApiBeDisplayed = false // 最後のデータかどうか
    var isApiAccessFinished = false // apiから新しいデータを受け取り終わったか

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_book_ranking)
        connMgr = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        handlerThread.start()

        // actionbarの設定
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true) // actionbarに戻るボタンを追加
        actionBar?.title = getString(R.string.toggleMenuOfMainView_SearchingBooks) // 本を探す

        // リストを表示
        getBooksInfoByCurrentStateAndUpdateBooksList()

        // 各ジャンルのボタンが押された時の処理
        // リストを消去 → ジャンルid変更 → apiページ番号0 → リストを表示
        btnOfChangingGenreToCommic.setOnClickListener {
            if (displayedGenreId=="001"){
                return@setOnClickListener
            } else {
                clearBooksList()
                displayedGenreId = "001"
                pageNumberOfApi = 0
                isLastPageOfApiBeDisplayed=false
                getBooksInfoByCurrentStateAndUpdateBooksList()
            }
        }
        btnOfChangingGenreToNovel.setOnClickListener {
            if (displayedGenreId=="004"){
                return@setOnClickListener
            } else {
                clearBooksList()
                displayedGenreId = "004"
                pageNumberOfApi = 0
                isLastPageOfApiBeDisplayed=false
                getBooksInfoByCurrentStateAndUpdateBooksList()
            }
        }
        btnOfChangingGenreToBusiness.setOnClickListener {
            if (displayedGenreId=="006"){
                return@setOnClickListener
            } else {
                clearBooksList()
                displayedGenreId = "006"
                pageNumberOfApi = 0
                isLastPageOfApiBeDisplayed=false
                getBooksInfoByCurrentStateAndUpdateBooksList()
            }
        }
        btnOfChangingGenreToPaperback.setOnClickListener {
            if (displayedGenreId=="019"){
                return@setOnClickListener
            } else {
                clearBooksList()
                displayedGenreId = "019"
                pageNumberOfApi = 0
                isLastPageOfApiBeDisplayed=false
                getBooksInfoByCurrentStateAndUpdateBooksList()
            }
        }
        btnOfChangingGenreToPocketbook.setOnClickListener {
            if (displayedGenreId=="020"){
                return@setOnClickListener
            } else {
                clearBooksList()
                displayedGenreId = "020"
                pageNumberOfApi = 0
                isLastPageOfApiBeDisplayed=false
                getBooksInfoByCurrentStateAndUpdateBooksList()
            }
        }
        btnOfChangingGenreToLightNovel.setOnClickListener {
            if (displayedGenreId=="017"){
                return@setOnClickListener
            } else {
                clearBooksList()
                displayedGenreId = "017"
                pageNumberOfApi = 0
                isLastPageOfApiBeDisplayed=false
                getBooksInfoByCurrentStateAndUpdateBooksList()
            }
        }
        btnOfChangingGenreToAll.setOnClickListener {
            if (displayedGenreId==""){
                return@setOnClickListener
            } else {
                clearBooksList()
                displayedGenreId = ""
                pageNumberOfApi = 0
                isLastPageOfApiBeDisplayed=false
                getBooksInfoByCurrentStateAndUpdateBooksList()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
    @SuppressLint("ResourceAsColor")

    // actionbarにオプションメニューを表示する
    // このメニューには文字列を入力することができる
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search, menu)
        searchView = menu?.findItem(R.id.searchVW)?.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        searchView.isSubmitButtonEnabled = true
        searchView.setIconifiedByDefault(false)
        searchView.setBackgroundColor(R.color.colorAccent)
        searchView.queryHint = getString(R.string.searchHint)
        return super.onCreateOptionsMenu(menu)
    }

    // オプションメニューの文字列が変化した時の処理
    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

    // オプションメニューの送信処理が行われた時の処理
    // オプションメニューに入力されていた文字列がisbnかタイトルかによって処理を分岐する
    override fun onQueryTextSubmit(query: String?): Boolean {
        if (query?.length==1){
            alert(getString(R.string.AlertOfEditView_ClickedSearchBtnOneWordOrLess)) { // 2文字以上入力してください
                yesButton {  }
            }.show()
            return false
        }
        if (query?.length == 10 || query?.length == 13) {
            // 検索文字列が10or13の時、isbnの可能性があるため処理を分割する
            val isbn = query.toLongOrNull()
            if (isbn==null){
                // queryがLong型に変換できなかった場合、queryが書籍名であると判断する
                // → 書籍名で検索を実行する
                // 検索結果表示画面へ遷移
                val intent=Intent(this@BookFilterActivity,BookSearchActivity::class.java)
                intent.putExtra("string",query)
                intent.putExtra("id","1")
                startActivity(intent)
            } else {
                // queryをLong型に変換できた場合、queryがisbnであると判断する
                // → isbnで検索を実行する
                val client = OkHttpClient()
                val request: Request = Request.Builder().let {
                    it.url(getString(R.string.ReadMemoApiServer_Domain)+"/book-info-getter/isbn?isbn=${query}")
                    it.get()
                    it.build()
                }
                client.newCall(request).enqueue(CallbackAdapterSearchingByIsbn())
            }
        } else {
            // 検索文字列が10or13以外の時、queryが書籍名であると判断する
            // → 書籍名で検索を実行する
            // 検索結果表示画面へ遷移
            val intent=Intent(this@BookFilterActivity,BookSearchActivity::class.java)
            intent.putExtra("string",query)
            intent.putExtra("id","1")
            startActivity(intent)
        }
        return false
    }

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

        val client = OkHttpClient()
        val request: Request = Request.Builder().let {
            it.url(getString(R.string.ReadMemoApiServer_Domain)+"/book-info-getter/genre?genre=${displayedGenreId}&page=${pageNumberOfApi}")
            it.get()
            it.build()
        }
        client.newCall(request).enqueue(CallbackAdapterSearchingByBookTitle())
    }

    // 書籍リストを削除する
    private fun clearBooksList(){
        // 処理を簡潔にするため書籍リストを一度クリアした後、再び書籍リストを作成している
        val booksListAdapterOfEditView = BooksListAdapterOfEditView(this, arrayOf(""), arrayOf(""), arrayOf(""))
        searchedBooksListInSearchView.adapter = booksListAdapterOfEditView
        listOfTitle.clear()
        listOfAuthors.clear()
        listOfPictureUrl.clear()
        listOfPublisher.clear()
        listOfOutline.clear()
        listOfAffiliateUrl.clear()
        listOfIsbn.clear()

        val booksListAdapterOfSearchView = BooksListAdapterOfSearchView(this@BookFilterActivity,listOfTitle,listOfAuthors,listOfPictureUrl)
        searchedBooksListInSearchView.adapter = booksListAdapterOfSearchView
    }

    // apiから通信が返ってきたときに動作する
    // 返ってきた結果を、配列に格納する
    inner class CallbackAdapterSearchingByBookTitle:Callback{
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
                // → 結果の30未満だった場合、最後のページであると判断する
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
                // To-Do:オブジェクトごとに処理できるように修正
                try {
                    var i = 0
                    while (i < parentJsonArray.length()) {
                        val detailJsonObj = parentJsonArray.getJSONObject(i)
                        val volumeInfo = detailJsonObj.getJSONObject("Item")
                        Log.d("Response Item Title","${i+1}番目のデータタイトル${volumeInfo.getString("title")}")
                        if (volumeInfo.has("author")) {
                            listOfAuthors.add(volumeInfo.getString("author"))
                        } else {
                            listOfAuthors.add("")
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
                            listOfAffiliateUrl.add(volumeInfo.getString("affiliateUrl"))
                        } else {
                            listOfAffiliateUrl.add("")
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

                // 1ページ目であれば書籍リストを新たに作成する
                if (pageNumberOfApi==1) {
                    handler.post(CallbackSetterOfInitBooksList())
                }
            } else {// Itemsをもっていなかったら
                // エラーメッセージを出力する
                this@BookFilterActivity.handler.post(ApiDisableInSearchingByBookTitle())
            }

            // Apiとの通信が終了したことを表す
            // このフラグを用いて、リストがスクロールされたときに多重にリクエストを送らないようにしている
            isApiAccessFinished=true
        }

        override fun onFailure(call: Call, e: IOException) {
            Log.v("failure API Response",e.localizedMessage)
            isApiAccessFinished=true
            this@BookFilterActivity.handler.post(FailureAPIResponse())
        }
    }

    // 取得した情報をフィールドに反映する
    // 結果が格納された配列から、書籍一覧を更新
    inner class CallbackSetterOfInitBooksList : Runnable{
        // Apiにアクセス中かどうか
        var isAccessingApi = false

        override fun run() {
            // 書籍リストの内容を更新
            val booksListAdapterOfSearchView = BooksListAdapterOfSearchView(this@BookFilterActivity,listOfTitle,listOfAuthors,listOfPictureUrl)

            runOnUiThread {
                searchedBooksListInSearchView.adapter = booksListAdapterOfSearchView
            }
            var alertState=false // 通知一回目false、それ以降true

            // 書籍リストの各アイテムについて、クリックされた時の処理
            // 本の詳細画面へ遷移する
            searchedBooksListInSearchView.setOnItemClickListener { _, _, _, id ->
                val intent=Intent(this@BookFilterActivity,BookDetailActivity::class.java)
                intent.putExtra("title",listOfTitle[id.toInt()])
                intent.putExtra("author",listOfAuthors[id.toInt()])
                intent.putExtra("pictureURL",listOfPictureUrl[id.toInt()])
                intent.putExtra("publisher",listOfPublisher[id.toInt()])
                intent.putExtra("outline",listOfOutline[id.toInt()])
                intent.putExtra("affiliateURL",listOfAffiliateUrl[id.toInt()])
                intent.putExtra("isbn",listOfIsbn[id.toInt()])
                startActivity(intent)
            }

            // 書籍リストがスクロールされた時の処理
            // 現在表示しているリストの最後尾まで到達した時、getBooksInfoByCurrentStateAndUpdateBooksListメソッドを呼び出し、内容を更新する
            searchedBooksListInSearchView.setOnScrollListener(
                object: AbsListView.OnScrollListener{
                    override fun onScroll(absListView: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
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

    // isbnで検索を実行した場合の処理
    // apiから取得したデータから本の追加画面へ遷移する準備
    // → Result_isbn_Searchを呼び出し、追加画面へ遷移する
    inner class CallbackAdapterSearchingByIsbn:Callback{
        override fun onResponse(call: Call, response: Response) {
            val responseText: String? = response.body()?.string()
            val parentJsonObj = JSONObject(responseText)

            // 取得したデータが0件だった場合、エラーメッセージを出力して処理を終了する
            if (parentJsonObj.getInt("count")==0){
                handler.post(GetNullBookInSearchingByISBN())
                return
            }

            var author = ""
            var pictureUrl = ""
            var title = ""
            var publisher = ""
            var outline = ""
            var affiliateUrl =""
            var isbn = ""
            try {
                val parentJsonArray = parentJsonObj.getJSONArray("Items")
                Log.d("Success","APIから取得したデータの件数:${parentJsonArray.length()}")
                val detailJsonObj = parentJsonArray.getJSONObject(0)
                val volumeInfo = detailJsonObj.getJSONObject("Item")
                Log.d("Response Item Title","${1}番目のデータタイトル${volumeInfo.getString("title")}")
                if (volumeInfo.has("author")) {
                    author = volumeInfo.getString("author")
                }
                if(volumeInfo.has("largeImageUrl")){
                    pictureUrl = volumeInfo.getString("largeImageUrl")
                }
                if (volumeInfo.has("title")){
                    title = volumeInfo.getString("title")
                }
                if (volumeInfo.has("publisherName")){
                    publisher = volumeInfo.getString("publisherName")
                }
                if (volumeInfo.has("itemCaption")){
                    outline = volumeInfo.getString("itemCaption")
                }
                if(volumeInfo.has("affiliateUrl")){
                    affiliateUrl = volumeInfo.getString("affiliateUrl")
                }
                if (volumeInfo.has("isbn")){
                    isbn = volumeInfo.getString("isbn")
                }
            } catch (e:JSONException) {
                e.printStackTrace()
            }
            handler.post(CallbackSetterSearchingByISBN(title,author,pictureUrl,publisher,outline,affiliateUrl,isbn))
        }

        override fun onFailure(call: Call, e: IOException) {
            Log.v("failure API Response",e.localizedMessage)
            handler.post(FailureAPIResponse())
        }
    }

    // タイトルなどの各情報を受け取り、本の追加画面へ遷移する処理
    inner class CallbackSetterSearchingByISBN(val title: String,
                                              private val author: String,
                                              private val picture: String,
                                              private val publisher: String,
                                              private val outline: String, private val affiliateUrl: String,
                                              private val isbn: String
    ):Runnable{
        override fun run() {
            // 書籍詳細画面へ遷移する
            // この際取得した書籍情報を渡す
            val intent=Intent(this@BookFilterActivity,BookDetailActivity::class.java)
            intent.putExtra("title",title)
            intent.putExtra("author",author)
            intent.putExtra("pictureURL",picture)
            intent.putExtra("publisher",publisher)
            intent.putExtra("outline",outline)
            intent.putExtra("affiliateURL",affiliateUrl)
            intent.putExtra("isbn",isbn)
            startActivity(intent)
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

    inner class GetNullBookInSearchingByISBN:Runnable{
        override fun run() {
            alert(getString(R.string.AlertMessage_GetNullBookInSearchingByISBN)) { // 該当する本が見つかりませんでした。ISBNが間違っていないか確認してください
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
