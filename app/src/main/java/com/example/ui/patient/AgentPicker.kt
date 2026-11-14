package com.example.ui.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.agent.AgentConfiguration
import com.example.data.agent.AgentProvider

/** Stores only a preferred provider/model. Runtime adapters are not connected yet. */
@Composable
fun AgentPicker() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("agent_preferences", 0) }
    var configuration by remember { mutableStateOf(AgentConfiguration.fromStored(preferences.getString("provider", null), preferences.getString("model", null), preferences.getString("name", null))) }
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }) {
        Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp)); Text(configuration.displayName)
    }
    if (open) {
        var provider by remember { mutableStateOf(configuration.provider) }
        var model by remember { mutableStateOf(configuration.model) }
        var name by remember { mutableStateOf(configuration.customName) }
        AlertDialog(onDismissRequest = { open = false }, title = { Text("Your agent") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose the engine behind CarePulse.")
                AgentProvider.entries.forEach { option ->
                    OutlinedButton(onClick = { provider = option; model = "" }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (provider == option) "✓ ${option.label}" else option.label)
                    }
                }
                if (provider == AgentProvider.CUSTOM) OutlinedTextField(name, { name = it.take(60) }, label = { Text("Agent name · e.g. DC") })
                OutlinedTextField(model, { model = it.take(100) }, label = { Text("Model ID (optional)") }, singleLine = true)
                Text("Not connected. Replies remain scripted. This saves a preference only; no data or credentials are sent.", style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = { TextButton(onClick = {
            configuration = AgentConfiguration(provider, model.trim(), name.trim())
            preferences.edit().putString("provider", provider.name).putString("model", model.trim()).putString("name", name.trim()).apply()
            open = false
        }) { Text("Save") } }, dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } })
    }
}
