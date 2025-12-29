package com.project.hadeseye

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.project.hadeseye.dialog.ShowDialog
import java.io.File

class APKScannerActivity : AppCompatActivity() {
    
    private lateinit var btnSelectApk: Button
    private lateinit var btnScanApk: Button
    private lateinit var tvApkPath: TextView
    private lateinit var tvResults: TextView
    private lateinit var showDialog: ShowDialog
    private var selectedApkPath: String? = null
    
    companion object {
        private const val PICK_APK_REQUEST = 1
        private const val STORAGE_PERMISSION_CODE = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apk_scanner)
        
        showDialog = ShowDialog(this)
        
        btnSelectApk = findViewById(R.id.btnSelectApk)
        btnScanApk = findViewById(R.id.btnScanApk)
        tvApkPath = findViewById(R.id.tvApkPath)
        tvResults = findViewById(R.id.tvResults)
        
        btnSelectApk.setOnClickListener {
            if (checkStoragePermission()) {
                selectApk()
            } else {
                requestStoragePermission()
            }
        }
        
        btnScanApk.setOnClickListener {
            if (selectedApkPath != null) {
                scanApk(selectedApkPath!!)
            } else {
                Toast.makeText(this, "Please select an APK file first", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE
        )
    }
    
    private fun selectApk() {
        val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT)
        intent.type = "application/vnd.android.package-archive"
        startActivityForResult(
            android.content.Intent.createChooser(intent, "Select APK File"),
            PICK_APK_REQUEST
        )
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == PICK_APK_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            val uri: Uri = data.data!!
            
            // Get actual file path
            val path = getPathFromUri(uri)
            if (path != null) {
                selectedApkPath = path
                tvApkPath.text = "Selected: ${File(path).name}"
            } else {
                Toast.makeText(this, "Could not access APK file", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun getPathFromUri(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndex("_data")
                if (columnIndex >= 0) {
                    return it.getString(columnIndex)
                }
            }
        }
        return uri.path
    }
    
    private fun scanApk(apkPath: String) {
        val loading = showDialog.loadingDialog("Scanning APK...")
        
        Thread {
            try {
                val scanner = APKScanner(this)
                val result = scanner.analyzeApk(apkPath)
                
                runOnUiThread {
                    loading.dismissWithAnimation()
                    
                    if (result.success) {
                        displayResults(result)
                        
                        when (result.riskLevel) {
                            "Critical" -> showDialog.threatDialog(
                                "Critical Risk Detected!",
                                "Risk Score: ${result.riskScore}/100\n\nDo NOT install this APK!"
                            )
                            "High" -> showDialog.maliciousDialog(
                                "High Risk APK",
                                "Risk Score: ${result.riskScore}/100\n\nExercise extreme caution!"
                            )
                            "Medium" -> Toast.makeText(
                                this,
                                "Medium risk detected. Review carefully.",
                                Toast.LENGTH_LONG
                            ).show()
                            else -> showDialog.successDialog(
                                "Low Risk",
                                "Risk Score: ${result.riskScore}/100\n\nAPK appears relatively safe."
                            )
                        }
                    } else {
                        Toast.makeText(this, result.error ?: "Scan failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    loading.dismissWithAnimation()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    
    private fun displayResults(result: APKScanner.APKResult) {
        val resultText = buildString {
            append("=== APK Security Analysis ===\n\n")
            append("Risk Level: ${result.riskLevel}\n")
            append("Risk Score: ${result.riskScore}/100\n\n")
            
            append("File Statistics:\n")
            append("• Total Files: ${result.totalFiles}\n")
            append("• DEX Files: ${result.dexCount}\n")
            append("• Native Libraries: ${result.nativeLibCount}\n")
            append("• Obfuscation: ${if (result.hasObfuscation) "Yes" else "No"}\n\n")
            
            if (result.securityIssues.isNotEmpty()) {
                append("Security Issues (${result.securityIssues.size}):\n")
                result.securityIssues.forEach { issue ->
                    append("⚠️ $issue\n")
                }
                append("\n")
            }
            
            if (result.suspiciousFiles.isNotEmpty()) {
                append("Suspicious Files (showing ${result.suspiciousFiles.size}):\n")
                result.suspiciousFiles.forEach { file ->
                    append("• $file\n")
                }
            }
        }
        
        tvResults.text = resultText
    }
}
