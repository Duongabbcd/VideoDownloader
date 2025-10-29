package com.ezt.priv.shortvideodownloader.ui.browse.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ezt.priv.shortvideodownloader.ui.browse.qualifier.LocalHistoryItem
import com.ezt.priv.shortvideodownloader.ui.browse.qualifier.data.LocalData
import io.reactivex.rxjava3.core.Flowable
import javax.inject.Inject
import javax.inject.Singleton


interface HistoryRepository {
    fun getAllHistory() : Flowable<List<LocalHistoryItem>>

    fun saveHistory(history: LocalHistoryItem)

    fun deleteHistory(history: LocalHistoryItem)

    fun deleteAllHistory()
}

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    @LocalData private val localDataSource: HistoryRepository
) : HistoryRepository {
    override fun getAllHistory(): Flowable<List<LocalHistoryItem>> {
        return localDataSource.getAllHistory()
    }

    override fun saveHistory(history: LocalHistoryItem) {
        localDataSource.saveHistory(history)
    }

    override fun deleteHistory(history: LocalHistoryItem) {
        localDataSource.deleteHistory(history)
    }

    override fun deleteAllHistory() {
        localDataSource.deleteAllHistory()
    }

}

class HistoryLocalDataSource @Inject constructor(
    private val historyDao: LocalHistoryDao
) : HistoryRepository {
    override fun getAllHistory() = historyDao.getHistory()
    override fun saveHistory(history: LocalHistoryItem) = historyDao.insertHistoryItem(history)
    override fun deleteHistory(history: LocalHistoryItem) = historyDao.deleteHistoryItem(history)
    override fun deleteAllHistory() = historyDao.clear()
}


@Dao
interface LocalHistoryDao {

    @Query("SELECT * FROM LocalHistoryItem")
    fun getHistory(): Flowable<List<LocalHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHistoryItem(item: LocalHistoryItem)

    @Delete
    fun deleteHistoryItem(localHistoryItem: LocalHistoryItem)

    @Query("DELETE FROM LocalHistoryItem")
    fun clear()
}