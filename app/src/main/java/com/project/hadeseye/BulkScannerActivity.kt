package com.project.hadeseye

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.project.hadeseye.dialog.ShowDialog
import com.project.hadeseye.services.virusTotalServices.VTScanning

class BulkScannerActivity : AppCompatActivity() {
    
    private lateinit var etBulkUrls: EditText
    private lateinit var btnScanBulk: Button
    private lateinit var tvResults: TextView
    private lateinit var showDialog: ShowDialog
    private lateinit var vtScanning: VTScanning
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bulk_scanner)
        
        showDialog = ShowDialog(this)
        vtScanning = VTScanning()
        auth = FirebaseAuth.getInstance()
        
        etBulkUrls = findViewById(R.id.etBulkUrls)
        btnScanBulk = findViewById(R.id.btnScanBulk)
        tvResults = findViewById(R.id.tvResults)
        
        btnScanBulk.setOnClickListener {
            val input = etBulkUrls.text.toString().trim()
            
            if (input.isEmpty()) {
                showDialog.invalidDialog("Error", "Please enter URLs (one per line)")
                return@setOnClickListener
            }
            
            val urls = input.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            
            if (urls.isEmpty()) {
                showDialog.invalidDialog("Error", "No valid URLs found")
                return@setOnClickListener
            }
            
            scanBulkUrls(urls)
        }
    }
    
    private fun scanBulkUrls(urls: List<String>) {
        val loading = showDialog.loadingDialog("Scanning ${urls.size} URLs...")
        
        Thread {
            val results = mutableListOf<String>()
            var safeCount = 0
            var maliciousCount = 0
            var errorCount = 0
            
            urls.forEachIndexed { index, url ->
                try {
                    runOnUiThread {
                        loading.setTitleText("Scanning ${index + 1}/${urls.size}\n$url")
                    }
                    
                    val result = vtScanning.vt_url_scan(this, url)
                    val malicious = result["malicious"]?.toIntOrNull() ?: 0
                    val verdict = result["verdict"] ?: "Unknown"
                    
                    if (malicious >= 3) {
                        results.add("❌ $url\n   $verdict\n")
                        maliciousCount++
                        
                        // Save to Firebase
                        saveToFirebase(url, "Malicious", malicious)
                    } else if (malicious >= 1) {
                        results.add("⚠️ $url\n   $verdict\n")
                        maliciousCount++
                        
                        saveToFirebase(url, "Threat", malicious)
                    } else {
                        results.add("✅ $url\n   $verdict\n")
                        safeCount++
                        
                        saveToFirebase(url, "Safe", malicious)
                    }
                    
                    // Avoid API rate limiting
                    Thread.sleep(2000)
                    
                } catch (e: Exception) {
                    results.add("⚠️ $url\n   Error: ${e.message}\n")
                    errorCount++
                }
            }
            
            runOnUiThread {
                loading.dismissWithAnimation()
                
                val summary = buildString {
                    append("=== BULK SCAN RESULTS ===\n\n")
                    append("Total Scanned: ${urls.size}\n")
                    append("✅ Safe: $safeCount\n")
                    append("❌ Malicious: $maliciousCount\n")
                    append("⚠️ Errors: $errorCount\n\n")
                    append("=========================\n\n")
                    append(results.joinToString("\n"))
                }
                
                tvResults.text = summary
                
                if (maliciousCount > 0) {
                    showDialog.threatDialog(
                        "Scan Complete",
                        "$maliciousCount malicious URLs detected!"
                    )
                } else {
                    showDialog.successDialog(
                        "Scan Complete",
                        "All URLs are safe!"
                    )
                }
            }
        }.start()
    }
    
    private fun saveToFirebase(url: String, status: String, malicious: Int) {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        val databaseRef = FirebaseDatabase.getInstance(
            "https://hadeseye-c26c7-default-rtdb.firebaseio.com/"
        ).getReference("users/scans/$uid/scans")
        
        val scanId = databaseRef.push().key ?: return
        val scanData = mapOf(
            "url" to url,
            "status" to status,
            "malicious" to malicious,
            "timestamp" to System.currentTimeMillis()
        )
        
        databaseRef.child(scanId).setValue(scanData)
    }
}
