package com.homeos.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.homeos.tv.data.TvConfig

/**
 * Manual setup with the remote. Typing a long Home Assistant token on a TV
 * keyboard is painful, so setup-tv.ps1 (adb) is the recommended path; this
 * screen is the fallback and shows what's currently configured.
 */
@Composable
fun SettingsScreen(
    config: TvConfig,
    status: SettingsStatus,
    onSaveHomeAssistant: (url: String, token: String, label: String) -> Unit,
    onSignInJellyfin: (url: String, user: String, password: String) -> Unit,
) {
    var haUrl by remember { mutableStateOf(config.haUrl) }
    var haToken by remember { mutableStateOf("") }
    var haLabel by remember { mutableStateOf(config.haLabel) }
    var jfUrl by remember { mutableStateOf(config.jellyfinUrl) }
    var jfUser by remember { mutableStateOf(config.jellyfinUserName) }
    var jfPassword by remember { mutableStateOf("") }

    androidx.compose.material3.MaterialTheme(colorScheme = darkColorScheme()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 48.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            status.message?.let { Text(it, color = MaterialTheme.colorScheme.onBackground) }

            Section("Home Assistant")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Field("URL (e.g. https://ha.home.example.com)", haUrl, { haUrl = it }, KeyboardType.Uri, Modifier.width(420.dp))
                Field("Label to show (default: tv)", haLabel, { haLabel = it }, modifier = Modifier.width(260.dp))
            }
            Field(
                if (config.haToken.isNotEmpty()) "Long-lived token (saved; leave blank to keep)" else "Long-lived access token",
                haToken, { haToken = it }, secret = true, modifier = Modifier.width(696.dp),
            )
            Button(onClick = { onSaveHomeAssistant(haUrl, haToken, haLabel); haToken = "" }) { Text("Save Home Assistant") }

            Section("Jellyfin")
            Field("Server URL (e.g. https://media.home.example.com)", jfUrl, { jfUrl = it }, KeyboardType.Uri, Modifier.width(696.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Field("Username", jfUser, { jfUser = it }, modifier = Modifier.width(340.dp))
                Field("Password", jfPassword, { jfPassword = it }, secret = true, modifier = Modifier.width(340.dp))
            }
            Button(
                onClick = { onSignInJellyfin(jfUrl, jfUser, jfPassword); jfPassword = "" },
                enabled = !status.busy,
            ) { Text(if (config.jellyfinConfigured) "Sign in again" else "Sign in") }
            if (config.jellyfinConfigured) {
                Text(
                    "Signed in as ${config.jellyfinUserName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Section("Steam")
            Text(
                "Games stream from the HomeOS PC with Moonlight. Install it from the app store, open it once and pair with the PIN it shows.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    secret: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { androidx.compose.material3.Text(label) },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else keyboard),
        modifier = modifier,
    )
}
