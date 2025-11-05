package com.project.hadeseye.services.virusTotalServices

import android.content.Context
import android.net.Uri
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.project.hadeseye.api.VirusTotalApi
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class VTScanning {
    val virus_total_api = VirusTotalApi()


    fun vt_url_scan(context: Context, scan_url: String): Map<String, String> {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val pyModule = py.getModule("vtScanning")
        val result = pyModule.callAttr("scan_url", virus_total_api.apikey, scan_url)

        val json = result.toString()
        val jsonObj = JSONObject(json)
        val map = mutableMapOf<String, String>()
        jsonObj.keys().forEach { key ->
            map[key] = jsonObj.getString(key)
        }
        return map
    }


    fun vt_ip_scan(context: Context, scan_ip: String): Map<String, String> {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val pyModule = py.getModule("vtScanning")
        val result = pyModule.callAttr("scan_ip", virus_total_api.apikey, scan_ip)

        val json = result.toString()
        val jsonObj = JSONObject(json)
        val map = mutableMapOf<String, String>()
        jsonObj.keys().forEach { key ->
            map[key] = jsonObj.getString(key)
        }
        return map
    }

    fun vt_domain_scan(context: Context, scanDomain: String): Map<String, String> {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val pyModule = py.getModule("vtScanning")
        val result = pyModule.callAttr("scan_domain", virus_total_api.apikey, scanDomain)

        val json = result.toString()
        val jsonObj = JSONObject(json)
        val map = mutableMapOf<String, String>()
        jsonObj.keys().forEach { key ->
            map[key] = jsonObj.getString(key)
        }
        return map
    }


    fun vt_file_scan(context: Context, fileUri: Uri?): Map<String, String> {
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
        val pyModule = py.getModule("vtScanning")
        val result = pyModule.callAttr("scan_file", virus_total_api.apikey, tempFile.absolutePath)

        val json = result.toString()
        val jsonObj = JSONObject(json)
        val map = mutableMapOf<String, String>()
        jsonObj.keys().forEach { key -> map[key] = jsonObj.getString(key) }
        return map
    }


}