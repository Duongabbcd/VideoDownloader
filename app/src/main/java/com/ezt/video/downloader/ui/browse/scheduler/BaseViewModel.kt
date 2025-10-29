package com.ezt.video.downloader.ui.browse.scheduler

import androidx.lifecycle.ViewModel

abstract class BaseViewModel : ViewModel() {

    abstract fun start()

    abstract fun stop()
}