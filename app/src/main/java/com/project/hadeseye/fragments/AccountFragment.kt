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
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.project.hadeseye.MainActivity
import com.project.hadeseye.R

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private lateinit var firebaseAuth: FirebaseAuth
private lateinit var googleSignInClient: GoogleSignInClient
class AccountFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        val btnLogout = view.findViewById<Button>(R.id.btnLogout)
        val profileImage = view.findViewById<ImageView>(R.id.profileImage)
        val accountName = view.findViewById<TextView>(R.id.accountName)
        val accountEmail = view.findViewById<TextView>(R.id.accountEmail)

        firebaseAuth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        val database = FirebaseDatabase.getInstance().reference
        val currentUser = firebaseAuth.currentUser

        currentUser?.let { user ->
            val userRef = database.child("users").child(user.uid)
            userRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val name = snapshot.child("name").value.toString()
                    val email = snapshot.child("email").value.toString()
                    val photoUrl = snapshot.child("photoUrl").value.toString()

                    accountName.text = name
                    accountEmail.text = email

                    if (photoUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .placeholder(R.drawable.malware)
                            .into(profileImage)
                    }
                }
            }
        }


        btnLogout.setOnClickListener {
            firebaseAuth.signOut()
            googleSignInClient.signOut()
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AccountFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}