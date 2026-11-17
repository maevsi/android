package si.maev.twa

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.provider.Settings

const val EXTRA_NOTIFICATION_CHANNEL_NAME = "notificationChannelName"
const val KEY_PERMISSION_STATUS = "permissionStatus"

private const val EXTRA_MESSENGER = "messenger"
private const val PERMISSION_POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"
private const val PERMISSION_STATUS_ALLOW = 0
private const val PERMISSION_STATUS_ASK = 2
private const val STATE_IS_SETTINGS_OPENED = "isSettingsOpened"

fun Context.areNotificationsEnabled(channelName: String): Boolean {
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (!notificationManager.areNotificationsEnabled()) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
    // `DelegationService` uses the channel name as the channel id.
    return notificationManager.getNotificationChannel(channelName)?.importance != NotificationManager.IMPORTANCE_NONE
}

// The browser reads this status only once per browser start and after each request, and it forwards requests only while the status is "ask", so "block" would stay until the browser restarts.
fun Context.notificationPermissionStatus(channelName: String): Int =
    if (areNotificationsEnabled(channelName)) PERMISSION_STATUS_ALLOW else PERMISSION_STATUS_ASK

fun Context.notificationSettingsIntent(): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", packageName, null))
    }

// The browser starts this activity when the web app requests the notification permission and waits for one message with the result.
class NotificationPermissionActivity : Activity() {
    private lateinit var channelName: String
    private var messenger: Messenger? = null
    private var isRationaleShownBefore = false
    private var isSettingsOpened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        channelName = intent.getStringExtra(EXTRA_NOTIFICATION_CHANNEL_NAME) ?: ""
        @Suppress("DEPRECATION")
        messenger = intent.getParcelableExtra(EXTRA_MESSENGER)

        if (savedInstanceState != null) {
            isSettingsOpened = savedInstanceState.getBoolean(STATE_IS_SETTINGS_OPENED)
            return
        }
        if (messenger == null || areNotificationsEnabled(channelName)) return finish()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isRationaleShownBefore =
                shouldShowRequestPermissionRationale(PERMISSION_POST_NOTIFICATIONS)
            requestPermissions(arrayOf(PERMISSION_POST_NOTIFICATIONS), 0)
        } else {
            openSettings()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        // Without a rationale before and after, the system showed no dialog or the user dismissed it without choosing, and a blocked channel is not part of the dialog at all, so the settings are the place left to enable notifications.
        val isDenied = isRationaleShownBefore ||
            shouldShowRequestPermissionRationale(PERMISSION_POST_NOTIFICATIONS)
        if (areNotificationsEnabled(channelName) || isDenied) finish() else openSettings()
    }

    // The settings cover this activity completely, so a restart means the user came back from them.
    override fun onRestart() {
        super.onRestart()
        if (isSettingsOpened) finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_IS_SETTINGS_OPENED, isSettingsOpened)
    }

    override fun finish() {
        // The browser leaves the web app's request pending until this message arrives.
        messenger?.send(Message.obtain().apply {
            data = Bundle().apply {
                putInt(KEY_PERMISSION_STATUS, notificationPermissionStatus(channelName))
            }
        })
        messenger = null
        super.finish()
    }

    private fun openSettings() {
        isSettingsOpened = true
        startActivity(notificationSettingsIntent())
    }
}
