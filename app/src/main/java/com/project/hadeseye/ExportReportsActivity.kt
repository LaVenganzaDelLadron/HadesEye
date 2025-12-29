package com.project.hadeseye

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.project.hadeseye.dialog.ShowDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExportReportsActivity : AppCompatActivity() {
    
    private lateinit var btnExportPdf: Button
    private lateinit var btnExportCsv: Button
    private lateinit var tvStatus: TextView
    private lateinit var showDialog: ShowDialog
    private lateinit var auth: FirebaseAuth
    
    companion object {
        private const val STORAGE_PERMISSION_CODE = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_reports)
        
        showDialog = ShowDialog(this)
        auth = FirebaseAuth.getInstance()
        
        btnExportPdf = findViewById(R.id.btnExportPdf)
        btnExportCsv = findViewById(R.id.btnExportCsv)
        tvStatus = findViewById(R.id.tvStatus)
        
        btnExportPdf.setOnClickListener {
            if (checkStoragePermission()) {
                exportToPdf()
            } else {
                requestStoragePermission()
            }
        }
        
        btnExportCsv.setOnClickListener {
            if (checkStoragePermission()) {
                exportToCsv()
            } else {
                requestStoragePermission()
            }
        }
    }
    
    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE
        )
    }
    
    private fun exportToPdf() {
        val loading = showDialog.loadingDialog("Generating PDF...")
        
        Thread {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    runOnUiThread {
                        loading.dismissWithAnimation()
                        Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }
                
                val uid = currentUser.uid
                val databaseRef = FirebaseDatabase.getInstance(
                    "https://hadeseye-c26c7-default-rtdb.firebaseio.com/"
                ).getReference("users/scans/$uid/scans")
                
                databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                .format(Date())
                            val fileName = "HadesEye_Report_$timestamp.pdf"
                            val filePath = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                fileName
                            )
                            
                            val writer = PdfWriter(filePath)
                            val pdfDoc = PdfDocument(writer)
                            val document = Document(pdfDoc)
                            
                            // Add title
                            document.add(Paragraph("HadesEye Security Report").setBold().setFontSize(20f))
                            document.add(Paragraph("Generated: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}"))
                            document.add(Paragraph("\n"))
                            
                            // Add scan results
                            var safeCount = 0
                            var threatCount = 0
                            var maliciousCount = 0
                            
                            for (scan in snapshot.children) {
                                val status = scan.child("status").getValue(String::class.java) ?: "Unknown"
                                val url = scan.child("url").getValue(String::class.java) ?: "N/A"
                                val fileName = scan.child("file_name").getValue(String::class.java) ?: "N/A"
                                val ip = scan.child("ip").getValue(String::class.java) ?: "N/A"
                                val domain = scan.child("domain").getValue(String::class.java) ?: "N/A"
                                val timestampValue = scan.child("timestamp").value
                                val date = timestampValue?.toString() ?: "Unknown"
                                
                                when (status) {
                                    "Safe" -> safeCount++
                                    "Threat" -> threatCount++
                                    "Malicious" -> maliciousCount++
                                }
                                
                                val scanInfo = buildString {
                                    append("Status: $status\n")
                                    if (url != "N/A") append("URL: $url\n")
                                    if (fileName != "N/A") append("File: $fileName\n")
                                    if (ip != "N/A") append("IP: $ip\n")
                                    if (domain != "N/A") append("Domain: $domain\n")
                                    append("Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(date.toLong()))}\n")
                                }
                                
                                document.add(Paragraph(scanInfo))
                                document.add(Paragraph("---\n"))
                            }
                            
                            // Add summary
                            document.add(Paragraph("\n=== Summary ===").setBold())
                            document.add(Paragraph("Total Scans: ${snapshot.childrenCount}"))
                            document.add(Paragraph("Safe: $safeCount"))
                            document.add(Paragraph("Threats: $threatCount"))
                            document.add(Paragraph("Malicious: $maliciousCount"))
                            
                            document.close()
                            
                            runOnUiThread {
                                loading.dismissWithAnimation()
                                tvStatus.text = "PDF exported to:\n${filePath.absolutePath}"
                                showDialog.successDialog("Export Complete", "PDF saved to Downloads folder")
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                loading.dismissWithAnimation()
                                Toast.makeText(this@ExportReportsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        runOnUiThread {
                            loading.dismissWithAnimation()
                            Toast.makeText(this@ExportReportsActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                })
                
            } catch (e: Exception) {
                runOnUiThread {
                    loading.dismissWithAnimation()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    
    private fun exportToCsv() {
        val loading = showDialog.loadingDialog("Generating CSV...")
        
        Thread {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    runOnUiThread {
                        loading.dismissWithAnimation()
                        Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }
                
                val uid = currentUser.uid
                val databaseRef = FirebaseDatabase.getInstance(
                    "https://hadeseye-c26c7-default-rtdb.firebaseio.com/"
                ).getReference("users/scans/$uid/scans")
                
                databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                .format(Date())
                            val fileName = "HadesEye_Report_$timestamp.csv"
                            val filePath = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                fileName
                            )
                            
                            val csv = StringBuilder()
                            csv.append("Status,URL,File,IP,Domain,Date\n")
                            
                            for (scan in snapshot.children) {
                                val status = scan.child("status").getValue(String::class.java) ?: "Unknown"
                                val url = scan.child("url").getValue(String::class.java) ?: "N/A"
                                val file = scan.child("file_name").getValue(String::class.java) ?: "N/A"
                                val ip = scan.child("ip").getValue(String::class.java) ?: "N/A"
                                val domain = scan.child("domain").getValue(String::class.java) ?: "N/A"
                                val timestampValue = scan.child("timestamp").value
                                val date = timestampValue?.toString() ?: "Unknown"
                                
                                csv.append("$status,\"$url\",\"$file\",\"$ip\",\"$domain\",$date\n")
                            }
                            
                            filePath.writeText(csv.toString())
                            
                            runOnUiThread {
                                loading.dismissWithAnimation()
                                tvStatus.text = "CSV exported to:\n${filePath.absolutePath}"
                                showDialog.successDialog("Export Complete", "CSV saved to Downloads folder")
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                loading.dismissWithAnimation()
                                Toast.makeText(this@ExportReportsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        runOnUiThread {
                            loading.dismissWithAnimation()
                            Toast.makeText(this@ExportReportsActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                })
                
            } catch (e: Exception) {
                runOnUiThread {
                    loading.dismissWithAnimation()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
