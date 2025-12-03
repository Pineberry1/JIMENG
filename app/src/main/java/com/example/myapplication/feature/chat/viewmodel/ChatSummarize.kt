package com.example.myapplication.feature.chat.viewmodel

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

fun getSummaryFile(context: Context, conversationId: String): File {
    val directory = File(context.filesDir, "summaries")
    if (!directory.exists()) {
        directory.mkdirs()
    }
    return File(directory, "$conversationId.summary")
}

suspend fun readSummary(context: Context, conversationId: String): String {
    return withContext(Dispatchers.IO) {
        val file = getSummaryFile(context, conversationId)
        if (file.exists()) file.readText() else ""
    }
}

suspend fun appendToSummary(context: Context, conversationId: String, newSummaryPart: String) {
    withContext(Dispatchers.IO) {
        val file = getSummaryFile(context, conversationId)
        file.appendText("\n\n" + newSummaryPart)
    }
}
suspend fun deleteSummary(context: Context, conversationId: String) {
    withContext(Dispatchers.IO) {
        val file = getSummaryFile(context, conversationId)
        if (file.exists()) {
            file.delete()
        }
    }
}
