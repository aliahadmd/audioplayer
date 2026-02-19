package me.aliahad.audioplayer

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "timestamp_bookmarks")
data class TimestampBookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val audioFileUri: String,
    val folderUri: String,
    val positionMs: Long,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface TimestampDao {
    @Query("SELECT * FROM timestamp_bookmarks WHERE audioFileUri = :audioFileUri AND folderUri = :folderUri ORDER BY positionMs ASC")
    fun getBookmarksForTrack(audioFileUri: String, folderUri: String): Flow<List<TimestampBookmark>>

    @Insert
    suspend fun insert(bookmark: TimestampBookmark)

    @Query("DELETE FROM timestamp_bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(entities = [TimestampBookmark::class], version = 1, exportSchema = false)
abstract class TimestampDatabase : RoomDatabase() {
    abstract fun timestampDao(): TimestampDao

    companion object {
        @Volatile
        private var instance: TimestampDatabase? = null

        fun getInstance(context: Context): TimestampDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TimestampDatabase::class.java,
                    "timestamp_bookmarks.db"
                ).build().also { instance = it }
            }
    }
}
