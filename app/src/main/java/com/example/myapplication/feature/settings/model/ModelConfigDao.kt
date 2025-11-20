package com.example.myapplication.feature.settings.model

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelConfigDao {

    /**
     * Gets all model configurations from the database.
     * Returns a Flow, so the UI can automatically update when data changes.
     */
    @Query("SELECT * FROM model_configs")
    fun getAll(): Flow<List<ModelConfigEntity>>

    /**
     * Inserts a new model configuration or updates an existing one.
     */
    @Upsert
    suspend fun upsert(modelConfig: ModelConfigEntity)

    /**
     * Gets a single model configuration by its name.
     */
    @Query("SELECT * FROM model_configs WHERE model_name = :modelName LIMIT 1")
    suspend fun getByName(modelName: String): ModelConfigEntity?
}
