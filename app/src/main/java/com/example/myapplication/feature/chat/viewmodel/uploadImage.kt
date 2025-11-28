package com.example.myapplication.feature.chat.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.myapplication.BuildConfig
import com.example.myapplication.feature.chat.model.DashScopeApiService
import com.example.myapplication.feature.chat.model.PolicyData
import com.example.myapplication.feature.chat.model.RetrofitClient
import com.example.myapplication.feature.chat.persistence.ImagePersistenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
object UploadImage {

    private val dashScopeService: DashScopeApiService = RetrofitClient.dashScopeService
    private val okHttpClient = OkHttpClient()
    private val api_key = BuildConfig.DASHSCOPE_API_KEY

    suspend fun uploadFileAndGetUrl(context: Context, modelName: String, uri: Uri): String {
        return withContext(Dispatchers.IO) {
            val filePath = getPathFromUri(context, uri)
                ?: throw IOException("Can't get file path from Uri")

            // 1. Get upload policy
            val policyData = getUploadPolicy(api_key, modelName)

            // 2. Upload file to OSS
            uploadFileToOss(policyData, filePath)
        }
    }
    suspend fun uploadFileAndGetUrlfromLocaluri(context: Context,modelName: String, uri: Uri): String {
        return withContext(Dispatchers.IO) {
            val internalFilePath = ImagePersistenceManager.getOrCreateInternalCopy(context,
                uri) ?: ""

            // 1. Get upload policy
            val policyData = getUploadPolicy(api_key, modelName)

            // 2. Upload file to OSS
            uploadFileToOss(policyData, internalFilePath)
        }
    }

    private suspend fun getUploadPolicy(apiKey: String, modelName: String): PolicyData {
        val response = dashScopeService.getUploadPolicy(
            authHeader = "Bearer $apiKey",
            model = modelName
        )
        return response.data
    }

    private fun uploadFileToOss(policyData: PolicyData, filePath: String): String {
        val file = File(filePath)
        if (!file.exists()) {
            throw IOException("File not found at path: $filePath")
        }
        val fileName = file.name
        val key = "${policyData.uploadDir}/${fileName}"

        val fileBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("OSSAccessKeyId", policyData.ossAccessKeyId)
            .addFormDataPart("Signature", policyData.signature)
            .addFormDataPart("policy", policyData.policy)
            .addFormDataPart("x-oss-object-acl", policyData.xOssObjectAcl)
            .addFormDataPart("x-oss-forbid-overwrite", policyData.xOssForbidOverwrite)
            .addFormDataPart("key", key)
            .addFormDataPart("success_action_status", "200")
            .addFormDataPart("file", fileName, fileBody)
            .build()

        val request = Request.Builder()
            .url(policyData.uploadHost)
            .post(multipartBody)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to upload file: ${response.code} ${response.message} ${response.body?.string()}")
            }
        }

        return "oss://$key"
    }

    private fun getPathFromUri(context: Context, uri: Uri): String? {
        return try {
            // 使用 ContentResolver 打开原始图片的输入流
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // 为了防止文件名冲突，我们为副本生成一个唯一的文件名
                val uniqueFileName =
                    "${UUID.randomUUID()}.jpg" // 或者根据MIME类型决定后缀

                // 定义文件在应用内部存储的存放位置 (例如，一个叫 'images' 的子目录)
                val internalImagesDir = File(context.filesDir, "images")
                if (!internalImagesDir.exists()) {
                    internalImagesDir.mkdirs() // 如果目录不存在，则创建
                }
                val internalFile = File(internalImagesDir, uniqueFileName)

                // 使用 FileOutputStream 将输入流写入到内部存储文件中
                FileOutputStream(internalFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                // 返回这个内部副本的绝对路径
                internalFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e("UploadImage", "Failed to copy file from Uri to internal storage: $uri", e)
            null // 发生任何异常都返回 null
        }
    }
}
