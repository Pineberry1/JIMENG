package com.example.myapplication.feature.settings.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.app.Application

class SettingsViewModel(private val repository: ModelSettingsRepository) : ViewModel() {

    // Expose a flow of model configurations to the UI
    val modelConfigs: StateFlow<List<ModelConfigEntity>> = repository.getAllConfigs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // When the ViewModel is created, ensure the database has the default data.
        viewModelScope.launch {
            repository.populateInitialData()
        }
    }

    /**
     * Updates the parameters for a specific model.
     */
    fun updateModelConfig(modelName: String, topP: Double, temperature: Double) {
        viewModelScope.launch {
            val updatedConfig = ModelConfigEntity(
                modelName = modelName,
                topP = topP,
                temperature = temperature
            )
            repository.saveConfig(updatedConfig)
        }
    }
}

/**
 * Factory to create a SettingsViewModel instance, providing the required Application context.
 */
class SettingsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val repository = ModelSettingsRepository(application)
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
