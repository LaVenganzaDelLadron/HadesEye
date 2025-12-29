package com.project.hadeseye.services

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONObject

class NetworkScanner {

    fun scanNetwork(context: Context, targetIp: String): Map<String, String> {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val pyModule = py.getModule("networkScanner")
        val result = pyModule.callAttr("scan_network", targetIp)

        val json = result.toString()
        val jsonObj = JSONObject(json)
        val map = mutableMapOf<String, String>()
        
        jsonObj.keys().forEach { key ->
            map[key] = jsonObj.getString(key)
        }
        
        return map
    }

    fun getLocalIp(context: Context): Map<String, String> {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val pyModule = py.getModule("networkScanner")
        val result = pyModule.callAttr("get_local_ip")

        val json = result.toString()
        val jsonObj = JSONObject(json)
        val map = mutableMapOf<String, String>()
        
        jsonObj.keys().forEach { key ->
            map[key] = jsonObj.getString(key)
        }
        
        return map
    }
}
