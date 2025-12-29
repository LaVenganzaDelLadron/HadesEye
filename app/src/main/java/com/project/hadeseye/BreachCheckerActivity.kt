package com.project.hadeseye

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.project.hadeseye.dialog.ShowDialog
import com.project.hadeseye.services.BreachChecker

class BreachCheckerActivity : AppCompatActivity() {
    
    private lateinit var etPassword: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnCheckPassword: Button
    private lateinit var btnCheckEmail: Button
    private lateinit var tvResult: TextView
    private lateinit var showDialog: ShowDialog
    private lateinit var breachChecker: BreachChecker
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_breach_checker)
        
        showDialog = ShowDialog(this)
        breachChecker = BreachChecker()
        
        etPassword = findViewById(R.id.etPassword)
        etEmail = findViewById(R.id.etEmail)
        btnCheckPassword = findViewById(R.id.btnCheckPassword)
        btnCheckEmail = findViewById(R.id.btnCheckEmail)
        tvResult = findViewById(R.id.tvResult)
        
        btnCheckPassword.setOnClickListener {
            val password = etPassword.text.toString()
            
            if (password.isEmpty()) {
                showDialog.invalidDialog("Error", "Please enter a password")
                return@setOnClickListener
            }
            
            checkPassword(password)
        }
        
        btnCheckEmail.setOnClickListener {
            val email = etEmail.text.toString().trim()
            
            if (email.isEmpty()) {
                showDialog.invalidDialog("Error", "Please enter an email")
                return@setOnClickListener
            }
            
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showDialog.invalidDialog("Invalid Email", "Please enter a valid email address")
                return@setOnClickListener
            }
            
            checkEmail(email)
        }
    }
    
    private fun checkPassword(password: String) {
        val loading = showDialog.loadingDialog("Checking password...")
        
        Thread {
            try {
                val result = breachChecker.checkPasswordBreach(this, password)
                
                runOnUiThread {
                    loading.dismissWithAnimation()
                    
                    val breachCount = result["breach_count"]?.toIntOrNull() ?: 0
                    val verdict = result["verdict"] ?: "Unknown"
                    val strength = result["strength"] ?: "Unknown"
                    
                    val resultText = buildString {
                        append("Password Strength: $strength\n\n")
                        append("Breach Count: $breachCount\n\n")
                        append("Status: $verdict")
                    }
                    
                    tvResult.text = resultText
                    
                    if (breachCount >= 10000) {
                        showDialog.threatDialog("🔥 CRITICAL BREACH!", verdict)
                    } else if (breachCount > 0) {
                        showDialog.maliciousDialog("⚠️ Password Compromised", verdict)
                    } else {
                        showDialog.successDialog("✅ Password Safe", verdict)
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
    
    private fun checkEmail(email: String) {
        val loading = showDialog.loadingDialog("Checking email...")
        
        Thread {
            try {
                val result = breachChecker.checkEmailBreach(this, email)
                
                runOnUiThread {
                    loading.dismissWithAnimation()
                    
                    val status = result["status"]
                    val verdict = result["verdict"] ?: "Unknown"
                    val breachCount = result["breach_count"]?.toIntOrNull() ?: 0
                    
                    val resultText = buildString {
                        append("Email: $email\n\n")
                        append("Breach Count: $breachCount\n\n")
                        
                        if (status == "breached") {
                            append("Breaches Found In:\n")
                            append("${result["breaches"]}\n\n")
                        }
                        
                        append("Status: $verdict")
                    }
                    
                    tvResult.text = resultText
                    
                    if (breachCount >= 5) {
                        showDialog.threatDialog("🔥 Multiple Breaches!", verdict)
                    } else if (breachCount > 0) {
                        showDialog.maliciousDialog("⚠️ Email Compromised", verdict)
                    } else {
                        showDialog.successDialog("✅ Email Safe", verdict)
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
