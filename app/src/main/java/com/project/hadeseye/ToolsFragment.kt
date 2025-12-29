package com.project.hadeseye

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment

class ToolsFragment : Fragment() {
    
    private lateinit var layoutHashScanner: LinearLayout
    private lateinit var layoutQRScanner: LinearLayout
    private lateinit var layoutBulkScanner: LinearLayout
    private lateinit var layoutExportReports: LinearLayout
    private lateinit var layoutBreachChecker: LinearLayout
    private lateinit var layoutNetworkScanner: LinearLayout
    private lateinit var layoutPhishingDetector: LinearLayout
    private lateinit var layoutAPKScanner: LinearLayout
    private lateinit var layoutURLProtection: LinearLayout
    private lateinit var layoutScheduledScans: LinearLayout
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tools, container, false)
        
        layoutHashScanner = view.findViewById(R.id.layoutHashScanner)
        layoutQRScanner = view.findViewById(R.id.layoutQRScanner)
        layoutBulkScanner = view.findViewById(R.id.layoutBulkScanner)
        layoutExportReports = view.findViewById(R.id.layoutExportReports)
        layoutBreachChecker = view.findViewById(R.id.layoutBreachChecker)
        layoutNetworkScanner = view.findViewById(R.id.layoutNetworkScanner)
        layoutPhishingDetector = view.findViewById(R.id.layoutPhishingDetector)
        layoutAPKScanner = view.findViewById(R.id.layoutAPKScanner)
        layoutURLProtection = view.findViewById(R.id.layoutURLProtection)
        layoutScheduledScans = view.findViewById(R.id.layoutScheduledScans)
        
        layoutHashScanner.setOnClickListener {
            startActivity(Intent(requireContext(), HashScannerActivity::class.java))
        }
        
        layoutQRScanner.setOnClickListener {
            startActivity(Intent(requireContext(), QRScannerActivity::class.java))
        }
        
        layoutBulkScanner.setOnClickListener {
            startActivity(Intent(requireContext(), BulkScannerActivity::class.java))
        }
        
        layoutExportReports.setOnClickListener {
            startActivity(Intent(requireContext(), ExportReportsActivity::class.java))
        }
        
        layoutBreachChecker.setOnClickListener {
            startActivity(Intent(requireContext(), BreachCheckerActivity::class.java))
        }
        
        layoutNetworkScanner.setOnClickListener {
            startActivity(Intent(requireContext(), NetworkScannerActivity::class.java))
        }
        
        layoutPhishingDetector.setOnClickListener {
            startActivity(Intent(requireContext(), ScreenshotPhishingActivity::class.java))
        }
        
        layoutAPKScanner.setOnClickListener {
            startActivity(Intent(requireContext(), APKScannerActivity::class.java))
        }
        
        layoutURLProtection.setOnClickListener {
            startActivity(Intent(requireContext(), URLProtectionActivity::class.java))
        }
        
        layoutScheduledScans.setOnClickListener {
            startActivity(Intent(requireContext(), ScheduledScansActivity::class.java))
        }
        
        return view
    }
}
