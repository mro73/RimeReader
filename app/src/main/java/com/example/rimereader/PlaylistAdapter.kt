package com.example.rimereader

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlaylistAdapter(
    private val playlist: List<AudioFile>,
    private val currentIndex: Int,
    private val formatTime: (Int) -> String,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.SongViewHolder>() {

    inner class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(R.id.textSongName)
        val textDuration: TextView = view.findViewById(R.id.textSongDuration)
        val icon: ImageView = view.findViewById(R.id.iconNote)

        init {
            view.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(adapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val audioFile = playlist[position]
        holder.textName.text = audioFile.title
        holder.textDuration.text = formatTime(audioFile.duration)

        if (position == currentIndex) {
            holder.textName.setTypeface(null, Typeface.BOLD)
            holder.icon.setColorFilter(Color.BLUE)
        } else {
            holder.textName.setTypeface(null, Typeface.NORMAL)
            holder.icon.clearColorFilter()
        }
    }

    override fun getItemCount(): Int = playlist.size
}