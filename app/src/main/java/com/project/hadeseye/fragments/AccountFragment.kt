package com.project.hadeseye.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.project.hadeseye.LoginActivity
import com.project.hadeseye.R
import com.project.hadeseye.UpdateInfoActivity
import com.project.hadeseye.dialog.ShowDialog
import java.text.SimpleDateFormat
import java.util.*

class AccountFragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var showDialog: ShowDialog
    private lateinit var profileImage: ImageView
    private lateinit var accountName: TextView
    private lateinit var accountEmail: TextView
    private lateinit var tvPhoneNumber: TextView
    private lateinit var tvAddress: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnEdit: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        // Initialize views
        profileImage = view.findViewById(R.id.profileImage)
        accountName = view.findViewById(R.id.accountName)
        accountEmail = view.findViewById(R.id.accountEmail)
        tvPhoneNumber = view.findViewById(R.id.tvPhoneNumber)
        tvAddress = view.findViewById(R.id.tvAddress)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnEdit = view.findViewById(R.id.btnEdit)


        showDialog = ShowDialog(requireContext())

        firebaseAuth = FirebaseAuth.getInstance()

        loadUserInfo()

        btnEdit.setOnClickListener {
            startActivity(Intent(requireContext(), UpdateInfoActivity::class.java))
        }

        // Logout
        btnLogout.setOnClickListener {
            logoutUser()
        }

        return view
    }

    private fun loadUserInfo() {
        val currentUser = firebaseAuth.currentUser
        currentUser?.let { user ->
            // Firebase basic info
            accountName.text = user.displayName ?: "Anonymous"
            accountEmail.text = user.email

            user.photoUrl?.let { url ->
                Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.malware)
                    .into(profileImage)
            }

            // Google Sign-In info (if used)
            val googleAccount = GoogleSignIn.getLastSignedInAccount(requireContext())
            googleAccount?.let { account ->
                accountName.text = account.displayName ?: accountName.text
                accountEmail.text = account.email ?: accountEmail.text
                account.photoUrl?.let { url ->
                    Glide.with(this)
                        .load(url)
                        .placeholder(R.drawable.malware)
                        .into(profileImage)
                }
            }

            // Extra info from Firebase Realtime Database
            val database = FirebaseDatabase.getInstance(
                "https://hadeseye-c26c7-default-rtdb.firebaseio.com/"
            ).getReference("account/users")

            database.child(user.uid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val name = snapshot.child("name").getValue(String::class.java)
                        val phone = snapshot.child("phone").getValue(String::class.java)
                        val address = snapshot.child("address").getValue(String::class.java)
                        val createdAtRaw = snapshot.child("memberSince").getValue(Long::class.java)

                        val formattedDate = createdAtRaw?.let { ts ->
                            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                .format(Date(ts))
                        } ?: "Unknown"

                        accountName.text = name ?: user.displayName ?: "No Name"
                        tvPhoneNumber.text = "Phone: ${phone ?: "Not set"}"
                        tvAddress.text = "Address: ${address ?: "Not set"}"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "❌ Failed to load info: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun logoutUser() {
        val showDialog = ShowDialog(requireContext())
        showDialog.logoutDialog(Runnable {
            // Proceed with logout
            firebaseAuth.signOut()

            val googleSignInClient = GoogleSignIn.getClient(
                requireContext(),
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
            )

            googleSignInClient.signOut().addOnCompleteListener {
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
        })
    }


    override fun onResume() {
        super.onResume()
        loadUserInfo()
    }
}
