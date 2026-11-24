package si.maev.twa

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsCallback.NAVIGATION_FINISHED
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.androidbrowserhelper.trusted.QualityEnforcer
import com.google.androidbrowserhelper.trusted.SessionStore
import com.google.androidbrowserhelper.trusted.SharedPreferencesTokenStore
import java.lang.ref.WeakReference

class LauncherActivity : com.google.androidbrowserhelper.trusted.LauncherActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return

        // a share or link that arrives while the hint shows starts another instance, which replaces it
        offlineHint?.get()?.finish()

        if (hasLoadedBefore() || isOnline()) {
            launchTwa()
        } else {
            offlineHint = WeakReference(this)
            setContentView(R.layout.activity_offline)
            findViewById<Button>(R.id.retryButton).setOnClickListener { if (isOnline()) launchTwa() }
        }
    }

    // the web app's service worker, which serves its offline page, only exists after one online launch
    override fun shouldLaunchImmediately(): Boolean = false

    // set once the browser finished loading a page, as a launch that gets interrupted earlier has not registered the service worker yet
    override fun getCustomTabsCallback(): CustomTabsCallback {
        val preferences = applicationContext.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)

        return object : QualityEnforcer() {
            override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
                super.onNavigationEvent(navigationEvent, extras)
                if (navigationEvent == NAVIGATION_FINISHED) preferences.edit { putBoolean(KEY_LOADED_BEFORE, true) }
            }
        }
    }

    override fun onDestroy() {
        if (offlineHint?.get() === this) offlineHint = null
        super.onDestroy()
    }

    private fun hasLoadedBefore() =
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).getBoolean(KEY_LOADED_BEFORE, false)

    // an always-on VPN such as Tailscale is the active network and reports internet even with nothing underneath it, so only non-VPN networks count
    @Suppress("DEPRECATION") // `allNetworks` has no synchronous replacement
    private fun isOnline(): Boolean {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return manager.allNetworks.any { network ->
            manager.getNetworkCapabilities(network)?.run {
                hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            } == true
        }
    }

    override fun createTwaLauncher(): TwaLauncher {
        return TwaLauncher(
            this,
            null,
            SessionStore.makeSessionId(taskId),
            SharedPreferencesTokenStore(this)
        )
    }

    // mirrors the web app's `protocol_handlers`, which Chrome does not apply to a TWA
    override fun getProtocolHandlers(): Map<String, Uri> =
        mapOf("web+vibetype" to "https://${getString(R.string.hostName)}/open?url=%s".toUri())

    private companion object {
        const val KEY_LOADED_BEFORE = "loadedBefore"
        const val PREFERENCES_NAME = "si.maev.twa.launch"

        var offlineHint: WeakReference<LauncherActivity>? = null
    }
}
