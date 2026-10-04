package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {

    @Query("""
        SELECT * FROM programs 
        WHERE channelTvgId = :tvgId 
        ORDER BY startTime ASC
    """)
    fun getProgramsForChannel(tvgId: String): Flow<List<ProgramEntity>>

    @Query("""
        SELECT * FROM programs 
        WHERE channelTvgId = :tvgId 
          AND endTime >= :startRange 
          AND startTime <= :endRange 
        ORDER BY startTime ASC
    """)
    fun getProgramsForChannelInRange(tvgId: String, startRange: Long, endRange: Long): Flow<List<ProgramEntity>>

    @Query("""
        SELECT * FROM programs 
        WHERE endTime >= :startRange 
          AND startTime <= :endRange 
        ORDER BY startTime ASC
    """)
    fun getAllProgramsInRange(startRange: Long, endRange: Long): Flow<List<ProgramEntity>>

    @Query("""
        SELECT * FROM programs 
        WHERE channelTvgId = :tvgId 
          AND startTime <= :now 
          AND endTime > :now 
        LIMIT 1
    """)
    suspend fun getCurrentProgram(tvgId: String, now: Long): ProgramEntity?

    @Query("""
        SELECT * FROM programs 
        WHERE startTime <= :now 
          AND endTime > :now
    """)
    fun getCurrentProgramsForNow(now: Long): Flow<List<ProgramEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<ProgramEntity>)

    @Query("DELETE FROM programs WHERE endTime < :thresholdTime")
    suspend fun deleteOldPrograms(thresholdTime: Long): Int

    @Query("DELETE FROM programs")
    suspend fun deleteAllPrograms()

    @Query("SELECT COUNT(*) FROM programs")
    fun getProgramCount(): Flow<Int>
}
