package com.project.hadeseye

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
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
        val screenshotPath = intent.getStringExtra("screenshot_path")

        binding.malicious.text = "Malicious: $malicious"
        binding.harmless.text = "Harmless: $harmless"
        binding.suspicious.text = "Suspicious: $suspicious"
        binding.undetected.text = "Undetected: $undetected"

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


    }
}