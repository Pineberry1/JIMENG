package com.example.myapplication.feature.settings.model

import android.app.Application
import com.example.myapplication.feature.chat.persistence.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ModelSettingsRepository(application: Application) {

    private val modelConfigDao: ModelConfigDao

    init {
        val database = AppDatabase.getDatabase(application)
        modelConfigDao = database.modelConfigDao()
    }

    suspend fun getConfig(modelName: String): ModelConfigEntity? {
        return modelConfigDao.getByName(modelName)
    }

    suspend fun saveConfig(config: ModelConfigEntity) {
        modelConfigDao.upsert(config)
    }

    fun getAllConfigs(): Flow<List<ModelConfigEntity>> {
        return modelConfigDao.getAll()
    }

    suspend fun populateInitialData() {
        if (getAllConfigs().first().isEmpty()) {
            val defaultConfig1 = ModelConfigEntity(
                modelName = "qwen-plus",
                temperature = 0.7,
                topP = 0.8
            )
            val defaultConfig2 = ModelConfigEntity(
                modelName = "qwen-image-plus",
                temperature = 0.7,
                topP = 0.8
            )
            saveConfig(defaultConfig1)
            saveConfig(defaultConfig2)
        }
    }
}
