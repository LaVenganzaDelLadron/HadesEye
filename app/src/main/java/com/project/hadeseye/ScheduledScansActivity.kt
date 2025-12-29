package com.project.hadeseye

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.project.hadeseye.dialog.ShowDialog
import com.project.hadeseye.services.virusTotalServices.VTScanning
import java.util.concurrent.TimeUnit

class ScheduledScansActivity : AppCompatActivity() {
    
    private lateinit var spinnerInterval: Spinner
    private lateinit var switchEnabled: Switch
    private lateinit var etUrlsToScan: EditText
    private lateinit var btnSaveSchedule: Button
    private lateinit var tvStatus: TextView
    private lateinit var showDialog: ShowDialog
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scheduled_scans)
        
        showDialog = ShowDialog(this)
        
        spinnerInterval = findViewById(R.id.spinnerInterval)
        switchEnabled = findViewById(R.id.switchEnabled)
        etUrlsToScan = findViewById(R.id.etUrlsToScan)
        btnSaveSchedule = findViewById(R.id.btnSaveSchedule)
        tvStatus = findViewById(R.id.tvStatus)
        
        // Setup interval spinner
        val intervals = arrayOf("Every 15 minutes", "Every 30 minutes", "Every 1 hour", "Every 6 hours", "Every 12 hours", "Daily")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intervals)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerInterval.adapter = adapter
        
        // Load saved preferences
        val prefs = getSharedPreferences("scheduled_scans", MODE_PRIVATE)
        switchEnabled.isChecked = prefs.getBoolean("enabled", false)
        etUrlsToScan.setText(prefs.getString("urls", ""))
        spinnerInterval.setSelection(prefs.getInt("interval", 2))
        
        updateStatus()
        
        btnSaveSchedule.setOnClickListener {
            saveSchedule()
        }
        
        switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (etUrlsToScan.text.toString().trim().isEmpty()) {
                    Toast.makeText(this, "Please add URLs to scan", Toast.LENGTH_SHORT).show()
                    switchEnabled.isChecked = false
                } else {
                    saveSchedule()
                }
            } else {
                cancelScheduledScans()
            }
        }
    }
    
    private fun saveSchedule() {
        val urls = etUrlsToScan.text.toString().trim()
        if (urls.isEmpty()) {
            Toast.makeText(this, "Please enter URLs to scan", Toast.LENGTH_SHORT).show()
            return
        }
        
        val interval = spinnerInterval.selectedItemPosition
        val enabled = switchEnabled.isChecked
        
        // Save preferences
        val prefs = getSharedPreferences("scheduled_scans", MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("enabled", enabled)
            putString("urls", urls)
            putInt("interval", interval)
            apply()
        }
        
        if (enabled) {
            scheduleScans(interval, urls)
            Toast.makeText(this, "Scheduled scans enabled", Toast.LENGTH_SHORT).show()
        } else {
            cancelScheduledScans()
            Toast.makeText(this, "Scheduled scans disabled", Toast.LENGTH_SHORT).show()
        }
        
        updateStatus()
    }
    
    private fun scheduleScans(intervalIndex: Int, urls: String) {
        val repeatInterval = when (intervalIndex) {
            0 -> 15L to TimeUnit.MINUTES
            1 -> 30L to TimeUnit.MINUTES
            2 -> 1L to TimeUnit.HOURS
            3 -> 6L to TimeUnit.HOURS
            4 -> 12L to TimeUnit.HOURS
            5 -> 24L to TimeUnit.HOURS
            else -> 1L to TimeUnit.HOURS
        }
        
        val data = Data.Builder()
            .putString("urls", urls)
            .build()
        
        val scanRequest = PeriodicWorkRequestBuilder<ScanWorker>(
            repeatInterval.first,
            repeatInterval.second
        )
            .setInputData(data)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "scheduled_scans",
            ExistingPeriodicWorkPolicy.REPLACE,
            scanRequest
        )
    }
    
    private fun cancelScheduledScans() {
        WorkManager.getInstance(this).cancelUniqueWork("scheduled_scans")
    }
    
    private fun updateStatus() {
        val prefs = getSharedPreferences("scheduled_scans", MODE_PRIVATE)
        val enabled = prefs.getBoolean("enabled", false)
        val urlCount = prefs.getString("urls", "")?.split("\n")?.size ?: 0
        
        tvStatus.text = if (enabled) {
            "Status: Active\nMonitoring $urlCount URL(s)"
        } else {
            "Status: Inactive"
        }
    }
    
    class ScanWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
        
        override fun doWork(): Result {
            val urls = inputData.getString("urls") ?: return Result.failure()
            val urlList = urls.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            
            var threatsFound = 0
            val results = mutableListOf<String>()
            
            for (url in urlList) {
                try {
                    val scanner = VTScanning()
                    val result = scanner.vt_url_scan(applicationContext, url)
                    val verdict = result["verdict"]?.lowercase() ?: "unknown"
                    results.add("$url: $verdict")

                    if (verdict.contains("malicious") || verdict.contains("threat") || verdict.contains("suspicious")) {
                        threatsFound++
                    }
                } catch (e: Exception) {
                    results.add("$url: error")
                }
                
                // Rate limiting
                Thread.sleep(2000)
            }
            
            // Send notification if threats found
            if (threatsFound > 0) {
                sendNotification(threatsFound, results)
            }
            
            return Result.success()
        }
        
        private fun sendNotification(threatsFound: Int, results: List<String>) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "scheduled_scans",
                    "Scheduled Scans",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }
            
            val notification = NotificationCompat.Builder(applicationContext, "scheduled_scans")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚠️ Threats Detected!")
                .setContentText("$threatsFound threat(s) found in scheduled scan")
                .setStyle(NotificationCompat.BigTextStyle().bigText(results.joinToString("\n")))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}
