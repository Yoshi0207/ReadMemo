package com.example.readmemo

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.squareup.picasso.Picasso
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_add_book.*
import okhttp3.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.yesButton
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.IllegalArgumentException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class BookDetailActivity : AppCompatActivity() {
    private lateinit var realm: Realm
    var numberOfPages = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_add_book)

        realm = Realm.getDefaultInstance()

        // 本の検索画面から送られてきたデータをパースする
        val title = intent.getStringExtra("title")
        val author = intent.getStringExtra("author")
        val pictureUrl = intent.getStringExtra("pictureURL")
        val publisher = intent.getStringExtra("publisher")
        val outline = intent.getStringExtra("outline")
        val affiliateURL = intent.getStringExtra("affiliateURL")
        val isbn = intent.getStringExtra("isbn")
        val date = ""

        // actionbarの設定
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.title = getString(R.string.toggleMenuOfMainView_SearchingBooks) // 本を探す

        // 各フィールドに送られてきた値をセットする
        bookTitleOfDetailView.text = title // タイトル
        bookAuthorOfDetailView.text = author // 著者
        bookPublisherOfDetailView.text = publisher // 出版社
        bookAbstractOfDetailView.text = outline // 概要

        // 送られてきた情報に表紙画像へのパスが存在したらフィールドに反映する
        if (pictureUrl?.length != 0){
            Picasso.get().load(pictureUrl).into(bookImageOfDetailView)
        }else{
            bookImageOfDetailView.setImageResource(R.drawable.ic_insert_photo_black_24dp)
        }

        // 送られてきた情報の中にisbnの情報が含まれていたら、GoogleBooksAPIからisbnに対応するページ数を取得する
        if (isbn !=""){
            val client = OkHttpClient()
            val request: Request = Request.Builder().let {
                it.url("https://www.googleapis.com/books/v1/volumes?q=isbn:${isbn}")
                it.get()
                it.build()
            }
            client.newCall(request).enqueue(GetPageCount())
        }

        // 著者で検索するボタンがクリックされた時の処理
        btnSearchingByAuthor.setOnClickListener {
            if (author==""){
                alert(getString(R.string.AlertMessage_AuthorDoesNotExist)) { // 著者が存在しません
                    yesButton {  }
                }.show()
                return@setOnClickListener
            }

            // 検索画面へ遷移
            // 著者で検索を指定
            val intent=Intent(this,BookSearchActivity::class.java)
            intent.putExtra("string",author)
            intent.putExtra("id","2")
            startActivity(intent)
        }

        // 出版社で検索するボタンがクリックされた時の処理
        btnSearchingByPublisher.setOnClickListener {
            if (publisher==""){
                alert(getString(R.string.AlertMessage_PublisherDoesNotExits)) { // 出版社が存在しません
                    yesButton {  }
                }.show()
                return@setOnClickListener
            }
            // 検索画面へ遷移
            // 出版社で検索を指定
            val intent=Intent(this,BookSearchActivity::class.java)
            intent.putExtra("string",publisher)
            intent.putExtra("id","4")
            startActivity(intent)
        }

        // 読みたい本に追加するボタンがクリックされた時の処理
        btnAddingBookWillRead.setOnClickListener {
            // Bookテーブルに、「ステータス:読みたい本」として書籍を登録する
            realm.executeTransaction {
                val maxId = realm.where<Book>().max("id")
                val nextId = (maxId?.toLong() ?: 0L) + 1
                val book = realm.createObject<Book>(nextId)
                book.title = title
                book.author = author
                book.detail = ""
                date.toDate("yyyy/MM/dd")?.let {
                    book.date = it
                }
                book.status = 2L
                book.publisher = publisher
                if (pictureUrl.isNotEmpty()){
                    book.picture = pictureUrl
                }
                book.outline = outline
                book.isbn = isbn.toLong()
                book.pageCount = numberOfPages
            }
            alert(getString(R.string.AlertMessage_AddRecordComplete)) { // 登録しました
                yesButton {  }
            }.show()
        }

        // 楽天ブックスで閲覧するボタンがクリックされた時の処理
        // アフィリエイトリンクを開く
        btnOpeningRakutenBooks.setOnClickListener {
            val url: String = affiliateURL
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            if (intent.resolveActivity(packageManager) != null){
                startActivity(intent)
            }
            return@setOnClickListener
        }
    }

    // GoogleBooksAPIと通信してisbnに対応する書籍のページ数を取得する処理
    // (楽天ブックスAPIではページ数が取得できないため)
    // 取得したページ数はインスタンス変数page_countに格納される
    inner class GetPageCount: Callback {
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
                this@BookDetailActivity.numberOfPages = numberOfPages.toLong()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        override fun onFailure(call: Call, e: IOException) {
            Log.v("failure G_API Response",e.localizedMessage)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    // 文字列を日付型へ変換する
    @SuppressLint("SimpleDateFormat")
    fun String.toDate(pattern: String = "yyyy/MM/dd HH:mm"): Date? {
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
}
