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
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    // 宣告變數
    private lateinit var networkMonitor: NetworkMonitor  // 負責監聽網路狀態
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback  // 網路狀態回調
    private lateinit var tvNetworkStatus: TextView  // 顯示網路狀態的 TextView

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // 啟用全螢幕顯示
        setContentView(R.layout.activity_main)

        // 設定視圖適應系統邊界
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.button).setOnClickListener {
            try {

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                        102)
                }
                // val audioUrl = "https://taira-komori.jpn.org/nature01tw.html/sound_os2/nature01/ocean_wave1.mp3" // 替換成你要下載的音檔 URL
                val audioUrl = "https://drive.google.com/uc?id=1VerV2SI1HNJCtUVjW0Vh__uvIdZrR9vs&export=download"
                downloadAudio(audioUrl)
            } catch (e:Exception) {
                Log.d("downloaderror", e.toString())
            }
        }

        // 取得 TextView 元件
        tvNetworkStatus = findViewById(R.id.textView)

        // 設定 rootView，適應系統邊界
        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 創建網路監聽器物件
        val networkMonitor = NetworkMonitor(this)

        // 設定網路監聽的回調函式
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("NetworkMonitor", "網路可用")  // 輸出日誌
                runOnUiThread { tvNetworkStatus.text = "網路狀態: 已連線 ✅" }  // 更新 UI
            }

            override fun onLost(network: Network) {
                Log.d("NetworkMonitor", "網路中斷")  // 輸出日誌
                runOnUiThread { tvNetworkStatus.text = "網路狀態: 已斷線 ❌" }  // 更新 UI
            }
        }

        // 註冊網路監聽
        networkMonitor.registerNetworkCallback(networkCallback)
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
            connectivityManager.unregisterNetworkCallback(callback)  // 取消監聽
        }
    }

    fun downloadAudio(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileName = "downloaded_audio.mp3"
                val file = File(getExternalFilesDir(null), fileName)
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                Log.e("DownloadError", "伺服器回應: ${connection.responseCode}") // 連線成功判斷
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
                    Log.d("DownloadSuccess", "下載完成: ${file.absolutePath}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("DownloadError", "下載失敗: ${e.message}")
                }
            }
        }
    }
}