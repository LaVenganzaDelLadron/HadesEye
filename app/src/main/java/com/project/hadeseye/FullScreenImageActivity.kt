package com.project.hadeseye

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.project.hadeseye.databinding.ActivityFullScreenImageBinding

class FullScreenImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFullScreenImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        val imagePath = intent.getStringExtra("image_path")

        if (!imagePath.isNullOrEmpty()) {
            Glide.with(this)
                .load(Uri.parse("file://$imagePath"))
                .placeholder(R.drawable.loading)
                .error(R.drawable.image_broken)
                .into(binding.fullScreenImage)
        }

        binding.fullScreenImage.setOnClickListener {
            finish()
        }
    }
}