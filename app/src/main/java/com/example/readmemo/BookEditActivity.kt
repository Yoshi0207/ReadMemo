package com.example.readmemo

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Checkable
import android.widget.RadioButton
import android.widget.SearchView
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_book_edit.*
import okhttp3.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.IllegalArgumentException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class BookEditActivity : AppCompatActivity(),SearchView.OnQueryTextListener {
    private lateinit var realm: Realm
    private lateinit var searchView: SearchView
    private val handler = Handler()
    private var recordId : Long? = -1L // 表示している書籍について、データベースのブックテーブルのIDを保持する変数
    private var status: Long? = 0 // 0:read 1:reading 2:will read
    var isbn = "" // 表示している書籍について、isbnを保持する変数
    var affiliateURL = "" // 表示している書籍について、アフィリエイトurlを保持する変数
    var numberOfPages = 0L // 表示している書籍について、ベージ数を保持する変数

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_edit)
        realm = Realm.getDefaultInstance()

        // search_list_view は検索バーに文字を入力した時に詳細に被せる形で表示される
        // → 最初は隠されている
        searchedBooksList.visibility = View.INVISIBLE

        // 以下は一時的に値を保持しておくためのフィールドを隠すための処理
        // To-Do:一時的に値を保存するフィールドの削除
        apiURL.visibility = View.INVISIBLE
        addedDate.visibility = View.INVISIBLE

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // actionbarの設定
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true) // actionbarに戻るボタンを追加

        // book_idはMainActivityから渡される変数
        // book_idが-1L以外の場合、このクラスはMainActivityから遷移している
        // → book_idはBookテーブルのid
        // book_idが存在しない場合(渡されていない場合)、新規追加ボタンから遷移した
        // → book_idはデフォルトの-1L
        recordId = intent?.getLongExtra("book_id", -1L)
        if (recordId != -1L) {
            // bookIdをキーとしてBookテーブルから値を取得
            val book = realm.where<Book>().equalTo("id", recordId).findFirst()
            titleField.setText(book?.title)
            authorField.setText(book?.author)
            detailField.setText(book?.detail)
            publisherField.setText(book?.publisher)
            if (book?.picture?.length != 0){
                val url = book?.picture
                val request: Request = Request.Builder().let {
                    it.url("$url")
                    it.get()
                    it.build()
                }
                val client = OkHttpClient()
                client.newCall(request).enqueue(CallbackAdapterDownloadAndUpdateImageView())
            }

            // statusによってデフォルトでチェックを入れるフィールドを変える
            status = book?.status
            if (status == 0L){
                // 読んだ本
                checkRadioBenOfStatus(R.id.statusHaveRead)
            }
            if (status == 1L){
                // 読んでいる本
                checkRadioBenOfStatus(R.id.statusReading)
            }
            if (status == 2L){
                // 読みたい本
                checkRadioBenOfStatus(R.id.statusWillRead)
            }

            // 削除ボタンを有効化する
            recordDeleteBtn.visibility = View.VISIBLE

            // ツールバーに表示する文章を設定
            actionBar?.title = getString(R.string.toolbarTitleOfBookEditView_Editing) // 本を編集する

            // 修正ボタンのテキストを変更する(デフォルトは登録する)
            val saveButton = findViewById<Button>(R.id.saveBtn)
            saveButton.text = getString(R.string.EditSaveBtnOfEditView)
        } else {
            // 削除ボタンを非表示にする
            recordDeleteBtn.visibility = View.INVISIBLE
            actionBar?.title = getString(R.string.toolbarTitleOfBookEditView_Register) // 本を登録する
        }

        // ステータス変更ボタンが押された時の処理
        radioGroup.setOnCheckedChangeListener{
                _, checkedId ->
            if (findViewById<RadioButton>(checkedId).text == getString(R.string.BookStatusNameOfEditView_HaveRead)){
                // 読んだ本
                status = 0L
            }
            if (findViewById<RadioButton>(checkedId).text == getString(R.string.BookStatusNameOfEditView_Reading)){
                // 読んでいる本
                status = 1L
            }
            if (findViewById<RadioButton>(checkedId).text == getString(R.string.BookStatusNameOfEditView_WillRead)){
                // 読みたい本
                status = 2L
            }
        }

        // 登録・修正ボタンが押された時の処理
        saveBtn.setOnClickListener {
            // 遷移前の画面から渡される値
            // Bookテーブルの一意キーであるidカラムの値を取得する
            // 渡されなかった場合は、-1Lで渡されていないことを表す
            val bookId = intent?.getLongExtra("book_id", -1L)
            
            // タイトルが入力されていない場合アラートを表示する(タイトルは必須項目)
            if (titleField.length() == 0) {
                alert(getString(R.string.AlertOfEditView_ClickRegisterBtnWithoutFillingTitle)) { // タイトルを入力してください
                    yesButton {  }
                }.show()
            }else{
                when (bookId) {
                    -1L -> {
                        // 新規登録処理
                        // → Bookテーブルに新たにレコードを作成する
                        realm.executeTransaction {
                            val maxId = realm.where<Book>().max("id")
                            val nextId = (maxId?.toLong() ?: 0L) + 1
                            val book = realm.createObject<Book>(nextId)
                            book.title = titleField.text.toString()
                            book.author = authorField.text.toString()
                            book.detail = detailField.text.toString()
                            addedDate.text.toString().toDate("yyyy/MM/dd")?.let {
                                book.date = it
                            }
                            book.status = status!!
                            book.publisher = publisherField.text.toString()
                            book.pageCount = numberOfPages
                            if (apiURL.text.toString().isNotEmpty()){
                                book.picture = apiURL.text.toString()
                            }
                            book.outline = outlinetext.text.toString()
                            if (isbn.toLongOrNull() != null){
                                book.isbn = isbn.toLong()
                            }
                        }
                        finish()
                    }
                    else -> {
                        // 修正処理
                        // Bookテーブルのレコードを修正する
                        realm.executeTransaction {
                            val book = realm.where<Book>().equalTo("id", bookId).findFirst()
                            book?.title = titleField.text.toString()
                            book?.author = authorField.text.toString()
                            book?.detail = detailField.text.toString()
                            book?.status = status!!
                            book?.publisher = publisherField.text.toString()
                        }
                        alert(getString(R.string.AlertOfEditView_EditRecordComplete)) { // 修正しました
                            yesButton {  }
                        }.show()
                    }
                }
            }
        }

        // 削除ボタンが押された時の処理
        // Bookテーブルからid=bookIdのレコードを削除する
        recordDeleteBtn.setOnClickListener {
            alert(getString(R.string.AlertOfEditView_ConfirmRecordDeleting)) { // 削除してもよろしいですか？
                yesButton {
                    realm.executeTransaction {
                        realm.where<Book>().equalTo("id", recordId)?.findFirst()?.deleteFromRealm()
                    }
                    finish()
                }
                noButton { }
            }.show()
        }
    }

    // 戻るボタンの挙動を設定
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    // ツールボタンの戻るボタンがタップされた時の挙動を制御する
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                if (recordId != -1L) {
                    // テーブルに登録されていた書籍の情報を閲覧していた場合の処理
                    val book = realm.where<Book>().equalTo("id", recordId).findFirst() ?: return false

                    val title = titleField?.text.toString()
                    val authors = authorField?.text.toString()
                    val publisher = publisherField?.text.toString()
                    val detail = detailField?.text.toString()

                    // 変更点がなかったら終了
                    if (title == book.title && authors == book.author && detail == book.detail && status == book.status && publisher == book.publisher) {
                        return false
                    }

                    // 確認メッセージを表示
                    alert(getString(R.string.AlertOfEditView_BackWithoutSaveAfterEdit)) { // 修正は保存されませんがよろしいですか？
                        yesButton {
                            finish()
                        }
                        noButton {}
                    }.show()
                } else {
                    // 新規に編集した書籍の場合の処理
                    // 編集した箇所がなかったら終了
                    if (titleField.length() == 0 && authorField.length() == 0 && detailField.length() == 0) {
                        return false
                    }

                    // 確認メッセージを表示
                    alert(getString(R.string.AlertOfEditView_BackWithoutSaveAfterAdd)) { // 入力を破棄してよろしいですか？
                        yesButton {
                            finish()
                        }
                        noButton {}
                    }.show()
                }
            }
        }
        return true
    }

    // OSの戻るボタンが押された時の挙動を制御する
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (recordId != -1L) {
                val book = realm.where<Book>().equalTo("id", recordId).findFirst()
                val titleState = titleField?.text.toString()
                val authorState = authorField?.text.toString()
                val publisherState = publisherField?.text.toString()
                val detailState = detailField?.text.toString()
                if (titleState == book?.title && authorState == book.author && detailState == book.detail && status == book.status && publisherState == book.publisher) {
                    finish()
                } else {
                    alert(getString(R.string.AlertOfEditView_BackWithoutSaveAfterEdit)) { // 修正は保存されませんがよろしいですか？
                        yesButton {
                            finish()
                        }
                        noButton {}
                    }.show()
                }
            } else {
                if (titleField.length() == 0 && authorField.length() == 0 && detailField.length() == 0){
                    finish()
                } else {
                    alert(getString(R.string.AlertOfEditView_BackWithoutSaveAfterAdd)) { // 入力を破棄してよろしいですか？
                        yesButton {
                            finish()
                        }
                        noButton {}
                    }.show()
                }
            }
            return false
        } else {
            return false
        }
    }

    // 検索バーを追加する処理
    // 新規登録の時のみ表示する
    // → bookIdが-1Lの時のみ表示する
    @SuppressLint("ResourceAsColor")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (recordId == -1L) {
            menuInflater.inflate(R.menu.search, menu)
            searchView = menu?.findItem(R.id.searchVW)?.actionView as SearchView
            searchView.setOnQueryTextListener(this)
            searchView.isSubmitButtonEnabled = true
            searchView.setIconifiedByDefault(false)
            searchView.setBackgroundColor(R.color.colorAccent)
            searchView.queryHint = getString(R.string.searchHint)
        }
        return super.onCreateOptionsMenu(menu)
    }

    // 検索バーの文字数が変化した時の処理
    // 文字数が0になったら検索結果一覧スペースを非表示にする
    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText?.length == 0) {
            searchedBooksList.visibility = View.INVISIBLE
            if (recordId == -1L) {
                recordDeleteBtn.visibility = View.INVISIBLE
            }
            saveBtn.visibility = View.VISIBLE
        }
        return false
    }

    // 検索実行ボタンが押された時の処理
    override fun onQueryTextSubmit(query: String?): Boolean {
        // queryがnullの場合処理を終了
        if (query==null) return false

        val searchingString: String
        val connMgr = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo // OSによってネットワークアクセスが許可されているかを取得する

        // ネットワークへ接続できない場合の処理
        // メッセージを表示して、処理を終了する
        if ( networkInfo == null || !networkInfo.isConnected ) {
            alert("ネットワーク接続を確認してください") {
                yesButton {  }
            }.show()
            return false
        }

        // 検索文字数が1文字の時、検索することはできない
        if (query.length ==1){
            searchedBooksList.visibility = View.INVISIBLE
            if (recordId == -1L) {
                recordDeleteBtn.visibility = View.INVISIBLE
            }
            alert(getString(R.string.AlertOfEditView_ClickedSearchBtnOneWordOrLess)) { // 2文字以上入力してください
                yesButton {  }
            }.show()
            return false
        }

        // 検索文字列が0文字の時、検索することはできない
        if (query.isEmpty()) {
            searchedBooksList.visibility = View.INVISIBLE
            if (recordId == -1L) {
                recordDeleteBtn.visibility = View.INVISIBLE
            }
            return false
        }

        // 検索文字列が10or13の時、isbnの可能性があるため処理を分割する
        if (query.length == 10 || query.length == 13) {
            val isbn = query.toLongOrNull()
            if (isbn == null) {
                // queryをLong型に変換できた場合、queryがisbnであると判断する
                // → isbnで検索を実行する
                recordDeleteBtn.visibility = View.INVISIBLE
                searchedBooksList.visibility = View.VISIBLE
                searchingString = query

                val client = OkHttpClient()

                val request: Request = Request.Builder().let {
                    it.url(getString(R.string.ReadMemoApiServer_Domain)+"/book-info-getter/title?title=${searchingString}&page=1")
                    it.get()
                    it.build()
                }
                client.newCall(request).enqueue(CallbackAdapterSearchingByBookTitle())
            } else {
                // queryがLong型に変換できなかった場合、queryが書籍名であると判断する
                // → 書籍名で検索を実行する
                recordDeleteBtn.visibility = View.INVISIBLE
                searchedBooksList.visibility = View.VISIBLE
                searchingString = query
                val client = OkHttpClient()
                val request: Request = Request.Builder().let {
                    it.url(getString(R.string.ReadMemoApiServer_Domain)+"/book-info-getter/isbn?isbn=${query}")
                    it.get()
                    it.build()
                }
                client.newCall(request).enqueue(CallbackAdapterSearchingByISBN())
            }
        } else {
            // 検索文字列が10or13以外の時、queryが書籍名であると判断する
            // → 書籍名で検索を実行する
            recordDeleteBtn.visibility = View.INVISIBLE
            searchedBooksList.visibility = View.VISIBLE
            searchingString = query

            val client = OkHttpClient()

            val request: Request = Request.Builder().let {
                it.url(getString(R.string.ReadMemoApiServer_Domain)+"/book-info-getter/title?title=${searchingString}&page=1")
                it.get()
                it.build()
            }
            client.newCall(request).enqueue(CallbackAdapterSearchingByBookTitle())
        }

        return false
    }

    // ラジオボタンにチェックを入れる
    private fun checkRadioBenOfStatus(viewId: Int) {
        (findViewById<RadioButton>(viewId) as Checkable).isChecked = true
    }

    // 文字列を日付型に変更する関数
    @SuppressLint("SimpleDateFormat")
    private fun String.toDate(pattern: String = "yyyy/MM/dd HH:mm"): Date? {
        val sdFormat = try {
            SimpleDateFormat(pattern)
        } catch (e: IllegalArgumentException) {
            null
        }
        return sdFormat?.let {
            try {
                it.parse(this)
            }catch (e: ParseException) {
                null
            }
        }
    }

    // ISBNを検索の対象とした場合の処理
    inner class CallbackAdapterSearchingByISBN : Callback {
        override fun onResponse(call: Call, response: Response) {
            val responseText: String? = response.body()?.string()
            val parentJsonObj = JSONObject(responseText)

            // 取得したデータが0件だった場合、エラーメッセージを出力して処理を終了する
            if (parentJsonObj.getInt("count")==0){
                handler.post(GetNullBookInSearchingByISBN())
                return
            }

            var authors = ""
            var picturePath = ""
            var title = ""
            var publisher = ""
            var outlines = ""
            var affiliateUrl = ""
            var isbn = ""
            try {
                val parentJsonArray = parentJsonObj.getJSONArray("Items")
                Log.d("Success", "APIから取得したデータの件数:${parentJsonArray.length()}")
                val detailJsonObj = parentJsonArray.getJSONObject(0)
                val volumeInfo = detailJsonObj.getJSONObject("Item")
                Log.d("Response Item Title", "${1}番目のデータタイトル${volumeInfo.getString("title")}")
                if (volumeInfo.has("author")) {
                    authors = volumeInfo.getString("author")
                }
                if (volumeInfo.has("largeImageUrl")) {
                    picturePath = volumeInfo.getString("largeImageUrl")
                }
                if (volumeInfo.has("title")) {
                    title = volumeInfo.getString("title")
                }
                if (volumeInfo.has("publisherName")) {
                    publisher = volumeInfo.getString("publisherName")
                }
                if (volumeInfo.has("itemCaption")) {
                    outlines = volumeInfo.getString("itemCaption")
                }
                if (volumeInfo.has("affiliateUrl")) {
                    affiliateUrl = volumeInfo.getString("affiliateUrl")
                }
                if (volumeInfo.has("isbn")) {
                    isbn = volumeInfo.getString("isbn")
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            handler.post(CallbackSetterSearchingByISBN(title,authors,picturePath,publisher,outlines,affiliateUrl,isbn))
        }

        override fun onFailure(call: Call, e: IOException) {
            handler.post(FailureAPIResponse())
        }
    }
    
    // ISBNで検索した結果をViewに反映させる処理
    inner class CallbackSetterSearchingByISBN(
        val title: String, val author: String,
        private val pictureURL: String, private val publisher: String,
        private val outline: String, affiliateUrl:String, private val isbn: String
    ) : Runnable{
        private val affiliate = affiliateUrl
        
        override fun run() {
            titleField.setText(title)
            authorField.setText(author)
            apiURL.text = pictureURL
            outlinetext.text = outline
            publisherField.setText(publisher)
            this@BookEditActivity.isbn = isbn
            affiliateURL = affiliate
            if (pictureURL != ""){
                val request: Request = Request.Builder().let {
                    it.url(pictureURL)
                    it.get()
                    it.build()
                }
                val client = OkHttpClient()
                client.newCall(request).enqueue(CallbackAdapterDownloadAndUpdateImageView())
            }

            // isbnが存在する場合、GoogleBooksAPIを用いてページ数を取得することができる
            // → GoogleBooksAPIと通信して、ページ数を取得する
            if (isbn !=""){
                val client = OkHttpClient()
                val request: Request = Request.Builder().let {
                    it.url("https://www.googleapis.com/books/v1/volumes?q=isbn:${isbn}")
                    it.get()
                    it.build()
                }
                client.newCall(request).enqueue(CallbackAdapterGettingPageInfoFromGoogleBooksAPI())
            }
            searchedBooksList.visibility = View.INVISIBLE
        }
    }

    // 書籍名を検索の対象とした場合の処理
    inner class CallbackAdapterSearchingByBookTitle : Callback {
        override fun onResponse(call: Call, response: Response) {
            val responseText: String? = response.body()?.string()
            val parentJsonObj = JSONObject(responseText)
            if (parentJsonObj.has("Items")) { // Itemsを持っていれば通常の処理
                val parentJsonArray = parentJsonObj.getJSONArray("Items")
                Log.d("Success", "APIから取得したデータの件数:${parentJsonArray.length()}")
                if (parentJsonArray.length() == 0) {
                    handler.post(GetNullBookInSearchingByBookTitle())
                } else {
                    val titleArray = Array(parentJsonArray.length()) { "" }
                    val authorArray = Array(parentJsonArray.length()) { "" }
                    val pictureUrlArray = Array(parentJsonArray.length()) { "" }
                    val publisherArray = Array(parentJsonArray.length()) { "" }
                    val outlineArray = Array(parentJsonArray.length()) { "" }
                    val affiliateArray = Array(parentJsonArray.length()) { "" }
                    val isbnArray = Array(parentJsonArray.length()) { "" }
                    try {
                        Log.d("Success", "APIから取得したデータの件数:${parentJsonArray.length()}")
                        var i = 0
                        while (i < parentJsonArray.length()) {
                            val detailJsonObj = parentJsonArray.getJSONObject(i)
                            val volumeInfo = detailJsonObj.getJSONObject("Item")
                            Log.d("Response Item Title", "${i + 1}番目のデータタイトル${volumeInfo.getString("title")}")
                            if (volumeInfo.has("author")) {
                                authorArray[i] = volumeInfo.getString("author")
                            } else {
                                authorArray[i] = ""
                            }
                            if (volumeInfo.has("largeImageUrl")) {
                                pictureUrlArray[i] = volumeInfo.getString("largeImageUrl")
                            } else {
                                pictureUrlArray[i] = ""
                            }
                            if (volumeInfo.has("title")) {
                                titleArray[i] = volumeInfo.getString("title")
                            }
                            if (volumeInfo.has("publisherName")) {
                                publisherArray[i] = volumeInfo.getString("publisherName")
                            } else {
                                publisherArray[i] = ""
                            }
                            if (volumeInfo.has("itemCaption")) {
                                outlineArray[i] = volumeInfo.getString("itemCaption")
                            } else {
                                outlineArray[i] = ""
                            }
                            if (volumeInfo.has("affiliateUrl")) {
                                affiliateArray[i] = volumeInfo.getString("affiliateUrl")
                            } else {
                                affiliateArray[i] = ""
                            }
                            if (volumeInfo.has("isbn")) {
                                isbnArray[i] = volumeInfo.getString("isbn")
                            } else {
                                isbnArray[i] = ""
                            }
                            i += 1
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    handler.post(CallbackSetterSearchingByBookTitle(titleArray, authorArray, pictureUrlArray, publisherArray, outlineArray, affiliateArray, isbnArray))
                }
            } else {
                handler.post(ApiDisableInSearchingByBookTitle())
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            Log.v("failure API Response",e.localizedMessage)
            handler.post(FailureAPIResponse())
        }
    }

    // 書籍名で検索した結果を検索結果一覧に反映させる処理
    inner class CallbackSetterSearchingByBookTitle(
        private val titleArray: Array<String>,
        private val authorArray: Array<String>,
        private val pictureUrlArray: Array<String>,
        private val publisherArray: Array<String>,
        private val outlineArray: Array<String>,
        private val affiliateArray: Array<String>,
        private val isbnArray: Array<String>
    ) : Runnable{
        override fun run() {
            saveBtn.visibility = View.INVISIBLE
            searchedBooksList.adapter = BooksListAdapterOfEditView(this@BookEditActivity,titleArray,authorArray,pictureUrlArray)

            // 書籍一覧の要素がクリックされた時の処理を追加
            searchedBooksList.setOnItemClickListener { _, _, _, id ->
                titleField.setText(titleArray[id.toInt()])
                authorField.setText(authorArray[id.toInt()])
                publisherField.setText(publisherArray[id.toInt()])
                outlinetext.text = outlineArray[id.toInt()]
                isbn = isbnArray[id.toInt()]
                affiliateURL = affiliateArray[id.toInt()]
                if (pictureUrlArray[id.toInt()] == ""){
                    searchedBooksList.visibility = View.INVISIBLE
                } else {
                    val url = pictureUrlArray[id.toInt()]
                    apiURL.text = url
                    val request: Request = Request.Builder().let {
                        it.url(url)
                        it.get()
                        it.build()
                    }
                    val client = OkHttpClient()
                    client.newCall(request).enqueue(CallbackAdapterDownloadAndUpdateImageView())
                    searchedBooksList.visibility = View.INVISIBLE
                }

                // isbnが存在する場合、GoogleBooksAPIを用いてページ数を取得することができる
                // → GoogleBooksAPIと通信して、ページ数を取得する
                if (isbn !=""){
                    val client = OkHttpClient()
                    val request: Request = Request.Builder().let {
                        it.url("https://www.googleapis.com/books/v1/volumes?q=isbn:${isbn}")
                        it.get()
                        it.build()
                    }
                    client.newCall(request).enqueue(CallbackAdapterGettingPageInfoFromGoogleBooksAPI())
                }
            }
            saveBtn.visibility = View.VISIBLE
        }
    }

    // 各書籍の表紙を検索結果一覧に表示する処理
    inner class CallbackAdapterDownloadAndUpdateImageView: Callback{
        override fun onResponse(call: Call, response: Response) {
            if (response.body()?.byteStream() != null){
                if (response.isSuccessful) {
                    val bitmap: Bitmap = BitmapFactory.decodeStream(response.body()!!.byteStream())
                    Handler(Looper.getMainLooper()).post {
                        bookImage.setImageBitmap(bitmap)
                    }
                }
            }
        }
        override fun onFailure(call: Call, e: IOException) {

        }
    }
    
    // GoogleBooksAPIと通信してページ数を取得する
    inner class CallbackAdapterGettingPageInfoFromGoogleBooksAPI:Callback{
        override fun onResponse(call: Call, response: Response) {
            val numberOfPages: Int
            try {
                val responseText: String? = response.body()?.string()
                val parentJsonObj = JSONObject(responseText)
                val parentJsonArray = parentJsonObj.getJSONArray("items")
                Log.d("Success API Response","APIから取得したデータの件数:${parentJsonArray.length()}")
                val detailJsonObj = parentJsonArray.getJSONObject(0)
                val volumeInfo = detailJsonObj.getJSONObject("volumeInfo")
                Log.d("Response Item Title","${0}番目のデータタイトル${volumeInfo.getString("title")}")
                numberOfPages = if (volumeInfo.has("pageCount")){
                    volumeInfo.getInt("pageCount")
                } else {
                    0
                }
                this@BookEditActivity.numberOfPages = numberOfPages.toLong()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        override fun onFailure(call: Call, e: IOException) {
            Log.v("failure G_API Response",e.localizedMessage)
        }
    }

    // APIとの通信でエラーが発生した場合の処理
    inner class FailureAPIResponse:Runnable{
        override fun run() {
            alert(getString(R.string.AlertOfEditView_FailureAPIResponse)) { // エラーが発生しました
                finish()
            }.show()
        }
    }

    // ISBNによる検索実行時に書籍情報を取得できなかった場合の処理
    inner class GetNullBookInSearchingByISBN:Runnable{
        override fun run() {
            alert("該当する本が見つかりませんでした\nISBNが間違っていないか確認してください") {
                yesButton {  }
            }.show()
        }
    }

    // 楽天APIと通信はできたが、リクエスト上限などの関係でエラーが返された場合の処理
    inner class ApiDisableInSearchingByBookTitle:Runnable{
        override fun run() {
            alert("リクエストが混み合っております"+"\n"+"しばらくお待ちください") {
                yesButton {  }
            }.show()
            searchedBooksList.visibility = View.INVISIBLE
            if (recordId == -1L) {
                recordDeleteBtn.visibility = View.INVISIBLE
            }
        }
    }

    // 書籍タイトルによる検索実行時に書籍情報を取得できなかった場合の処理
    inner class GetNullBookInSearchingByBookTitle:Runnable{
        override fun run() {
            alert("検索結果が存在しませんでした") {
                yesButton { }
            }.show()
            searchedBooksList.visibility = View.INVISIBLE
            if (recordId == -1L) {
                recordDeleteBtn.visibility = View.INVISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }
}