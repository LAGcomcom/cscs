package com.feiyang.smssync

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LocalSms::class], version = 2)  // <-- 把 version 从 1 改成 2
abstract class AppDatabase : RoomDatabase() {
    abstract fun smsDao(): SmsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sms_database"
                )
                    .fallbackToDestructiveMigration()  // <-- 加上这行
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
