package com.example.a55th_simulation_session1

import MusicItem
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
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
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var tvNetworkStatus: TextView
    private lateinit var adapter: Adapter
    private val songList = mutableListOf<String>()
    private val imageList = mutableListOf<String>()
    private val urllist = mutableListOf<String>()
    private val musicItemList = mutableListOf<MusicItem>()

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvNetworkStatus = findViewById(R.id.textView)
        val viewPager = findViewById<ViewPager2>(R.id.ViewPager)

        adapter = Adapter(
            musicList = musicItemList,
            onDownloadClick = { position ->
                musicItemList[position].SongName?.let {
                    musicItemList[position].SongURL?.let { it1 ->
                        downloadAudio(
                            it1,
                            it, position)
                    }
                }
                Toast.makeText(this, "下載歌曲: ${musicItemList[position].SongName}", Toast.LENGTH_SHORT).show()
            }
        )
        viewPager.adapter = adapter

        MusicRepository.fetchMusicList { musicList ->
            runOnUiThread {
                if (musicList.isNotEmpty()) {
                    songList.clear()
                    imageList.clear()
                    urllist.clear()
                    musicItemList.clear()
                    musicList.mapNotNull { it.SongName }.let { songList.addAll(it) }
                    musicList.mapNotNull { it.imageURL }.let { imageList.addAll(it) }
                    musicList.mapNotNull { it.SongURL }.let { urllist.addAll(it) }
                    musicItemList.addAll(musicList)
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "無音樂資料", Toast.LENGTH_SHORT).show()
                }
            }
        }

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

    private fun downloadAudio(url: String, fileName: String, position: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val file = File(getExternalFilesDir(null), fileName)
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

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
                val progress = (totalBytes * 100) / fileSize

                withContext(Dispatchers.Main) {
                    adapter.updateDownloadProgress(position, progress)
                }
            }

            inputStream.close()
            outputStream.close()
            connection.disconnect()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Download Success!", Toast.LENGTH_SHORT).show()
                Log.d("Download", "下載完成: ${file.absolutePath}")
                adapter.updateDownloadProgress(position, 100)
            }
        }
    }
}