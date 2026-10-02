package com.example.data.local

import androidx.room.*
import com.example.data.model.DigitizedPaper
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperDao {
    @Query("SELECT * FROM digitized_papers ORDER BY timestamp DESC")
    fun getAllPapers(): Flow<List<DigitizedPaper>>

    @Query("SELECT * FROM digitized_papers WHERE id = :id")
    suspend fun getPaperById(id: Int): DigitizedPaper?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaper(paper: DigitizedPaper): Long

    @Update
    suspend fun updatePaper(paper: DigitizedPaper)

    @Delete
    suspend fun deletePaper(paper: DigitizedPaper)

    @Query("DELETE FROM digitized_papers WHERE id = :id")
    suspend fun deletePaperById(id: Int)
}
