package com.project.hadeseye.fragments

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.project.hadeseye.R
import com.project.hadeseye.ResultScanActivity
import com.project.hadeseye.services.virusTotalServices.VTScanning
import com.project.hadeseye.dialog.ShowDialog
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
    private var selectedFileUri: Uri? = null
    lateinit var showDialog: ShowDialog
    lateinit var vtScanning: VTScanning
    private lateinit var urlScanning: URLScanning
    private val ipRegex = Regex("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$")
    private val urlRegex = Regex("^(https?://)?([\\w.-]+@)?([\\w.-]+)\\.([a-z]{2,})([/\\w .-]*)*/?$")

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

        viewFlipper = view.findViewById(R.id.viewFlipper)
        viewFlipper.displayedChild = VIEW_FILE_SCAN

        val scanFile = view.findViewById<Button>(R.id.btnFileScan)
        val scanUrl = view.findViewById<Button>(R.id.btnUrlScan)

        btnScanUrl = view.findViewById(R.id.btnStartUrlScan)
        btnStartFile = view.findViewById(R.id.btnStartFileScan)

        urlInput = view.findViewById(R.id.urlInput)
        btnAddFile = view.findViewById(R.id.btnAddFile)






        btnAddFile.setOnClickListener { pickFile() }

        btnStartFile.setOnClickListener {
            vtFileScan()
        }
        btnScanUrl.setOnClickListener {
            val input = urlInput.text.toString().trim()
            when {
                ipRegex.matches(input) -> {
                    vtScanIp()
                }
                urlRegex.matches(input) -> {
                    vTuSScanUrl()
                }
                else -> {
                    showDialog.invalidDialog("Error", "Invalid URL or IP format")
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



    private fun vTuSScanUrl() {
        val url = urlInput.text.toString().trim()
        if (url.isEmpty()) {
            showDialog.invalidDialog("Error", "Field cannot be empty")
        }

        showDialog.loadingDialog("Just wait for a moment")
        Thread {
            try {
                val vtResult = vtScanning.vt_url_scan(requireContext(), url)
                val usResult = urlScanning.us_url_scan(requireContext(), url)
                val screenshotPath = usResult["screenshot_path"]


                val intent = Intent(requireContext(), ResultScanActivity::class.java)
                intent.putExtra("malicious", vtResult["malicious"])
                intent.putExtra("harmless", vtResult["harmless"])
                intent.putExtra("suspicious", vtResult["suspicious"])
                intent.putExtra("undetected", vtResult["undetected"])
                intent.putExtra("screenshot_path", screenshotPath)

                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "US Result: $usResult", Toast.LENGTH_LONG).show()
                    Log.d("URLScanning", "US Result: $usResult")
                    showDialog.loadingDialog("Scanning Url.....").dismissWithAnimation()
                    startActivity(intent)
                }

            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    showDialog.loadingDialog("Scanning Url.....").dismissWithAnimation()
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    private fun vtScanIp() {
        val ip = urlInput.text.toString().trim()
        if (ip.isEmpty()) {
            showDialog.invalidDialog("Error", "Field cannot be empty")
        }

        showDialog.loadingDialog("Just wait for a moment")
        Thread {
            try {
                val result = vtScanning.vt_ip_scan(requireContext(), ip)

                val intent = Intent(requireContext(), ResultScanActivity::class.java)
                intent.putExtra("malicious", result["malicious"])
                intent.putExtra("harmless", result["harmless"])
                intent.putExtra("suspicious", result["suspicious"])
                intent.putExtra("undetected", result["undetected"])

                requireActivity().runOnUiThread {
                    showDialog.loadingDialog("Scanning Url.....").dismissWithAnimation()
                    startActivity(intent)
                }

            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    showDialog.loadingDialog("Scanning Url.....").dismissWithAnimation()
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun vtFileScan() {
        if (selectedFileUri == null) {
            showDialog.invalidDialog("Error", "No file selected")
            return
        }
        showDialog.loadingDialog("Just wait for a moment")
        Thread {
            try {
                val result = vtScanning.vt_file_scan(requireContext(), selectedFileUri)

                val intent = Intent(requireContext(), ResultScanActivity::class.java)
                intent.putExtra("malicious", result["malicious"])
                intent.putExtra("harmless", result["harmless"])
                intent.putExtra("suspicious", result["suspicious"])
                intent.putExtra("undetected", result["undetected"])

                requireActivity().runOnUiThread {
                    showDialog.loadingDialog("Scanning Url.....").dismissWithAnimation()
                    startActivity(intent)
                }

            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    showDialog.loadingDialog("Scanning Url.....").dismissWithAnimation()
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
