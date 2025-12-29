package com.project.hadeseye

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.project.hadeseye.dialog.ShowDialog

class ScreenshotPhishingActivity : AppCompatActivity() {
    
    private lateinit var btnSelectImage: Button
    private lateinit var btnAnalyzeText: Button
    private lateinit var etManualText: EditText
    private lateinit var ivScreenshot: ImageView
    private lateinit var tvResults: TextView
    private lateinit var showDialog: ShowDialog
    
    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val STORAGE_PERMISSION_CODE = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screenshot_phishing)
        
        showDialog = ShowDialog(this)
        
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnAnalyzeText = findViewById(R.id.btnAnalyzeText)
        etManualText = findViewById(R.id.etManualText)
        ivScreenshot = findViewById(R.id.ivScreenshot)
        tvResults = findViewById(R.id.tvResults)
        
        btnSelectImage.setOnClickListener {
            if (checkStoragePermission()) {
                selectImage()
            } else {
                requestStoragePermission()
            }
        }
        
        btnAnalyzeText.setOnClickListener {
            val text = etManualText.text.toString().trim()
            if (text.isNotEmpty()) {
                analyzeText(text)
            } else {
                Toast.makeText(this, "Please enter text to analyze", Toast.LENGTH_SHORT).show()
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
    
    private fun selectImage() {
        val intent = android.content.Intent()
        intent.type = "image/*"
        intent.action = android.content.Intent.ACTION_GET_CONTENT
        startActivityForResult(
            android.content.Intent.createChooser(intent, "Select Screenshot"),
            PICK_IMAGE_REQUEST
        )
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            val imageUri: Uri = data.data!!
            try {
                val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                ivScreenshot.setImageBitmap(bitmap)
                Toast.makeText(this, "Image loaded. Please manually extract text and paste below.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun analyzeText(text: String) {
        val loading = showDialog.loadingDialog("Analyzing for phishing...")
        
        Thread {
            try {
                val detector = PhishingDetector(this)
                val result = detector.analyzeText(text)
                
                runOnUiThread {
                    loading.dismissWithAnimation()
                    
                    if (result.success) {
                        displayResults(result)
                        
                        when (result.riskLevel) {
                            "Critical" -> showDialog.threatDialog(
                                "Critical Phishing Risk!",
                                "Risk Score: ${result.riskScore}/100\n\nDo NOT interact with this content!"
                            )
                            "High" -> showDialog.maliciousDialog(
                                "High Phishing Risk",
                                "Risk Score: ${result.riskScore}/100\n\nExercise extreme caution!"
                            )
                            "Medium" -> Toast.makeText(
                                this,
                                "Medium risk detected. Review carefully.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(this, result.error ?: "Analysis failed", Toast.LENGTH_SHORT).show()
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
    
    private fun displayResults(result: PhishingDetector.PhishingResult) {
        val resultText = buildString {
            append("=== Phishing Analysis ===\n\n")
            append("Risk Level: ${result.riskLevel}\n")
            append("Risk Score: ${result.riskScore}/100\n\n")
            
            if (result.detectedUrls.isNotEmpty()) {
                append("Detected URLs (${result.detectedUrls.size}):\n")
                result.detectedUrls.forEach { url ->
                    append("• $url\n")
                }
                append("\n")
            }
            
            if (result.suspiciousKeywords.isNotEmpty()) {
                append("Suspicious Keywords (${result.suspiciousKeywords.size}):\n")
                result.suspiciousKeywords.forEach { keyword ->
                    append("• $keyword\n")
                }
                append("\n")
            }
            
            append("Recommendations:\n")
            result.recommendations.forEach { rec ->
                append("• $rec\n")
            }
        }
        
        tvResults.text = resultText
    }
}
