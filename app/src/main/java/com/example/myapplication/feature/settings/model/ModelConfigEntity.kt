package com.example.myapplication.feature.settings.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_configs")
data class ModelConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "model_name")
    val modelName: String,

    @ColumnInfo(name = "top_p")
    val topP: Double,

    @ColumnInfo(name = "temperature")
    val temperature: Double
)
