package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SpeechClip
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeechClipDao {
    @Query("SELECT * FROM speech_clips ORDER BY createdAt DESC")
    fun getAllClips(): Flow<List<SpeechClip>>

    @Query("SELECT * FROM speech_clips WHERE userProfileId = :profileId ORDER BY createdAt DESC")
    fun getClipsForProfile(profileId: Long): Flow<List<SpeechClip>>

    @Query("SELECT * FROM speech_clips WHERE id = :id LIMIT 1")
    suspend fun getClipById(id: Long): SpeechClip?

    @Query("SELECT * FROM speech_clips WHERE languageCode = :langCode ORDER BY createdAt DESC")
    fun getClipsByLanguage(langCode: String): Flow<List<SpeechClip>>

    @Query("SELECT * FROM speech_clips WHERE voiceStyle = :style ORDER BY createdAt DESC")
    fun getClipsByStyle(style: String): Flow<List<SpeechClip>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: SpeechClip): Long

    @Update
    suspend fun updateClip(clip: SpeechClip)

    @Delete
    suspend fun deleteClip(clip: SpeechClip)

    @Query("DELETE FROM speech_clips WHERE id = :id")
    suspend fun deleteClipById(id: Long)

    @Query("SELECT SUM(fileSizeBytes) FROM speech_clips")
    fun getTotalStorageUsed(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM speech_clips")
    fun getTotalClipCount(): Flow<Int>

    @Query("DELETE FROM speech_clips")
    suspend fun clearAll()
}
