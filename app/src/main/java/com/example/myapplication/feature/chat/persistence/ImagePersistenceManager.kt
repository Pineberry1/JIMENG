// In D:/app/JIMENG/app/src/main/java/com/example/myapplication/feature/chat/util/ImagePersistenceManager.kt
package com.example.myapplication.feature.chat.persistence

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

object ImagePersistenceManager {

    // 1. 计算文件内容的MD5哈希值
    private fun calculateMD5(context: Context, uri: Uri): String? {
        return try {
            val md = MessageDigest.getInstance("MD5")

            // --- 核心修复：根据URI的协议选择正确的打开方式 ---
            val inputStream = if (uri.scheme == "content") {
                // 如果是 content URI (来自相册等)，使用 ContentResolver
                context.contentResolver.openInputStream(uri)
            } else {
                // 如果是 file URI (来自我们的内部存储)，直接创建文件输入流
                // uri.path 会返回文件的绝对路径
                uri.path?.let { File(it).inputStream() }
            }
            // --- 修复结束 ---

            inputStream?.use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    md.update(buffer, 0, read)
                }
            }
            // 将byte数组转换为16进制字符串
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("ImagePersistenceManager", "Failed to calculate MD5 for uri: $uri", e)
            null
        }
    }

    // 2. 将哈希值与内部文件路径关联起来 (使用SharedPreferences做持久化)
    private fun getInternalPathForHash(context: Context, hash: String): String? {
        val prefs = context.getSharedPreferences("image_hashes", Context.MODE_PRIVATE)
        return prefs.getString(hash, null)
    }

    private fun saveHashToPathMapping(context: Context, hash: String, path: String) {
        val prefs = context.getSharedPreferences("image_hashes", Context.MODE_PRIVATE)
        prefs.edit().putString(hash, path).apply()
    }

    // 3. 核心方法：获取或创建内部副本
    fun getOrCreateInternalCopy(context: Context, originalUri: Uri): String? {
        // 计算原始Uri内容的哈希值
        val hash = calculateMD5(context, originalUri) ?: return null

        // 检查这个哈希是否已经有对应的内部文件
        val existingPath = getInternalPathForHash(context, hash)
        if (existingPath != null && File(existingPath).exists()) {
            Log.d("ImagePersistenceManager", "Hash hit. Reusing existing file: $existingPath")
            return existingPath // 如果文件存在，直接返回路径
        }

        // 如果哈希不存在，或者文件被删了，则创建新副本
        Log.d("ImagePersistenceManager", "Hash miss. Creating new internal copy.")
        return try {
            context.contentResolver.openInputStream(originalUri)?.use { inputStream ->
                // 使用哈希值作为文件名，更具唯一性
                val internalImagesDir = File(context.filesDir, "images")
                if (!internalImagesDir.exists()) internalImagesDir.mkdirs()

                val internalFile = File(internalImagesDir, hash) // 用哈希做文件名
                FileOutputStream(internalFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                val newPath = internalFile.absolutePath
                // 保存新的哈希-路径映射
                saveHashToPathMapping(context, hash, newPath)
                newPath
            }
        } catch (e: Exception) {
            Log.e("ImagePersistenceManager", "Failed to create internal copy", e)
            null
        }
    }
}
