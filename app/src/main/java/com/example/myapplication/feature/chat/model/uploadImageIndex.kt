
package com.example.myapplication.feature.chat.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_upload_index")
data class uploadImageIndex(
    @PrimaryKey
    val localUri: String, // 本地文件URI作为主键
    val ossUrl: String,   // 上传后的OSS URL
    val creationTimestamp: Long = System.currentTimeMillis() // 记录创建时的时间戳（毫秒）
)
