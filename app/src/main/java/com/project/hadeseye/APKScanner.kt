package com.project.hadeseye

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.PyException
import org.json.JSONObject

class APKScanner(private val context: Context) {
    
    fun analyzeApk(apkPath: String): APKResult {
        return try {
            val py = Python.getInstance()
            val module = py.getModule("apkScanner")
            val result = module.callAttr("analyze_apk", apkPath).toString()
            
            val jsonResult = JSONObject(result)
            val status = jsonResult.getString("status")
            
            if (status == "success") {
                val suspiciousFiles = mutableListOf<String>()
                val filesArray = jsonResult.getJSONArray("suspicious_files")
                for (i in 0 until filesArray.length()) {
                    suspiciousFiles.add(filesArray.getString(i))
                }
                
                val securityIssues = mutableListOf<String>()
                val issuesArray = jsonResult.getJSONArray("security_issues")
                for (i in 0 until issuesArray.length()) {
                    securityIssues.add(issuesArray.getString(i))
                }
                
                APKResult(
                    success = true,
                    riskScore = jsonResult.getInt("risk_score"),
                    riskLevel = jsonResult.getString("risk_level"),
                    dexCount = jsonResult.getInt("dex_count"),
                    nativeLibCount = jsonResult.getInt("native_lib_count"),
                    totalFiles = jsonResult.getInt("total_files"),
                    suspiciousFiles = suspiciousFiles,
                    securityIssues = securityIssues,
                    hasObfuscation = jsonResult.getBoolean("has_obfuscation"),
                    error = null
                )
            } else {
                APKResult(
                    success = false,
                    error = jsonResult.getString("message")
                )
            }
        } catch (e: PyException) {
            APKResult(success = false, error = "Python Error: ${e.message}")
        } catch (e: Exception) {
            APKResult(success = false, error = "Error: ${e.message}")
        }
    }
    
    fun quickCheck(apkPath: String): QuickCheckResult {
        return try {
            val py = Python.getInstance()
            val module = py.getModule("apkScanner")
            val result = module.callAttr("quick_apk_check", apkPath).toString()
            
            val jsonResult = JSONObject(result)
            val status = jsonResult.getString("status")
            
            if (status == "success") {
                QuickCheckResult(
                    success = true,
                    validApk = jsonResult.getBoolean("valid_apk"),
                    hasManifest = jsonResult.getBoolean("has_manifest"),
                    hasDex = jsonResult.getBoolean("has_dex"),
                    hasResources = jsonResult.getBoolean("has_resources"),
                    fileCount = jsonResult.getInt("file_count"),
                    error = null
                )
            } else {
                QuickCheckResult(
                    success = false,
                    error = jsonResult.getString("message")
                )
            }
        } catch (e: PyException) {
            QuickCheckResult(success = false, error = "Python Error: ${e.message}")
        } catch (e: Exception) {
            QuickCheckResult(success = false, error = "Error: ${e.message}")
        }
    }
    
    data class APKResult(
        val success: Boolean,
        val riskScore: Int = 0,
        val riskLevel: String = "",
        val dexCount: Int = 0,
        val nativeLibCount: Int = 0,
        val totalFiles: Int = 0,
        val suspiciousFiles: List<String> = emptyList(),
        val securityIssues: List<String> = emptyList(),
        val hasObfuscation: Boolean = false,
        val error: String? = null
    )
    
    data class QuickCheckResult(
        val success: Boolean,
        val validApk: Boolean = false,
        val hasManifest: Boolean = false,
        val hasDex: Boolean = false,
        val hasResources: Boolean = false,
        val fileCount: Int = 0,
        val error: String? = null
    )
}
