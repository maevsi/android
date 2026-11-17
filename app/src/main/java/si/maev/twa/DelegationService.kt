package si.maev.twa

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.browser.trusted.TrustedWebActivityCallbackRemote
import com.google.androidbrowserhelper.locationdelegation.LocationDelegationExtraCommandHandler


class DelegationService : com.google.androidbrowserhelper.trusted.DelegationService() {
    override fun onCreate() {
        super.onCreate()
        registerExtraCommandHandler(LocationDelegationExtraCommandHandler())
    }

    // The library answers these two commands itself, with "block" once it has asked and a request activity that reports "block" after a denial, so they must not reach it.
    override fun onExtraCommand(
        commandName: String, args: Bundle, callbackRemote: TrustedWebActivityCallbackRemote?
    ): Bundle? {
        val channelName = args.getString(EXTRA_NOTIFICATION_CHANNEL_NAME)
        val result = Bundle()

        when (commandName.takeIf { channelName != null }) {
            "checkNotificationPermission" -> result.putInt(
                KEY_PERMISSION_STATUS, notificationPermissionStatus(channelName!!)
            )

            "getNotificationPermissionRequestPendingIntent" -> result.putParcelable(
                "notificationPermissionRequestPendingIntent", PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, NotificationPermissionActivity::class.java).putExtra(
                        EXTRA_NOTIFICATION_CHANNEL_NAME, channelName
                    ),
                    // The browser fills in the messenger that receives the result.
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            )

            else -> return super.onExtraCommand(commandName, args, callbackRemote)
        }

        result.putBoolean("success", true)
        return result
    }

    override fun onAreNotificationsEnabled(channelName: String) =
        areNotificationsEnabled(channelName)

    override fun onNotifyNotificationWithChannel(
        platformTag: String, platformId: Int, notification: Notification, channelName: String
    ): Boolean {
        val mNotificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        var notificationBuilt = notification

        // The browser treats `true` as "shown", so a blocked notification must report `false`.
        if (!mNotificationManager.areNotificationsEnabled()) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder: Notification.Builder =
                Notification.Builder.recoverBuilder(this, notification)
            // The channel name doubles as its id; changing the id would leave installed apps with a second channel of the same name.
            builder.setChannelId(channelName)
            val channel = NotificationChannel(
                channelName, channelName, NotificationManager.IMPORTANCE_HIGH
            )
            mNotificationManager.createNotificationChannel(channel)
            if (mNotificationManager.getNotificationChannel(channelName)?.importance == NotificationManager.IMPORTANCE_NONE) return false
            notificationBuilt = builder.setSubText("").build()
        }
        mNotificationManager.notify(platformTag, platformId, notificationBuilt)
        return true
    }
}
