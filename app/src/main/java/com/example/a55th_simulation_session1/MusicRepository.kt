import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

object MusicRepository {
    private val client = OkHttpClient()
    private const val JSON_URL = "https://drive.google.com/uc?id=1Civ_XqRNn49IFHIvlIP1sJng-xE0UG2h&export=download"

    fun fetchMusicList(callback: (List<MusicItem>) -> Unit) {
        val request = Request.Builder().url(JSON_URL).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MusicRepository", "下載失敗: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { json ->
                    val musicResponse = Gson().fromJson(json, MusicResponse::class.java)
                    callback(musicResponse.resultList)
                }
            }
        })
    }
}