package com.fidzzz.wifisystem.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "activity")
data class ActivityEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val title: String,
    val detail: String,
    val ok: Boolean
)

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity ORDER BY ts DESC LIMIT 500")
    fun all(): Flow<List<ActivityEntry>>

    @Insert
    suspend fun insert(e: ActivityEntry)

    @Query("DELETE FROM activity")
    suspend fun clear()
}

@Database(entities = [ActivityEntry::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): ActivityDao
}
