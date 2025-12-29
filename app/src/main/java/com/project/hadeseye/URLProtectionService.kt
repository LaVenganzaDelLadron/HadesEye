package com.project.hadeseye

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import com.project.hadeseye.services.virusTotalServices.VTScanning

class URLProtectionService : Service() {
    
    private lateinit var clipboardManager: ClipboardManager
    private var lastCheckedUrl: String? = null
    
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkClipboard()
    }
    
    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun checkClipboard() {
        val clipData = clipboardManager.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text?.toString()
            
            if (text != null && isUrl(text) && text != lastCheckedUrl) {
                lastCheckedUrl = text
                scanUrl(text)
            }
        }
    }
    
    private fun isUrl(text: String): Boolean {
        return text.startsWith("http://", ignoreCase = true) ||
               text.startsWith("https://", ignoreCase = true) ||
               text.matches(Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*"))
    }
    
    private fun scanUrl(url: String) {
        Thread {
            try {
                val scanner = VTScanning()
                val result = scanner.vt_url_scan(this, url)

                val verdict = result["verdict"]?.lowercase() ?: "unknown"
                when {
                    verdict.contains("malicious") || verdict.contains("threat") -> {
                        showNotification(
                            "⚠️ THREAT DETECTED",
                            "The copied URL is DANGEROUS!\n$url",
                            true
                        )
                    }
                    verdict.contains("suspicious") -> {
                        showNotification(
                            "⚠️ SUSPICIOUS URL",
                            "The copied URL may be malicious:\n$url",
                            true
                        )
                    }
                    verdict.contains("safe") -> {
                        showNotification(
                            "✓ URL Safe",
                            "The copied URL appears safe:\n$url",
                            false
                        )
                    }
                }
            } catch (e: Exception) {
                // Silent fail - don't interrupt user
            }
        }.start()
    }
    
    private fun showNotification(title: String, message: String, isWarning: Boolean) {
        // Show toast for immediate feedback
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(this, "$title\n$message", Toast.LENGTH_LONG).show()
        }
        
        // Could also create a proper notification here
        // Using NotificationManager and NotificationCompat.Builder
    }
    
    override fun onDestroy() {
        super.onDestroy()
        clipboardManager.removePrimaryClipChangedListener(clipboardListener)
    }
}
