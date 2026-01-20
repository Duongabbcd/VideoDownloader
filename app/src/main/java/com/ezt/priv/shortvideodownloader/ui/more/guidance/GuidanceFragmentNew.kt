package com.ezt.priv.shortvideodownloader.ui.more.guidance

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.ads.AdmobUtils
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.ads.type.NativeAds
import com.ezt.priv.shortvideodownloader.databinding.ViewpagerGuidanceItempageBinding
import com.ezt.priv.shortvideodownloader.ui.intro.IntroActivityNew.Companion.numberPage
import com.ezt.priv.shortvideodownloader.ui.intro.IntroFragmentNew.CallbackIntro
import com.ezt.priv.shortvideodownloader.ui.language.LanguageActivity
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Common.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GuidanceFragmentNew : Fragment() {
//    @Inject
//    lateinit var analyticsLogger: AnalyticsLogger

    private val binding by lazy { ViewpagerGuidanceItempageBinding.inflate(layoutInflater) }
    private lateinit var callbackIntro: CallbackIntro
    private var position = 0
    private var reload = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity is CallbackIntro) callbackIntro = activity as CallbackIntro
        position = arguments?.getInt(ARG_POSITION) ?: 0
        if (arguments != null) {
            println("IntroActivityNew.numberPage: $numberPage")
            fragmentPosition4()
        }

        binding.nextBtn.setOnClickListener {
            var positionIntro = 0
            when (binding.guidanceTitle.text) {
                getString(R.string.intro_1_1) -> {
                    positionIntro = 1
                }

                getString(R.string.intro_2_1) -> {
                    positionIntro = 2
                }

                getString(R.string.intro_3_1) -> {
                    positionIntro = 3
                }

                getString(R.string.intro_3_2) -> {
                    positionIntro = 4
                }
            }
            callbackIntro.onNext(position, positionIntro)

            val ctx = context ?: return@setOnClickListener
//            analyticsLogger.updateUserProperties(
//                ctx,
//                "intro_screen",
//                -1
//            )
            println("nextBtn is here")

        }

//        binding.skipBtn.setOnClickListener {
//            val ctx = context ?: return@setOnClickListener
//           analyticsLogger.updateUserProperties(
//                ctx,
//                "intro_screen",
//               -1
//            )
//            println("nextBtn is here")
//            callbackIntro.onNext(numberPage - 1, 3)
//        }
    }



    private fun fragmentPosition4() {
//        binding.lottieSlide.gone()
        when (position) {
            0 -> {
                setUiIntro1()
//                binding.lottieSlide.visible()
            }

            1 -> {
                setUiIntro2()
            }

            2 -> {
                setUiIntro3()
            }

            3 -> {
                setUiIntro4()
            }

            4 -> {
                setUiIntro5()
            }
        }
    }


    private fun setUiIntro1() {
        binding.guidanceTitle.text = getString(R.string.intro_1_1)
        binding.guidancePic.setImageResource(R.drawable.guide1)
        binding.introStep.setImageResource(R.drawable.icon_intro1_1)
//        binding.intro2.visible()
//     binding.intro3.gone()
//        binding.intro4.gone()
    }

    private fun setUiIntro2() {
        println("setUiIntro2")
        binding.guidanceTitle.text = getString(R.string.intro_2_1)
        binding.guidancePic.setImageResource(R.drawable.guide2)
        binding.introStep.setImageResource(R.drawable.icon_intro2_1)
//        binding.intro2.visible()
//     binding.intro3.gone()
//        binding.intro4.gone()
    }


    private fun setUiIntro3() {
        binding.guidanceTitle.text = getString(R.string.intro_3_2)
        binding.guidancePic.setImageResource(R.drawable.guide3)
        binding.introStep.setImageResource(R.drawable.icon_intro3_1)
//        binding.skipBtn.gone()
//        binding.intro2.visible()
//     binding.intro3.gone()
//        binding.intro4.gone()
    }

    private fun setUiIntro4() {
        binding.guidanceTitle.text = getString(R.string.intro_3_1)
        binding.guidancePic.setImageResource(R.drawable.guide4)
        binding.introStep.setImageResource(R.drawable.icon_intro3_1)
//        binding.skipBtn.gone()
//        binding.intro2.visible()
//     binding.intro3.gone()
//        binding.intro4.gone()
    }


    private fun setUiIntro5() {

//        binding.skipBtn.gone()
//        binding.intro2.visible()
//     binding.intro3.gone()
//        binding.intro4.gone()
        binding.guidanceTitle.gone()
        binding.guidancePic.gone()
        binding.disclaimer.visible()
        binding.disclaimer1.visible()
        binding.disclaimer2.visible()
        binding.nextBtn.text = resources.getString(R.string.got_it)
        binding.introStep.setImageResource(R.drawable.icon_intro4_1)
    }


    override fun onResume() {
        super.onResume()
    }


    companion object {
        private const val ARG_POSITION = "position"
        private val TAG = GuidanceFragmentNew::class.java.simpleName
        fun newInstance(position: Int): GuidanceFragmentNew {
            val fragment = GuidanceFragmentNew()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }
}