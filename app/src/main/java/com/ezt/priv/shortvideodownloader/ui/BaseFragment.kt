package com.ezt.priv.shortvideodownloader.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.ezt.priv.shortvideodownloader.ui.home.MainActivity
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.util.Utils.hideKeyBoard

abstract class BaseFragment<T : ViewBinding>(private val inflate: Inflate<T>) : Fragment() {
    protected val binding: T by lazy { inflate(layoutInflater) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!isAdded && activity == null) {
            return
        }
        reloadData(false)
    }

    open fun reloadData(isDisplayLoading: Boolean) {
        if (MainActivity.Companion.isChangeTheme) {
            return
        }
    }

    open fun openFragment(fragment: Fragment) {
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.frameLayout, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    protected inline fun withSafeContext(action: (Context) -> Unit) {
        if (!isAdded || context == null) return
        val ctx = requireContext()
        action(ctx)
    }

    fun hideKeyBoard(textView: TextView) {
        if (context == null) {
            return
        } else {
            requireContext().hideKeyBoard(textView)
        }
    }
}