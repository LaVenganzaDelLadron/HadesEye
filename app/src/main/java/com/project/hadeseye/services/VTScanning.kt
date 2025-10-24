package com.project.hadeseye.services

import com.chaquo.python.Python
import android.content.Context
import android.net.Uri
import com.chaquo.python.android.AndroidPlatform
import com.project.hadeseye.api.VirusTotalApi
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class VTScanning {
    val virus_total_api = VirusTotalApi()


    fun url_scan(context: Context, scan_url: String): Map<String, String> {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val pyModule = py.getModule("vtScanning")
        val result = pyModule.callAttr("scan_url", virus_total_api.apikey, scan_url)

        val json = result.toString()
        val jsonObj = org.json.JSONObject(json)
        val map = mutableMapOf<String, String>()
        jsonObj.keys().forEach { key ->
            map[key] = jsonObj.getString(key)
        }
        return map
    }

    fun file_scan(context: Context, fileUri: Uri?): Map<String, String> {
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
        val jsonObj = org.json.JSONObject(json)
        val map = mutableMapOf<String, String>()
        jsonObj.keys().forEach { key -> map[key] = jsonObj.getString(key) }
        return map
    }


}