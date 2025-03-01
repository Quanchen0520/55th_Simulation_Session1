package com.example.a55th_simulation_session1

import MusicItem
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import coil.load

class Adapter(
    private val musicList: List<MusicItem>,
    private val onDownloadClick: (Int) -> Unit
) : RecyclerView.Adapter<Adapter.UserViewHolder>() {
    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingPosition: Int? = null
    private val handler = Handler(Looper.getMainLooper())
    private val seekBarUpdateRunnable = HashMap<Int, Runnable>()
    private val downloadsInProgress = mutableMapOf<Int, Boolean>()
    private val downloadProgressMap = mutableMapOf<Int, Int>()
    private val downloadCompleted = mutableMapOf<Int, Boolean>()

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val songImage: ImageView = itemView.findViewById(R.id.imageView9)
        val downloadButton: ImageButton = itemView.findViewById(R.id.imageBtn)
        val playBtn: ImageButton = itemView.findViewById(R.id.playBtn)
        val loadingProgressBar: ProgressBar = itemView.findViewById(R.id.loadingProgressBar)
        val playbackSeekBar: SeekBar = itemView.findViewById(R.id.playbackSeekBar)

        fun bind(position: Int, musicItem: MusicItem) {
            itemView.findViewById<TextView>(R.id.SongName).text = musicItem.SongName
            songImage.load(musicItem.imageURL)
            setupUI(position)
            setupListeners(position, musicItem.SongURL)
        }

        private fun setupUI(position: Int) {
            playBtn.setImageResource(if (currentlyPlayingPosition == position) R.drawable.baseline_stop_circle_24 else R.drawable.baseline_play_arrow_24)
            playbackSeekBar.visibility = if (currentlyPlayingPosition == position) View.VISIBLE else View.GONE
            loadingProgressBar.apply {
                visibility = if (downloadsInProgress[position] == true) View.VISIBLE else View.GONE
                progress = downloadProgressMap[position] ?: 0
            }
            downloadButton.apply {
                setImageResource(if (downloadCompleted[position] == true) R.drawable.baseline_cloud_done_24 else R.drawable.baseline_arrow_circle_down_24)
                isEnabled = downloadCompleted[position] != true
            }
        }

        private fun setupListeners(position: Int, url: String?) {
            playBtn.setOnClickListener { if (currentlyPlayingPosition == position) stopAudio() else url?.let { playAudio(it, position, this) } }
            downloadButton.setOnClickListener { if (downloadCompleted[position] != true) startDownload(position) }
            playbackSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) mediaPlayer?.seekTo(progress * mediaPlayer!!.duration / 100)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        private fun startDownload(position: Int) {
            downloadsInProgress[position] = true
            loadingProgressBar.visibility = View.VISIBLE
            loadingProgressBar.progress = 0
            onDownloadClick(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = UserViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.home_list_style, parent, false)
    )

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        musicList.getOrNull(position)?.let { holder.bind(position, it) }
    }

    override fun getItemCount() = musicList.size

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
            setOnPreparedListener { start(); updateSeekBar(position, holder) }
            setOnCompletionListener { stopAudio() }
            prepareAsync()
        }
    }

    private fun stopAudio() {
        currentlyPlayingPosition?.let {
            seekBarUpdateRunnable.remove(it)?.let { handler.removeCallbacks(it) }
            notifyItemChanged(it)
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
                        holder.playbackSeekBar.progress = (player.currentPosition * 100) / player.duration
                        handler.postDelayed(this, 500)  // 這裡的 `this` 指的是 `Runnable`，而不是 `Adapter`
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
