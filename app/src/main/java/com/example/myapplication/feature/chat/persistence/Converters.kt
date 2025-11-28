// file: com/example/myapplication/feature/chat/model/Converters.kt

package com.example.myapplication.feature.chat.persistence

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        // 如果列表为空或null，则返回null
        if (value.isNullOrEmpty()) {
            return null
        }
        // 使用 Gson 将 List<String> 转换为 JSON 格式的字符串
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        // 如果字符串是空的或null，则返回null
        if (value.isNullOrEmpty()) {
            return null
        }
        // 定义要转换回的类型
        val listType = object : TypeToken<List<String>>() {}.type
        // 使用 Gson 将 JSON 字符串解析回 List<String>
        return gson.fromJson(value, listType)
    }
}
