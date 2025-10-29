package com.ezt.video.downloader.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ezt.video.downloader.database.dao.ResultDao
import com.ezt.video.downloader.database.dao.HistoryDao
import com.ezt.video.downloader.database.dao.DownloadDao
import com.ezt.video.downloader.database.dao.CommandTemplateDao
import com.ezt.video.downloader.database.dao.SearchHistoryDao
import com.ezt.video.downloader.database.dao.CookieDao
import com.ezt.video.downloader.database.dao.LogDao
import com.ezt.video.downloader.database.dao.TerminalDao
import com.ezt.video.downloader.database.dao.ObserveSourcesDao
import com.ezt.video.downloader.database.models.main.CommandTemplate
import com.ezt.video.downloader.database.models.main.CookieItem
import com.ezt.video.downloader.database.models.main.DownloadItem
import com.ezt.video.downloader.database.models.main.HistoryItem
import com.ezt.video.downloader.database.models.main.LogItem
import com.ezt.video.downloader.database.models.main.ResultItem
import com.ezt.video.downloader.database.models.main.SearchHistoryItem
import com.ezt.video.downloader.database.models.main.TemplateShortcut
import com.ezt.video.downloader.database.models.main.TerminalItem
import com.ezt.video.downloader.database.models.observeSources.ObserveSourcesItem
import com.ezt.video.downloader.ui.browse.qualifier.LocalHistoryItem
import com.ezt.video.downloader.ui.browse.repository.LocalHistoryDao

@TypeConverters(Converters::class)
@Database(
    entities = [
        ResultItem::class,
        HistoryItem::class,
        DownloadItem::class,
        CommandTemplate::class,
        SearchHistoryItem::class,
        TemplateShortcut::class,
        CookieItem::class,
        LogItem::class,
        TerminalItem::class,
        ObserveSourcesItem::class,
        LocalHistoryItem::class
    ],
    version = 2,
    autoMigrations = [], exportSchema = true
)
abstract class VideoDownloadDB : RoomDatabase() {
    abstract val resultDao : ResultDao
    abstract val historyDao : HistoryDao
    abstract val downloadDao : DownloadDao
    abstract val commandTemplateDao : CommandTemplateDao
    abstract val searchHistoryDao: SearchHistoryDao
    abstract val cookieDao: CookieDao
    abstract val logDao: LogDao
    abstract val terminalDao: TerminalDao
    abstract val observeSourcesDao: ObserveSourcesDao
    abstract val localHistoryDao: LocalHistoryDao

    enum class SORTING{
        DESC, ASC
    }
    companion object {
        //prevents multiple instances of db getting created at the same time
        @Volatile
        private var instance : VideoDownloadDB? = null
        //if its not null return it, otherwise create db
        fun getInstance(context: Context) : VideoDownloadDB {
            return instance ?: synchronized(this){

                val dbInstance = Room.databaseBuilder(
                    context.applicationContext,
                    VideoDownloadDB::class.java,
                    "VideoDownloaderDB"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                instance = dbInstance
                dbInstance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS LocalHistoryItem (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT,
                url TEXT NOT NULL,
                datetime INTEGER NOT NULL,
                favicon BLOB
            )
            """.trimIndent()
                )
            }
        }


    }
}