package com.ezt.priv.shortvideodownloader.ui.welcome

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.ezt.priv.shortvideodownloader.ads.type.OpenAds
import com.ezt.priv.shortvideodownloader.databinding.ActivityWelcomeBinding
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.ui.splash.SplashActivity

class WelcomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SplashActivity.isAllowedToReset = false
        OpenAds.resetOpenAds() // Clear any previous ad reference

        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        println("isFromService: $isFromService")

        if (isFromService) {
            isFromService = false
            binding.title.text = resources.getString(R.string.please_wait)
            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 500)
            return
        }

        binding.title.text = resources.getString(R.string.welcome_back)

        if (RemoteConfig.AD_RETURN_APP == "1") {
            OpenAds.initOpenAds(this@WelcomeActivity) { isReady ->
                if (isReady) {
                    OpenAds.showOpenAds(
                        this@WelcomeActivity
                    ) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            finish()
                        }, 1000)
                    }
                } else {
                    Handler(Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 1000)
                }
            }
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 1000)
        }


    }

    companion object {
        private val TAG = WelcomeActivity::class.java.simpleName
        var isFromService = false
    }
}