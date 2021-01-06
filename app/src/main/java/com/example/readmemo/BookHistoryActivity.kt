package com.example.readmemo

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.format.DateFormat
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_book_library.*
import org.jetbrains.anko.startActivity
import java.lang.IllegalArgumentException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class BookHistoryActivity : AppCompatActivity() {
    private lateinit var realm: Realm
    private val calendar: Calendar = Calendar.getInstance() // カレンダー機能を提供する

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_library)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // actionbarの設定
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true) // actionbarに戻るボタンを追加
        actionBar?.title = getString(R.string.toolbarTitleOfBookEditView) // actionbarのタイトルを設定

        // 現在の日付を取得
        // → yyyy/MM形式
        val date: Date = calendar.time
        val yyMM = DateFormat.format("yyyy/MM",date).toString()
        dateOfTermOfBooksBeDisplayed.text = yyMM
        changeTermAndBooksBeDisplayed(yyMM)

        // 読了した書籍の一覧の要素をクリックした時の処理
        // 書籍編集画面に遷移する
        booksList.setOnItemClickListener { parent, _, position, _ ->
            val book = parent.getItemAtPosition(position) as Book
            startActivity<BookEditActivity>("book_id" to book.id)
        }

        // 日付の左に設置されているボタン「<」をクリックした時の処理
        // 日付を1ヶ月前に送る
        leftMove.setOnClickListener {
            calendar.add(Calendar.MONTH,-1)
            val oneMonthAgoDateOfTermBeDisplayed: Date = calendar.time
            val oneMonthAgoDate = DateFormat.format("yyyy/MM",oneMonthAgoDateOfTermBeDisplayed).toString()
            dateOfTermOfBooksBeDisplayed.text = oneMonthAgoDate
            changeTermAndBooksBeDisplayed(oneMonthAgoDate)
        }

        // 日付の右に設置されているボタン「>」をクリックした時の処理
        // 日付を1ヶ月後に送る
        rightMove.setOnClickListener {
            calendar.add(Calendar.MONTH,1)
            val oneMonthLaterDateOfTermBeDisplayed: Date = calendar.time
            val oneMonthLaterDate = DateFormat.format("yyyy/MM",oneMonthLaterDateOfTermBeDisplayed).toString()
            dateOfTermOfBooksBeDisplayed.text = oneMonthLaterDate
            changeTermAndBooksBeDisplayed(oneMonthLaterDate)
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

    @SuppressLint("SetTextI18n")
    private fun changeTermAndBooksBeDisplayed(date: String) { // dateはyy/MM形式
        val month = date.split("/")[1]
        val monthEnd = if (month == "01" || month == "03" || month == "05" || month == "07" || month == "08" || month == "10" || month == "12") {
            "${date}/32" // 31にすると31日が含まれなくなるため
        } else if (month == "02") {
            "${date}/29"
        } else {
            "${date}/31"
        }
        val monthStartDate = "${date}/01".toDate("yyyy/MM/dd")!!
        val monthEndDate = monthEnd.toDate("yyyy/MM/dd")!!

        var numberOfBooks : Long = 0
        var numberOfPages : Long = 0

        // monthStart~monthEndに存在する、ステータスが読了のBookテーブルのレコードを取得する
        realm = Realm.getDefaultInstance()
        val books = realm.where<Book>().equalTo("status",0L).between("date", monthStartDate, monthEndDate).findAll()

        // 取得した情報を読了した書籍の一覧に反映
        booksList.adapter = BooksListAdapterOfMainView(books)

        // 合計冊数・ページ数の情報を更新
        val maxId = realm.where<Book>().max("id")
        var i = 0
        while (i <= maxId?.toLong() ?: 0L) {
            val book = realm.where<Book>().equalTo("id",i).between("date", monthStartDate, monthEndDate).findFirst()

            if (book==null) {
                i += 1
                continue
            }

            // レコードが読了の情報であれば
            if (book.status == 0L){
                numberOfBooks += 1
                numberOfPages += book.pageCount
            }
            i += 1
        }
        libraryCount.text = "$numberOfBooks" + getString(R.string.methodOfCountingBooks)
        libraryPage.text = "$numberOfPages" + getString(R.string.methodOfCountingPages)
    }

    // 文字列を日付型に変換するメソッド
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
}
