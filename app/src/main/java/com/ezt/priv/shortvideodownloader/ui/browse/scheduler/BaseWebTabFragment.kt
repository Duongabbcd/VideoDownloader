package com.ezt.priv.shortvideodownloader.ui.browse.scheduler

import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ShareCompat
import androidx.databinding.Observable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.ui.browse.BrowseActivity
import com.ezt.priv.shortvideodownloader.ui.browse.shared.SharedPrefHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class BaseWebTabFragment : Fragment() {
    protected val browseActivity: BrowseActivity
        get() = requireActivity() as BrowseActivity

    @Inject
    lateinit var sharedPrefHelper: SharedPrefHelper


    abstract fun shareWebLink()

    abstract fun bookmarkCurrentUrl()

}