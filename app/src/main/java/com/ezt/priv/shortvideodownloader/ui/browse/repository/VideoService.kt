package com.ezt.priv.shortvideodownloader.ui.browse.repository

import android.util.Log
import com.ezt.priv.shortvideodownloader.ui.browse.proxy_utils.CookieUtils
import com.ezt.priv.shortvideodownloader.ui.browse.proxy_utils.CustomProxyController
import com.ezt.priv.shortvideodownloader.ui.browse.qualifier.Proxy
import com.ezt.priv.shortvideodownloader.ui.browse.qualifier.VideFormatEntityList
import com.ezt.priv.shortvideodownloader.ui.browse.qualifier.VideoFormatEntity
import com.ezt.priv.shortvideodownloader.ui.browse.qualifier.VideoInfo
import com.ezt.priv.shortvideodownloader.ui.browse.viewmodel.VideoInfoWrapper
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import okhttp3.Request
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

interface VideoService {
    fun getVideoInfo(
        url: Request,
        isM3u8OrMpd: Boolean = false,
        isAudioCheck: Boolean
    ): VideoInfoWrapper?
}

@Singleton
open class VideoServiceLocal @Inject constructor(
    private val proxyController: CustomProxyController
) : VideoService {
    companion object {
        const val MP4_EXT = "mp4"
        private const val FACEBOOK_HOST = ".facebook."
        private const val COOKIE_HEADER = "Cookie"
    }

    override fun getVideoInfo(
        url: Request,
        isM3u8OrMpd: Boolean,
        isAudioCheck: Boolean
    ): VideoInfoWrapper? {
        Log.d("VideoService","Getting info url...:  $url  ${url.headers["Cookie"]}")

        var result: VideoInfoWrapper? = null

        try {
            result = handleYoutubeDlUrl(url, isM3u8OrMpd, isAudioCheck)
        } catch (e: Throwable) {
            Log.d("VideoService","YoutubeDL Error: $e")
        }

        return result
    }

    private fun handleYoutubeDlUrl(
        url: Request,
        isM3u8OrMpd: Boolean = false,
        isAudioCheck: Boolean
    ): VideoInfoWrapper {
        val request = YoutubeDLRequest(url.url.toString())
        url.headers.forEach { (name, value) ->
            if (name != COOKIE_HEADER) {
                request.addOption("--add-header", "$name:${value}")
            }
        }

        val currentProxy = proxyController.getCurrentRunningProxy()
        if (currentProxy != Proxy.noProxy()) {
            attachProxyToRequest(request, currentProxy)
        }

        val tmpCookieFile = CookieUtils.addCookiesToRequest(url.url.toString(), request)

        try {
            val info = YoutubeDL.getInstance().getInfo(request)
            val formats = info.formats?.map { videoEntityFromFormat(it) } ?: emptyList()
            val filtered = if (url.url.toString().contains(FACEBOOK_HOST)) {
                formats.filter {
                    it.formatId?.lowercase(Locale.ROOT)?.contains(Regex("hd|sd")) == true
                }
            } else {
                emptyList()
            }

            val listFormats = VideFormatEntityList(filtered.ifEmpty {
                if (isAudioCheck) {
                    formats
                } else {
                    formats.filter { it.vcodec != "none" || it.acodec == "none" }
                }
            })

            return VideoInfoWrapper(
                VideoInfo(
                    title = info.title ?: "no title", formats = listFormats
                ).apply {
                    ext = info.ext ?: MP4_EXT
                    thumbnail = info.thumbnail ?: ""
                    duration = info.duration.toLong()
                    originalUrl = url.url.toString()
                    downloadUrls = if (isM3u8OrMpd) emptyList() else listOf(url)
                    isRegularDownload = false
                })
        } catch (e: Throwable) {
            throw e
        } finally {
            tmpCookieFile.delete()
        }
    }

    private fun attachProxyToRequest(request: YoutubeDLRequest, currentProxy: Proxy) {
        val user = proxyController.getProxyCredentials().first
        val password = proxyController.getProxyCredentials().second
        if (user.isNotEmpty() && password.isNotEmpty()) {
            request.addOption(
                "--proxy", "http://${user}:${password}@${currentProxy.host}:${currentProxy.port}"
            )
        } else {
            request.addOption(
                "--proxy", "${currentProxy.host}:${currentProxy.port}"
            )
        }
    }

    private fun videoEntityFromFormat(videoFormat: VideoFormat): VideoFormatEntity {
        val size = if(videoFormat.url != null) getFileSizeFromUrl(videoFormat.url) else 0
        return VideoFormatEntity(
            asr = videoFormat.asr,
            tbr = videoFormat.tbr,
            abr = videoFormat.abr,
            format = videoFormat.format,
            formatId = videoFormat.formatId,
            formatNote = videoFormat.formatNote,
            ext = videoFormat.ext,
            preference = videoFormat.preference,
            vcodec = videoFormat.vcodec,
            acodec = videoFormat.acodec,
            width = videoFormat.width,
            height = videoFormat.height,
            fileSize = if(size > 0) size else videoFormat.fileSize,
            fileSizeApproximate = videoFormat.fileSizeApproximate,
            fps = videoFormat.fps,
            url = videoFormat.url,
            manifestUrl = videoFormat.manifestUrl,
            httpHeaders = videoFormat.httpHeaders
        )
    }

    fun getFileSizeFromUrl(url: String?): Long {
        return try {
            if(url.isNullOrEmpty()) {
                return -1
            }
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.getHeaderFieldLong("Content-Length", -1)
        } catch (e: Exception) {
            -1
        }
    }

}
