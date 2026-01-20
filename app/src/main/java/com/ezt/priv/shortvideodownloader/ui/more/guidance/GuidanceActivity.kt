package com.ezt.priv.shortvideodownloader.ui.more.guidance

import android.os.Bundle
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.databinding.ActivityGuidanceBinding
import com.ezt.priv.shortvideodownloader.ui.BaseActivity2
import com.ezt.priv.shortvideodownloader.ui.intro.IntroFragmentNew
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GuidanceActivity : BaseActivity2<ActivityGuidanceBinding>(ActivityGuidanceBinding::inflate), IntroFragmentNew.CallbackIntro {
    private lateinit var guidanceAdapter: GuidanceAdapter
    var position: Int = 0
    private var now = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        now = System.currentTimeMillis()
        binding.apply {
            closeBtn.setOnClickListener {
                finish()
            }
        }
        viewPager()
    }

    private fun viewPager() {

        println("isIntroFullFail:${RemoteConfig.NATIVE_FULL_SCREEN_INTRO_070625}")
        numberPage = 5

        if (binding.viewpager.adapter == null) {
            guidanceAdapter = GuidanceAdapter(this)
            binding.viewpager.adapter = guidanceAdapter
            binding.viewpager.setOffscreenPageLimit(numberPage)
            binding.viewpager.isUserInputEnabled = true
        }
    }

    override fun onNext(position: Int, introPos: Int) {
        println("onNext: $position and $introPos")
        if(position <= introPos - 1) {
            showAfterIntro1 {
                binding.viewpager.currentItem++
            }
        } else {
            finish()
        }

    }

    private fun showAfterIntro1(callback : () -> Unit){
        callback.invoke()
    }

    override fun closeAds() {
        //DO NOTHING
    }

    override fun disableSwip() {
        //DO NOTHING
    }

    companion object {
        private val TAG = GuidanceActivity::class.java.simpleName
        var numberPage = 4
    }
}