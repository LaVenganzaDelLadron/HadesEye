package com.project.hadeseye.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.project.hadeseye.*

class ToolsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tools, container, false)

        // Hash Scanner
        view.findViewById<LinearLayout>(R.id.layoutHashScanner).setOnClickListener {
            startActivity(Intent(requireContext(), HashScannerActivity::class.java))
        }

        // QR Code Scanner
        view.findViewById<LinearLayout>(R.id.layoutQRScanner).setOnClickListener {
            startActivity(Intent(requireContext(), QRScannerActivity::class.java))
        }

        // Breach Checker
        view.findViewById<LinearLayout>(R.id.layoutBreachChecker).setOnClickListener {
            startActivity(Intent(requireContext(), BreachCheckerActivity::class.java))
        }

        // Network Scanner
        view.findViewById<LinearLayout>(R.id.layoutNetworkScanner).setOnClickListener {
            startActivity(Intent(requireContext(), NetworkScannerActivity::class.java))
        }

        // Bulk Scanner
        view.findViewById<LinearLayout>(R.id.layoutBulkScanner).setOnClickListener {
            startActivity(Intent(requireContext(), BulkScannerActivity::class.java))
        }

        // Export Reports
        view.findViewById<LinearLayout>(R.id.layoutExportReports).setOnClickListener {
            startActivity(Intent(requireContext(), ExportReportsActivity::class.java))
        }

        // Phishing Detector
        view.findViewById<LinearLayout>(R.id.layoutPhishingDetector).setOnClickListener {
            startActivity(Intent(requireContext(), ScreenshotPhishingActivity::class.java))
        }

        // APK Scanner
        view.findViewById<LinearLayout>(R.id.layoutAPKScanner).setOnClickListener {
            startActivity(Intent(requireContext(), APKScannerActivity::class.java))
        }

        // URL Protection
        view.findViewById<LinearLayout>(R.id.layoutURLProtection).setOnClickListener {
            startActivity(Intent(requireContext(), URLProtectionActivity::class.java))
        }

        // Scheduled Scans
        view.findViewById<LinearLayout>(R.id.layoutScheduledScans).setOnClickListener {
            startActivity(Intent(requireContext(), ScheduledScansActivity::class.java))
        }

        return view
    }

    companion object {
        fun newInstance() = ToolsFragment()
    }
}
