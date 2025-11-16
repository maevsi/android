package si.maev.twa

import com.google.androidbrowserhelper.trusted.SessionStore
import com.google.androidbrowserhelper.trusted.SharedPreferencesTokenStore
import si.maev.twa.androidbrowserhelper.LauncherActivity

class LauncherActivity : LauncherActivity() {

    override fun createTwaLauncher(): TwaLauncher {
        return TwaLauncher(
            this,
            null,
            SessionStore.makeSessionId(taskId),
            SharedPreferencesTokenStore(this)
        )
    }
}