package com.ezt.priv.shortvideodownloader.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ezt.priv.shortvideodownloader.database.models.main.SegmentedVideo

@Dao
interface SegmentedVideoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: SegmentedVideo)

    @Query("SELECT * FROM segmented WHERE title=:name LIMIT 1")
    suspend fun getSegmentedVideo(name: String): List<SegmentedVideo>
}