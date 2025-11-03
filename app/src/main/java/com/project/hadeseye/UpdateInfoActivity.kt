package com.project.hadeseye

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.project.hadeseye.databinding.ActivityUpdateInfoBinding

class UpdateInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateInfoBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            Log.d("UpdateInfoActivity", "Current user UID: ${currentUser.uid}")
            loadUserInfo(currentUser.uid)
        } else {
            Log.e("UpdateInfoActivity", "No user is logged in.")
        }

        // ✅ Save updated info
        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()

            Log.d("UpdateInfoActivity", "Attempting to update user info:")
            Log.d("UpdateInfoActivity", "Name: $name, Phone: $phone, Address: $address")

            if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
                Log.w("UpdateInfoActivity", "Update failed: one or more fields are empty.")
                return@setOnClickListener
            }

            val uid = currentUser?.uid ?: return@setOnClickListener
            val database = FirebaseDatabase.getInstance(
                "https://hadeseye-c26c7-default-rtdb.firebaseio.com/"
            )
            val userRef = database.getReference("account/users").child(uid)

            val updates = mapOf(
                "name" to name,
                "phone" to phone,
                "address" to address
            )

            Log.d("UpdateInfoActivity", "📡 Updating Firebase at path: account/users/$uid")
            Log.d("UpdateInfoActivity", "📦 Data being sent: $updates")

            userRef.updateChildren(updates)
                .addOnSuccessListener {
                    Log.i("UpdateInfoActivity", "✅ Update successful for user: $uid")
                    Toast.makeText(this, "✅ Info updated successfully!", Toast.LENGTH_SHORT).show()

                    // Confirmation toast showing what changed
                    Toast.makeText(
                        this,
                        "Updated:\nName: $name\nPhone: $phone\nAddress: $address",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                }
                .addOnFailureListener {
                    Log.e("UpdateInfoActivity", "❌ Update failed: ${it.message}")
                    Toast.makeText(this, "❌ Failed to update: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        // ✅ Cancel Button
        binding.btnCancel.setOnClickListener {
            Log.d("UpdateInfoActivity", "Cancel button pressed — closing activity.")
            finish()
        }
    }

    private fun loadUserInfo(uid: String) {
        val userRef = FirebaseDatabase.getInstance(
            "https://hadeseye-c26c7-default-rtdb.firebaseio.com/"
        ).getReference("account/users").child(uid)

        Log.d("UpdateInfoActivity", "📥 Loading user info for UID: $uid")

        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val name = snapshot.child("name").getValue(String::class.java)
                val phone = snapshot.child("phone").getValue(String::class.java)
                val address = snapshot.child("address").getValue(String::class.java)

                Log.d("UpdateInfoActivity", "✅ Data fetched: name=$name, phone=$phone, address=$address")

                binding.etName.setText(name ?: "")
                binding.etPhone.setText(phone ?: "")
                binding.etAddress.setText(address ?: "")
            } else {
                Log.w("UpdateInfoActivity", "⚠️ No snapshot found for UID: $uid")
            }
        }.addOnFailureListener {
            Log.e("UpdateInfoActivity", "❌ Failed to load user info: ${it.message}")
            Toast.makeText(this, "Failed to load user info: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }
}
