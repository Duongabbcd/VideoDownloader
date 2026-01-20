package com.ezt.priv.shortvideodownloader.ui.whatsapp.bottomsheet

import android.content.Context
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.databinding.BottomSheetRequiredPermissionBinding
import com.ezt.priv.shortvideodownloader.ui.BaseBottomSheetDialog
import com.ezt.priv.shortvideodownloader.ui.intro.IntroFragmentNew.Companion.setSpannableString

class RequiredPermissionBottomSheet(private val context: Context, private val onClickListener: () -> Unit) : BaseBottomSheetDialog<BottomSheetRequiredPermissionBinding>(context) {
    override fun getViewBinding(): BottomSheetRequiredPermissionBinding {
        return BottomSheetRequiredPermissionBinding.inflate(layoutInflater)
    }

    override fun initViews() {
        setContentView(binding.root)

        binding.apply {
            closeBtn.setOnClickListener {
                dismiss()
            }

            continueBtn.setOnClickListener {
                onClickListener()
                dismiss()
            }

            val first = context.resources.getString(R.string.open_folder_desc)
            val highlight1 = ".Statuses"
            setSpannableString(first, listOf(highlight1), binding.requiredPerDesc, "#0091EA")
        }
    }
}