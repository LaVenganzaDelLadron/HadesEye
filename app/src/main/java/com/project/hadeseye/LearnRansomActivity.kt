package com.project.hadeseye

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.project.hadeseye.databinding.ActivityLearnRansomBinding

class LearnRansomActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLearnRansomBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLearnRansomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarLearnRansom)
        toolbar.setNavigationOnClickListener { onBackPressed() }


    }
}