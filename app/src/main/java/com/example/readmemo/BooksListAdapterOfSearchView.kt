package com.example.readmemo

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso

class BooksListAdapterOfSearchView(context: Activity, private val title:MutableList<String>, private val author:MutableList<String>, private val picture:MutableList<String>)
    :ArrayAdapter<String>(context,R.layout.ranking_listview,title) {
    inner class ViewHolder(cell: View) {
        val title: TextView = cell.findViewById(android.R.id.text1)
        val author: TextView = cell.findViewById(android.R.id.text2)
        val picture: ImageView = cell.findViewById(R.id.thumbnail)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder
        when (convertView) {
            null -> {
                val inflater = LayoutInflater.from(parent.context)
                view = inflater.inflate(R.layout.ranking_listview, parent, false)
                viewHolder = ViewHolder(view)
                view.tag = viewHolder
            }
            else -> {
                view = convertView
                viewHolder = view.tag as ViewHolder
            }
        }
        viewHolder.title.text = title[position]
        viewHolder.author.text = author[position]
        if (picture[position].isNotEmpty()) {
            Picasso.get().load(picture[position]).into(viewHolder.picture)
        } else {
            viewHolder.picture.setImageResource(R.drawable.ic_insert_photo_black_24dp)
        }
        return view
    }
}