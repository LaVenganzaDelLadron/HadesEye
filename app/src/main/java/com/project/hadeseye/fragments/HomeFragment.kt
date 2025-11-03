package com.project.hadeseye.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.project.hadeseye.DashboardActivity
import com.project.hadeseye.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import com.project.hadeseye.LearnMalwareActivity
import com.project.hadeseye.LearnPhishActivity
import com.project.hadeseye.LearnRansomActivity

class HomeFragment : Fragment() {

    private lateinit var databaseRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var tvScansCount: TextView
    private lateinit var tvThreatCounts: TextView
    private lateinit var tvMaliciousCounts: TextView
    private lateinit var btnPhishingRead: Button
    private lateinit var btnMalwareRead: Button
    private lateinit var btnRansomwareRead: Button
    private lateinit var recentActivityContainer: LinearLayout


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        tvScansCount = view.findViewById(R.id.tvScansCount)
        tvThreatCounts = view.findViewById(R.id.tvThreatCounts)
        tvMaliciousCounts = view.findViewById(R.id.tvMaliciousCounts)
        recentActivityContainer = view.findViewById(R.id.recentActivityContainer)
        btnPhishingRead = view.findViewById(R.id.btnPhishingRead)
        btnMalwareRead = view.findViewById(R.id.btnMalwareRead)
        btnRansomwareRead = view.findViewById(R.id.btnRansomwareRead)


        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            databaseRef = FirebaseDatabase.getInstance(
                "https://hadeseye-c26c7-default-rtdb.firebaseio.com/"
            ).getReference("users/scans/$uid/scans")

            fetchUserScanCounts()
            fetchRecentActivity()
        }

        val btnStartScan = view.findViewById<View>(R.id.btnStartScan)
        val btnViewReports = view.findViewById<View>(R.id.btnViewReports)

        btnStartScan.setOnClickListener {
            val dashboardActivity = activity as? DashboardActivity
            dashboardActivity?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                ?.selectedItemId = R.id.scan
        }

        btnViewReports.setOnClickListener {
            val dashboardActivity = activity as? DashboardActivity
            dashboardActivity?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                ?.selectedItemId = R.id.report
        }


        btnPhishingRead.setOnClickListener {
            val intent = Intent(requireContext(), LearnPhishActivity::class.java)
            startActivity(intent)
        }

        btnMalwareRead.setOnClickListener {
            val intent = Intent(requireContext(), LearnMalwareActivity::class.java)
            startActivity(intent)
        }

        btnRansomwareRead.setOnClickListener {
            val intent = Intent(requireContext(), LearnRansomActivity::class.java)
            startActivity(intent)
        }






        return view
    }

    private fun fetchUserScanCounts() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var safeCount = 0
                var threatCount = 0
                var maliciousCount = 0

                for (scan in snapshot.children) {
                    val status = scan.child("status").getValue(String::class.java)
                    when (status) {
                        "Safe" -> safeCount++
                        "Threat" -> threatCount++
                        "Malicious" -> maliciousCount++
                    }
                }

                tvScansCount.text = safeCount.toString()
                tvThreatCounts.text = threatCount.toString()
                tvMaliciousCounts.text = maliciousCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchRecentActivity() {
        // Order by timestamp descending, limit to 3 most recent scans
        databaseRef.orderByChild("timestamp").limitToLast(3)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val activities = mutableListOf<Map<String, String>>()

                    for (scan in snapshot.children) {
                        // Check which field exists and use it as display name
                        val ip = scan.child("ip").getValue(String::class.java)
                        val url = scan.child("url").getValue(String::class.java)
                        val fileName = scan.child("file_name").getValue(String::class.java)
                        val screenshotPath = scan.child("screenshotPath").getValue(String::class.java)

                        // Priority: file_name > url > ip > screenshotPath
                        val displayName = when {
                            !fileName.isNullOrEmpty() -> fileName
                            !url.isNullOrEmpty() -> url
                            !ip.isNullOrEmpty() -> ip
                            !screenshotPath.isNullOrEmpty() -> screenshotPath.substringAfterLast('/')
                            else -> "Unknown Scan"
                        }

                        val status = scan.child("status").getValue(String::class.java) ?: "Unknown"
                        activities.add(mapOf("name" to displayName, "status" to status))
                    }

                    // Reverse to show newest first
                    activities.reverse()
                    updateRecentActivityUI(activities)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }


    private fun updateRecentActivityUI(activities: List<Map<String, String>>) {
        recentActivityContainer.removeAllViews()
        val sizePx = (12 * resources.displayMetrics.density).toInt() // 12dp circle

        for (activity in activities) {
            val layout = LinearLayout(requireContext())
            layout.orientation = LinearLayout.HORIZONTAL
            layout.layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            layout.gravity = Gravity.CENTER_VERTICAL
            layout.setPadding(0, 12, 0, 12)

            // Status Indicator
            val statusIndicator = View(requireContext())
            val params = LinearLayout.LayoutParams(sizePx, sizePx)
            params.marginEnd = (16 * resources.displayMetrics.density).toInt()
            statusIndicator.layoutParams = params

            when (activity["status"]) {
                "Safe" -> statusIndicator.setBackgroundResource(R.drawable.green_circle)
                "Threat" -> statusIndicator.setBackgroundResource(R.drawable.red_circle)
                "Malicious" -> statusIndicator.setBackgroundResource(R.drawable.yellow_circle)
                else -> statusIndicator.setBackgroundColor(Color.GRAY)
            }

            // Activity Name
            val tvName = TextView(requireContext())
            tvName.layoutParams = LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
            tvName.text = activity["name"]
            tvName.setTextColor(Color.WHITE)
            tvName.textSize = 14f

            layout.addView(statusIndicator)
            layout.addView(tvName)

            // Divider
            val divider = View(requireContext())
            val dividerParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                (1 * resources.displayMetrics.density).toInt()
            )
            divider.layoutParams = dividerParams
            divider.setBackgroundColor(Color.parseColor("#333333"))

            recentActivityContainer.addView(layout)
            recentActivityContainer.addView(divider)
        }
    }
}
