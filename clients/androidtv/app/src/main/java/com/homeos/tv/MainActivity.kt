package com.homeos.tv

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.homeos.tv.data.InstalledApps
import com.homeos.tv.player.PlayerActivity
import com.homeos.tv.ui.HomeScreen
import com.homeos.tv.ui.HomeViewModel
import com.homeos.tv.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val settingsStatus by viewModel.settingsStatus.collectAsStateWithLifecycle()
                var showSettings by rememberSaveable { mutableStateOf(false) }

                if (showSettings) {
                    BackHandler { showSettings = false }
                    SettingsScreen(
                        config = state.config,
                        status = settingsStatus,
                        onSaveHomeAssistant = viewModel::saveHomeAssistant,
                        onSignInJellyfin = viewModel::signInJellyfin,
                    )
                } else {
                    HomeScreen(
                        state = state,
                        onPressTile = viewModel::pressTile,
                        onPlay = { card -> startActivity(PlayerActivity.intent(this, card)) },
                        onLaunchApp = { pkg ->
                            if (!InstalledApps.launch(this, pkg)) {
                                Toast.makeText(this, "Couldn't open that app", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setVisible(true)
    }

    override fun onPause() {
        viewModel.setVisible(false)
        super.onPause()
    }
}
