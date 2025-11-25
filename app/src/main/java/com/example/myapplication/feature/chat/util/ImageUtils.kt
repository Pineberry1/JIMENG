package com.example.myapplication.feature.chat.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.OutputStream
import java.util.UUID

/**
 * Downloads an image from a URL using OkHttp and saves it to the device's gallery.
 * This entire operation is performed on the IO dispatcher.
 * @return `true` if successful, `false` otherwise.
 */
internal suspend fun saveImageToGallery(context: Context, imageUrl: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            // 1. Download image bytes using OkHttpClient
            val client = OkHttpClient()
            val request = Request.Builder().url(imageUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to download image: ${response.code}")
            }
            val inputStream = response.body?.byteStream() ?: throw Exception("Response body is null")

            // 2. Decode bitmap from the byte stream
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // 3. Save Bitmap to MediaStore
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

            true // Return true on success
        } catch (e: Exception) {
            e.printStackTrace()
            false // Return false on failure
        }
    }
}
