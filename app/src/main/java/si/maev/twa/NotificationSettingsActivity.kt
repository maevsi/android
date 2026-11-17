package si.maev.twa

import android.app.Activity
import android.os.Bundle

// Chrome adds CATEGORY_BROWSABLE to every `intent:` navigation, which system settings activities do not declare, so the web app reaches them through this browsable activity.
class NotificationSettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivity(notificationSettingsIntent())
        finish()
    }
}
