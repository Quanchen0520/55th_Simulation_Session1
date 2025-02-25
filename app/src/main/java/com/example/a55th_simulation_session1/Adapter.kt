package com.example.a55th_simulation_session1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class Adapter(
    private val songList: List<String>,
    private val imageList: List<Int>,
    private val urllist: List<String>,  // 加入對應的音樂 URL 列表
    private val onDownloadClick: (Int) -> Unit,
    private val onPlayClick: (String) -> Unit // 讓 Activity 處理播放邏輯
) : RecyclerView.Adapter<Adapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val songName: TextView = itemView.findViewById(R.id.SongName)
        private val songImage: ImageView = itemView.findViewById(R.id.imageView9)
        private val downloadButton: ImageButton = itemView.findViewById(R.id.imageBtn)
        private val playBtn: ImageButton = itemView.findViewById(R.id.playBtn)

        fun bind(song: String, imageRes: Int, position: Int) {
            songName.text = song
            songImage.setImageResource(imageRes)
            downloadButton.setOnClickListener { onDownloadClick(position) }
            playBtn.setOnClickListener { onPlayClick(urllist[position]) } // 播放對應的音樂
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = UserViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.home_list_style, parent, false)
    )

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(songList[position], imageList[position], position)
    }

    override fun getItemCount() = songList.size
}