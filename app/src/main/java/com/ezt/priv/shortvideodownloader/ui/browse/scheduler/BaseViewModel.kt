package com.ezt.priv.shortvideodownloader.ui.browse.scheduler

import androidx.lifecycle.ViewModel

abstract class BaseViewModel : ViewModel() {

    abstract fun start()

    abstract fun stop()
}