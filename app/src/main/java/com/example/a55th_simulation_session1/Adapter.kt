package com.example.a55th_simulation_session1

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

class Adapter(
    private val songList: List<String>, // 歌曲名稱列表
    private val imageList: List<Int>, // 對應圖片資源 ID 列表
    private val urllist: List<String>, // 音樂網址列表
    private val onDownloadClick: (Int) -> Unit // 下載按鈕點擊事件回調
) : RecyclerView.Adapter<Adapter.UserViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null // 媒體播放器
    private var currentlyPlayingPosition: Int? = null // 當前播放的歌曲索引
    private val handler = Handler(Looper.getMainLooper()) // 處理執行緒，用於更新 UI
    private val seekBarUpdateRunnable = HashMap<Int, Runnable>() // 用於更新 SeekBar 的 Runnable
    private val downloadsInProgress = HashMap<Int, Boolean>() // 記錄下載中的歌曲索引
    private val downloadProgressMap = HashMap<Int, Int>() // 下載進度的映射表
    private val downloadCompleted = HashMap<Int, Boolean>() // 記錄已完成下載的歌曲索引

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val songName: TextView = itemView.findViewById(R.id.SongName) // 歌曲名稱
        val songImage: ImageView = itemView.findViewById(R.id.imageView9) // 歌曲圖片
        val downloadButton: ImageButton = itemView.findViewById(R.id.imageBtn) // 下載按鈕
        val playBtn: ImageButton = itemView.findViewById(R.id.playBtn) // 播放按鈕
        val loadingProgressBar: ProgressBar = itemView.findViewById(R.id.loadingProgressBar) // 下載進度條
        val playbackSeekBar: SeekBar = itemView.findViewById(R.id.playbackSeekBar) // 播放進度條

        fun bind(position: Int) {
            songName.text = songList[position] // 設定歌曲名稱
            songImage.setImageResource(imageList[position]) // 設定歌曲圖片

            setupInitialState(position) // 設定初始狀態

            // 根據下載狀態變更 UI
            if (downloadCompleted[position] == true) {
                downloadButton.setImageResource(R.drawable.baseline_cloud_done_24) // 下載完成圖示
                downloadButton.isEnabled = false // 禁用按鈕
            } else {
                downloadButton.setImageResource(R.drawable.baseline_arrow_circle_down_24) // 可下載圖示
                downloadButton.isEnabled = true
            }

            // 顯示或隱藏下載進度條
            if (downloadsInProgress[position] == true) {
                loadingProgressBar.visibility = View.VISIBLE
                loadingProgressBar.progress = downloadProgressMap[position] ?: 0
            } else {
                loadingProgressBar.visibility = View.GONE
            }

            // 設定播放按鈕點擊事件
            playBtn.setOnClickListener {
                if (currentlyPlayingPosition == position) {
                    stopAudio()
                } else {
                    playAudio(urllist[position], position, this)
                }
            }

            // 設定下載按鈕點擊事件
            downloadButton.setOnClickListener {
                if (downloadCompleted[position] != true) {
                    downloadsInProgress[position] = true
                    loadingProgressBar.visibility = View.VISIBLE
                    loadingProgressBar.progress = 0
                    onDownloadClick(position) // 呼叫下載回調函式
                }
            }

            // 設定 SeekBar 監聽器
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
        holder.bind(position)
    }

    override fun getItemCount() = songList.size // 返回歌曲數量

    fun updateDownloadProgress(position: Int, progress: Int) {
        downloadProgressMap[position] = progress

        if (progress >= 100) {
            downloadsInProgress[position] = false
            downloadCompleted[position] = true
        }

        notifyItemChanged(position, "progress_update")
    }

    private fun playAudio(url: String, position: Int, holder: UserViewHolder) {
        stopAudio()
        currentlyPlayingPosition = position

        holder.playBtn.setImageResource(R.drawable.baseline_stop_circle_24)
        holder.playbackSeekBar.visibility = View.VISIBLE

        mediaPlayer = MediaPlayer().apply {
            setDataSource(url) // 設定音樂來源
            setOnPreparedListener {
                start() // 開始播放
                updateSeekBar(position, holder) // 更新 SeekBar
            }
            setOnCompletionListener {
                stopAudio() // 播放結束後停止音樂
            }
            prepareAsync() // 非同步準備音樂
        }
    }

    private fun stopAudio() {
        currentlyPlayingPosition?.let { prevPosition ->
            seekBarUpdateRunnable[prevPosition]?.let { handler.removeCallbacks(it) }
            seekBarUpdateRunnable.remove(prevPosition)
            notifyItemChanged(prevPosition, "stop_audio")
        }
        mediaPlayer?.release() // 釋放播放器資源
        mediaPlayer = null
        currentlyPlayingPosition = null
    }

    private fun updateSeekBar(position: Int, holder: UserViewHolder) {
        val runnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val progress = (player.currentPosition * 100) / player.duration
                        holder.playbackSeekBar.progress = progress // 更新 SeekBar 進度
                        handler.postDelayed(this, 500) // 每 500ms 更新一次
                    }
                }
            }
        }
        seekBarUpdateRunnable[position] = runnable
        handler.post(runnable)
    }

    fun releaseResources() {
        stopAudio()
        handler.removeCallbacksAndMessages(null) // 清除所有回調
    }
}