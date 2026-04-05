package id.harissabil.hayah.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "read_history")
data class ReadHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val pageNumber: Int,
    val timestamp: Long,
    val totalTimeReadSeconds: Int,
)
