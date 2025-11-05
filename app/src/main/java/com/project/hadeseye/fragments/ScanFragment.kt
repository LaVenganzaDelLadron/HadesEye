package com.project.hadeseye.fragments

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.project.hadeseye.R
import com.project.hadeseye.ResultScanActivity
import com.project.hadeseye.services.virusTotalServices.VTScanning
import com.project.hadeseye.dialog.ShowDialog
import com.project.hadeseye.services.virusTotalServices.hybridAnalysisServices.HAScanning
import com.project.hadeseye.services.virusTotalServices.urlServices.URLScanning

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ScanFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var viewFlipper: ViewFlipper
    private val VIEW_FILE_SCAN = 0
    private val VIEW_URL_SCAN = 1

    private lateinit var btnAddFile: Button
    private lateinit var btnScanUrl: Button
    private lateinit var btnStartFile: Button
    private lateinit var urlInput: EditText
    private lateinit var recentActivityContainer: LinearLayout
    private lateinit var databaseRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var selectedFileUri: Uri? = null
    lateinit var showDialog: ShowDialog
    lateinit var vtScanning: VTScanning
    lateinit var haScanning: HAScanning
    private lateinit var urlScanning: URLScanning
    private val ipRegex = Regex("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$")
    private val urlRegex = Regex("^(https?://)?([\\w.-]+@)?([\\w.-]+)\\.([a-z]{2,})([/\\w .-]*)*/?$")
    private val domainRegex = Regex("^(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$")


    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            val mimeType = requireContext().contentResolver.getType(uri)

            when {
                mimeType?.startsWith("image/") == true -> {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    btnAddFile.text = ""
                    btnAddFile.background = BitmapDrawable(resources, bitmap)
                }

                mimeType == "application/vnd.android.package-archive" -> {
                    btnAddFile.setBackgroundResource(R.drawable.ic_apk)
                    btnAddFile.text = ""
                }

                else -> {
                    btnAddFile.setBackgroundResource(R.drawable.ic_file)
                    btnAddFile.text = ""
                }
            }

            Toast.makeText(requireContext(), "Selected: ${getFileName(uri)}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "No file selected", Toast.LENGTH_SHORT).show()
        }
    }







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
        val view = inflater.inflate(R.layout.fragment_scan, container, false)

        showDialog = ShowDialog(requireContext())
        vtScanning = VTScanning()
        urlScanning = URLScanning()
        haScanning = HAScanning()

        viewFlipper = view.findViewById(R.id.viewFlipper)
        viewFlipper.displayedChild = VIEW_FILE_SCAN

        val scanFile = view.findViewById<Button>(R.id.btnFileScan)
        val scanUrl = view.findViewById<Button>(R.id.btnUrlScan)

        btnScanUrl = view.findViewById(R.id.btnStartUrlScan)
        btnStartFile = view.findViewById(R.id.btnStartFileScan)

        urlInput = view.findViewById(R.id.urlInput)
        btnAddFile = view.findViewById(R.id.btnAddFile)
        recentActivityContainer = view.findViewById(R.id.recentActivityContainer)


        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            databaseRef = FirebaseDatabase.getInstance(
                "https://hadeseye-c26c7-default-rtdb.firebaseio.com/"
            ).getReference("users/scans/$uid/scans")

            fetchRecentActivity()
        }




        btnAddFile.setOnClickListener { pickFile() }

        btnStartFile.setOnClickListener {
            vTHaFileScan()
        }
        btnScanUrl.setOnClickListener {
            val input = urlInput.text.toString().trim()
            when {
                ipRegex.matches(input) -> {
                    vtScanIp()
                }
                domainRegex.matches(input) -> {
                    vTHaDomainScan()
                }
                urlRegex.matches(input) -> {
                    vTuSScanUrl()
                }
                else -> {
                    showDialog.invalidDialog("Error", "Invalid URL, Domain, or IP format")
                }
            }
        }




        scanFile.setOnClickListener {
            if (viewFlipper.displayedChild != VIEW_FILE_SCAN) {
                viewFlipper.displayedChild = VIEW_FILE_SCAN
            }
        }
        scanUrl.setOnClickListener {
            if (viewFlipper.displayedChild != VIEW_URL_SCAN) {
                viewFlipper.displayedChild = VIEW_URL_SCAN
            }
        }

        return view
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
                        val domain = scan.child("domain").getValue(String::class.java)
                        val screenshotPath = scan.child("screenshotPath").getValue(String::class.java)

                        // Priority: file_name > url > ip > screenshotPath
                        val displayName = when {
                            !fileName.isNullOrEmpty() -> fileName
                            !url.isNullOrEmpty() -> url
                            !ip.isNullOrEmpty() -> ip
                            !domain.isNullOrEmpty() -> domain
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


    private fun vTuSScanUrl() {
        val url = urlInput.text.toString().trim()
        if (url.isEmpty()) {
            showDialog.invalidDialog("Error", "Field cannot be empty")
            return
        }

        // Store the dialog instance
        val loading = showDialog.loadingDialog("Scanning URL")

        Thread {
            try {
                val vtResult = vtScanning.vt_url_scan(requireContext(), url)
                val usResult = urlScanning.us_url_scan(requireContext(), url)
                val haResult = haScanning.ha_url_scan(requireContext(), url)

                val screenshotPath = usResult["screenshot_path"]

                val intent = Intent(requireContext(), ResultScanActivity::class.java).apply {
                    putExtra("url", url)
                    putExtra("malicious", vtResult["malicious"])
                    putExtra("harmless", vtResult["harmless"])
                    putExtra("suspicious", vtResult["suspicious"])
                    putExtra("undetected", vtResult["undetected"])
                    putExtra("threat_level", haResult["threat_level"])
                    putExtra("threat_score", haResult["threat_score"])
                    putExtra("verdict", haResult["verdict"])
                    putExtra("screenshot_path", screenshotPath)
                }

                requireActivity().runOnUiThread {
                    loading.dismissWithAnimation()
                    startActivity(intent)
                }

            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    loading.dismissWithAnimation()
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("ScanFragment", "Error: ${e.message}", e)
                }
            }
        }.start()
    }


    private fun vtScanIp() {
        val ip = urlInput.text.toString().trim()
        if (ip.isEmpty()) {
            showDialog.invalidDialog("Error", "Field cannot be empty")
            return
        }

        val loading = showDialog.loadingDialog("Scanning IP Address...")
        Thread {
            try {
                val vtResult = vtScanning.vt_ip_scan(requireContext(), ip)
                val haResult = haScanning.ha_ip_scan(requireContext(), ip)  // ✅ add Hybrid Analysis IP scan

                val intent = Intent(requireContext(), ResultScanActivity::class.java).apply {
                    putExtra("ip", ip)
                    putExtra("malicious", vtResult["malicious"])
                    putExtra("harmless", vtResult["harmless"])
                    putExtra("suspicious", vtResult["suspicious"])
                    putExtra("undetected", vtResult["undetected"])
                    putExtra("threat_level", haResult["threat_level"]?: "N/A")
                    putExtra("threat_score", haResult["threat_score"]?: "N/A")
                    putExtra("verdict", haResult["verdict"]?: "N/A")
                }

                requireActivity().runOnUiThread {
                    loading.dismissWithAnimation()
                    startActivity(intent)
                }

            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    loading.dismissWithAnimation()
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("ScanFragment", "IP Scan Error: ${e.message}", e)
                }
            }
        }.start()
    }

    private fun vTHaDomainScan() {
        val domain = urlInput.text.toString().trim()
        if (domain.isEmpty()) {
            showDialog.invalidDialog("Error", "Field cannot be empty")
            return
        }

        val loading = showDialog.loadingDialog("Scanning Domain...")

        Thread {
            try {
                val vtResult = vtScanning.vt_domain_scan(requireContext(), domain)
                val usResult = urlScanning.us_url_scan(requireContext(), domain)
                val haResult = haScanning.ha_domain_scan(requireContext(), domain)

                val screenshotPath = usResult["screenshot_path"]

                val intent = Intent(requireContext(), ResultScanActivity::class.java).apply {
                    putExtra("domain", domain)
                    putExtra("malicious", vtResult["malicious"])
                    putExtra("harmless", vtResult["harmless"])
                    putExtra("suspicious", vtResult["suspicious"])
                    putExtra("undetected", vtResult["undetected"])
                    putExtra("threat_level", haResult["threat_level"])
                    putExtra("threat_score", haResult["threat_score"])
                    putExtra("verdict", haResult["verdict"])
                    putExtra("screenshot_path", screenshotPath)
                }

                requireActivity().runOnUiThread {
                    loading.dismissWithAnimation()
                    startActivity(intent)
                }

            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    loading.dismissWithAnimation()
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("ScanFragment", "Error: ${e.message}", e)
                }
            }
        }.start()
    }




    private fun vTHaFileScan() {
        if (selectedFileUri == null) {
            showDialog.invalidDialog("Error", "No file selected")
            return
        }
        val loading = showDialog.loadingDialog("Scanning File...")
        Thread {
            try {
                val result = vtScanning.vt_file_scan(requireContext(), selectedFileUri)
                val haResult = haScanning.ha_file_scan(requireContext(), selectedFileUri)

                val intent = Intent(requireContext(), ResultScanActivity::class.java)
                intent.putExtra("file_name", getFileName(selectedFileUri!!))
                intent.putExtra("malicious", result["malicious"])
                intent.putExtra("harmless", result["harmless"])
                intent.putExtra("suspicious", result["suspicious"])
                intent.putExtra("undetected", result["undetected"])
                intent.putExtra("threat_level", haResult["threat_level"]?: "N/A")
                intent.putExtra("threat_score", haResult["threat_score"]?: "N/A")
                intent.putExtra("verdict", haResult["verdict"] ?: "N/A")


                requireActivity().runOnUiThread {
                    loading.dismissWithAnimation()
                    startActivity(intent)
                }


            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    showDialog.loadingDialog("Scanning File.....").dismissWithAnimation()
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    private fun pickFile() {
        filePickerLauncher.launch("*/*")
    }
    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1 && result != null) {
                result = result.substring(cut!! + 1)
            }
        }
        return result ?: "unknown"
    }


    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ScanFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
