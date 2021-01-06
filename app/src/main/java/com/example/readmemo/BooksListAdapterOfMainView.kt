package com.example.readmemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import io.realm.OrderedRealmCollection
import io.realm.RealmBaseAdapter

class BooksListAdapterOfMainView(data: OrderedRealmCollection<Book>?) : RealmBaseAdapter<Book>(data) {
    // ListViewの各要素の型
    inner class  ViewHolder(cell: View) {
        val title: TextView = cell.findViewById(android.R.id.text1)
        val author: TextView = cell.findViewById(android.R.id.text2)
        val picture: ImageView = cell.findViewById(R.id.thumbnail)
    }

    // ListViewを更新する
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val viewHolder: ViewHolder

        when (convertView) {
            null -> {
                val inflater = LayoutInflater.from(parent?.context)
                view = inflater.inflate(R.layout.list_layout, parent, false)
                viewHolder = ViewHolder(view)
                view.tag = viewHolder
            }
            else -> {
                view = convertView
                viewHolder = view.tag as ViewHolder
            }
        }

        // 引数として渡した書籍の情報を各要素に反映する
        adapterData?.run {
            val book = get(position) // positionは要素の位置
            viewHolder.title.text = book.title
            viewHolder.author.text = book.author

            // もし表紙のURLへのパスが登録されていたら
            if (book?.picture?.length != 0){
                // 画像を読み込み、各書籍の表紙の枠に反映させる
                Picasso.get().load(book.picture).into(viewHolder.picture)
            } else {
                viewHolder.picture.setImageResource(R.drawable.ic_insert_photo_black_24dp)
            }
        }
        return view
    }
}
