package com.ezt.priv.shortvideodownloader.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ezt.priv.shortvideodownloader.database.dao.ResultDao
import com.ezt.priv.shortvideodownloader.database.dao.HistoryDao
import com.ezt.priv.shortvideodownloader.database.dao.DownloadDao
import com.ezt.priv.shortvideodownloader.database.dao.CommandTemplateDao
import com.ezt.priv.shortvideodownloader.database.dao.SearchHistoryDao
import com.ezt.priv.shortvideodownloader.database.dao.CookieDao
import com.ezt.priv.shortvideodownloader.database.dao.LogDao
import com.ezt.priv.shortvideodownloader.database.dao.TerminalDao
import com.ezt.priv.shortvideodownloader.database.dao.ObserveSourcesDao
import com.ezt.priv.shortvideodownloader.database.dao.SegmentedVideoDao
import com.ezt.priv.shortvideodownloader.database.models.main.CommandTemplate
import com.ezt.priv.shortvideodownloader.database.models.main.CookieItem
import com.ezt.priv.shortvideodownloader.database.models.main.DownloadItem
import com.ezt.priv.shortvideodownloader.database.models.main.HistoryItem
import com.ezt.priv.shortvideodownloader.database.models.main.LogItem
import com.ezt.priv.shortvideodownloader.database.models.main.ResultItem
import com.ezt.priv.shortvideodownloader.database.models.main.SearchHistoryItem
import com.ezt.priv.shortvideodownloader.database.models.main.SegmentedVideo
import com.ezt.priv.shortvideodownloader.database.models.main.TemplateShortcut
import com.ezt.priv.shortvideodownloader.database.models.main.TerminalItem
import com.ezt.priv.shortvideodownloader.database.models.observeSources.ObserveSourcesItem
import com.ezt.priv.shortvideodownloader.ui.browse.qualifier.LocalHistoryItem
import com.ezt.priv.shortvideodownloader.ui.browse.repository.LocalHistoryDao

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
        LocalHistoryItem::class,
        SegmentedVideo::class,
    ],
    version = 3,
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
    abstract val segmentedVideoDao: SegmentedVideoDao

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
                    .addMigrations(MIGRATION_2_3)
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS segmented (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                totalSegments INTEGER NOT NULL
            )
            """.trimIndent()
                )
            }
        }



    }
}