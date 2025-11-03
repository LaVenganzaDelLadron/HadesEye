package com.project.hadeseye

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.project.hadeseye.databinding.ActivitySigunpBinding
import com.project.hadeseye.dialog.ShowDialog
import java.util.regex.Pattern

class SignupActivity : AppCompatActivity() {

    private val database = FirebaseDatabase.getInstance(
        "https://hadeseye-c26c7-default-rtdb.firebaseio.com/"
    ).getReference("account/users")

    private lateinit var binding: ActivitySigunpBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private var dialog = ShowDialog(this)
    private var isPasswordVisible = true
    private var isConfirmPasswordVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySigunpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        setupPasswordToggle(binding.passwordInput, true)
        setupPasswordToggle(binding.confirmPasswordInput, false)
        setupGoogleSignIn()

        binding.signupButton.setOnClickListener { handleSignup() }
        binding.googleIcon.setOnClickListener { signInWithGoogle() }
        binding.alreadyHaveAccount.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun handleSignup() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()
        val confirmPassword = binding.confirmPasswordInput.text.toString().trim()

        // Validations
        if (!isEmailValid(email)) {
            dialog.invalidDialog("Error", "Invalid email")
            return
        }
        if (!isPasswordValid(password)) {
            dialog.invalidDialog("Error", "Password must contain upper, lower, number, symbol and at least 8 chars")
            return
        }
        if (password != confirmPassword) {
            dialog.invalidDialog("Error", "Passwords do not match")
            return
        }

        // Create user
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = firebaseAuth.currentUser
                user?.let {
                    val userData = mapOf(
                        "uid" to it.uid,
                        "email" to it.email,
                        "name" to (it.displayName ?: "Anonymous"),
                        "phone" to "",
                        "address" to "",
                        "photoUrl" to (it.photoUrl?.toString() ?: ""),
                        "memberSince" to System.currentTimeMillis()
                    )
                    database.child("users").child(it.uid).setValue(userData)
                        .addOnSuccessListener {
                            Log.d("FirebaseDebug", "✅ User data added successfully for UID: ${user.uid}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseDebug", "❌ Failed to add user data: ${e.message}")
                        }
                }


                dialog.successDialog(
                    "Success", "Signup Successful", "OK",
                    Runnable {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            } else {
                dialog.invalidDialog("Error", "Failed to signup. Check logs for details.")
            }
        }
    }

    private fun signInWithGoogle() {
        launcher.launch(googleSignInClient.signInIntent)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignInTask(task)
        }
    }

    private fun handleGoogleSignInTask(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            task.result?.let { updateUIWithGoogle(it) }
        } else {
            Log.w("SignupActivity", "Google Sign-In failed", task.exception)
            dialog.invalidDialog("Error", "Failed to signup. Check logs for details.")
        }
    }

    private fun updateUIWithGoogle(account: GoogleSignInAccount) {
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
                        .addOnSuccessListener { Log.d("FirebaseDebug", "✅ Google user data added successfully") }
                        .addOnFailureListener { e -> Log.e("FirebaseDebug", "❌ Failed to add Google user data: ${e.message}") }
                }
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                dialog.invalidDialog("Error", "Google login failed. Check logs.")
            }
        }
    }

    // Password toggle
    private fun setupPasswordToggle(editText: EditText, isMainPassword: Boolean) {
        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                if (event.rawX >= (editText.right - editText.compoundDrawables[drawableEnd].bounds.width())) {
                    if (isMainPassword) {
                        isPasswordVisible = !isPasswordVisible
                        togglePasswordVisibility(editText, isPasswordVisible)
                    } else {
                        isConfirmPasswordVisible = !isConfirmPasswordVisible
                        togglePasswordVisibility(editText, isConfirmPasswordVisible)
                    }
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun togglePasswordVisibility(editText: EditText, visible: Boolean) {
        editText.inputType =
            if (visible) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        editText.setCompoundDrawablesWithIntrinsicBounds(
            0, 0,
            if (visible) R.drawable.eye else R.drawable.eye_closed, 0
        )
        editText.setSelection(editText.text.length)
    }

    private fun isEmailValid(email: String): Boolean {
        val pattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
        return pattern.matcher(email).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        val pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%=*?&])[A-Za-z\\d@\$!=%*?&]{8,}$")
        return pattern.matcher(password).matches()
    }
}
