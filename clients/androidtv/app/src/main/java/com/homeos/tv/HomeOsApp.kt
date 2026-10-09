package com.homeos.tv

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.homeos.tv.data.ConfigStore
import com.homeos.tv.data.HomeAssistantClient
import com.homeos.tv.data.Http
import com.homeos.tv.data.JellyfinClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class HomeOsApp : Application(), SingletonImageLoader.Factory {
    lateinit var configStore: ConfigStore
        private set
    lateinit var jellyfin: JellyfinClient
        private set
    val homeAssistant = HomeAssistantClient()

    /** For work that must outlive a screen, e.g. telling Jellyfin playback stopped. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        configStore = ConfigStore(this)
        jellyfin = JellyfinClient(configStore.deviceId)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { Http.client })) }
            .build()
}

val android.content.Context.homeOs: HomeOsApp get() = applicationContext as HomeOsApp
