package com.example.chess.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPositionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(position: SavedPositionEntity): Long

    @Update
    suspend fun update(position: SavedPositionEntity)

    @Delete
    suspend fun delete(position: SavedPositionEntity)

    @Query("DELETE FROM saved_positions")
    suspend fun clearAll()

    @Query("SELECT * FROM saved_positions ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<SavedPositionEntity>>

    @Query("SELECT * FROM saved_positions WHERE title LIKE '%' || :query || '%' OR fen LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchPositions(query: String): Flow<List<SavedPositionEntity>>

    @Query("SELECT COUNT(*) FROM saved_positions")
    suspend fun getCount(): Int
}
