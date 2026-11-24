package ru.caola.apla.data

import android.content.Context
import org.json.JSONObject

class DemoRepository(private val context: Context) {
    val data: JSONObject by lazy {
        context.assets.open("demo_data.json").bufferedReader().use { JSONObject(it.readText()) }
    }
    fun section(name: String): JSONObject = data.optJSONObject(name) ?: JSONObject()
}
