package com.ezt.priv.shortvideodownloader.ui.browse

import android.util.Log
import com.ezt.priv.shortvideodownloader.ui.browse.proxy_utils.OkHttpProxyClient
import com.ezt.priv.shortvideodownloader.ui.browse.viewmodel.VideoDetectionTabViewModel
import okhttp3.Headers
import okhttp3.Request

class VideoUtils {
    companion object {
        fun getContentTypeByUrl(
            url: String,
            headers: Headers?,
            okHttpProxyClient: OkHttpProxyClient
        ): ContentType {

            val regex = Regex("\\.(js|css|m4s|ts)$|^blob:")
            val check = regex.containsMatchIn(url)
            if (check) {
                return ContentType.OTHER
            }

            val request = Request.Builder()
                .url(url)
                .headers(headers ?: Headers.headersOf())
                .get()
                .build()

            return runCatching {
                okHttpProxyClient.getProxyOkHttpClient().newCall(request).execute()
                    .use { response ->
                        val contentTypeStr = response.header("Content-Type")

                        when {
                            contentTypeStr?.contains("mpegurl") == true -> ContentType.M3U8
                            contentTypeStr?.contains("dash") == true -> ContentType.MPD
                            contentTypeStr?.contains("video") == true ->{
                                Log.d(VideoDetectionTabViewModel.TAG, "contentTypeStr 1: $contentTypeStr")
                                ContentType.VIDEO
                            }
                            contentTypeStr?.contains(
                                "audio",
                                ignoreCase = true
                            ) == true -> ContentType.AUDIO

                            contentTypeStr?.contains("application/octet-stream") == true -> {
                                response.body.charStream().use { reader ->
                                    val content = reader.read(CharArray(7), 0, 7)
                                        .takeIf { it > 0 } // check if any chars are read
                                        ?.let { String(CharArray(7)) } ?: ""
                                    when {
                                        content.startsWith("#EXTM3U") -> ContentType.M3U8
                                        content.contains("<MPD") -> ContentType.MPD
                                        else -> ContentType.OTHER
                                    }
                                }
                            }

                            else -> ContentType.OTHER
                        }
                    }
            }.getOrDefault(ContentType.OTHER)
        }
    }
}