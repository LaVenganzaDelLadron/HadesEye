package com.project.hadeseye

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.project.hadeseye.dialog.ShowDialog
import com.project.hadeseye.services.NetworkScanner

class NetworkScannerActivity : AppCompatActivity() {
    
    private lateinit var etTargetIp: EditText
    private lateinit var btnGetLocalIp: Button
    private lateinit var btnScanNetwork: Button
    private lateinit var tvResult: TextView
    private lateinit var showDialog: ShowDialog
    private lateinit var networkScanner: NetworkScanner
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_scanner)
        
        showDialog = ShowDialog(this)
        networkScanner = NetworkScanner()
        
        etTargetIp = findViewById(R.id.etTargetIp)
        btnGetLocalIp = findViewById(R.id.btnGetLocalIp)
        btnScanNetwork = findViewById(R.id.btnScanNetwork)
        tvResult = findViewById(R.id.tvResult)
        
        btnGetLocalIp.setOnClickListener {
            getLocalIp()
        }
        
        btnScanNetwork.setOnClickListener {
            val ip = etTargetIp.text.toString().trim()
            
            if (ip.isEmpty()) {
                showDialog.invalidDialog("Error", "Please enter an IP address")
                return@setOnClickListener
            }
            
            if (!isValidIp(ip)) {
                showDialog.invalidDialog("Invalid IP", "Please enter a valid IP address")
                return@setOnClickListener
            }
            
            scanNetwork(ip)
        }
    }
    
    private fun isValidIp(ip: String): Boolean {
        return ip.matches(Regex("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"))
    }
    
    private fun getLocalIp() {
        Thread {
            try {
                val result = networkScanner.getLocalIp(this)
                
                runOnUiThread {
                    val localIp = result["local_ip"]
                    if (localIp != null) {
                        etTargetIp.setText(localIp)
                        Toast.makeText(this, "Local IP: $localIp", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Unable to get local IP", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    
    private fun scanNetwork(ip: String) {
        val loading = showDialog.loadingDialog("Scanning network ports...\nThis may take a minute")
        
        Thread {
            try {
                val result = networkScanner.scanNetwork(this, ip)
                
                runOnUiThread {
                    loading.dismissWithAnimation()
                    
                    val verdict = result["verdict"] ?: "Unknown"
                    val riskLevel = result["risk_level"] ?: "Unknown"
                    val openPortsCount = result["open_ports_count"]?.toIntOrNull() ?: 0
                    
                    val resultText = buildString {
                        append("Target IP: $ip\n\n")
                        append("Risk Level: $riskLevel\n")
                        append("Open Ports: $openPortsCount\n\n")
                        
                        if (openPortsCount > 0) {
                            append("Ports: ${result["open_ports"]}\n\n")
                            append("Services:\n${result["services"]}\n\n")
                            
                            val vulnerablePorts = result["vulnerable_ports"]
                            if (vulnerablePorts != "None") {
                                append("⚠️ Vulnerable Ports:\n$vulnerablePorts\n\n")
                            }
                        }
                        
                        append("Verdict: $verdict")
                    }
                    
                    tvResult.text = resultText
                    
                    if (riskLevel == "High") {
                        showDialog.threatDialog("🔥 High Risk Detected!", verdict)
                    } else if (riskLevel == "Medium") {
                        showDialog.maliciousDialog("⚠️ Medium Risk", verdict)
                    } else {
                        showDialog.successDialog("✅ Scan Complete", verdict)
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
