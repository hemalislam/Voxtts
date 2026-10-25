package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.SpeechClip
import com.example.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [UserProfile::class, SpeechClip::class],
    version = 1,
    exportSchema = false
)
abstract class VoxCraftDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun speechClipDao(): SpeechClipDao

    companion object {
        @Volatile
        private var INSTANCE: VoxCraftDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): VoxCraftDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoxCraftDatabase::class.java,
                    "voxcraft_database.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialProfiles(database.userProfileDao())
                    }
                }
            }

            private suspend fun populateInitialProfiles(dao: UserProfileDao) {
                if (dao.getCount() == 0) {
                    val defaultProfile = UserProfile(
                        name = "Hemal Islam",
                        email = "hemalislamcoc@gmail.com",
                        avatarColorHex = "#0284C7",
                        defaultLanguageCode = "bn",
                        defaultVoiceStyle = "narrative",
                        defaultVoiceName = "Kore",
                        preferredAudioFormat = "wav",
                        speechRate = 1.0f,
                        pitch = 1.0f,
                        volume = 1.0f,
                        isActiveProfile = true
                    )
                    dao.insertProfile(defaultProfile)

                    val executiveProfile = UserProfile(
                        name = "Executive Pro",
                        email = "formal@voxcraft.studio",
                        avatarColorHex = "#6366F1",
                        defaultLanguageCode = "en-US",
                        defaultVoiceStyle = "formal",
                        defaultVoiceName = "Charon",
                        preferredAudioFormat = "mp3",
                        speechRate = 1.05f,
                        pitch = 0.95f,
                        volume = 1.0f,
                        isActiveProfile = false
                    )
                    dao.insertProfile(executiveProfile)

                    val spanishProfile = UserProfile(
                        name = "Estudio Español",
                        email = "espanol@voxcraft.studio",
                        avatarColorHex = "#EC4899",
                        defaultLanguageCode = "es",
                        defaultVoiceStyle = "casual",
                        defaultVoiceName = "Aoede",
                        preferredAudioFormat = "wav",
                        speechRate = 1.0f,
                        pitch = 1.02f,
                        volume = 1.0f,
                        isActiveProfile = false
                    )
                    dao.insertProfile(spanishProfile)
                }
            }
        }
    }
}
