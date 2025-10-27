package com.ezt.video.downloader.ui.browse.module

import android.app.Application
import android.content.Context
import com.ezt.video.downloader.MyApplication
import com.ezt.video.downloader.ui.browse.scheduler.BaseSchedulers
import com.ezt.video.downloader.ui.browse.scheduler.BaseSchedulersImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @ApplicationContext
    abstract fun bindApplicationContext(application: MyApplication): Context

    @Binds
    abstract fun bindApplication(application: MyApplication): Application

    @Singleton
    @Binds
    abstract fun bindBaseSchedulers(baseSchedulers: BaseSchedulersImpl): BaseSchedulers

}