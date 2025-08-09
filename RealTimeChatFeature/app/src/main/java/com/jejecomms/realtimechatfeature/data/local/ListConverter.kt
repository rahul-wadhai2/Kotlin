package com.jejecomms.realtimechatfeature.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * A TypeConverter class for Room database operations.
 */
class ListConverter {
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        if (list == null) return null
        return Gson().toJson(list)
    }

    @TypeConverter
    fun toStringList(json: String?): List<String>? {
        if (json == null) return null
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(json, type)
    }
}