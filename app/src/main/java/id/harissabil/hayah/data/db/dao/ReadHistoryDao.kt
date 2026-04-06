package id.harissabil.hayah.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.harissabil.hayah.data.db.entity.ReadHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReadHistoryEntity)

    @Query("SELECT COUNT(*) FROM read_history WHERE timestamp >= :sinceTimestamp")
    fun countEntriesSince(sinceTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM read_history")
    fun countAllEntries(): Flow<Int>
}
