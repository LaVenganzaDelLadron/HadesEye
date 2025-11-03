package com.project.hadeseye.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ViewFlipper
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.project.hadeseye.R
import com.project.hadeseye.model.ScanHistory
import com.project.hadeseye.adapter.ReportAdapter

class ReportFragment : Fragment() {

    private lateinit var recyclerAll: RecyclerView
    private lateinit var recyclerSafe: RecyclerView
    private lateinit var recyclerThreat: RecyclerView
    private lateinit var recyclerMalicious: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var viewFlipper: ViewFlipper

    private lateinit var btnAll: Button
    private lateinit var btnSafe: Button
    private lateinit var btnThreat: Button
    private lateinit var btnMalicious: Button

    private lateinit var databaseRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var allList = mutableListOf<ScanHistory>()
    private var safeList = mutableListOf<ScanHistory>()
    private var threatList = mutableListOf<ScanHistory>()
    private var maliciousList = mutableListOf<ScanHistory>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_report, container, false)

        // ViewFlipper and buttons
        viewFlipper = view.findViewById(R.id.viewFlipper)
        btnAll = view.findViewById(R.id.btnAll)
        btnSafe = view.findViewById(R.id.btnSafe)
        btnThreat = view.findViewById(R.id.btnThreat)
        btnMalicious = view.findViewById(R.id.btnMalicious)

        // Search
        searchInput = view.findViewById(R.id.searchInput)

        // RecyclerViews inside included layouts
        recyclerAll = view.findViewById(R.id.recyclerViewReportsAll)
        recyclerSafe = view.findViewById(R.id.recyclerViewReportsSafe)
        recyclerThreat = view.findViewById(R.id.recyclerViewReportsThreat)
        recyclerMalicious = view.findViewById(R.id.recyclerViewReportsMalicious)

        recyclerAll.layoutManager = LinearLayoutManager(requireContext())
        recyclerSafe.layoutManager = LinearLayoutManager(requireContext())
        recyclerThreat.layoutManager = LinearLayoutManager(requireContext())
        recyclerMalicious.layoutManager = LinearLayoutManager(requireContext())

        // Firebase
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            databaseRef = FirebaseDatabase.getInstance(
                "https://hadeseye-c26c7-default-rtdb.firebaseio.com/"
            ).getReference("users/scans/$uid/scans")

            fetchReports()
        }

        // Button Listeners
        btnAll.setOnClickListener { viewFlipper.displayedChild = 0 }
        btnSafe.setOnClickListener { viewFlipper.displayedChild = 1 }
        btnThreat.setOnClickListener { viewFlipper.displayedChild = 2 }
        btnMalicious.setOnClickListener { viewFlipper.displayedChild = 3 }

        // Search Filter
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterReports(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        return view
    }

    private fun fetchReports() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allList.clear()
                safeList.clear()
                threatList.clear()
                maliciousList.clear()

                for (scan in snapshot.children) {
                    val status = scan.child("status").getValue(String::class.java) ?: "Unknown"
                    val url = scan.child("url").getValue(String::class.java) ?: "N/A"
                    val fileName = scan.child("file_name").getValue(String::class.java) ?: "N/A"
                    val ip = scan.child("ip").getValue(String::class.java) ?: "N/A"


                    // Safe conversion for timestamp
                    val timestampValue = scan.child("timestamp").value
                    val date = timestampValue?.toString() ?: "Unknown"

                    val item = ScanHistory(url, status, date, fileName, ip)

                    allList.add(item)
                    when (status) {
                        "Safe" -> safeList.add(item)
                        "Threat" -> threatList.add(item)
                        "Malicious" -> maliciousList.add(item)
                    }
                }

                recyclerAll.adapter = ReportAdapter(allList)
                recyclerSafe.adapter = ReportAdapter(safeList)
                recyclerThreat.adapter = ReportAdapter(threatList)
                recyclerMalicious.adapter = ReportAdapter(maliciousList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseDebug", "Failed to fetch reports: ${error.message}")
            }
        })
    }


    private fun filterReports(query: String) {
        val filtered = allList.filter {
            it.url.contains(query, ignoreCase = true)
        }
        recyclerAll.adapter = ReportAdapter(filtered.toMutableList())
    }
}
