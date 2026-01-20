package com.ezt.priv.shortvideodownloader.ui.browse.binding

import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableInt
import com.google.android.material.imageview.ShapeableImageView

object ShapeableImageBinding {
    @BindingAdapter("app:srcCompat")
    @JvmStatic
    fun setImageDrawable(view: ShapeableImageView, drawable: Any?) {
        val resId = when (drawable) {
            is Int -> drawable
            is ObservableInt -> drawable.get()
            else -> 0
        }

        if (resId != 0) {
            view.setImageResource(resId)
        } else {
            view.setImageDrawable(null)
        }
    }
}