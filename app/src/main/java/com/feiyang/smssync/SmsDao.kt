package com.feiyang.smssync

import androidx.room.*

@Dao
interface SmsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sms: LocalSms)

    @Query("SELECT * FROM sms ORDER BY timestamp DESC")
    suspend fun getAll(): List<LocalSms>

    @Query("DELETE FROM sms")
    suspend fun clearAll()
}
