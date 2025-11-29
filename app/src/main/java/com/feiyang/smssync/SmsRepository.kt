package com.feiyang.smssync

class SmsRepository(private val dao: SmsDao) {
    suspend fun insert(sms: LocalSms) = dao.insert(sms)
    suspend fun getAll(): List<LocalSms> = dao.getAll()
    suspend fun clearAll() = dao.clearAll()
}
