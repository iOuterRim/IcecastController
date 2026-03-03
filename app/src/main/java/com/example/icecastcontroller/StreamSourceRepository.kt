package com.example.icecastcontroller

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StreamSourceRepository(context: Context) {

    private val prefs = context.getSharedPreferences("stream_sources", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "sources_json"

    fun getSources(): MutableList<StreamSource> {
        val json = prefs.getString(key, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<StreamSource>>() {}.type
            gson.fromJson(json, type)
        } else {
            // Beim ersten Start Demo-Quellen laden
            StreamSource.EXAMPLES.toMutableList()
        }
    }

    fun saveSources(sources: List<StreamSource>) {
        prefs.edit {
            putString(key, gson.toJson(sources))
        }
    }

    fun addSource(source: StreamSource) {
        val sources = getSources()
        sources.add(source)
        saveSources(sources)
    }

    fun removeSource(id: String) {
        val sources = getSources()
        sources.removeAll { it.id == id }
        saveSources(sources)
    }
}
