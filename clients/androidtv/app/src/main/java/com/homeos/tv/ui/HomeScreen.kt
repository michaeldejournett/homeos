package com.homeos.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CompactCard
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.homeos.tv.data.AppCatalog
import com.homeos.tv.data.HaEntity
import com.homeos.tv.data.HomeAssistantClient
import com.homeos.tv.data.MediaCard
import com.homeos.tv.data.TvApp
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private val RowPadding = PaddingValues(horizontal = 48.dp)

@Composable
fun HomeScreen(
    state: HomeState,
    onPressTile: (HaEntity) -> Unit,
    onPlay: (MediaCard) -> Unit,
    onLaunchApp: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    // Land on the first real row rather than the settings button.
    val firstItem = remember { FocusRequester() }
    var firstAssigned = false
    fun firstModifier(): Modifier =
        if (firstAssigned) Modifier else Modifier.focusRequester(firstItem).also { firstAssigned = true }

    val config = state.config
    val apps = state.apps

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item { Header(onOpenSettings) }

        if (!config.haConfigured || !config.jellyfinConfigured) {
            item { SetupHint(config.haConfigured, config.jellyfinConfigured, onOpenSettings) }
        }

        errorLine(state.homeError)
        if (state.homeTiles.isNotEmpty()) {
            val mod = firstModifier()
            row("Home") {
                items(state.homeTiles, key = { it.id }) { entity ->
                    HomeTile(entity, onClick = { onPressTile(entity) },
                        modifier = if (entity == state.homeTiles.first()) mod else Modifier)
                }
            }
        }

        errorLine(state.mediaError)
        mediaRow("Continue watching", state.continueWatching, onPlay, ::firstModifier)
        mediaRow("Next up", state.nextUp, onPlay, ::firstModifier)

        if (apps.streaming.isNotEmpty()) {
            val mod = firstModifier()
            row("Streaming") {
                items(apps.streaming, key = { it.packageName }) { app ->
                    AppTile(app, onClick = { onLaunchApp(app.packageName) },
                        modifier = if (app == apps.streaming.first()) mod else Modifier)
                }
            }
        }

        mediaRow("Recently added", state.recentlyAdded, onPlay, ::firstModifier)

        row("Games") {
            items(apps.games, key = { it.packageName }) { app ->
                AppTile(app, onClick = { onLaunchApp(app.packageName) })
            }
            if (apps.games.none { it.packageName == AppCatalog.MOONLIGHT }) {
                item { InfoTile("Install Moonlight to play your Steam library") }
            }
        }

        if (apps.other.isNotEmpty()) {
            row("All apps") {
                items(apps.other, key = { it.packageName }) { app ->
                    AppTile(app, onClick = { onLaunchApp(app.packageName) })
                }
            }
        }
    }

    LaunchedEffect(state.homeTiles.isNotEmpty(), state.continueWatching.isNotEmpty(), apps.streaming.isNotEmpty()) {
        runCatching { firstItem.requestFocus() }
    }
}

private fun LazyListScope.row(title: String, content: LazyListScope.() -> Unit) {
    item(key = "row-$title") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 48.dp),
            )
            LazyRow(
                contentPadding = RowPadding,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }
}

private fun LazyListScope.mediaRow(
    title: String,
    cards: List<MediaCard>,
    onPlay: (MediaCard) -> Unit,
    firstModifier: () -> Modifier,
) {
    if (cards.isEmpty()) return
    val first = firstModifier()
    row(title) {
        items(cards, key = { "$title-${it.id}" }) { card ->
            MediaTile(card, onClick = { onPlay(card) }, modifier = if (card == cards.first()) first else Modifier)
        }
    }
}

private fun LazyListScope.errorLine(message: String?) {
    if (message == null) return
    item(key = "error-$message") {
        Text(
            message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 48.dp),
        )
    }
}

@Composable
private fun Header(onOpenSettings: () -> Unit) {
    val now by produceState(Date()) {
        while (true) {
            value = Date()
            delay(15_000)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("HomeOS", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.weight(1f))
        Text(
            DateFormat.getTimeInstance(DateFormat.SHORT).format(now),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.width(24.dp))
        Button(onClick = onOpenSettings) { Text("Settings") }
    }
}

@Composable
private fun SetupHint(haReady: Boolean, jellyfinReady: Boolean, onOpenSettings: () -> Unit) {
    val missing = buildList {
        if (!haReady) add("Home Assistant")
        if (!jellyfinReady) add("Jellyfin")
    }.joinToString(" and ")
    Card(onClick = onOpenSettings, modifier = Modifier.padding(horizontal = 48.dp).fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Connect $missing", style = MaterialTheme.typography.titleMedium)
            Text(
                "Open Settings, or run scripts\\windows\\setup-tv.ps1 on the HomeOS PC to set this TV up over the network.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun HomeTile(entity: HaEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val actionable = HomeAssistantClient.actionFor(entity) != null
    val on = HomeAssistantClient.isOn(entity)
    Card(
        onClick = { if (actionable) onClick() },
        modifier = modifier.size(width = 196.dp, height = 110.dp),
        colors = CardDefaults.colors(
            containerColor = if (on) Color(0xFF3A3420) else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(HomeAssistantClient.glyph(entity), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Text(
                    entity.state.replace('_', ' '),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (on) Color(0xFFFFD36B) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(entity.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MediaTile(card: MediaCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CompactCard(
        onClick = onClick,
        modifier = modifier.width(256.dp).aspectRatio(16f / 9f),
        image = {
            AsyncImage(
                model = card.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            card.progress?.let { progress ->
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color(0x66000000))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(Color(0xFF5AB0FF)))
                }
            }
        },
        title = { Text(card.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 12.dp)) },
        subtitle = {
            if (card.subtitle.isNotEmpty()) {
                Text(
                    card.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                )
            }
        },
    )
}

@Composable
private fun AppTile(app: TvApp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.width(196.dp).aspectRatio(16f / 9f)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val art = app.art
            when {
                art != null && app.isBanner ->
                    Image(art, contentDescription = app.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                art != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(art, contentDescription = null, modifier = Modifier.size(56.dp))
                    Text(app.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
                else -> Text(app.label, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun InfoTile(text: String) {
    Card(onClick = {}, modifier = Modifier.width(260.dp).aspectRatio(16f / 9f)) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
