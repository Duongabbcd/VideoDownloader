package com.ezt.priv.shortvideodownloader.ui.browse.module

import android.app.Application
import com.ezt.priv.shortvideodownloader.MyApplication
import com.ezt.priv.shortvideodownloader.database.VideoDownloadDB
import com.ezt.priv.shortvideodownloader.ui.browse.repository.LocalHistoryDao
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
