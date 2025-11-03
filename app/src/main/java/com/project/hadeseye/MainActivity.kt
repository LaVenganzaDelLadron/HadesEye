package com.project.hadeseye

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.project.hadeseye.databinding.ActivityMainBinding
import com.project.hadeseye.dialog.ShowDialog

class MainActivity : AppCompatActivity() {
    private val database = FirebaseDatabase.getInstance(
        "https://hadeseye-c26c7-default-rtdb.firebaseio.com/"
    ).getReference("account/users")
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private var dialog = ShowDialog(this)
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            // User is already logged in, go directly to Dashboard
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish() // prevent back button from returning to MainActivity
            return
        }

        // Request permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
        }

        // Google SignIn setup
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Button listeners
        binding.googleIcon.setOnClickListener { signInWithGoogle() }
        binding.signupButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
        binding.loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }


    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleResultTask(task)
        }
    }

    private fun handleResultTask(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            val account: GoogleSignInAccount? = task.result
            if (account != null) {
                updateUI(account)
            }
        } else {
            Log.w("LoginActivity", "Google Sign-In failed", task.exception)
            dialog.invalidDialog("Error", "Failed to signup. Check logs for details.")
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        Log.d("LoginActivity", "Updating UI with account: ${account.email}")
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = firebaseAuth.currentUser
                user?.let {
                    val userData = mapOf(
                        "uid" to it.uid,
                        "email" to it.email,
                        "name" to (it.displayName ?: "Google User"),
                        "phone" to "",
                        "address" to "",
                        "photoUrl" to (it.photoUrl?.toString() ?: ""),
                        "memberSince" to System.currentTimeMillis()
                    )
                    database.child("users").child(it.uid).setValue(userData)
                        .addOnSuccessListener {
                            Log.d("FirebaseDebug", "✅ Google user data added successfully for UID")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseDebug", "❌ Failed to add Google user data: ${e.message}")
                        }

                }

                intent = Intent(this, DashboardActivity::class.java)
                startActivity(intent)
                finish()
            }

        }
    }

}