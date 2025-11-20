package com.example.myapplication.feature.settings.model

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository that acts as a single source of truth for model settings.
 * It abstracts the data source (Room) from the rest of the app.
 */
class ModelSettingsRepository(context: Context) {

    private val modelConfigDao: ModelConfigDao

    init {
        val database = AppDatabase.getDatabase(context)
        modelConfigDao = database.modelConfigDao()
    }

    /**
     * Gets a flow of all model configurations from the database.
     */
    fun getAllConfigs(): Flow<List<ModelConfigEntity>> = modelConfigDao.getAll()

    /**
     * Gets the configuration for a specific model.
     */
    suspend fun getConfig(modelName: String): ModelConfigEntity? {
        return modelConfigDao.getByName(modelName)
    }

    /**
     * Updates a model's configuration.
     */
    suspend fun updateConfig(modelConfig: ModelConfigEntity) {
        modelConfigDao.upsert(modelConfig)
    }

    /**
     * Checks if the database is empty and, if so, populates it with default values.
     * This should be called when the application starts.
     */
    suspend fun populateInitialData() {
        if (modelConfigDao.getAll().first().isEmpty()) {
            val defaultConfigs = listOf(
                ModelConfigEntity(modelName = "qwen-plus", topP = 0.8, temperature = 0.7),
                ModelConfigEntity(modelName = "qwen-image-plus", topP = 0.8, temperature = 0.7)
            )
            defaultConfigs.forEach { config ->
                modelConfigDao.upsert(config)
            }
        }
    }
}
