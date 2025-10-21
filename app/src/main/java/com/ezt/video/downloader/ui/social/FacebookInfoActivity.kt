package com.ezt.video.downloader.ui.social

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.ezt.video.downloader.databinding.ActivityFacebookInfoBinding
import com.ezt.video.downloader.ui.BaseActivity2
import com.ezt.video.downloader.R
import com.ezt.video.downloader.database.models.main.ResultItem
import com.ezt.video.downloader.database.viewmodel.DownloadViewModel
import com.ezt.video.downloader.database.viewmodel.ResultViewModel
import com.ezt.video.downloader.ui.home.MainActivity
import com.ezt.video.downloader.ui.info.DownloadInfoActivity
import com.ezt.video.downloader.util.Common.gone
import com.ezt.video.downloader.util.Common.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FacebookInfoActivity : BaseActivity2<ActivityFacebookInfoBinding>(ActivityFacebookInfoBinding::inflate){
    private val facebookURL by lazy {
        intent.getStringExtra("facebookURL") ?: ""
    }

    private lateinit var resultViewModel : ResultViewModel
    private lateinit var downloadViewModel : DownloadViewModel

    private var sharedPreferences: SharedPreferences? = null

    private var isFacebook = false
    private var queryList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        downloadViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
        resultViewModel = ViewModelProvider(this)[ResultViewModel::class.java]

        resultViewModel.getFilteredList().observe(this) { items ->
            kotlin.runCatching {
//                recentlySearch?.isVisible = it.isNotEmpty()
                items.onEach {
                    println("showSingleDownloadSheet 4: $it")
                }
                if(resultViewModel.repository.itemCount.value > 1 || resultViewModel.repository.itemCount.value == -1){
                    println("It is here")
                }else if (resultViewModel.repository.itemCount.value == 1){
                    if (sharedPreferences!!.getBoolean("download_card", true)){
                        if(items.size == 1 ){

                            showSingleDownloadSheet(
                                items[0],
                                DownloadViewModel.Type.valueOf(sharedPreferences!!.getString("preferred_download_type", "video")!!)
                            )
                        }
                    }
                }
            }
        }

        binding.apply {
            appIcon.isVisible = facebookURL.isNotEmpty()

            isFacebook = facebookURL.contains("facebook", true)
            val appImage = if(isFacebook) R.drawable.icon_facebook else R.drawable.icon_instagram
            Glide.with(this@FacebookInfoActivity).load(appImage).into(appIcon)

            searchBar.setText(facebookURL)
            queryList.add(facebookURL)
            startSearchFacebookURL()

            backIcon.setOnClickListener {
                val clipboard = this@FacebookInfoActivity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))

               finish()
            }
        }
    }


    private fun startSearchFacebookURL() {
        binding.loading.visible()
        lifecycleScope.launch(Dispatchers.IO){
            resultViewModel.deleteAll()

            val check1 = sharedPreferences!!.getBoolean("quick_download", false)
            val check2 = sharedPreferences!!.getString("preferred_download_type", "video") == "command"
            val check3 = Patterns.WEB_URL.matcher(queryList.first()).matches()
            val check4 = sharedPreferences!!.getBoolean("download_card", true)
            println("startSearch 1: $check1 and $check2 and $check3 and ${queryList.size}")
            if(check1|| check2){
                if (queryList.size == 1 && check3 ){
                    println("startSearch 2: $check4")
                    if (check4) {
                        withContext(Dispatchers.Main){
                            println("showSingleDownloadSheet 6")
                            showSingleDownloadSheet(
                                resultItem = downloadViewModel.createEmptyResultItem(queryList.first()),
                                type = DownloadViewModel.Type.valueOf(sharedPreferences!!.getString("preferred_download_type", "video")!!)
                            )
                            binding.loading.gone()
                        }
                    } else {
                        val downloadItem = downloadViewModel.createDownloadItemFromResult(
                            result = downloadViewModel.createEmptyResultItem(queryList.first()),
                            givenType = DownloadViewModel.Type.valueOf(sharedPreferences!!.getString("preferred_download_type", "video")!!)
                        )
                        downloadViewModel.queueDownloads(listOf(downloadItem))
                        withContext(Dispatchers.Main) {
                            binding.loading.gone()
                        }
                    }

                }else{
                    resultViewModel.parseQueries(queryList){
                        lifecycleScope.launch {
                            withContext(Dispatchers.Main) {
                                binding.loading.gone()
                            }
                        }
                    }

                }
            }else{
                resultViewModel.parseQueries(queryList){
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            binding.loading.gone()
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun showSingleDownloadSheet(
        resultItem: ResultItem,
        type: DownloadViewModel.Type,
        disableUpdateData : Boolean = false
    ){
        val intent = Intent(this@FacebookInfoActivity, DownloadInfoActivity::class.java)

        intent.putExtra("result", resultItem)
        intent.putExtra("facebookURL", facebookURL)
        intent.putExtra("isFromFB", true)
        intent.putExtra("type", downloadViewModel.getDownloadType(type, resultItem.url))
        if (disableUpdateData) {
            intent.putExtra("disableUpdateData", true)
        }

        startActivity(intent)
    }

    companion object {
        var facebookURL = ""

    }
}