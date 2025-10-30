package com.project.hadeseye.services.virusTotalServices.urlServices

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.project.hadeseye.api.UrlScanApi
import org.json.JSONObject

class URLScanning {

    private val urlScanApi = UrlScanApi()

    fun us_url_scan(context: Context, scanUrl: String): Map<String, String> {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val pyModule = py.getModule("urlScanning")
        val result = pyModule.callAttr("scan_url", urlScanApi.apikey, scanUrl)

        val json = result.toString()
        val jsonObj = JSONObject(json)
        val map = mutableMapOf<String, String>()

        jsonObj.keys().forEach { key ->
            map[key] = jsonObj.getString(key)
        }

        return map
    }
}
