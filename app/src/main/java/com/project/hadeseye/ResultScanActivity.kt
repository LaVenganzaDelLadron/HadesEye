package com.project.hadeseye

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.project.hadeseye.databinding.ActivityResultScanBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ResultScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultScanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            finish()
        }

        val url = intent.getStringExtra("url")
        val ip = intent.getStringExtra("ip")
        val domain = intent.getStringExtra("domain")
        val fineName = intent.getStringExtra("file_name")
        val malicious = intent.getStringExtra("malicious")
        val harmless = intent.getStringExtra("harmless")
        val suspicious = intent.getStringExtra("suspicious")
        val undetected = intent.getStringExtra("undetected")
        val threatLevel = intent.getStringExtra("threat_level")
        val threatScore = intent.getStringExtra("threat_score")
        val verdict = intent.getStringExtra("verdict")
        val screenshotPath = intent.getStringExtra("screenshot_path")

        binding.malicious.text = "Malicious: $malicious"
        binding.harmless.text = "Harmless: $harmless"
        binding.suspicious.text = "Suspicious: $suspicious"
        binding.undetected.text = "Undetected: $undetected"
        binding.threatLevel.text = "Threat Level: $threatLevel"
        binding.threatScore.text = "Threat Score: $threatScore"
        binding.verdict.text = "Verdict: $verdict"




        if (!screenshotPath.isNullOrEmpty()) {
            Glide.with(this)
                .load(Uri.parse("file://$screenshotPath"))
                .placeholder(R.drawable.loading)
                .error(R.drawable.image_broken)
                .into(binding.screenShot)

            binding.screenShot.setOnClickListener {
                val intent = Intent(this, FullScreenImageActivity::class.java)
                intent.putExtra("image_path", screenshotPath)
                startActivity(intent)
            }
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid ?: return

        val maliciousCount = malicious?.toIntOrNull() ?: 0
        val suspiciousCount = suspicious?.toIntOrNull() ?: 0

        val status = when {
            maliciousCount >= 10 -> "Malicious"
            maliciousCount >= 2 && suspiciousCount >= 1 -> "Threat"
            maliciousCount >= 2 -> "Threat"
            else -> "Safe"
        }


        val scanData = mapOf(
            "url" to url,
            "ip" to ip,
            "domain" to domain,
            "file_name" to fineName,
            "malicious" to maliciousCount,
            "harmless" to (harmless?.toIntOrNull() ?: 0),
            "suspicious" to suspiciousCount,
            "undetected" to (undetected?.toIntOrNull() ?: 0),
            "threat_level" to threatLevel,
            "threat_score" to threatScore,
            "verdict" to verdict,
            "screenshotPath" to screenshotPath,
            "status" to status,
            "timestamp" to System.currentTimeMillis()
        )

        val database = FirebaseDatabase.getInstance(
            "https://hadeseye-c26c7-default-rtdb.firebaseio.com/"
        ).getReference("users/scans")

        database.child(uid).child("scans").push().setValue(scanData)
            .addOnSuccessListener {
                when (status) {
                    "Safe" -> {
                        binding.statusText.text = "✅ Safe"
                        binding.statusText.setTextColor(getColor(R.color.green))
                    }
                    "Malicious" -> {
                        binding.statusText.text = "⚠️ Malicious"
                        binding.statusText.setTextColor(getColor(R.color.yellow))
                    }
                    "Threat" -> {
                        binding.statusText.text = "🚨 Threat"
                        binding.statusText.setTextColor(getColor(R.color.red))
                    }
                }
            }
            .addOnFailureListener { e ->
                binding.statusText.text = "❌ Failed to upload: ${e.message}"
            }
    }
}
