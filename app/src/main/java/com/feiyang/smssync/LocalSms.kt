package com.feiyang.smssync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms")
data class LocalSms(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val address: String,
    val body: String,
    val timestamp: Long
)
