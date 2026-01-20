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
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.ezt.priv.shortvideodownloader.ui.downloadcard.DownloadMultipleBottomSheetDialog
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity.Companion.loadBanner
import com.ezt.priv.shortvideodownloader.ui.info.DownloadInfoActivity
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Common.visible
import com.ezt.priv.shortvideodownloader.util.NotificationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.observeOn
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
    private val isDisabled by lazy {
        intent.getBooleanExtra("isDisabled", false)
    }
    private val connectionViewModel: InternetConnectionViewModel by viewModels()
    private var isLoading = false
    private var notificationUtil: NotificationUtil? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationUtil = NotificationUtil(this)
        onBackPressedDispatcher.addCallback(this) {
            if (!isLoading) {
                isEnabled = false

                onBackPressedDispatcher.onBackPressed()
            }
        }


        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        downloadViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
        resultViewModel = ViewModelProvider(this)[ResultViewModel::class.java]

        connectionViewModel.isConnectedLiveData.observe(this) { isConnected ->
//            checkInternetConnected(isConnected)
        }


        facebookURL = intent.getStringExtra("facebookURL") ?: ""

        resultViewModel.getFilteredList().observe(this) { items ->
            kotlin.runCatching {
//                recentlySearch?.isVisible = it.isNotEmpty()
                val output = items.last()
                if (!binding.searchBar.text.isEmpty()) {
                    showSingleDownloadSheet(
                        output,
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

        lifecycleScope.launchWhenStarted {
            resultViewModel.errorMessage.collect { msg ->
                Toast.makeText(this@FacebookInfoActivity, msg, Toast.LENGTH_LONG).show()
            }
        }

        InterAds.preloadInterAds(
            this@FacebookInfoActivity,
            alias = InterAds.ALIAS_INTER_DOWNLOAD,
            adUnit = InterAds.INTER_AD1
        )

        binding.apply {
            appIcon.isVisible = facebookURL.isNotEmpty()

            mainMenu.setOnClickListener {
                // Do the action that used to be inside the menu
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        resultViewModel.cancelParsingQueries()
                    }
                }

                binding.searchBar.text = ""
            }

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
                executeBackScreen()
            }

            appIcon.setOnClickListener {
                val packageName = if (isFacebook) "com.facebook.katana" else {
                    if (facebookURL.contains("tiktok", true)) {
                        "com.zhiliaoapp.musicall"
                    } else {
                        "com.instagram.android"
                    }
                }
                if (packageName.isNotEmpty() && isAppInstalled(packageName)) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                    }
                } else {

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
                searchBar.text = ""
            } else {
                searchBar.text = facebookURL
                queryList.add(facebookURL)
                println("isDisabled: $isDisabled")
                if (!isDisabled) {
                    startSearchFacebookURL()
                } else {
                    hideLoading()
                }

            }

            openInstagram.setOnClickListener {
                val packageName = "com.instagram.android"
                if (packageName.isNotEmpty() && isAppInstalled(packageName)) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                    }
                } else {
                    Toast.makeText(
                        this@FacebookInfoActivity,
                        resources.getString(R.string.installed_app_required),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


        }
    }

    private fun executeBackScreen() {
        val clipboard =
            this@FacebookInfoActivity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        try {
            clipboard.clearPrimaryClip()  // Preferred
        } catch (e: Exception) {
            // Fallback for OEMs that misbehave
            clipboard.setPrimaryClip(ClipData.newPlainText(null, null))
        }

        InterAds.showPreloadInter(
            this@FacebookInfoActivity,
            alias = InterAds.ALIAS_INTER_DOWNLOAD,
            {
                startActivity(Intent(this@FacebookInfoActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                })
            },
            {
                startActivity(Intent(this@FacebookInfoActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                })
            })
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
        showLoading()
        binding.root.isEnabled = false

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
                            hideLoading().also {
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
                            hideLoading().also {
                                binding.root.isEnabled = true
                            }
                        }
                    }

                } else {
                    resultViewModel.parseQueries(listOf(queryList.last())) {
                        lifecycleScope.launch {
                            withContext(Dispatchers.Main) {
                                hideLoading().also {
                                    binding.root.isEnabled = true
                                }
                            }
                        }
                    }

                }
            } else {
                resultViewModel.parseQueries(listOf(queryList.last())) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            hideLoading().also {
                                binding.root.isEnabled = true
                            }
                        }
                    }
                }
            }
        }
    }

    fun showLoading() {
        isLoading = true
        enableFullImmersiveMode()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
        binding.loading.visible()
    }

    fun hideLoading() {
        isLoading = false
        binding.loading.gone()
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        disableFullImmersiveMode()
    }

    private fun disableFullImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, binding.root)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    private fun enableFullImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, binding.root)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleNotificationExtras(it) }
    }

    private var pendingDownloadIds: LongArray? = null
    private fun handleNotificationExtras(currentIntent: Intent) {
        val extras = currentIntent.extras ?: return
        val condition = extras.getBoolean("showDownloadsWithUpdatedFormats", false)
        println("handleNotificationExtras: $condition")
        if (condition) {
            pendingDownloadIds = extras.getLongArray("downloadIds")
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
        pendingDownloadIds?.let { ids ->
            notificationUtil?.cancelDownloadNotification(NotificationUtil.FORMAT_UPDATING_FINISHED_NOTIFICATION_ID)
            downloadViewModel.turnDownloadItemsToProcessingDownloads(
                ids.toList(),
                deleteExisting = true
            )

            val dialog = DownloadMultipleBottomSheetDialog.newInstance(ids)
            dialog.show(supportFragmentManager, "DownloadMultipleDialog")
            pendingDownloadIds = null
        }

        binding.apply {
            pasteBtn.setOnClickListener {
                val clipboard: ClipboardManager =
                    application.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboard.primaryClip?.getItemAt(0)?.text
                if (clip == null) {
                    Toast.makeText(
                        this@FacebookInfoActivity,
                        resources.getString(R.string.link_empty),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                if (clip == "") {
                    Toast.makeText(
                        this@FacebookInfoActivity,
                        resources.getString(R.string.link_empty),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (!clip.contains("instagram", true)) {
                    Toast.makeText(
                        this@FacebookInfoActivity,
                        resources.getString(R.string.link_invalid_2),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }


                facebookURL = clip.toString()
                searchBar.text = clip
            }

            downloadBtn.setOnClickListener {
                val clipboard: ClipboardManager =
                    application.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

                val clip = clipboard.primaryClip?.getItemAt(0)?.text
                if (clip == null) {
                    Toast.makeText(
                        this@FacebookInfoActivity,
                        resources.getString(R.string.link_empty),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (searchBar.text == "") {
                    Toast.makeText(
                        this@FacebookInfoActivity,
                        resources.getString(R.string.link_empty),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (!searchBar.text.contains("instagram", true)) {
                    Toast.makeText(
                        this@FacebookInfoActivity,
                        resources.getString(R.string.not_allowed),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                queryList.add(clip.toString())
                startSearchFacebookURL()
            }
        }
    }

    override fun onBackPressed() {
        executeBackScreen()
    }

    @SuppressLint("RestrictedApi")
    private fun showSingleDownloadSheet(
        resultItem: ResultItem,
        type: DownloadViewModel.Type,
        disableUpdateData: Boolean = false
    ) {
        binding.searchBar.text = ""
        val intent = Intent(this@FacebookInfoActivity, DownloadInfoActivity::class.java)
        println("showSingleDownloadSheet: ${resultItem.formats}")
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
        private val TAG = FacebookInfoActivity::class.java.simpleName
    }
}