package com.example.myapplication.feature.chat.model

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.Response
interface DashScopeApiService {

    @Streaming
    @POST("compatible-mode/v1/chat/completions")
    suspend fun generateTxt(
        @Header("Authorization") authorization: String,
        @Body request: TextGenerationRequest
    ): ResponseBody

    @POST("compatible-mode/v1/chat/completions")
    suspend fun generateTxtOnce(
        @Header("Authorization") authorization: String,
        @Body request: TextGenerationRequest
    ): Response<ResponseBody>
    @Streaming
    @POST("compatible-mode/v1/chat/completions")
    suspend fun generateTxtWithImage(
        @Header("Authorization") authorization: String,
        @Header("X-DashScope-OssResourceResolve") ossResourceResolve: String = "enable",
        @Body request: TextGenerationRequest
    ): ResponseBody

    @POST("api/v1/services/aigc/multimodal-generation/generation")
    suspend fun generateImage(
        @Header("Authorization") authorization: String,
        @Body request: QwenImageRequest
    ): ResponseBody

    @GET("api/v1/uploads")
    suspend fun getUploadPolicy(
        @Header("Authorization") authHeader: String,
        @Query("action") action: String = "getPolicy",
        @Query("model") model: String
    ): UploadPolicyResponse
}

data class UploadPolicyResponse(
    val data: PolicyData
)

data class PolicyData(
    @SerializedName("upload_dir")
    val uploadDir: String,
    @SerializedName("oss_access_key_id")
    val ossAccessKeyId: String,
    val signature: String,
    val policy: String,
    @SerializedName("x_oss_object_acl")
    val xOssObjectAcl: String,
    @SerializedName("x_oss_forbid_overwrite")
    val xOssForbidOverwrite: String,
    @SerializedName("upload_host")
    val uploadHost: String
)
