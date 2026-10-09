package com.homeos.tv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.homeos.tv.data.AppRows
import com.homeos.tv.data.HaEntity
import com.homeos.tv.data.InstalledApps
import com.homeos.tv.data.MediaCard
import com.homeos.tv.data.TvConfig
import com.homeos.tv.data.normalizeBaseUrl
import com.homeos.tv.homeOs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeState(
    val config: TvConfig = TvConfig(),
    val homeTiles: List<HaEntity> = emptyList(),
    val homeError: String? = null,
    val continueWatching: List<MediaCard> = emptyList(),
    val nextUp: List<MediaCard> = emptyList(),
    val recentlyAdded: List<MediaCard> = emptyList(),
    val mediaError: String? = null,
    val apps: AppRows = AppRows(emptyList(), emptyList(), emptyList()),
)

data class SettingsStatus(val busy: Boolean = false, val message: String? = null)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.homeOs
    private val _state = MutableStateFlow(HomeState(config = app.configStore.config.value))
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _settingsStatus = MutableStateFlow(SettingsStatus())
    val settingsStatus: StateFlow<SettingsStatus> = _settingsStatus.asStateFlow()

    /** True while the home screen is visible; polling stops while a video plays. */
    private val visible = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            combine(app.configStore.config, visible) { config, isVisible -> config to isVisible }
                .collectLatest { (config, isVisible) ->
                    _state.update { it.copy(config = config) }
                    if (!isVisible) return@collectLatest
                    refreshApps()
                    coroutineScope {
                        launch { refreshMedia(config) }
                        launch { pollHome(config) }
                    }
                }
        }
    }

    fun setVisible(isVisible: Boolean) {
        visible.value = isVisible
    }

    fun pressTile(entity: HaEntity) {
        val config = _state.value.config
        viewModelScope.launch {
            runCatching { app.homeAssistant.press(config, entity) }
                .onFailure { e -> _state.update { it.copy(homeError = e.message) } }
            // Pick up the new state without waiting for the next poll.
            delay(400)
            loadHome(config)
        }
    }

    fun saveHomeAssistant(url: String, token: String, label: String) {
        app.configStore.update {
            it.copy(
                haUrl = normalizeBaseUrl(url),
                haToken = token.trim().ifEmpty { it.haToken },
                haLabel = label.trim().ifEmpty { "tv" },
            )
        }
        _settingsStatus.value = SettingsStatus(message = "Home Assistant saved.")
    }

    fun signInJellyfin(url: String, username: String, password: String) {
        val base = normalizeBaseUrl(url)
        _settingsStatus.value = SettingsStatus(busy = true, message = "Signing in to Jellyfin…")
        viewModelScope.launch {
            _settingsStatus.value = runCatching { app.jellyfin.login(base, username.trim(), password) }.fold(
                onSuccess = { login ->
                    app.configStore.update {
                        it.copy(
                            jellyfinUrl = base,
                            jellyfinToken = login.token,
                            jellyfinUserId = login.userId,
                            jellyfinUserName = login.userName,
                        )
                    }
                    SettingsStatus(message = "Signed in to Jellyfin as ${login.userName}.")
                },
                onFailure = { SettingsStatus(message = "Jellyfin sign-in failed: ${it.message}") },
            )
        }
    }

    private suspend fun refreshApps() {
        val rows = withContext(Dispatchers.Default) { InstalledApps.load(app) }
        _state.update { it.copy(apps = rows) }
    }

    private suspend fun refreshMedia(config: TvConfig) {
        if (!config.jellyfinConfigured) return
        val result = runCatching {
            coroutineScope {
                val resume = async { app.jellyfin.continueWatching(config) }
                val next = async { app.jellyfin.nextUp(config) }
                val latest = async { app.jellyfin.recentlyAdded(config) }
                Triple(resume.await(), next.await(), latest.await())
            }
        }
        _state.update { s ->
            result.fold(
                onSuccess = { (resume, next, latest) ->
                    s.copy(continueWatching = resume, nextUp = next, recentlyAdded = latest, mediaError = null)
                },
                onFailure = { s.copy(mediaError = "Jellyfin: ${it.message}") },
            )
        }
    }

    private suspend fun pollHome(config: TvConfig) {
        if (!config.haConfigured) return
        while (true) {
            loadHome(config)
            delay(POLL_MS)
        }
    }

    private suspend fun loadHome(config: TvConfig) {
        val result = runCatching { app.homeAssistant.labelledEntities(config) }
        _state.update { s ->
            result.fold(
                onSuccess = { s.copy(homeTiles = it, homeError = null) },
                onFailure = { s.copy(homeError = "Home Assistant: ${it.message}") },
            )
        }
    }

    private companion object {
        const val POLL_MS = 5_000L
    }
}
