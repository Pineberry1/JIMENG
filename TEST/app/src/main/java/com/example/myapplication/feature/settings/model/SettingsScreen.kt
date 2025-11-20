package com.example.myapplication.feature.settings.model

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context.applicationContext as Application)
    )
    val modelConfigs by viewModel.modelConfigs.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var selectedConfig by remember { mutableStateOf<ModelConfigEntity?>(null) }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(modelConfigs) { config ->
            ModelConfigItem(config = config) {
                selectedConfig = it
                showDialog = true
            }
            Divider()
        }
    }

    if (showDialog && selectedConfig != null) {
        EditParametersDialog(
            config = selectedConfig!!,
            onDismiss = { showDialog = false },
            onSave = {
                viewModel.updateModelConfig(it.modelName, it.topP, it.temperature)
                showDialog = false
            }
        )
    }
}

@Composable
private fun ModelConfigItem(config: ModelConfigEntity, onCustomizeClick: (ModelConfigEntity) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = config.modelName, style = MaterialTheme.typography.titleMedium)
            Text(text = "Top P: ${String.format("%.2f", config.topP)}, Temp: ${String.format("%.2f", config.temperature)}", style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = { onCustomizeClick(config) }) {
            Text("Customize")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditParametersDialog(
    config: ModelConfigEntity,
    onDismiss: () -> Unit,
    onSave: (ModelConfigEntity) -> Unit
) {
    var tempTopP by remember { mutableStateOf(config.topP.toFloat()) }
    var tempTemperature by remember { mutableStateOf(config.temperature.toFloat()) }

    AlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Edit: ${config.modelName}", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(24.dp))

                ParameterSlider(label = "Top P", value = tempTopP, onValueChange = { tempTopP = it })
                Spacer(Modifier.height(16.dp))
                ParameterSlider(label = "Temperature", value = tempTemperature, onValueChange = { tempTemperature = it })

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onSave(config.copy(topP = tempTopP.toDouble(), temperature = tempTemperature.toDouble()))
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun ParameterSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Text(text = "$label: ${String.format("%.2f", value)}")
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f
        )
    }
}
