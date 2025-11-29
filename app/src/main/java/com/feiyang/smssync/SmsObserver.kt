package com.feiyang.smssync

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony

class SmsObserver(
    context: Context,
    private val onSms: (Sms) -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private val contentResolver = context.contentResolver
    private val uri: Uri = Telephony.Sms.CONTENT_URI
    private var lastId = -1L            // 防止重复处理

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        queryLatest()
    }

    private fun queryLatest() {
        contentResolver.query(
            uri,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            ),
            null, null,
            "${Telephony.Sms.DEFAULT_SORT_ORDER} LIMIT 1"
        )?.use { c ->
            if (c.moveToFirst()) {
                val id = c.getLong(0)
                if (id != lastId) {
                    lastId = id
                    onSms(
                        Sms(
                            id.toString(),
                            c.getString(1) ?: "Unknown",
                            c.getString(2) ?: "",
                            c.getLong(3)
                        )
                    )
                }
            }
        }
    }
}
