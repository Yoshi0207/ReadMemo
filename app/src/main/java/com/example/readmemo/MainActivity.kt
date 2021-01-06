package com.example.readmemo

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.startActivity

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener{
    private lateinit var realm: Realm
    private var datetime : Long = 0L // 書籍リストを日付で 0L:降順, 1L:昇順
    private var status : Long = 0L // リストに表示する書籍のステータス 0L:全て, 1L:読了, 2L:読書中, 3L:読みたい

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // 書籍追加ボタン
        // クリックすると書籍追加画面に遷移する
        bookRegisterBtn.setOnClickListener {
            startActivity<BookEditActivity>()
        }

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout_of_main_view, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout_of_main_view.addDrawerListener(toggle)
        toggle.syncState()

        // ナビゲーションビュー(横からスワイプするメニュー)
        // 表示する書籍の絞り込みや、検索画面への遷移
        toggleMenuOfMainView.setNavigationItemSelectedListener(this)

        // 読書本のリスト
        // アイテムをクリックすると編集画面に遷移する
        registeredBooksList.setOnItemClickListener { parent, _, position, _ ->
            val book = parent.getItemAtPosition(position) as Book
            startActivity<BookEditActivity>("book_id" to book.id)
        }

        // PreferenceManagerに保存されている値を取り出し、状態に反映する
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        pref.apply {
            datetime = getLong("LINE",0)
            status = getLong("STATUSES",0)
        }

        // 読書本の情報をデータベースから取得し、リストに反映する
        getRegisteredBooksRecordAndUpdateBooksList()
    }

    // OSの戻るボタンが押された時の処理
    override fun onBackPressed() {
        if (drawer_layout_of_main_view.isDrawerOpen(GravityCompat.START)) {
            drawer_layout_of_main_view.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // オプションメニュー(右上のボタン)を表示する
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    // オプションメニュー内の項目がクリックされた時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.datetimeDESC -> {
                datetime = 0L
                getRegisteredBooksRecordAndUpdateBooksList()
                true
            }
            R.id.datetimeASC -> {
                datetime = 1L
                getRegisteredBooksRecordAndUpdateBooksList()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ナビゲーションメニューがクリックされた時の処理
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.showAllBooks -> {
                // 全ての本をリストに表示する
                status = 0L
                changeDisplayedStringOfToolbar()
                getRegisteredBooksRecordAndUpdateBooksList()
            }
            R.id.showHaveReadBooks -> {
                // 読了した本をリストに表示する
                status = 1L
                changeDisplayedStringOfToolbar()
                getRegisteredBooksRecordAndUpdateBooksList()
            }
            R.id.showReadingBoosk -> {
                // 読書中の本をリストに表示する
                status = 2L
                changeDisplayedStringOfToolbar()
                getRegisteredBooksRecordAndUpdateBooksList()
            }
            R.id.showWillReadBooks -> {
                // 読みたい本をリストに表示する
                status = 3L
                changeDisplayedStringOfToolbar()
                getRegisteredBooksRecordAndUpdateBooksList()
            }
            R.id.changeViewToReadingRecord -> {
                // 読書記録を表示する
                startActivity<BookHistoryActivity>()
            }
            R.id.changeViewToSearchingBooks -> {
                // 本検索画面を表示する
                startActivity<BookFilterActivity>()
            }
        }

        drawer_layout_of_main_view.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onResume() {
        super.onResume()
        changeDisplayedStringOfToolbar()
    }

    override fun onPause() {
        super.onPause()
        // PreferenceManagerに現在の状態を保存する
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = pref.edit()
        editor.putLong("LINE", datetime).putLong("STATUSES", status).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    // データベースから保存されている書籍の情報を取得し、リストに反映する
    private fun getRegisteredBooksRecordAndUpdateBooksList() {
        when (status){
            0L -> {
                if (datetime == 0L) {
                    realm = Realm.getDefaultInstance()
                    val books = realm.where<Book>().findAll().sort("date", Sort.DESCENDING)
                    registeredBooksList.adapter = BooksListAdapterOfMainView(books)
                }else{
                    realm = Realm.getDefaultInstance()
                    val books = realm.where<Book>().findAll().sort("date")
                    registeredBooksList.adapter = BooksListAdapterOfMainView(books)
                }
            }
            1L -> {
                if (datetime == 0L) {
                    realm = Realm.getDefaultInstance()
                    val books = realm.where<Book>().equalTo("status",0L).findAll().sort("date", Sort.DESCENDING)
                    registeredBooksList.adapter = BooksListAdapterOfMainView(books)
                }else{
                    realm = Realm.getDefaultInstance()
                    val books = realm.where<Book>().equalTo("status",0L).findAll().sort("date")
                    registeredBooksList.adapter = BooksListAdapterOfMainView(books)
                }
            }
            2L -> {
                if (datetime == 0L) {
                    realm = Realm.getDefaultInstance()
                    val books = realm.where<Book>().equalTo("status",1L).findAll().sort("date", Sort.DESCENDING)
                    registeredBooksList.adapter = BooksListAdapterOfMainView(books)
                }else{
                    realm = Realm.getDefaultInstance()
                    val books = realm.where<Book>().equalTo("status",1L).findAll().sort("date")
                    registeredBooksList.adapter = BooksListAdapterOfMainView(books)
                }
            }
            3L -> {
                if (datetime == 0L) {
                    realm = Realm.getDefaultInstance()
                    val books = realm.where<Book>().equalTo("status",2L).findAll().sort("date", Sort.DESCENDING)
                    registeredBooksList.adapter = BooksListAdapterOfMainView(books)
                }else{
                    realm = Realm.getDefaultInstance()
                    val books = realm.where<Book>().equalTo("status",2L).findAll().sort("date")
                    registeredBooksList.adapter = BooksListAdapterOfMainView(books)
                }
            }
        }
    }

    // 表示する本に応じてツールバーに表示する文字を変更する
    private fun changeDisplayedStringOfToolbar(){
        when(status){
            0L->{
                // 全ての本
                toolbar.title = getString(R.string.toggleMenuOfMainView_AllBooks)
            }
            1L->{
                // 読んだ本
                toolbar.title = getString(R.string.toggleMenuOfMainView_HaveReadBooks)
            }
            2L->{
                // 読んでいる本
                toolbar.title = getString(R.string.toggleMenuOfMainView_ReadingBooks)
            }
            3L->{
                // 読みたい本
                toolbar.title = getString(R.string.toggleMenuOfMainView_WillReadBooks)
            }
        }
    }
}
