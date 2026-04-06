package id.harissabil.hayah.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.harissabil.hayah.data.db.entity.KeywordCacheEntity

@Dao
interface KeywordCacheDao {
    @Query("SELECT * FROM keyword_cache WHERE keyword = :keyword")
    suspend fun getByKeyword(keyword: String): KeywordCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cache: KeywordCacheEntity)

    @Query("UPDATE keyword_cache SET last_shown_index = :index, last_shown_time = :time WHERE keyword = :keyword")
    suspend fun updateShownState(
        keyword: String,
        index: Int,
        time: Long,
    )
}
