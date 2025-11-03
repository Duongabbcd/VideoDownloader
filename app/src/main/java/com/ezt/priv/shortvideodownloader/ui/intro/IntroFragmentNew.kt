package com.ezt.priv.shortvideodownloader.ui.intro

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
import com.ezt.priv.shortvideodownloader.ads.RemoteConfig
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.ads.AdmobUtils
import com.ezt.priv.shortvideodownloader.ads.type.NativeAds
import com.ezt.priv.shortvideodownloader.databinding.ViewpagerIntroItempageBinding
import com.ezt.priv.shortvideodownloader.ui.intro.IntroActivityNew.Companion.numberPage
import com.ezt.priv.shortvideodownloader.ui.language.LanguageActivity
import com.ezt.priv.shortvideodownloader.util.Common.gone
import com.ezt.priv.shortvideodownloader.util.Common.visible
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class IntroFragmentNew : Fragment() {
//    @Inject
//    lateinit var analyticsLogger: AnalyticsLogger

    private val binding by lazy { ViewpagerIntroItempageBinding.inflate(layoutInflater) }
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
        if (RemoteConfig.ADS_DISABLE == "0") {
            binding.rlNative.gone()
        }

        if (activity is CallbackIntro) callbackIntro = activity as CallbackIntro
        position = arguments?.getInt(ARG_POSITION) ?: 0
        if (arguments != null) {
            println("IntroActivityNew.numberPage: $numberPage")
            when (numberPage) {
                3 -> {
                    fragmentPosition3()
                }

                4 -> {
                    fragmentPosition4()
                }

                5 -> {
                    fragmentPosition5()
                }
            }
        }

        binding.nextBtn.setOnClickListener {
            var positionIntro = 0
            when (binding.title.text) {
                getString(R.string.intro_1) -> {
                    positionIntro = 1
                }

                getString(R.string.intro_2) -> {
                    positionIntro = 2
                }

                getString(R.string.intro_3) -> {
                    positionIntro = 3
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


    private fun fragmentPosition5() {
        showView(true)
        when (position) {
            0 -> {
                setUiIntro1()
            }

            1 -> {
                setUiIntro2()
            }

            2 -> {
                setUiIntro3()
//                binding.lottieSlide.visible()
            }

            3 -> {
                showNativeFull()
            }

            4 -> {
                setUiIntro4()
            }
        }
    }

    private fun fragmentPosition4() {
        showView(true)
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
        }
    }

    private fun fragmentPosition42() {
        showView(true)
        when (position) {
            0 -> {
                setUiIntro1()
            }

            1 -> {
                setUiIntro2()
            }

            2 -> {
                showNativeFull()
            }

            3 -> {
                setUiIntro3()
            }
        }
    }

    private fun fragmentPosition3() {
        showView(true)
        println("fragmentPosition3 called, position = $position")
        when (position) {
            0 -> {
                println("fragmentPosition3 - position 0 - preload alias 1")

                binding.rlNative.visible()
                setUiIntro1()
            }

            1 -> {
                println("fragmentPosition3 - position 1 - no preload")
                setUiIntro2()
            }

            2 -> {
                println("fragmentPosition3 - position 2 - preload alias 3")

                binding.rlNative.visible()
                setUiIntro3()
            }
        }
    }

    private fun setUiIntro1() {
        showNativeIntro(0)
        binding.title.text = getString(R.string.intro_1)
        val first = getString(R.string.intro_1)
        val highlight1 = getString(R.string.highlight_1)
        setSpannableString(first, listOf(highlight1), binding.title)
        binding.image2.setImageResource(R.drawable.bg_intro1)
        binding.introIcon.setImageResource(R.drawable.icon_intro1)
        binding.nextBtn.gone()
//        binding.intro2.visible()
//     binding.intro3.gone()
//        binding.intro4.gone()
    }

    private fun setUiIntro2() {
        println("setUiIntro2")
        showNativeIntro(1)
        binding.title.text = getString(R.string.intro_2)
        val second = getString(R.string.intro_2)
        val highlight2 = getString(R.string.highlight_2)
        setSpannableString(second, listOf(highlight2), binding.title)
        binding.image2.setImageResource(R.drawable.bg_intro2)
        binding.introIcon.setImageResource(R.drawable.icon_intro2)
        binding.nextBtn.gone()
//        binding.intro2.visible()
//     binding.intro3.gone()
//        binding.intro4.gone()
    }


    private fun setUiIntro3() {
        showNativeIntro(2)
        binding.title.text = getString(R.string.intro_3)
        val third = getString(R.string.intro_3)
        val highlight3 = getString(R.string.highlight_3)
        setSpannableString(third, listOf(highlight3), binding.title)
        binding.image2.setImageResource(R.drawable.bg_intro3)
        binding.introIcon.setImageResource(R.drawable.icon_intro3)
        binding.nextBtn.gone()
        binding.disclaimer.gone()
//        binding.skipBtn.gone()
//        binding.intro2.visible()
//     binding.intro3.gone()
//        binding.intro4.gone()
    }



    private fun setUiIntro4() {
        showNativeIntro(4)
        binding.title.text = getString(R.string.intro_4)
        val third = getString(R.string.intro_4)
        val highlight3 = getString(R.string.highlight_4)
        setSpannableString(third, listOf(highlight3), binding.title)
        binding.image2.gone()
        binding.disclaimer.visible()
        binding.introIcon.setImageResource(R.drawable.icon_intro5)
        binding.nextBtn.visible()
//        binding.skipBtn.gone()
//        binding.intro2.visible()
//     binding.intro3.gone()
//        binding.intro4.gone()
    }


    private fun showView(isShow: Boolean) {
        binding.apply {
            if (!isShow && AdmobUtils.isNetworkConnected(requireActivity())) {
                scrollView.gone()
                bottomControlLayout.gone()
                frNative.gone()
                frNativeFull.visible()
                layoutFull.visible()
                closeAds.visible()
            } else {
                scrollView.visible()
                frNative.visible()
                bottomControlLayout.visible()
                frNativeFull.gone()
                layoutFull.gone()
//                binding.lottieSlide.visible()
                closeAds.gone()
            }
        }
    }

    private fun showNativeFull() {
//        binding.lottieSlide.gone()
        showView(false)
        activity?.let { it ->
            binding.rlNative.visibility = View.GONE
            LanguageActivity.showNative(it,
                NativeAds.ALIAS_NATIVE_FULLSCREEN,
                binding.frNativeFull,
                fullScreen = true,
                onLoadDone = {
                    binding.mLoadingView.root.visibility = View.GONE
                },
                onLoadFailed = {
                    if (RemoteConfig.NATIVE_INTRO == "0") {
                        binding.rlNative.visibility = View.GONE
                        return@showNative
                    }
                    NativeAds.preloadNativeAds(
                        it,
                        alias = NativeAds.ALIAS_NATIVE_FULLSCREEN,
                        adId = NativeAds.NATIVE_INTRO_FULLSCREEN
                    )
                    LanguageActivity.showNative(it, alias = NativeAds.ALIAS_NATIVE_FULLSCREEN,
                        binding.frNativeFull, fullScreen = true, {
                            binding.mLoadingView.root.visibility = View.GONE
                        }, {
                            Handler(Looper.getMainLooper()).postDelayed({
                                binding.rlNative.visibility = View.GONE
                            }, 1500) // Delay 1500 ms (1.5 seconds)
                        })
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()

        try {
            if (binding.layoutFull.isVisible) {
                binding.closeAds.setOnClickListener {
                    callbackIntro.closeAds()
                }
                if (AdmobUtils.isNetworkConnected(requireContext())) {
                    callbackIntro.disableSwip()
                    if (position == 1) {
                        if (reload) {
                            reload = false
                            return
                        }
                    }
                } else {
                    binding.frNativeFull.gone()
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun showNativeIntro(position: Int) {
        when (position) {
            0 -> {
                if (RemoteConfig.ADS_DISABLE == "0") {
                    binding.rlNative.gone()
                    return
                }
                binding.frNative.visible()
                activity?.let { it ->
                    LanguageActivity.showNative(it,
                        NativeAds.ALIAS_NATIVE_INTRO_1,
                        binding.frNative,
                        onLoadDone = {
                            binding.mLoadingView.root.visibility = View.GONE
                        },
                        onLoadFailed = {
                            if (RemoteConfig.NATIVE_INTRO == "0") {
                                binding.rlNative.visibility = View.GONE
                                return@showNative
                            }
                            NativeAds.preloadNativeAds(
                                it,
                                alias = NativeAds.ALIAS_NATIVE_INTRO_1,
                                adId = NativeAds.NATIVE_INTRO_1
                            )
                            LanguageActivity.showNative(it, alias = NativeAds.ALIAS_NATIVE_INTRO_1,
                                binding.frNative, false, {
                                    binding.mLoadingView.root.visibility = View.GONE
                                }, {
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        binding.rlNative.visibility = View.GONE
                                    }, 1500) // Delay 1500 ms (1.5 seconds)
                                })
                        }
                    )
                }

            }

            1 -> {
                if (RemoteConfig.ADS_DISABLE == "0") {
                    binding.rlNative.gone()
                    return
                }
                binding.frNative.visible()
                activity?.let { it ->
                    LanguageActivity.showNative(it,
                        NativeAds.ALIAS_NATIVE_INTRO_2,
                        binding.frNative,
                        onLoadDone = {
                            binding.mLoadingView.root.visibility = View.GONE
                        },
                        onLoadFailed = {
                            if (RemoteConfig.NATIVE_INTRO == "0") {
                                binding.rlNative.visibility = View.GONE
                                return@showNative
                            }
                            NativeAds.preloadNativeAds(
                                it,
                                alias = NativeAds.ALIAS_NATIVE_INTRO_2,
                                adId = NativeAds.NATIVE_INTRO_2
                            )
                            LanguageActivity.showNative(it, alias = NativeAds.ALIAS_NATIVE_INTRO_2,
                                binding.frNative, false, {
                                    binding.mLoadingView.root.visibility = View.GONE
                                }, {
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        binding.rlNative.visibility = View.GONE
                                    }, 1500) // Delay 1500 ms (1.5 seconds)
                                })
                        }
                    )
                }
            }

            2 -> {
                binding.rlNative.gone()
            }

            4 -> {
                if (RemoteConfig.ADS_DISABLE == "0") {
                    binding.rlNative.gone()
                    return
                }
                binding.frNative.visible()
                activity?.let { it ->
                    LanguageActivity.showNative(it,
                        NativeAds.ALIAS_NATIVE_INTRO_5,
                        binding.frNative,
                        onLoadDone = {
                            binding.mLoadingView.root.visibility = View.GONE
                        },
                        onLoadFailed = {
                            if (RemoteConfig.NATIVE_INTRO == "0") {
                                binding.rlNative.visibility = View.GONE
                                return@showNative
                            }
                            NativeAds.preloadNativeAds(
                                it,
                                alias = NativeAds.ALIAS_NATIVE_INTRO_5,
                                adId = NativeAds.NATIVE_INTRO_5
                            )
                            LanguageActivity.showNative(it, alias = NativeAds.ALIAS_NATIVE_INTRO_5,
                                binding.frNative, false, {
                                    binding.mLoadingView.root.visibility = View.GONE
                                }, {
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        binding.rlNative.visibility = View.GONE
                                    }, 1500) // Delay 1500 ms (1.5 seconds)
                                })
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val ARG_POSITION = "position"
        private val TAG = IntroFragmentNew::class.java.simpleName
        fun newInstance(position: Int): IntroFragmentNew {
            val fragment = IntroFragmentNew()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }

        fun setSpannableString(fullText: String, target: List<String>, textView: TextView, colorCode: String = "#15AD72") {
            val spannable = SpannableString(fullText)

            // Set all text to black (optional if default is black)
            spannable.setSpan(
                ForegroundColorSpan(Color.BLACK),
                0,
                fullText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            target.onEach { item ->
                val start = fullText.indexOf(item)
                if (start >= 0) {
                    spannable.setSpan(
                        ForegroundColorSpan(Color.parseColor(colorCode)),
                        start,
                        start + item.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }



            textView.text = spannable
        }
    }


    interface CallbackIntro {
        fun onNext(position: Int, introPos: Int)
        fun closeAds()
        fun disableSwip()
    }
}