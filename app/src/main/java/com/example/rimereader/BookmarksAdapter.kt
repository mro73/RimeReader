package com.example.rimereader

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BookmarksAdapter(
    private val onBookmarkClick: (Bookmark) -> Unit,
    private val onDeleteClick: (Bookmark) -> Unit
) : RecyclerView.Adapter<BookmarksAdapter.BookmarkViewHolder>() {

    private var bookmarks: List<Bookmark> = emptyList()

    fun submitList(newList: List<Bookmark>) {
        bookmarks = newList
        notifyDataSetChanged()
    }

    inner class BookmarkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textTime: TextView = view.findViewById(R.id.textBookmarkTime)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteBookmark)

        init {
            view.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) onBookmarkClick(bookmarks[adapterPosition])
            }
            btnDelete.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) onDeleteClick(bookmarks[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark, parent, false)
        return BookmarkViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        val bookmark = bookmarks[position]
        holder.textTime.text = "Czas: ${bookmark.displayTime}"
    }

    override fun getItemCount(): Int = bookmarks.size
}