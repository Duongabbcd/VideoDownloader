package com.ezt.video.downloader.ui.browse.module

import android.app.Application
import com.ezt.video.downloader.MyApplication
import com.ezt.video.downloader.database.VideoDownloadDB
import com.ezt.video.downloader.ui.browse.repository.LocalHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(app: Application): VideoDownloadDB {
        return VideoDownloadDB.getInstance(app)
    }

    @Provides
    fun provideLocalHistoryDao(db: VideoDownloadDB): LocalHistoryDao {
        return db.localHistoryDao // or db.historyDao() if it's a function
    }
}
