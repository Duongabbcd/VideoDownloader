package com.ezt.priv.shortvideodownloader.ui.whatsapp.bottomsheet

import android.content.Context
import com.ezt.priv.shortvideodownloader.databinding.BottomSheetRequiredPermissionBinding
import com.ezt.priv.shortvideodownloader.ui.BaseBottomSheetDialog

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
        }
    }
}