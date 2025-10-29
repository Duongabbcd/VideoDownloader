package com.ezt.video.downloader.ui.browse.module

import com.ezt.video.downloader.ui.browse.qualifier.data.LocalData
import com.ezt.video.downloader.ui.browse.qualifier.data.RemoteData
import com.ezt.video.downloader.ui.browse.repository.HistoryLocalDataSource
import com.ezt.video.downloader.ui.browse.repository.HistoryRepository
import com.ezt.video.downloader.ui.browse.repository.HistoryRepositoryImpl
import com.ezt.video.downloader.ui.browse.repository.VideoRemoteDataSource
import com.ezt.video.downloader.ui.browse.repository.VideoRepository
import com.ezt.video.downloader.ui.browse.repository.VideoRepositoryImpl
import com.ezt.video.downloader.ui.browse.repository.VideoService
import com.ezt.video.downloader.ui.browse.repository.VideoServiceLocal
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVideoService(videoServiceLocal: VideoServiceLocal): VideoService


    // ✅ Remote data source (network / API)
    @Binds
    @Singleton
    @RemoteData
    abstract fun bindVideoRemoteDataSource(
        remoteDataSource: VideoRemoteDataSource
    ): VideoRepository

    // ✅ Main repository (with caching)
    @Binds
    @Singleton
    abstract fun bindVideoRepositoryImpl(
        videoRepository: VideoRepositoryImpl
    ): VideoRepository

    // ✅ History repo is fine
    @Binds
    @Singleton
    abstract fun bindHistoryRepositoryImpl(
        historyRepository: HistoryRepositoryImpl
    ): HistoryRepository

    @Singleton
    @Binds
    @LocalData
    abstract fun bindHistoryLocalDataSource(localDataSource: HistoryLocalDataSource): HistoryRepository
}
