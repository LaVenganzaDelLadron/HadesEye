package com.project.hadeseye

import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.SharedPreferences

class URLProtectionActivity : AppCompatActivity() {
    
    private lateinit var switchProtection: Switch
    private lateinit var btnTestUrl: Button
    private lateinit var prefs: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_url_protection)
        
        prefs = getSharedPreferences("url_protection", MODE_PRIVATE)
        
        switchProtection = findViewById(R.id.switchProtection)
        btnTestUrl = findViewById(R.id.btnTestUrl)
        
        // Load saved state
        val isEnabled = prefs.getBoolean("protection_enabled", false)
        switchProtection.isChecked = isEnabled
        
        if (isEnabled) {
            startProtectionService()
        }
        
        switchProtection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startProtectionService()
                Toast.makeText(this, "Real-time URL protection enabled", Toast.LENGTH_SHORT).show()
            } else {
                stopProtectionService()
                Toast.makeText(this, "Real-time URL protection disabled", Toast.LENGTH_SHORT).show()
            }
            
            prefs.edit().putBoolean("protection_enabled", isChecked).apply()
        }
        
        btnTestUrl.setOnClickListener {
            // Copy a test malicious URL to clipboard
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("test", "http://malware-test.example.com")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Test URL copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun startProtectionService() {
        val intent = Intent(this, URLProtectionService::class.java)
        startService(intent)
    }
    
    private fun stopProtectionService() {
        val intent = Intent(this, URLProtectionService::class.java)
        stopService(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (!switchProtection.isChecked) {
            stopProtectionService()
        }
    }
}
