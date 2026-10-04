package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels ORDER BY orderIndex ASC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE groupTitle = :group ORDER BY orderIndex ASC")
    fun getChannelsByGroup(group: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY orderIndex ASC")
    fun getFavoriteChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT groupTitle FROM channels WHERE groupTitle != '' ORDER BY groupTitle ASC")
    fun getDistinctGroups(): Flow<List<String>>

    @Query("""
        SELECT * FROM channels 
        WHERE channelName LIKE '%' || :query || '%' 
           OR groupTitle LIKE '%' || :query || '%' 
           OR tvgName LIKE '%' || :query || '%'
        ORDER BY orderIndex ASC
    """)
    fun searchChannels(query: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE streamUrl = :streamUrl LIMIT 1")
    suspend fun getChannelByUrl(streamUrl: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE streamUrl = :streamUrl")
    suspend fun updateFavorite(streamUrl: String, isFavorite: Boolean)

    @Query("DELETE FROM channels")
    suspend fun deleteAllChannels()

    @Query("SELECT COUNT(*) FROM channels")
    fun getChannelCount(): Flow<Int>
}
