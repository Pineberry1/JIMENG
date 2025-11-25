package com.example.myapplication.feature.chat.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.util.UUID

object ImageSaver {

    fun save(
        context: Context,
        imageUrl: String,
        callback: (Boolean, String) -> Unit
    ) {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false) // Required for saving to file
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = result.drawable.toBitmap()
                try {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "${UUID.randomUUID()}.jpg")
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                        }
                    }

                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        ?: throw Exception("MediaStore URI is null")

                    val stream: OutputStream? = resolver.openOutputStream(uri)
                    stream?.use { s ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, s)
                    } ?: throw Exception("OutputStream is null")

                    callback(true, "图片已保存到相册")
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(false, "保存失败: ${e.message}")
                }
            } else {
                callback(false, "图片加载失败")
            }
        }
    }
}
