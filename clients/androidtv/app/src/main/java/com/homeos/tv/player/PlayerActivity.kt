package com.homeos.tv.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.homeos.tv.data.JellyfinClient.PlaybackEvent
import com.homeos.tv.data.MediaCard
import com.homeos.tv.homeOs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Plays a Jellyfin item: the original file first (no server CPU), falling back
 * to a server transcode if the TV can't decode it. Reports progress so
 * "Continue watching" stays in sync with every other Jellyfin client.
 */
@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {
    private val app get() = homeOs
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var itemId: String
    private val playSessionId = UUID.randomUUID().toString().replace("-", "")
    private var transcoding = false
    private var started = false
    private var progressJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return finish()
        val config = app.configStore.config.value

        val dataSource = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(mapOf("Authorization" to app.jellyfin.authorizationHeader(config.jellyfinToken)))
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
            .build()
        playerView = PlayerView(this).apply {
            player = this@PlayerActivity.player
            keepScreenOn = true
            setShowSubtitleButton(true)
        }
        setContentView(playerView)

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> if (!started) {
                        started = true
                        report(PlaybackEvent.Start)
                        startProgressReports()
                    }
                    Player.STATE_ENDED -> finish()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (started) report(PlaybackEvent.Progress)
            }

            override fun onPlayerError(error: PlaybackException) {
                if (!transcoding) {
                    // Usually an unsupported codec/container: ask Jellyfin to transcode.
                    transcoding = true
                    val position = player.currentPosition
                    player.setMediaItem(transcodeItem(), position)
                    player.prepare()
                } else {
                    Toast.makeText(this@PlayerActivity, "Can't play this: ${error.errorCodeName}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        })

        val startMs = intent.getLongExtra(EXTRA_START_MS, 0)
        player.setMediaItem(MediaItem.fromUri(app.jellyfin.directStreamUrl(config, itemId)), startMs)
        player.playWhenReady = true
        player.prepare()
        playerView.requestFocus()
    }

    private fun transcodeItem(): MediaItem =
        MediaItem.Builder()
            .setUri(app.jellyfin.transcodeUrl(app.configStore.config.value, itemId, playSessionId))
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()

    private fun startProgressReports() {
        progressJob?.cancel()
        progressJob = lifecycleScope.launch {
            while (isActive) {
                delay(10_000)
                report(PlaybackEvent.Progress)
            }
        }
    }

    private fun report(event: PlaybackEvent) {
        val config = app.configStore.config.value
        val position = player.currentPosition
        val paused = !player.isPlaying
        val transcode = transcoding
        // App scope: the Stopped report has to survive this activity finishing.
        app.appScope.launch {
            runCatching {
                app.jellyfin.reportPlayback(config, event, itemId, position, paused, transcode, playSessionId)
            }
        }
    }

    // Route remote keys to the player so the D-pad shows controls and seeks.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        playerView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)

    override fun onStop() {
        super.onStop()
        if (::player.isInitialized) player.pause()
    }

    override fun onDestroy() {
        progressJob?.cancel()
        if (::player.isInitialized) {
            if (started) report(PlaybackEvent.Stopped)
            player.release()
        }
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ITEM_ID = "item_id"
        private const val EXTRA_START_MS = "start_ms"

        fun intent(context: Context, card: MediaCard): Intent =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_ITEM_ID, card.id)
                .putExtra(EXTRA_START_MS, card.resumePositionMs)
    }
}
