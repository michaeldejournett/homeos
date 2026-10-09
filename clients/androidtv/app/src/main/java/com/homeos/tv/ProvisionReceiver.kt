package com.homeos.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.homeos.tv.data.normalizeBaseUrl
import kotlinx.coroutines.launch

/**
 * Lets the PC set the TV up over adb instead of typing tokens with a remote:
 *
 *   adb shell am broadcast -a com.homeos.tv.PROVISION -n com.homeos.tv/.ProvisionReceiver \
 *     --es ha_url https://ha.example.com --es ha_token <token> [--es ha_label tv] \
 *     --es jellyfin_url https://media.example.com --es jellyfin_user <name> --es jellyfin_password <pw>
 *
 * Only callers holding android.permission.DUMP (the adb shell) can reach it.
 * Every extra is optional; only the ones given are changed.
 */
class ProvisionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.homeOs
        val pending = goAsync()
        app.appScope.launch {
            val result = runCatching { provision(app, intent) }
                .fold(onSuccess = { it }, onFailure = { "error: ${it.message}" })
            Log.i(TAG, result)
            runCatching { pending.resultData = result }
            pending.finish()
        }
    }

    private suspend fun provision(app: HomeOsApp, intent: Intent): String {
        fun extra(name: String) = intent.getStringExtra(name)?.trim()?.takeIf { it.isNotEmpty() }
        val done = mutableListOf<String>()

        val haUrl = extra("ha_url")
        val haToken = extra("ha_token")
        val haLabel = extra("ha_label")
        if (haUrl != null || haToken != null || haLabel != null) {
            app.configStore.update { c ->
                c.copy(
                    haUrl = haUrl?.let(::normalizeBaseUrl) ?: c.haUrl,
                    haToken = haToken ?: c.haToken,
                    haLabel = haLabel ?: c.haLabel,
                )
            }
            done += "home assistant"
        }

        val jfUrl = extra("jellyfin_url")?.let(::normalizeBaseUrl)
        val jfUser = extra("jellyfin_user")
        val jfPassword = intent.getStringExtra("jellyfin_password").orEmpty()
        if (jfUrl != null) app.configStore.update { it.copy(jellyfinUrl = jfUrl) }
        if (jfUser != null) {
            val url = jfUrl ?: app.configStore.config.value.jellyfinUrl
            val login = app.jellyfin.login(url, jfUser, jfPassword)
            app.configStore.update {
                it.copy(
                    jellyfinUrl = url,
                    jellyfinToken = login.token,
                    jellyfinUserId = login.userId,
                    jellyfinUserName = login.userName,
                )
            }
            done += "jellyfin (${login.userName})"
        } else if (jfUrl != null) {
            done += "jellyfin url"
        }

        return if (done.isEmpty()) "nothing to do" else "ok: " + done.joinToString()
    }

    private companion object {
        const val TAG = "HomeOsProvision"
    }
}
