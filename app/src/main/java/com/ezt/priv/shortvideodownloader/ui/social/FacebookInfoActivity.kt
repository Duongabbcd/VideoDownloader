package com.ezt.priv.shortvideodownloader.ui.social

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.ezt.priv.shortvideodownloader.databinding.ActivityFacebookInfoBinding
import com.ezt.priv.shortvideodownloader.ui.BaseActivity2
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.ads.type.BannerAds.BANNER_HOME
import com.ezt.priv.shortvideodownloader.ads.type.InterAds
import com.ezt.priv.shortvideodownloader.database.models.main.ResultItem
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import com.ezt.priv.shortvideodownloader.database.viewmodel.ResultViewModel
import com.ezt.priv.shortvideodownloader.ui.connection.InternetConnectionViewModel
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity.Companion.loadBanner
import com.ezt.priv.shortvideodownloader.ui.info.DownloadInfoActivity
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Common.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.getValue

class FacebookInfoActivity :
    BaseActivity2<ActivityFacebookInfoBinding>(ActivityFacebookInfoBinding::inflate) {
    private var facebookURL = ""

    private lateinit var resultViewModel: ResultViewModel
    private lateinit var downloadViewModel: DownloadViewModel

    private var sharedPreferences: SharedPreferences? = null

    private var isFacebook = false
    private var queryList = mutableListOf<String>()

    private val connectionViewModel: InternetConnectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        downloadViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
        resultViewModel = ViewModelProvider(this)[ResultViewModel::class.java]

        connectionViewModel.isConnectedLiveData.observe(this) { isConnected ->
            checkInternetConnected(isConnected)
        }


        facebookURL = intent.getStringExtra("facebookURL") ?: ""

        resultViewModel.getFilteredList().observe(this) { items ->
            kotlin.runCatching {
//                recentlySearch?.isVisible = it.isNotEmpty()
                items.onEach {
                    println("showSingleDownloadSheet 4: $it")
                }
                if (resultViewModel.repository.itemCount.value > 1 || resultViewModel.repository.itemCount.value == -1) {
                    println("It is here")
                } else if (resultViewModel.repository.itemCount.value == 1) {
                    if (sharedPreferences!!.getBoolean("download_card", true)) {
                        if (items.size == 1) {

                            showSingleDownloadSheet(
                                items[0],
                                DownloadViewModel.Type.valueOf(
                                    sharedPreferences!!.getString(
                                        "preferred_download_type",
                                        "video"
                                    )!!
                                )
                            )
                        }
                    }
                }
            }
        }

        InterAds.preloadInterAds(
            this@FacebookInfoActivity,
            alias = InterAds.ALIAS_INTER_DOWNLOAD,
            adUnit = InterAds.INTER_AD1
        )

        binding.apply {
            appIcon.isVisible = facebookURL.isNotEmpty()

            isFacebook = facebookURL.contains("facebook", true)
            igGuidance.isVisible = !isFacebook && !facebookURL.contains("tiktok", true)
            val appImage = if (isFacebook) R.drawable.icon_facebook else {
                if (facebookURL.contains("tiktok", true)) {
                    R.drawable.icon_tiktok
                } else {
                    R.drawable.icon_instagram
                }
            }
            val appName = if (isFacebook) "Facebook" else {
                if (facebookURL.contains("tiktok", true)) {
                    "Tiktok"
                } else {
                    "Instagram"
                }
            }
            currentAppName.text = appName
            Glide.with(this@FacebookInfoActivity).load(appImage).into(appIcon)
            backIcon.setOnClickListener {
                isDisabled = false
                val clipboard =
                    this@FacebookInfoActivity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                InterAds.showPreloadInter(
                    this@FacebookInfoActivity,
                    alias = InterAds.ALIAS_INTER_DOWNLOAD,
                    {
                        startActivity(Intent(this@FacebookInfoActivity, MainActivity::class.java))
                        finish()
                    },
                    {
                        startActivity(Intent(this@FacebookInfoActivity, MainActivity::class.java))
                        finish()
                    })
            }

            appIcon.setOnClickListener {
                val packageName = if (isFacebook) "com.facebook.katana" else {
                    if (facebookURL.contains("tiktok", true)) {
                        "com.zhiliaoapp.musicall"
                    } else {
                        "com.instagram.android"
                    }
                }
                if (!packageName.isNullOrEmpty() && isAppInstalled(packageName)) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                    }
                }

            }

            if (facebookURL.equals(
                    "https://www.facebook.com/",
                    true
                ) || facebookURL.equals(
                    "https://www.instagram.com/",
                    true
                ) || facebookURL.equals("https://www.tiktok.com", true)
            ) {
                println("facebookURL: $facebookURL")
                searchBar.setText("")
            } else {
                searchBar.setText(facebookURL)
                queryList.add(facebookURL)

                if (!isDisabled) {
                    startSearchFacebookURL()
                }

            }

            openInstagram.setOnClickListener {
                val packageName = "com.instagram.android"
                if (!packageName.isNullOrEmpty() && isAppInstalled(packageName)) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                    }
                }
            }


        }
    }

    private fun checkInternetConnected(isConnected: Boolean) {
        if (!isConnected) {
            binding.origin.gone()
            binding.noInternet.root.visible()
            binding.noInternet.tryAgain.setOnClickListener {
                val connected = connectionViewModel.isConnectedLiveData.value == true
                if (connected) {
                    binding.origin.visible()
                    binding.noInternet.root.visibility = View.VISIBLE
                    // Maybe reload your data
                } else {
                    Toast.makeText(
                        this,
                        R.string.no_connection,
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        } else {
            binding.origin.visible()
            binding.noInternet.root.gone()
        }
    }

    private fun startSearchFacebookURL() {
        binding.loading.visible().also {
            binding.root.isEnabled = false
        }

        lifecycleScope.launch(Dispatchers.IO) {
            resultViewModel.deleteAll()

            val check1 = sharedPreferences!!.getBoolean("quick_download", false)
            val check2 =
                sharedPreferences!!.getString("preferred_download_type", "video") == "command"
            val check3 = Patterns.WEB_URL.matcher(queryList.first()).matches()
            val check4 = sharedPreferences!!.getBoolean("download_card", true)
            println("startSearch 1: $check1 and $check2 and $check3 and ${queryList.size}")
            if (check1 || check2) {
                if (queryList.size == 1 && check3) {
                    println("startSearch 2: $check4")
                    if (check4) {
                        withContext(Dispatchers.Main) {
                            println("showSingleDownloadSheet 6")
                            showSingleDownloadSheet(
                                resultItem = downloadViewModel.createEmptyResultItem(queryList.first()),
                                type = DownloadViewModel.Type.valueOf(
                                    sharedPreferences!!.getString(
                                        "preferred_download_type",
                                        "video"
                                    )!!
                                )
                            )
                            binding.loading.gone().also {
                                binding.root.isEnabled = true
                            }
                        }
                    } else {
                        val downloadItem = downloadViewModel.createDownloadItemFromResult(
                            result = downloadViewModel.createEmptyResultItem(queryList.first()),
                            givenType = DownloadViewModel.Type.valueOf(
                                sharedPreferences!!.getString(
                                    "preferred_download_type",
                                    "video"
                                )!!
                            )
                        )
                        downloadViewModel.queueDownloads(listOf(downloadItem))
                        withContext(Dispatchers.Main) {
                            binding.loading.gone().also {
                                binding.root.isEnabled = true
                            }
                        }
                    }

                } else {
                    resultViewModel.parseQueries(queryList) {
                        lifecycleScope.launch {
                            withContext(Dispatchers.Main) {
                                binding.loading.gone().also {
                                    binding.root.isEnabled = true
                                }
                            }
                        }
                    }

                }
            } else {
                resultViewModel.parseQueries(queryList) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            binding.loading.gone().also {
                                binding.root.isEnabled = true
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(
            TAG,
            "Banner Conditions: ${RemoteConfig.BANNER_ALL_2} and ${RemoteConfig.ADS_DISABLE}"
        )
        if (RemoteConfig.BANNER_ALL_2 == "0" || RemoteConfig.ADS_DISABLE == "0") {
            binding.frBanner.root.gone()
        } else {
            loadBanner(this, BANNER_HOME)
        }
        binding.apply {
            pasteBtn.setOnClickListener {
                val clipboard: ClipboardManager =
                    application.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

                var clip = clipboard.primaryClip!!.getItemAt(0).text
                facebookURL = clip.toString()
                searchBar.setText(clip)
            }

            downloadBtn.setOnClickListener {
                if(searchBar.text == "") {
                    return@setOnClickListener
                }

                val clipboard: ClipboardManager =
                    application.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

                var clip = clipboard.primaryClip!!.getItemAt(0).text
                queryList.add(clip.toString())
                startSearchFacebookURL()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun showSingleDownloadSheet(
        resultItem: ResultItem,
        type: DownloadViewModel.Type,
        disableUpdateData: Boolean = false
    ) {
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


    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    companion object {
        var isDisabled = false
        private val TAG = FacebookInfoActivity::class.java.simpleName
    }
}