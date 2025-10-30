package com.ezt.priv.shortvideodownloader.ui.home

import android.app.ActionBar.LayoutParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.anggrayudi.storage.file.getAbsolutePath
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.ads.type.BannerAds
import com.ezt.priv.shortvideodownloader.ads.type.BannerAds.BANNER_HOME
import com.ezt.priv.shortvideodownloader.ads.type.InterAds
import com.ezt.priv.shortvideodownloader.ads.type.NativeAds
import com.ezt.priv.shortvideodownloader.database.VideoDownloadDB
import com.ezt.priv.shortvideodownloader.database.repository.DownloadRepository
import com.ezt.priv.shortvideodownloader.database.viewmodel.CookieViewModel
import com.ezt.priv.shortvideodownloader.database.viewmodel.DownloadViewModel
import com.ezt.priv.shortvideodownloader.database.viewmodel.ResultViewModel
import com.ezt.priv.shortvideodownloader.database.viewmodel.SettingsViewModel
import com.ezt.priv.shortvideodownloader.databinding.ActivityMainBinding
import com.ezt.priv.shortvideodownloader.ui.BaseActivity2
import com.ezt.priv.shortvideodownloader.ui.connection.InternetConnectionViewModel
import com.ezt.priv.shortvideodownloader.ui.downloads.DownloadQueueMainFragment
import com.ezt.priv.shortvideodownloader.ui.downloads.HistoryFragment
import com.ezt.priv.shortvideodownloader.ui.more.settings.SettingsActivity
import com.ezt.priv.shortvideodownloader.util.Common
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Common.visible
import com.ezt.priv.shortvideodownloader.util.CrashListener
import com.ezt.priv.shortvideodownloader.util.FileUtil
import com.ezt.priv.shortvideodownloader.util.NavbarUtil
import com.ezt.priv.shortvideodownloader.util.NavbarUtil.applyNavBarStyle
import com.ezt.priv.shortvideodownloader.util.ThemeUtil
import com.ezt.priv.shortvideodownloader.util.UiUtil
import com.ezt.priv.shortvideodownloader.util.UpdateUtil
import com.ezt.priv.shortvideodownloader.work.CryptoConstants
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.snackbar.Snackbar
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.system.exitProcess

class MainActivity : BaseActivity2<ActivityMainBinding>(ActivityMainBinding::inflate) {

    lateinit var context: Context
    private lateinit var preferences: SharedPreferences
    private lateinit var resultViewModel: ResultViewModel
    private lateinit var cookieViewModel: CookieViewModel
    private lateinit var downloadViewModel: DownloadViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private var navigationView: NavigationView? = null
    private var navigationBarView: NavigationBarView? = null
    private lateinit var navHostFragment : NavHostFragment
    private lateinit var navController : NavController

    private val connectionViewModel: InternetConnectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NativeAds.preloadNativeAds(
            this@MainActivity,
            alias = NativeAds.ALIAS_NATIVE_HOME,
            adId = NativeAds.NATIVE_HOME
        )

        val videoDownloaderPath: String = FileUtil.getDefaultApplicationPath()
        isHomeActivity = true
        connectionViewModel.isConnectedLiveData.observe(this) { isConnected ->
            checkInternetConnected(isConnected)
        }
        addingNoMediaFiles(videoDownloaderPath)

        handleIncomingIntent(intent)

        if(Common.getCountOpenApp(this@MainActivity) == 0) {
            Common.setCountOpenApp(this@MainActivity, 1)
            CryptoConstants.createAESKey(this@MainActivity)
        }

        CrashListener(this).registerExceptionHandler()
        ThemeUtil.updateTheme(this)
        window.navigationBarColor = SurfaceColors.SURFACE_2.getColor(this)
        context = baseContext
        resultViewModel = ViewModelProvider(this)[ResultViewModel::class.java]
        cookieViewModel = ViewModelProvider(this)[CookieViewModel::class.java]
        downloadViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        preferences = PreferenceManager.getDefaultSharedPreferences(context)


        if (preferences.getBoolean("incognito", false)) {
            lifecycleScope.launch(Dispatchers.IO){
                resultViewModel.deleteAll()
            }
        }

        InterAds.preloadInterAds(
            this@MainActivity,
            alias = InterAds.ALIAS_INTER_DOWNLOAD,
            adUnit = InterAds.INTER_AD1
        )

        askPermissions()
//        checkUpdate()

        navHostFragment = supportFragmentManager.findFragmentById(R.id.frame_layout) as NavHostFragment
        navController = navHostFragment.findNavController()
//        kotlin.runCatching {
//            navigationView = findViewById(R.id.navigationView)
//        }
        kotlin.runCatching {
            navigationBarView = findViewById(R.id.bottomNavigationView)
        }

        navigationBarView?.apply {
            window.decorView.setOnApplyWindowInsetsListener { view: View, windowInsets: WindowInsets? ->
                val windowInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(
                    windowInsets!!, view
                )
                val isImeVisible = windowInsetsCompat.isVisible(WindowInsetsCompat.Type.ime())
                visibility = if (isImeVisible) View.GONE else View.VISIBLE
                view.onApplyWindowInsets(windowInsets)
            }
        }

        NavbarUtil.init(this)

        navigationBarView?.apply {
            if (savedInstanceState == null){
                val graph = navController.navInflater.inflate(R.navigation.nav_graph)
                graph.setStartDestination(NavbarUtil.getStartFragmentId(this@MainActivity))
                navController.graph = graph
            }
            applyNavBarStyle()

            val showingDownloadQueue = NavbarUtil.getNavBarItems(this@MainActivity).any { n -> n.itemId == R.id.downloadQueueMainFragment && n.isVisible }

            setupWithNavController(navController)
            setOnItemReselectedListener {
                when (it.itemId) {
                    R.id.homeFragment -> {
                        kotlin.runCatching {
                            (navHostFragment.childFragmentManager.primaryNavigationFragment!! as HomeFragment).scrollToTop()
                        }
                    }
                    R.id.downloadQueueMainFragment -> {
                        kotlin.runCatching {
                            (navHostFragment.childFragmentManager.primaryNavigationFragment!! as DownloadQueueMainFragment).scrollToActive()
                        }
                    }
                    R.id.historyFragment -> {
//                        if(!showingDownloadQueue) {
//                            navController.navigate(R.id.downloadQueueMainFragment)
//                        }else{
//                            kotlin.runCatching {
//                                (navHostFragment.childFragmentManager.primaryNavigationFragment!! as HistoryFragment).scrollToTop()
//                            }
//                        }
                        (navHostFragment.childFragmentManager.primaryNavigationFragment!! as HistoryFragment).scrollToTop()
                    }
                    R.id.moreFragment -> {
                        val intent = Intent(context, SettingsActivity::class.java)
                        startActivity(intent)
                    }
                }
            }

            val activeDownloadsBadge = if (showingDownloadQueue) {
                getOrCreateBadge(R.id.downloadQueueMainFragment)
            }else{
                getOrCreateBadge(R.id.historyFragment)
            }
            lifecycleScope.launch {
                downloadViewModel.activePausedDownloadsCount.collectLatest {
                    if (it == 0) {
                        activeDownloadsBadge.isVisible = false
                        activeDownloadsBadge.clearNumber()
                    }
                    else {
                        activeDownloadsBadge.isVisible = true
                        activeDownloadsBadge.number = it
                    }
                }
            }

            val showingNavbarItems = NavbarUtil.getNavBarItems(this@MainActivity).filter { it.isVisible }.map { it.itemId }
            navController.addOnDestinationChangedListener { _, destination, _ ->
                Handler(Looper.getMainLooper()).post {
                    if (showingNavbarItems.contains(destination.id)) {
                        showBottomNavigation()
                    }else{
                        hideBottomNavigation()
                    }
                }

            }

            visibilityChanged {
                if (it.isVisible){
                    val curr = navController.currentDestination?.id
                    if (!showingNavbarItems.contains(curr)) hideBottomNavigation()
                }
            }
        }

        navigationView?.apply {
            setupWithNavController(navController)
            //terminate button
            menu.getItem(8).setOnMenuItemClickListener {
                if (preferences.getBoolean("ask_terminate_app", true)){
                    var doNotShowAgain = false
                    val terminateDialog = MaterialAlertDialogBuilder(this@MainActivity)
                    terminateDialog.setTitle(getString(R.string.confirm_delete_history))
                    val dialogView = layoutInflater.inflate(R.layout.dialog_terminate_app, null)
                    val checkbox = dialogView.findViewById<CheckBox>(R.id.doNotShowAgain)
                    terminateDialog.setView(dialogView)
                    checkbox.setOnCheckedChangeListener { compoundButton, _ ->
                        doNotShowAgain = compoundButton.isChecked
                    }

                    terminateDialog.setNegativeButton(getString(R.string.cancel)) { dialogInterface: DialogInterface, _: Int -> dialogInterface.cancel() }
                    terminateDialog.setPositiveButton(getString(R.string.ok)) { _: DialogInterface?, _: Int ->
                        runBlocking {
                            val job : Job = lifecycleScope.launch(Dispatchers.IO) {
                                val activeDownloads = downloadViewModel.getActiveDownloads().toMutableList()
                                activeDownloads.map { it.status = DownloadRepository.Status.Queued.toString() }
                                activeDownloads.forEach {
                                    downloadViewModel.updateDownload(it)
                                }
                            }
                            runBlocking {
                                job.join()
                                if (doNotShowAgain){
                                    preferences.edit().putBoolean("ask_terminate_app", false).apply()
                                }
                                finishAndRemoveTask()
                                finishAffinity()
                                exitProcess(0)
                            }
                        }
                    }
                    terminateDialog.show()
                }else{
                    finishAndRemoveTask()
                    exitProcess(0)
                }
                true
            }
            //settings button
            menu.getItem(9).setOnMenuItemClickListener {
                val intent = Intent(context, SettingsActivity::class.java)
                startActivity(intent)
                true
            }

            getHeaderView(0).findViewById<TextView>(R.id.title).text = ThemeUtil.getStyledAppName(this@MainActivity)
        }

        cookieViewModel.updateCookiesFile()
        val intent = intent
        handleIntents(intent)

        if (preferences.getBoolean("auto_update_ytdlp", false)){
            return
            CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
                kotlin.runCatching {
                    if(VideoDownloadDB.getInstance(this@MainActivity).downloadDao.getDownloadsCountByStatus(listOf("Active", "Queued")) == 0){
                        if (UpdateUtil(this@MainActivity).updateYoutubeDL().status == UpdateUtil.YTDLPUpdateStatus.DONE) {
                            val version = YoutubeDL.getInstance().version(context)
                            val snack = Snackbar.make(findViewById(R.id.frame_layout),
                                this@MainActivity.getString(R.string.ytld_update_success) + " [${version}]",
                                Snackbar.LENGTH_LONG)

                            navigationBarView?.apply {
                                snack.setAnchorView(this)
                            }
                            snack.show()
                        }
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

    private fun addingNoMediaFiles(videoDownloaderPath: String) {

        val folder = File(videoDownloaderPath)

        if (!folder.exists()) {
            folder.mkdirs()  // Create folder if it doesn't exist
        }

        val nomediaFile = File(folder, ".nomedia")

        if (!nomediaFile.exists()) {
            try {
                val created = nomediaFile.createNewFile()
                if (created) {
                    println(".nomedia file created")
                } else {
                    println("Failed to create .nomedia file")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            println(".nomedia already exists")
        }
    }


    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
            Log.d("ShareReceiver", "Received shared URL: $sharedUrl")
            if (!sharedUrl.isNullOrEmpty()) {
                isSharedURL = true
                // ✅ Set clipboard immediately
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("shared_url", sharedUrl)
                clipboard.setPrimaryClip(clip)
            }
        }
    }
    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBundle("nav_state", navController.saveState())
    }
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        navController.restoreState(savedInstanceState.getBundle("nav_state"))
    }

    private fun View.visibilityChanged(action: (View) -> Unit) {
        this.viewTreeObserver.addOnGlobalLayoutListener {
            val newVis: Int = this.visibility
            if (this.tag as Int? != newVis) {
                this.tag = this.visibility
                // visibility has changed
                action(this)
            }
        }
    }


    fun hideBottomNavigation(){
        navigationBarView?.apply {
            if (this is BottomNavigationView){
                this@MainActivity.findViewById<FragmentContainerView>(R.id.frame_layout).updateLayoutParams<ConstraintLayout.LayoutParams> {
                    bottomToTop = ConstraintLayout.LayoutParams.UNSET
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                }
                this.animate()?.translationY(this.height.toFloat())?.setDuration(300)?.withEndAction {
                    this.visibility = View.GONE
                }?.start()
            }else if (this is NavigationRailView){
                this@MainActivity.findViewById<FragmentContainerView>(R.id.frame_layout).updateLayoutParams {
                    this.width = LayoutParams.MATCH_PARENT
                }

                if (resources.getBoolean(R.bool.is_right_to_left)){
                    this.animate()?.translationX(this.width.toFloat())?.setDuration(300)?.withEndAction {
                        this.visibility = View.GONE
                    }?.start()
                }else{
                    this.animate()?.translationX(-this.width.toFloat())?.setDuration(300)?.withEndAction {
                        this.visibility = View.GONE
                    }?.start()
                }
            }
        }


    }

    fun showBottomNavigation(){
        navigationBarView?.apply {
            if (this is BottomNavigationView){
                this@MainActivity.findViewById<FragmentContainerView>(R.id.frame_layout).updateLayoutParams<ConstraintLayout.LayoutParams> {
                    bottomToTop = R.id.bottomNavigationView
                    bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                }
                this.animate()?.translationY(0F)?.setDuration(300)?.withEndAction {
                    this.visibility = View.VISIBLE
                }?.start()
            }else if (this is NavigationRailView){
                this@MainActivity.findViewById<FragmentContainerView>(R.id.frame_layout).updateLayoutParams {
                    this.width = 0
                }
                this.animate()?.translationX(0F)?.setDuration(300)?.withEndAction {
                    this.visibility = View.VISIBLE
                }?.start()
            }
        }

    }

    fun disableBottomNavigation(){
        navigationBarView?.menu?.forEach { it.isEnabled = false }
        navigationView?.menu?.forEach { it.isEnabled = false }
    }

    fun enableBottomNavigation(){
        navigationBarView?.menu?.forEach { it.isEnabled = true }
        navigationView?.menu?.forEach { it.isEnabled = true }
    }

    override fun onResume() {
        super.onResume()
        Log.d(
           TAG,
            "Banner Conditions: ${RemoteConfig.BANNER_ALL_2} and ${RemoteConfig.ADS_DISABLE_2}"
        )
        if (RemoteConfig.BANNER_ALL_2 == "0" || RemoteConfig.ADS_DISABLE_2 == "0") {
            binding.frBanner.root.gone()
        } else {
            loadBanner(this, BANNER_HOME)
        }
        //incognito header
        val incognitoHeader = findViewById<TextView>(R.id.incognito_header)
        if (preferences.getBoolean("incognito", false)){
            incognitoHeader.visibility = View.VISIBLE
            window.statusBarColor = (incognitoHeader.background as ColorDrawable).color
        }else{
            window.statusBarColor = getColor(android.R.color.transparent)
            incognitoHeader.visibility = View.GONE
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
        handleIntents(intent)
    }

    private fun handleIntents(intent: Intent) {
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null) {
            Log.e(TAG, action)
            try {
                val uri = if (Build.VERSION.SDK_INT >= 33){
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                }else{
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }

                var downloadType = DownloadViewModel.Type.valueOf(preferences.getString("preferred_download_type", "video")!!)
                if (preferences.getBoolean("quick_download", false) || downloadType == DownloadViewModel.Type.command) {
                    val docFile = DocumentFile.fromSingleUri(this, uri!!)
                    if (docFile?.exists() == true){
                        val bundle = Bundle()
                        val path = docFile.getAbsolutePath(this)
                        if (downloadType == DownloadViewModel.Type.auto) {
                            downloadType = downloadViewModel.getDownloadType(null, path)
                        }

                        bundle.putParcelable("result", downloadViewModel.createEmptyResultItem(path))
                        bundle.putSerializable("type", downloadType)
                        navController.navigate(R.id.downloadBottomSheetDialog, bundle)
                        return
                    }
                }

                val `is` = contentResolver.openInputStream(uri!!)
                val textBuilder = StringBuilder()
                val reader: Reader = BufferedReader(
                    InputStreamReader(
                        `is`, Charset.forName(
                            StandardCharsets.UTF_8.name()
                        )
                    )
                )
                var c: Int
                while (reader.read().also { c = it } != -1) {
                    textBuilder.append(c.toChar())
                }
                val bundle = Bundle()
                bundle.putString("url", textBuilder.toString())
                navController.popBackStack(R.id.homeFragment, true)
                navController.navigate(
                    R.id.homeFragment,
                    bundle
                )
            } catch (e: Exception) {
//                Toast.makeText(context, "Couldn't read file", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }else if (action == Intent.ACTION_VIEW){

            val navbarItems = NavbarUtil.getNavBarItems(this)
            when(intent.getStringExtra("destination")){
                "Downloads" -> {
                    if (navbarItems.any { n -> n.itemId == R.id.historyFragment && n.isVisible }) {
                        navController.popBackStack(navController.graph.startDestinationId, true)
                    }
                    navController.navigate(R.id.historyFragment)
                }
                "Queue" -> {
                    if (navbarItems.any { n -> n.itemId == R.id.downloadQueueMainFragment && n.isVisible }) {
                        navController.popBackStack(navController.graph.startDestinationId, true)
                    }

                    val bundle = Bundle()
                    intent.getStringExtra("tab")?.apply {
                        bundle.putString("tab", this)
                    }
                    intent.getLongExtra("reconfigure", 0L).apply {
                        if (this != 0L){
                            bundle.putLong("reconfigure", this)
                        }
                    }
                    navController.navigate(R.id.downloadQueueMainFragment, bundle)
                }
                "Search" -> {
                    val bundle = Bundle()
                    bundle.putBoolean("search", true)
                    navController.popBackStack(R.id.homeFragment, true)
                    navController.navigate(
                        R.id.homeFragment,
                        bundle
                    )
                }
            }
        }
    }


    private fun checkUpdate() {
        if (preferences.getBoolean("update_app", false)) {
            val updateUtil = UpdateUtil(this)
            CoroutineScope(Dispatchers.IO).launch {
                val res = updateUtil.tryGetNewVersion()
                if (res.isSuccess) {
                    if (preferences.getBoolean("automatic_backup", false)) {
                        settingsViewModel.backup()
                    }
                    withContext(Dispatchers.Main) {
                        UiUtil.showNewAppUpdateDialog(res.getOrNull()!!, this@MainActivity, preferences)
                    }
                }

            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        exitProcess(0)
    }

    override fun onDestroy() {
        super.onDestroy()
        isHomeActivity = false
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        var isChangeTheme = false
        var isSharedURL = false
        var currentTabPosition = -1

        fun loadBanner(activity: AppCompatActivity, banner: String = BannerAds.BANNER_HOME) {
            println("RemoteConfig.BANNER_COLLAP_ALL_070625: ${RemoteConfig.BANNER_ALL_2}")
            if (RemoteConfig.BANNER_ALL_2 != "0") {
                BannerAds.initBannerAds(activity, banner) { isDisplayed ->
                    if (isDisplayed) {
                        val root = activity.findViewById<FrameLayout>(R.id.frBanner)
                        root.isVisible = true
                        activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_layout).gone()
                    } else {
                        Handler(Looper.getMainLooper()).postDelayed({
                            BannerAds.initBannerAds(activity, banner) {isDisplayed ->
                                if (isDisplayed) {
                                    val root = activity.findViewById<FrameLayout>(R.id.frBanner)
                                    root.isVisible = true
                                    activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_layout).gone()
                                } else  {
                                    Handler(Looper.getMainLooper()).postDelayed( {
                                        activity.findViewById<FrameLayout>(R.id.frBanner).isVisible = false
                                    }, 1000)
                                }
                            }
                        }, 1000)
                    }

                }
            }

        }
    }
}