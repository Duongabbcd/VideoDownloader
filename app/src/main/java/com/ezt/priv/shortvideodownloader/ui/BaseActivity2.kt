package com.ezt.priv.shortvideodownloader.ui

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.viewbinding.ViewBinding
import com.ezt.priv.shortvideodownloader.AppStateListener
import com.ezt.priv.shortvideodownloader.MyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity
import com.ezt.priv.shortvideodownloader.ui.player.PlayerActivity
import com.ezt.priv.shortvideodownloader.ui.player.PlayerActivity.Companion.returnedFromSettings
import com.ezt.priv.shortvideodownloader.ui.splash.SplashActivity
import com.ezt.priv.shortvideodownloader.ui.welcome.WelcomeActivity
import com.ezt.priv.shortvideodownloader.util.Common
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.system.exitProcess

typealias Inflate<T> = (LayoutInflater) -> T

@Suppress("DEPRECATION")
abstract class BaseActivity2<T : ViewBinding>(private val inflater: Inflate<T>) :
    BaseActivity(), AppStateListener {
    val binding: T by lazy { inflater(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Activity created fresh ${SplashActivity.isAllowedToReset}")
        if (SplashActivity.isAllowedToReset || savedInstanceState == null) {
            Log.d(TAG, "Activity created fresh")
        } else {
            Log.d(TAG, "Activity recreated with $savedInstanceState in ${javaClass.simpleName}")
            val intent = Intent(this, SplashActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
            return
        }

        val isDarkMode = false
        val flag: Int
        Log.d(TAG, "isDarkMode $isDarkMode")

        Common.setLocale(this@BaseActivity2, Common.getPreLanguage(this))
        if (isDarkMode && this != PlayerActivity) {
            setTheme(R.style.BaseTheme)
            flag =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        } else {
            setTheme(R.style.BaseTheme)
            flag =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // Force light theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Initialize lastKnownUiMode
        lastKnownUiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        MyApplication.instance.registerAppStateListener(this)

        window.decorView.systemUiVisibility = flag

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if ((visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000)
                    window.decorView.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            )

                    val isInDarkMode = false
                    val fl = if (isInDarkMode) {
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    } else {
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    }
                    window.decorView.systemUiVisibility = fl
                }
            }
        }

        window.navigationBarColor = Color.TRANSPARENT
        window.statusBarColor = Color.TRANSPARENT
        setContentView(binding.root)
    }


    open fun openFragment(fragment: Fragment) {
        val manager: FragmentManager = supportFragmentManager
        val transaction: FragmentTransaction = manager.beginTransaction()
        transaction.replace(R.id.frameLayout,fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val systemUiMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val oldUiMode = 0
        Log.d(TAG, "System UI mode changed from $oldUiMode to $systemUiMode")

        if (oldUiMode != systemUiMode && !returnedFromSettings) {
            // Save new value
            lastKnownUiMode = systemUiMode

            // Restart app or move to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MyApplication.instance.unregisterAppStateListener(this)
    }

    override fun onAppReturnedToForeground() {
        val totalTime = (System.currentTimeMillis() - now) / 1000L
        println("${this::class.java.simpleName}: App returned to foreground and $totalTime and isHomeActivity: $isHomeActivity")

        android.os.Handler(Looper.getMainLooper()).post {
            val currentTop = MyApplication.instance.currentActivity
            if (this != currentTop) {
                println("${this::class.java.simpleName}: is not the top ($currentTop)")
                return@post
            }

            if(totalTime >= 60 * 30 || MyApplication.isUnderMemory) {
                // App was in background too long
                MyApplication.isUnderMemory = false
            } else if (totalTime >= 10 && isHomeActivity) {
                println("${this::class.java.simpleName}: launching WelcomeActivity")
                startActivity(Intent(this, WelcomeActivity::class.java))
            }
        }
    }

    override fun onAppWentToBackground() {
        Log.d(TAG, "${this::class.java.simpleName}: App went to background")
        now = System.currentTimeMillis()
    }

    fun changeFullscreen(enable: Boolean = false) {
        if (enable) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, binding.root).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(
                window,
                binding.root
            ).show(WindowInsetsCompat.Type.systemBars())
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (i in permissions.indices) {
            if (permissions.contains(Manifest.permission.POST_NOTIFICATIONS)) continue
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                createPermissionRequestDialog()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkNotificationPermission(): Boolean {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun createPermissionRequestDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setTitle(getString(R.string.warning))
        dialog.setMessage(getString(R.string.request_permission_desc))
        dialog.setOnCancelListener { exit() }
        dialog.setNegativeButton(getString(R.string.exit_app)) { _: DialogInterface?, _: Int -> exit() }
        dialog.setPositiveButton(getString(R.string.ok)) { _: DialogInterface?, _: Int ->
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
            startActivity(intent)
            exitProcess(0)
        }
        dialog.show()
    }

    private fun exit() {
        finishAffinity()
        exitProcess(0)
    }


    companion object {
        private var now = 0L
        var isHomeActivity = false

        private var TAG = BaseActivity::class.java.simpleName
        var lastKnownUiMode = Configuration.UI_MODE_NIGHT_NO // default light mode
    }
}
