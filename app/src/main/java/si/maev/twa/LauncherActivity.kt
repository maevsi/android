package si.maev.twa

import android.net.Uri
import androidx.core.net.toUri
import com.google.androidbrowserhelper.trusted.SessionStore
import com.google.androidbrowserhelper.trusted.SharedPreferencesTokenStore

class LauncherActivity : com.google.androidbrowserhelper.trusted.LauncherActivity() {

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
}
