package com.ezt.video.downloader.ui.browse.module

import com.ezt.video.downloader.ui.browse.qualifier.data.LocalData
import com.ezt.video.downloader.ui.browse.qualifier.data.RemoteData
import com.ezt.video.downloader.ui.browse.repository.HistoryRepository
import com.ezt.video.downloader.ui.browse.repository.HistoryRepositoryImpl
import com.ezt.video.downloader.ui.browse.repository.VideoRepository
import com.ezt.video.downloader.ui.browse.repository.VideoRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

//    @Singleton
//    @Binds
//    @LocalData
//    abstract fun bindConfigLocalDataSource(localDataSource: ConfigLocalDataSource): ConfigRepository
//
//    @Singleton
//    @Binds
//    @RemoteData
//    abstract fun bindConfigRemoteDataSource(remoteDataSource: ConfigRemoteDataSource): ConfigRepository
//
//    @Singleton
//    @Binds
//    abstract fun bindConfigRepositoryImpl(configRepository: ConfigRepositoryImpl): ConfigRepository
//
//    @Singleton
//    @Binds
//    @LocalData
//    abstract fun bindTopPagesLocalDataSource(localDataSource: TopPagesLocalDataSource): TopPagesRepository
//
//    @Singleton
//    @Binds
//    @RemoteData
//    abstract fun bindTopPagesRemoteDataSource(remoteDataSource: TopPagesRemoteDataSource): TopPagesRepository
//
//    @Singleton
//    @Binds
//    abstract fun bindTopPagesRepositoryImpl(topPagesRepository: TopPagesRepositoryImpl): TopPagesRepository
//
//    @Singleton
//    @Binds
//    @LocalData
//    abstract fun bindVideoLocalDataSource(localDataSource: VideoLocalDataSource): VideoRepository
//
//    @Singleton
//    @Binds
//    @RemoteData
//    abstract fun bindVideoRemoteDataSource(remoteDataSource: VideoRemoteDataSource): VideoRepository

    @Singleton
    @Binds
    abstract fun bindVideoRepositoryImpl(videoRepository: VideoRepositoryImpl): VideoRepository
//
//    @Singleton
//    @Binds
//    @LocalData
//    abstract fun bindProgressLocalDataSource(localDataSource: ProgressLocalDataSource): ProgressRepository
//
//    @Singleton
//    @Binds
//    @LocalData
//    abstract fun bindHistoryLocalDataSource(localDataSource: HistoryLocalDataSource): HistoryRepository
//
//
//    @Singleton
//    @Binds
//    abstract fun bindProgressRepositoryImpl(progressRepository: ProgressRepositoryImpl): ProgressRepository

    @Singleton
    @Binds
    abstract fun bindHistoryRepositoryImpl(historyRepository: HistoryRepositoryImpl): HistoryRepository

//    @Singleton
//    @Binds
//    @LocalData
//    abstract fun bindAdBlockHostsLocalDataSource(adBlockHostsLocalDataSource: AdBlockHostsLocalDataSource): AdBlockHostsRepository
//
//    @Singleton
//    @Binds
//    @RemoteData
//    abstract fun bindAdBlockHostsRemoteDataSource(adBlockHostsRemoteDataSource: AdBlockHostsRemoteDataSource): AdBlockHostsRepository
//    @Singleton
//    @Binds
//    abstract fun bindAdBlockHostsRepositoryImpl(adBlockHostsRepository: AdBlockHostsRepositoryImpl): AdBlockHostsRepository

}