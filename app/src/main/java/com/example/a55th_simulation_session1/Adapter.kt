package com.example.a55th_simulation_session1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class Adapter(
    private val songList: List<String>,
    private val onDownloadClick: (String) -> Unit
) : RecyclerView.Adapter<Adapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val songName: TextView = itemView.findViewById(R.id.SongName)
        val downloadButton: ImageButton = itemView.findViewById(R.id.imageButton)

        fun bind(song: String, position: Int, onDownloadClick: (String) -> Unit) {
            songName.text = song
            downloadButton.setOnClickListener {
                onDownloadClick(position.toString())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.home_list_style, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(songList[position],  position, onDownloadClick)
    }

    override fun getItemCount(): Int = songList.size
}