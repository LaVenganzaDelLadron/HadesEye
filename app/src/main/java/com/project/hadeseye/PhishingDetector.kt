package com.project.hadeseye

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.PyException
import org.json.JSONObject

class PhishingDetector(private val context: Context) {
    
    fun analyzeText(text: String): PhishingResult {
        return try {
            val py = Python.getInstance()
            val module = py.getModule("phishingDetector")
            val result = module.callAttr("analyze_text_for_phishing", text).toString()
            
            val jsonResult = JSONObject(result)
            val status = jsonResult.getString("status")
            
            if (status == "success") {
                val urls = mutableListOf<String>()
                val urlsArray = jsonResult.getJSONArray("detected_urls")
                for (i in 0 until urlsArray.length()) {
                    urls.add(urlsArray.getString(i))
                }
                
                val keywords = mutableListOf<String>()
                val keywordsArray = jsonResult.getJSONArray("suspicious_keywords")
                for (i in 0 until keywordsArray.length()) {
                    keywords.add(keywordsArray.getString(i))
                }
                
                val recommendations = mutableListOf<String>()
                val recsArray = jsonResult.getJSONArray("recommendations")
                for (i in 0 until recsArray.length()) {
                    recommendations.add(recsArray.getString(i))
                }
                
                PhishingResult(
                    success = true,
                    riskScore = jsonResult.getInt("risk_score"),
                    riskLevel = jsonResult.getString("risk_level"),
                    detectedUrls = urls,
                    suspiciousKeywords = keywords,
                    recommendations = recommendations,
                    error = null
                )
            } else {
                PhishingResult(
                    success = false,
                    error = jsonResult.getString("message")
                )
            }
        } catch (e: PyException) {
            PhishingResult(success = false, error = "Python Error: ${e.message}")
        } catch (e: Exception) {
            PhishingResult(success = false, error = "Error: ${e.message}")
        }
    }
    
    data class PhishingResult(
        val success: Boolean,
        val riskScore: Int = 0,
        val riskLevel: String = "",
        val detectedUrls: List<String> = emptyList(),
        val suspiciousKeywords: List<String> = emptyList(),
        val recommendations: List<String> = emptyList(),
        val error: String? = null
    )
}
