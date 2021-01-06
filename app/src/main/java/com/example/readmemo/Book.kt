package com.example.readmemo

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class Book : RealmObject() {
    @PrimaryKey
    var id: Long = 0 // id
    var status : Long = 0 // 読了しているか否か
    var date: Date = Date() // 読んだ日付
    var picture : String = "" // Google booksの写真URLを保存
    var title: String = "" // 本のタイトル
    var author: String = "" // 本の著者
    var detail: String = "" // 感想
    var publisher: String = "" // 出版社
    var outline : String = "" // あらすじ
    var date_published : Date = Date() // 出版日
    var pageCount : Long = 0L
    var isbn : Long = 0L
    var option : String = "" // 予備
    var extra : String = "" // 予備
    var extra2: Int = 0 // 予備
    var extra3 : Long = 0 //予備
}