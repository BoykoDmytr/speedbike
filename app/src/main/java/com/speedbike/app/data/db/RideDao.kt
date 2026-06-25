package com.speedbike.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    @Insert
    suspend fun insert(ride: RideEntity): Long

    @Query("SELECT * FROM rides ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE id = :id")
    suspend fun getById(id: Long): RideEntity?

    @Query("DELETE FROM rides WHERE id = :id")
    suspend fun deleteById(id: Long)
}
