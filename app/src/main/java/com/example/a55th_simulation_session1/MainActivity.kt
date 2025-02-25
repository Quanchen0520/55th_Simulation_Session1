package com.example.a55th_simulation_session1

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import android.Manifest
import android.media.MediaPlayer
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    // 宣告變數
    private lateinit var networkMonitor: NetworkMonitor  // 負責監聽網路狀態
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback  // 網路狀態回調
    private lateinit var tvNetworkStatus: TextView  // 顯示網路狀態的 TextView
    private var mediaPlayer: MediaPlayer? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // val recyclerView = findViewById<RecyclerView>(R.id.recycler)

        val urllist = listOf(
            "https://drive.google.com/uc?id=1VerV2SI1HNJCtUVjW0Vh__uvIdZrR9vs&export=download",
            "https://drive.google.com/uc?id=1b7sJBghbCwW_lEApmSt2BlqXqMqn0MHh&export=download",
            "https://drive.google.com/uc?id=1F5oLYOPD3KbMAEK7Sz72Yg9ZE94kfpUV&export=download",
            "https://drive.google.com/uc?id=1yH2VQ6dm51zmOMEyiygJzZ1gTZ2H66a_&export=download")

        // 取得 TextView 元件
        tvNetworkStatus = findViewById(R.id.textView)

        // 建立網路監聽器物件
        val networkMonitor = NetworkMonitor(this)

        // 網路連線判斷
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread { tvNetworkStatus.text = "網路狀態: 已連線 ✅" }
            }

            override fun onLost(network: Network) {
                runOnUiThread { tvNetworkStatus.text = "網路狀態: 已斷線 ❌" }
            }
        }

        // 註冊網路監聽
        networkMonitor.registerNetworkCallback(networkCallback)

        val viewPager = findViewById<ViewPager2>(R.id.ViewPager)

        val songList = listOf("Ocean Wave", "Rain Thunder", "Brook", "Jungle")

        val imageList = listOf(
            R.drawable.ocean_wave,
            R.drawable.rain_thunder,
            R.drawable.brook,
            R.drawable.glassland)

        val adapter = Adapter(songList, imageList, urllist,
            onDownloadClick = { position ->
                downloadAudio(urllist[position], songList[position])
                Toast.makeText(this, "下載歌曲: ${songList[position]}", Toast.LENGTH_SHORT).show()
            },
            onPlayClick = { url ->
                playAudio(url) // 點擊後播放音樂
            }
        )

        viewPager.adapter = adapter

//        val songList = listOf("ocean_wave", "rain_thunder", "Charlie", "David")
//
//        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
//        recyclerView.adapter = Adapter(songList) { position ->
//            Toast.makeText(this, "已下載: ${songList[position]}", Toast.LENGTH_SHORT).show()
//            val AudioName = songList[position]
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
//                != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this,
//                    arrayOf(Manifest.permission.READ_MEDIA_AUDIO), 102)
//            }
//            downloadAudio(urllist[position], AudioName)
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 取消註冊網路監聽，避免記憶體洩漏
        networkMonitor.unregisterNetworkCallback(networkCallback)
    }

    //網路監聽類別，負責監聽裝置的網路狀態
    class NetworkMonitor(context: Context) {
        private val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        //註冊網路監聽
        fun registerNetworkCallback(callback: ConnectivityManager.NetworkCallback) {
            val request = NetworkRequest.Builder().build()  // 建立網路請求
            connectivityManager.registerNetworkCallback(request, callback)  // 註冊監聽
        }

        //取消註冊網路監聽
        fun unregisterNetworkCallback(callback: ConnectivityManager.NetworkCallback) {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    private fun downloadAudio(url: String, fileName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = File(getExternalFilesDir(null), fileName)
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                Log.e("Download", "伺服器回應: ${connection.responseCode}")

                val inputStream: InputStream = connection.inputStream
                val outputStream: OutputStream = file.outputStream()
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                inputStream.close()
                outputStream.close()
                connection.disconnect()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity,"Download Success!", Toast.LENGTH_SHORT).show()
                    Log.d("Download", "下載完成: ${file.absolutePath}")
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("Download", "下載失敗: ${e.message}")
                }
            }
        }
    }

    private fun playAudio(url: String) {
        try {
            mediaPlayer?.release() // 釋放舊的 MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { start() }
                setOnErrorListener { _, what, extra ->
                    Log.e("MediaPlayer", "播放錯誤: what=$what, extra=$extra")
                    //Toast.makeText(context, "播放失敗", Toast.LENGTH_SHORT).show()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("MediaPlayer", "初始化錯誤: ${e.message}")
        }
    }
}