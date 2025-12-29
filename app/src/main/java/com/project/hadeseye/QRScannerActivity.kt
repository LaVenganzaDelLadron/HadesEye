package com.project.hadeseye

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.project.hadeseye.dialog.ShowDialog
import com.project.hadeseye.services.virusTotalServices.VTScanning
import com.project.hadeseye.services.virusTotalServices.urlServices.URLScanning

class QRScannerActivity : AppCompatActivity() {
    
    private lateinit var codeScanner: CodeScanner
    private lateinit var scannerView: CodeScannerView
    private lateinit var tvScannedUrl: TextView
    private lateinit var btnScanUrl: Button
    private lateinit var showDialog: ShowDialog
    private lateinit var vtScanning: VTScanning
    private lateinit var urlScanning: URLScanning
    
    private var scannedUrl: String = ""
    
    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)
        
        showDialog = ShowDialog(this)
        vtScanning = VTScanning()
        urlScanning = URLScanning()
        
        scannerView = findViewById(R.id.scanner_view)
        tvScannedUrl = findViewById(R.id.tvScannedUrl)
        btnScanUrl = findViewById(R.id.btnScanUrl)
        
        if (checkCameraPermission()) {
            setupScanner()
        } else {
            requestCameraPermission()
        }
        
        btnScanUrl.setOnClickListener {
            if (scannedUrl.isNotEmpty()) {
                scanUrl(scannedUrl)
            } else {
                Toast.makeText(this, "No URL scanned yet", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupScanner() {
        codeScanner = CodeScanner(this, scannerView)
        
        codeScanner.decodeCallback = DecodeCallback { result ->
            runOnUiThread {
                scannedUrl = result.text
                tvScannedUrl.text = "Scanned: $scannedUrl"
                
                // Show preview dialog
                showDialog.successDialog(
                    "QR Code Detected",
                    "URL: $scannedUrl\n\nWould you like to scan this URL for threats?",
                    "Scan Now"
                ) {
                    scanUrl(scannedUrl)
                }
            }
        }
        
        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }
    }
    
    private fun scanUrl(url: String) {
        val loading = showDialog.loadingDialog("Scanning URL...")
        
        Thread {
            try {
                val vtResult = vtScanning.vt_url_scan(this, url)
                val usResult = urlScanning.us_url_scan(this, url)
                
                runOnUiThread {
                    loading.dismissWithAnimation()
                    
                    val malicious = vtResult["malicious"]?.toIntOrNull() ?: 0
                    val verdict = vtResult["verdict"] ?: "Unknown"
                    
                    if (malicious >= 3) {
                        showDialog.threatDialog("⚠️ Dangerous URL!", "$verdict\n\n$url")
                    } else if (malicious >= 1) {
                        showDialog.maliciousDialog("⚠️ Suspicious URL", "$verdict\n\n$url")
                    } else {
                        showDialog.successDialog("✅ Safe URL", "$verdict\n\n$url")
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
    
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupScanner()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (::codeScanner.isInitialized) {
            codeScanner.startPreview()
        }
    }
    
    override fun onPause() {
        if (::codeScanner.isInitialized) {
            codeScanner.releaseResources()
        }
        super.onPause()
    }
}
