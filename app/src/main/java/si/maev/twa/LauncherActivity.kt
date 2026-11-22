package si.maev.twa

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.androidbrowserhelper.trusted.SessionStore
import com.google.androidbrowserhelper.trusted.SharedPreferencesTokenStore
import java.io.File

class LauncherActivity : com.google.androidbrowserhelper.trusted.LauncherActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Chrome 152+ rejects the splash image unless this app holds a grant on it, and Android
        // records no grant for the provider's owner without the persistable flag. Remove once
        // https://github.com/GoogleChrome/android-browser-helper/issues/623 ships a fix.
        // The path mirrors the library's internal `SplashImageTransferTask` constants.
        grantUriPermission(
            packageName,
            FileProvider.getUriForFile(
                this,
                getString(R.string.providerAuthority),
                File(filesDir, "twa_splash/splash_image.png"),
            ),
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
        )
        super.onCreate(savedInstanceState)
        if (isFinishing) return

        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        if (preferences.getBoolean(KEY_LAUNCHED_BEFORE, false)) {
            launchTwa()
        } else if (isOnline()) {
            launchTwa()
            preferences.edit { putBoolean(KEY_LAUNCHED_BEFORE, true) }
        } else {
            setContentView(R.layout.activity_offline)
            findViewById<Button>(R.id.retryButton).setOnClickListener { retryLaunch() }
        }
    }

    // the web app's service worker, which serves its offline page, only exists after one online launch
    override fun shouldLaunchImmediately(): Boolean = false

    private fun retryLaunch() {
        if (!isOnline()) return
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit { putBoolean(KEY_LAUNCHED_BEFORE, true) }
        launchTwa()
    }

    private fun isOnline(): Boolean {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
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
        const val PREFERENCES_NAME = "si.maev.twa.launch"
        const val KEY_LAUNCHED_BEFORE = "launchedBefore"
    }
}
