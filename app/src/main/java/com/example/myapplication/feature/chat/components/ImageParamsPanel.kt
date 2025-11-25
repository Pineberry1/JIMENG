package com.example.myapplication.feature.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.myapplication.feature.chat.model.ImageGenParams

@Composable
fun ImageParamsPanel(
    params: ImageGenParams,
    onParamsChanged: (ImageGenParams) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("图片生成参数", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))

        TextField(
            value = params.negativePrompt,
            onValueChange = { onParamsChanged(params.copy(negativePrompt = it)) },
            label = { Text("负向提示") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("扩展提示")
            Switch(
                checked = params.promptExtend,
                onCheckedChange = { onParamsChanged(params.copy(promptExtend = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("水印")
            Switch(
                checked = params.watermark,
                onCheckedChange = { onParamsChanged(params.copy(watermark = it)) }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = params.width,
                onValueChange = { onParamsChanged(params.copy(width = it)) },
                label = { Text("宽度") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            TextField(
                value = params.height,
                onValueChange = { onParamsChanged(params.copy(height = it)) },
                label = { Text("高度") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
