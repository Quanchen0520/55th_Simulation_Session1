package com.example.a55th_simulation_session1

import MusicItem
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class Adapter(
    private val songList: List<String>,
    private var musicList: List<MusicItem>,
    private val onDownloadClick: (Int) -> Unit
) : RecyclerView.Adapter<Adapter.UserViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingPosition: Int? = null
    private val handler = Handler(Looper.getMainLooper())
    private val seekBarUpdateRunnable = HashMap<Int, Runnable>()
    private val downloadsInProgress = HashMap<Int, Boolean>()
    private val downloadProgressMap = HashMap<Int, Int>()
    private val downloadCompleted = HashMap<Int, Boolean>()

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val songImage: ImageView = itemView.findViewById(R.id.imageView9)
        val downloadButton: ImageButton = itemView.findViewById(R.id.imageBtn)
        val playBtn: ImageButton = itemView.findViewById(R.id.playBtn)
        val loadingProgressBar: ProgressBar = itemView.findViewById(R.id.loadingProgressBar)
        val playbackSeekBar: SeekBar = itemView.findViewById(R.id.playbackSeekBar)


        fun bind(position: Int, songName: String, imageUrl: String) {
            val musicItem = musicList[position]

            itemView.findViewById<TextView>(R.id.SongName).text = songName
            songImage.load(imageUrl)

            setupInitialState(position)

            if (downloadCompleted[position] == true) {
                downloadButton.setImageResource(R.drawable.baseline_cloud_done_24)
                downloadButton.isEnabled = false
            } else {
                downloadButton.setImageResource(R.drawable.baseline_arrow_circle_down_24)
                downloadButton.isEnabled = true
            }

            if (downloadsInProgress[position] == true) {
                loadingProgressBar.visibility = View.VISIBLE
                loadingProgressBar.progress = downloadProgressMap[position] ?: 0
            } else {
                loadingProgressBar.visibility = View.GONE
            }

            playBtn.setOnClickListener {
                if (currentlyPlayingPosition == position) {
                    stopAudio()
                } else {
                    musicItem.SongURL?.let { it1 -> playAudio(it1, position, this) }
                }
            }

            downloadButton.setOnClickListener {
                if (downloadCompleted[position] != true) {
                    downloadsInProgress[position] = true
                    loadingProgressBar.visibility = View.VISIBLE
                    loadingProgressBar.progress = 0
                    onDownloadClick(position)
                }
            }

            playbackSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        mediaPlayer?.seekTo(progress * mediaPlayer!!.duration / 100)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        private fun setupInitialState(position: Int) {
            playBtn.setImageResource(
                if (currentlyPlayingPosition == position) R.drawable.baseline_stop_circle_24
                else R.drawable.baseline_play_arrow_24
            )
            playbackSeekBar.visibility = if (currentlyPlayingPosition == position) View.VISIBLE else View.GONE
            loadingProgressBar.visibility = if (downloadsInProgress[position] == true) View.VISIBLE else View.GONE
            loadingProgressBar.progress = downloadProgressMap[position] ?: 0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = UserViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.home_list_style, parent, false)
    )

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        if (position >= musicList.size) return  // Prevent out of bounds exception
        val musicItem = musicList[position]
        musicItem.imageURL?.let { musicItem.SongName?.let { it1 -> holder.bind(position, it1, it) } }
    }


    override fun getItemCount() = songList.size

    fun updateDownloadProgress(position: Int, progress: Int) {
        downloadProgressMap[position] = progress
        if (progress >= 100) {
            downloadsInProgress[position] = false
            downloadCompleted[position] = true
        }
        notifyItemChanged(position)
    }

    private fun playAudio(url: String, position: Int, holder: UserViewHolder) {
        stopAudio()
        currentlyPlayingPosition = position

        holder.playBtn.setImageResource(R.drawable.baseline_stop_circle_24)
        holder.playbackSeekBar.visibility = View.VISIBLE

        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener {
                start()
                updateSeekBar(position, holder)
            }
            setOnCompletionListener { stopAudio() }
            prepareAsync()
        }
    }

    private fun stopAudio() {
        currentlyPlayingPosition?.let { prevPosition ->
            seekBarUpdateRunnable[prevPosition]?.let { handler.removeCallbacks(it) }
            seekBarUpdateRunnable.remove(prevPosition)
            notifyItemChanged(prevPosition)
        }
        mediaPlayer?.release()
        mediaPlayer = null
        currentlyPlayingPosition = null
    }

    private fun updateSeekBar(position: Int, holder: UserViewHolder) {
        val runnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val progress = (player.currentPosition * 100) / player.duration
                        holder.playbackSeekBar.progress = progress
                        handler.postDelayed(this, 500)
                    }
                }
            }
        }
        seekBarUpdateRunnable[position] = runnable
        handler.post(runnable)
    }

    fun releaseResources() {
        stopAudio()
        handler.removeCallbacksAndMessages(null)
    }
}