package com.project.hadeseye

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.project.hadeseye.databinding.ActivityResultScanBinding

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

        val malicious = intent.getStringExtra("malicious")
        val harmless = intent.getStringExtra("harmless")
        val suspicious = intent.getStringExtra("suspicious")
        val undetected = intent.getStringExtra("undetected")

        binding.malicious.text = "Malicious: $malicious"
        binding.harmless.text = "Harmless: $harmless"
        binding.suspicious.text = "Suspicious: $suspicious"
        binding.undetected.text = "Undetected: $undetected"


    }
}