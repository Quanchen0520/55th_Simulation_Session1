package com.example.a55th_simulation_session1

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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 設定可下載音檔的網址
        val urllist = listOf(
            "https://drive.google.com/uc?id=1VerV2SI1HNJCtUVjW0Vh__uvIdZrR9vs&export=download",
            "https://drive.google.com/uc?id=1b7sJBghbCwW_lEApmSt2BlqXqMqn0MHh&export=download",
            "https://drive.google.com/uc?id=1F5oLYOPD3KbMAEK7Sz72Yg9ZE94kfpUV&export=download",
            "https://drive.google.com/uc?id=1yH2VQ6dm51zmOMEyiygJzZ1gTZ2H66a_&export=download"
        )

        // 初始化UI元件
        tvNetworkStatus = findViewById(R.id.textView)
        val viewPager = findViewById<ViewPager2>(R.id.ViewPager)
        val songList = listOf("Ocean Wave", "Rain Thunder", "Brook", "Jungle")
        val imageList = listOf(
            R.drawable.ocean_wave,
            R.drawable.rain_thunder,
            R.drawable.brook,
            R.drawable.glassland
        )

        // 設定ViewPager的adapter，並定義下載按鈕的行為
        adapter = Adapter(
            songList = songList,
            imageList = imageList,
            urllist = urllist,
            onDownloadClick = { position ->
                // 點擊下載時呼叫下載方法
                downloadAudio(urllist[position], songList[position], position)
                Toast.makeText(this, "下載歌曲: ${songList[position]}", Toast.LENGTH_SHORT).show()
            },
        )

        viewPager.adapter = adapter

        // 初始化網路監控
        networkMonitor = NetworkMonitor(this)
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            // 網路可用時顯示訊息
            override fun onAvailable(network: Network) {
                runOnUiThread { tvNetworkStatus.text = "網路狀態: 已連線 ✅" }
            }
            // 網路斷開時顯示訊息
            override fun onLost(network: Network) {
                runOnUiThread { tvNetworkStatus.text = "網路狀態: 已斷線 ❌" }
            }
        }
        networkMonitor.registerNetworkCallback(networkCallback)
    }

    // 當Activity銷毀時解除網路監控
    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.unregisterNetworkCallback(networkCallback)
        adapter.releaseResources()  // 釋放資源
    }

    // 網路監控類別，用來檢查網路狀態
    class NetworkMonitor(context: Context) {
        private val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // 註冊網路監控
        fun registerNetworkCallback(callback: ConnectivityManager.NetworkCallback) {
            val request = NetworkRequest.Builder().build()
            connectivityManager.registerNetworkCallback(request, callback)
        }

        // 取消網路監控
        fun unregisterNetworkCallback(callback: ConnectivityManager.NetworkCallback) {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    // 下載音訊檔案的方法
    private fun downloadAudio(url: String, fileName: String, position: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 建立檔案存放位置
                val file = File(getExternalFilesDir(null), fileName)
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                Log.e("Download", "伺服器回應: ${connection.responseCode}")

                // 開始讀取音檔資料流
                val inputStream: InputStream = connection.inputStream
                val outputStream: OutputStream = file.outputStream()
                val buffer = ByteArray(1024)
                var bytesRead: Int
                var totalBytes = 0
                val fileSize = connection.contentLength

                // 開始下載並寫入檔案
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                    val progress = (totalBytes * 100) / fileSize

                    // 更新下載進度
                    withContext(Dispatchers.Main) {
                        adapter.updateDownloadProgress(position, progress)
                    }
                }

                // 關閉資料流
                inputStream.close()
                outputStream.close()
                connection.disconnect()

                // 下載完成，顯示提示訊息
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Download Success!", Toast.LENGTH_SHORT).show()
                    Log.d("Download", "下載完成: ${file.absolutePath}")
                    adapter.updateDownloadProgress(position, 100)
                }

            } catch (e: Exception) {
                // 下載失敗時顯示錯誤訊息
                withContext(Dispatchers.Main) {
                    Log.e("Download", "下載失敗: ${e.message}")
                }
            }
        }
    }
}