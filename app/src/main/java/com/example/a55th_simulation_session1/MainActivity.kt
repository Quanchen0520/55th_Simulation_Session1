package com.example.a55th_simulation_session1

import MusicItem
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    // 定義網路監測器
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var tvNetworkStatus: TextView
    private lateinit var adapter: Adapter

    // 用來存放歌曲資訊的列表
    private val songList = mutableListOf<String>()
    private val imageList = mutableListOf<String>()
    private val urllist = mutableListOf<String>()
    private val musicItemList = mutableListOf<MusicItem>()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 取得 UI 中的 TextView
        tvNetworkStatus = findViewById(R.id.textView)
        val viewPager = findViewById<ViewPager2>(R.id.ViewPager)

        // 初始化 Adapter，並設定點擊下載按鈕時的事件
        viewPager.adapter = Adapter(
            musicList = musicItemList,
            onDownloadClick = { position ->
                val songName = musicItemList[position].SongName
                val songUrl = musicItemList[position].SongURL
                if (songName != null && songUrl != null) {
                    downloadAudio(songUrl, songName, position)
                    Toast.makeText(this, "下載歌曲: $songName", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // 從 MusicRepository 取得音樂列表
        MusicRepository.fetchMusicList { musicList ->
            runOnUiThread {
                if (musicList.isNotEmpty()) {
                    songList.clear()
                    imageList.clear()
                    urllist.clear()
                    musicItemList.clear()

                    // 直接使用 mapNotNull，不需要 let
                    songList.addAll(musicList.mapNotNull { it.SongName })
                    imageList.addAll(musicList.mapNotNull { it.imageURL })
                    urllist.addAll(musicList.mapNotNull { it.SongURL })
                    musicItemList.addAll(musicList)

                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "無音樂資料", Toast.LENGTH_SHORT).show()
                }
            }
        }


        // 初始化網路監測器
        networkMonitor = NetworkMonitor(this)
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread { tvNetworkStatus.text = "網路狀態: 已連線 ✅" }
            }

            override fun onLost(network: Network) {
                runOnUiThread { tvNetworkStatus.text = "網路狀態: 已斷線 ❌" }
            }
        }
        networkMonitor.registerNetworkCallback(networkCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.unregisterNetworkCallback(networkCallback)
        adapter.releaseResources()
    }

    // 網路監測器類別
    class NetworkMonitor(context: Context) {
        private val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        fun registerNetworkCallback(callback: ConnectivityManager.NetworkCallback) {
            val request = NetworkRequest.Builder().build()
            connectivityManager.registerNetworkCallback(request, callback)
        }

        fun unregisterNetworkCallback(callback: ConnectivityManager.NetworkCallback) {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    // 音樂下載函式
    private fun downloadAudio(url: String, fileName: String, position: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val file = File(getExternalFilesDir(null), fileName) // 取得下載存放位置
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            Log.e("Download", "伺服器回應: ${connection.responseCode}")

            val inputStream: InputStream = connection.inputStream
            val outputStream: OutputStream = file.outputStream()
            val buffer = ByteArray(1024)
            var bytesRead: Int
            var totalBytes = 0
            val fileSize = connection.contentLength

            // 讀取下載的數據並寫入檔案
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
                val progress = (totalBytes * 100) / fileSize

                // 在主執行緒更新 UI
                withContext(Dispatchers.Main) {
                    adapter.updateDownloadProgress(position, progress)
                }
            }

            inputStream.close()
            outputStream.close()
            connection.disconnect()

            // 下載完成通知使用者
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "下載完成!", Toast.LENGTH_SHORT).show()
                Log.d("Download", "下載完成: ${file.absolutePath}")
                adapter.updateDownloadProgress(position, 100)
            }
        }
    }
}