package com.project.hadeseye

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.project.hadeseye.dialog.ShowDialog
import com.project.hadeseye.services.HashScanning

class HashScannerActivity : AppCompatActivity() {
    
    private lateinit var etHash: EditText
    private lateinit var btnScanHash: Button
    private lateinit var tvResult: TextView
    private lateinit var showDialog: ShowDialog
    private lateinit var hashScanning: HashScanning
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hash_scanner)
        
        showDialog = ShowDialog(this)
        hashScanning = HashScanning()
        
        etHash = findViewById(R.id.etHash)
        btnScanHash = findViewById(R.id.btnScanHash)
        tvResult = findViewById(R.id.tvResult)
        
        btnScanHash.setOnClickListener {
            val hash = etHash.text.toString().trim()
            
            if (hash.isEmpty()) {
                showDialog.invalidDialog("Error", "Please enter a hash value")
                return@setOnClickListener
            }
            
            if (!isValidHash(hash)) {
                showDialog.invalidDialog("Invalid Hash", "Please enter a valid MD5, SHA1, or SHA256 hash")
                return@setOnClickListener
            }
            
            scanHash(hash)
        }
    }
    
    private fun isValidHash(hash: String): Boolean {
        // MD5: 32 chars, SHA1: 40 chars, SHA256: 64 chars
        return when (hash.length) {
            32, 40, 64 -> hash.matches(Regex("^[a-fA-F0-9]+$"))
            else -> false
        }
    }
    
    private fun scanHash(hash: String) {
        val loading = showDialog.loadingDialog("Scanning hash...")
        
        Thread {
            try {
                val result = hashScanning.scanHash(this, hash)
                
                runOnUiThread {
                    loading.dismissWithAnimation()
                    
                    val status = result["status"]
                    val verdict = result["verdict"] ?: "Unknown"
                    val malicious = result["malicious"]?.toIntOrNull() ?: 0
                    
                    val resultText = buildString {
                        append("Hash: ${result["hash"]}\n\n")
                        append("Verdict: $verdict\n\n")
                        append("Malicious: ${result["malicious"]}\n")
                        append("Suspicious: ${result["suspicious"]}\n")
                        append("Harmless: ${result["harmless"]}\n")
                        append("Undetected: ${result["undetected"]}\n\n")
                        
                        if (status == "not_found") {
                            append("File Names: Not in database\n")
                        } else {
                            append("File Names: ${result["file_names"]}\n")
                            append("File Type: ${result["file_type"]}\n")
                            append("File Size: ${result["file_size"]} bytes")
                        }
                    }
                    
                    tvResult.text = resultText
                    
                    if (malicious >= 5) {
                        showDialog.threatDialog("🔥 Malicious Hash!", verdict)
                    } else if (malicious >= 2) {
                        showDialog.maliciousDialog("⚠️ Suspicious Hash", verdict)
                    } else {
                        showDialog.successDialog("✅ Clean Hash", verdict)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    loading.dismissWithAnimation()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
