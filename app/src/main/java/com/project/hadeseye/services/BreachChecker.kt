package com.project.hadeseye.services

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONObject

class BreachChecker {

    fun checkPasswordBreach(context: Context, password: String): Map<String, String> {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val pyModule = py.getModule("breachChecker")
        val result = pyModule.callAttr("check_password_breach", password)

        val json = result.toString()
        val jsonObj = JSONObject(json)
        val map = mutableMapOf<String, String>()
        
        jsonObj.keys().forEach { key ->
            map[key] = jsonObj.getString(key)
        }
        
        return map
    }

    fun checkEmailBreach(context: Context, email: String): Map<String, String> {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val pyModule = py.getModule("breachChecker")
        val result = pyModule.callAttr("check_email_breach", email)

        val json = result.toString()
        val jsonObj = JSONObject(json)
        val map = mutableMapOf<String, String>()
        
        jsonObj.keys().forEach { key ->
            map[key] = jsonObj.getString(key)
        }
        
        return map
    }
}
