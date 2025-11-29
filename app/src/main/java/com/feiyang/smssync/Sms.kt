package com.feiyang.smssync

data class Sms(
    val id: String,
    val address: String,
    val body: String,
    val date: Long
)
