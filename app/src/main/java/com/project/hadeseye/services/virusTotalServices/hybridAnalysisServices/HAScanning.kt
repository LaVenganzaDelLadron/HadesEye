package com.project.hadeseye.services.virusTotalServices.hybridAnalysisServices

import android.content.Context
import android.net.Uri
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.project.hadeseye.api.HybridAnalysisApi
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class HAScanning {
    private val haApi = HybridAnalysisApi()

    fun ha_url_scan(context: Context, scanUrl: String): Map<String, String> {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val pyModule = py.getModule("haScanning")
        val result = pyModule.callAttr("scan_url_ha", haApi.apikey, scanUrl)

        val json = result.toString()
        val jsonObj = JSONObject(json)
        val map = mutableMapOf<String, String>()

        jsonObj.keys().forEach { key ->
            map[key] = jsonObj.getString(key)
        }

        return map
    }

    fun ha_ip_scan(context: Context, scanIp: String): Map<String, String> {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val pyModule = py.getModule("haScanning")
        val result = pyModule.callAttr("scan_ip_ha", haApi.apikey, scanIp)

        val json = result.toString()
        val jsonObj = JSONObject(json)
        val map = mutableMapOf<String, String>()

        jsonObj.keys().forEach { key ->
            map[key] = jsonObj.getString(key)
        }
        return map
    }

    fun ha_domain_scan(context: Context, scanDomain: String): Map<String, String> {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val pyModule = py.getModule("haScanning")
        val result = pyModule.callAttr("scan_domain_ha", haApi.apikey, scanDomain)

        val json = result.toString()
        val jsonObj = JSONObject(json)
        val map = mutableMapOf<String, String>()

        jsonObj.keys().forEach { key ->
            map[key] = jsonObj.getString(key)
        }
        return map
    }


    fun ha_file_scan(context: Context, fileUri: Uri?): Map<String, String> {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val inputStream: InputStream? = context.contentResolver.openInputStream(fileUri!!)
        val tempFile = File(context.cacheDir, "upload_file.tmp")
        val outputStream = FileOutputStream(tempFile)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()

        val py = Python.getInstance()
        val pyModule = py.getModule("haScanning")
        val result = pyModule.callAttr("scan_file_ha", haApi.apikey, tempFile.absolutePath)

        val json = result.toString()
        val jsonObj = JSONObject(json)
        val map = mutableMapOf<String, String>()
        jsonObj.keys().forEach { key -> map[key] = jsonObj.getString(key) }
        return map
    }

}
