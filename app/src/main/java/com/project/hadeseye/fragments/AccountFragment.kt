package com.project.hadeseye.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.project.hadeseye.R

class AccountFragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        val profileImage = view.findViewById<ImageView>(R.id.profileImage)
        val accountName = view.findViewById<TextView>(R.id.accountName)
        val accountEmail = view.findViewById<TextView>(R.id.accountEmail)

        firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser

        currentUser?.let { user ->
            accountName.text = user.displayName ?: "Anonymous"
            accountEmail.text = user.email

            val photoUrl = user.photoUrl?.toString()
            if (!photoUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.malware)
                    .into(profileImage)
            }
        }

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

        return view
    }
}
