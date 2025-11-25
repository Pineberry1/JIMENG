package com.example.myapplication.feature.chat.model

data class ImageGenParams(
    val negativePrompt: String = "",
    val promptExtend: Boolean = true,
    val watermark: Boolean = false,
    val width: String = "1328",
    val height: String = "1328"
)
