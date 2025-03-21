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

/**
 * 音樂列表的 Adapter，負責處理歌曲顯示、播放、下載功能。
 * @param musicList 音樂資料列表
 * @param onDownloadClick 下載按鈕點擊事件
 */
class Adapter(
    private val musicList: List<MusicItem>,
    private val onDownloadClick: (Int) -> Unit
) : RecyclerView.Adapter<Adapter.UserViewHolder>() {
    private var mediaPlayer: MediaPlayer? = null // 媒體播放器
    private var currentlyPlayingPosition: Int? = null // 記錄當前播放的音樂索引
    private val handler = Handler(Looper.getMainLooper())
    private val seekBarUpdateRunnable = HashMap<Int, Runnable>() // 追蹤 SeekBar 更新的 Runnable
    private val downloadsInProgress = mutableMapOf<Int, Boolean>() // 下載進度狀態
    private val downloadProgressMap = mutableMapOf<Int, Int>() // 下載進度數值
    private val downloadCompleted = mutableMapOf<Int, Boolean>() // 下載完成狀態

    /**
     * RecyclerView 的 ViewHolder，負責綁定 UI 元件。
     */
    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val songImage: ImageView = itemView.findViewById(R.id.imageView9) // 音樂圖片
        private val downloadButton: ImageButton = itemView.findViewById(R.id.imageBtn) // 下載按鈕
        val playBtn: ImageButton = itemView.findViewById(R.id.playBtn) // 播放按鈕
        private val loadingProgressBar: ProgressBar = itemView.findViewById(R.id.loadingProgressBar) // 下載進度條
        val playbackSeekBar: SeekBar = itemView.findViewById(R.id.playbackSeekBar) // 播放進度條

        /**
         * 綁定數據到 UI 上
         */
        fun bind(position: Int, musicItem: MusicItem) {
            itemView.findViewById<TextView>(R.id.SongName).text = musicItem.SongName
            songImage.load(musicItem.imageURL)
            setupUI(position)
            setupListeners(position, musicItem.SongURL)
        }


        /**
         * 設定 UI 狀態
         */
        private fun setupUI(position: Int) {
            playBtn.setImageResource(
                if (currentlyPlayingPosition == position) {
                    R.drawable.baseline_stop_circle_24
                } else {
                    R.drawable.baseline_play_arrow_24
                }
            )

            playbackSeekBar.visibility =
                if (currentlyPlayingPosition == position) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            loadingProgressBar.apply {
                visibility =
                    if (downloadsInProgress[position] == true) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                progress = downloadProgressMap[position] ?: 0
            }

            downloadButton.apply {
                setImageResource(
                    if (downloadCompleted[position] == true) {
                        R.drawable.baseline_cloud_done_24 // 下載完成的圖示
                    } else {
                        R.drawable.baseline_arrow_circle_down_24 // 下載按鈕的圖示
                    }
                )
                isEnabled = downloadCompleted[position] != true // 禁用已下載的按鈕
            }
        }

        /**
         * 設定按鈕監聽器
         */
        private fun setupListeners(position: Int, url: String?) {
            playBtn.setOnClickListener {
                if (currentlyPlayingPosition == position) {
                    stopAudio()
                } else {
                    url?.let {
                        playAudio(it, position, this)
                    }
                }
            }
            downloadButton.setOnClickListener {
                if (downloadCompleted[position] != true) {
                    startDownload(position)
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

        /**
         * 開始下載歌曲
         */
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
        musicList.getOrNull(position)?.let {
            holder.bind(position, it)
        }
    }

    override fun getItemCount() = musicList.size

    /**
     * 更新下載進度
     */
    fun updateDownloadProgress(position: Int, progress: Int) {
        downloadProgressMap[position] = progress
        if (progress >= 100) {
            downloadsInProgress[position] = false
            downloadCompleted[position] = true
        }
        notifyItemChanged(position)
    }

    /**
     * 播放音樂
     */
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

    /**
     * 停止音樂
     */
    private fun stopAudio() {
        currentlyPlayingPosition?.let {
            seekBarUpdateRunnable.remove(it)?.let { handler.removeCallbacks(it) }
            notifyItemChanged(it)
        }
        mediaPlayer?.release()
        mediaPlayer = null
        currentlyPlayingPosition = null
    }

    /**
     * 更新 SeekBar 進度
     */
    private fun updateSeekBar(position: Int, holder: UserViewHolder) {
        val runnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        holder.playbackSeekBar.progress = (player.currentPosition * 100) / player.duration
                        handler.postDelayed(this, 500)
                    }
                }
            }
        }
        seekBarUpdateRunnable[position] = runnable
        handler.post(runnable)
    }

    /**
     * 釋放資源
     */
    fun releaseResources() {
        stopAudio()
        handler.removeCallbacksAndMessages(null)
    }
}
