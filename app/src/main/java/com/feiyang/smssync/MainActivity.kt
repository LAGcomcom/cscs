package com.feiyang.smssync

import android.Manifest
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.os.Bundle
import android.os.Environment
import android.provider.Telephony
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.*

fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)  // 确保 timestamp 是 Long 类型的毫秒时间戳
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(date)
}

class MainActivity : AppCompatActivity() {

    private val smsList = mutableListOf<Sms>()
    private lateinit var adapter: SmsAdapter
    private lateinit var observer: SmsObserver
    private lateinit var db: AppDatabase
    private var phoneNumber: String? = null

    // 权限请求 launcher
    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            Log.d("权限返回", "结果: $result")
            if (result.values.all { it }) {
                startObserve()
                loadSavedSms()
                initPhoneNumber()
            } else {
                Toast.makeText(this, "必须允许读取短信才能正常工作", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)

        adapter = SmsAdapter(smsList)
        val recycler = findViewById<RecyclerView>(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        val endpointInput = findViewById<EditText>(R.id.endpointInput)
        val endpointSaveButton = findViewById<Button>(R.id.endpointSaveButton)
        endpointInput.setText(getEndpoint())
        endpointSaveButton.setOnClickListener {
            val v = endpointInput.text.toString().trim()
            if (v.isNotEmpty()) {
                val prefs = getSharedPreferences("smssync_prefs", MODE_PRIVATE)
                prefs.edit().putString("sms_endpoint", v).apply()
                Toast.makeText(this, "后端地址已保存", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.exportButton).setOnClickListener {
            exportSmsToFile()
        }

        checkPermissionsAndStart()
    }

    // ✅ 权限检查逻辑
    private fun checkPermissionsAndStart() {
        val permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS
        )

        val denied = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (denied.isEmpty()) {
            startObserve()
            loadSavedSms()
            initPhoneNumber()
        } else {
            permissionLauncher.launch(permissions)
        }
    }
    fun Sms.toLocalSms(): LocalSms {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())


        return LocalSms(
            address = this.address,
            body = this.body,
            timestamp = this.date
        )
    }

    private fun loadSavedSms() {
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) {
                db.smsDao().getAll()
            }
            val converted = saved.map {
                Sms(
                    id = 0.toString(),
                    address = it.address,
                    body = it.body,
                    date = it.timestamp
                )
            }
            smsList.addAll(converted)
            adapter.submit(smsList.toList())
        }
    }



    private fun startObserve() {
        observer = SmsObserver(this) { sms ->
            smsList.add(0, sms)
            adapter.submit(smsList.toList())
            lifecycleScope.launch(Dispatchers.IO) {
                db.smsDao().insert(sms.toLocalSms())
            }
            uploadSms(sms)
        }
        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            observer
        )
    }


    private fun uploadSms(sms: Sms) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val json = JSONObject()
                        .put("address", sms.address)
                        .put("body", sms.body)
                        .put("timestamp", sms.date)
                        .put("phone_number", phoneNumber ?: JSONObject.NULL)

                    val body = json.toString()
                        .toRequestBody("application/json".toMediaType())

                    val request = Request.Builder()
                        .url(getEndpoint())
                        .post(body)
                        .build()

                    val client = OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build()

                    val response = client.newCall(request).execute()
                    response.close()
                }
            } catch (e: Exception) {
                Log.e("上传失败", e.toString())
            }
        }
    }

    private fun getEndpoint(): String {
        val prefs = getSharedPreferences("smssync_prefs", MODE_PRIVATE)
        val def = getString(R.string.sms_endpoint)
        return prefs.getString("sms_endpoint", def) ?: def
    }

    private fun exportSmsToFile() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "sms_export")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "sms_export.csv")
                val writer = FileWriter(file)
                writer.write("Address,Body,Timestamp\n")
                for (sms in smsList) {
                    writer.write("\"${sms.address}\",\"${sms.body}\",\"${sms.date}\"\n")
                }
                writer.flush()
                writer.close()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "已导出到: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun initPhoneNumber() {
        val prefs = getSharedPreferences("smssync_prefs", MODE_PRIVATE)
        val saved = prefs.getString("phone_number", null)
        if (!saved.isNullOrBlank()) {
            phoneNumber = saved
            return
        }
        val hasPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            val auto = getAutoPhoneNumber()
            if (!auto.isNullOrBlank()) {
                phoneNumber = auto
                prefs.edit().putString("phone_number", auto).apply()
            } else {
                promptManualPhoneNumber()
            }
        } else {
            promptManualPhoneNumber()
        }
    }

    private fun getAutoPhoneNumber(): String? {
        var num: String? = null
        try {
            val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            num = tm.line1Number
        } catch (_: Exception) {}
        if (num.isNullOrBlank()) {
            try {
                val sm = getSystemService(SubscriptionManager::class.java)
                val list = sm.activeSubscriptionInfoList
                num = list?.firstOrNull()?.number
            } catch (_: Exception) {}
        }
        return num?.trim()?.ifEmpty { null }
    }

    private fun promptManualPhoneNumber() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("设置手机号")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val v = input.text.toString().trim()
                if (v.isNotEmpty()) {
                    phoneNumber = v
                    val prefs = getSharedPreferences("smssync_prefs", MODE_PRIVATE)
                    prefs.edit().putString("phone_number", v).apply()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::observer.isInitialized) {
            contentResolver.unregisterContentObserver(observer)
        }
    }
}
