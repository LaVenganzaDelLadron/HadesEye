package com.project.hadeseye.services

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.project.hadeseye.api.VirusTotalApi
import org.json.JSONObject

class HashScanning {
    private val virusTotalApi = VirusTotalApi()

    fun scanHash(context: Context, hash: String): Map<String, String> {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val pyModule = py.getModule("hashScanning")
        val result = pyModule.callAttr("scan_hash", virusTotalApi.apikey, hash)

        val json = result.toString()
        val jsonObj = JSONObject(json)
        val map = mutableMapOf<String, String>()
        
        jsonObj.keys().forEach { key ->
            map[key] = jsonObj.getString(key)
        }
        
        return map
    }
}
