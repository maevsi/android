package si.maev.twa

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.androidbrowserhelper.locationdelegation.LocationDelegationExtraCommandHandler


class DelegationService : com.google.androidbrowserhelper.trusted.DelegationService() {
    override fun onCreate() {
        super.onCreate()
        registerExtraCommandHandler(LocationDelegationExtraCommandHandler())
    }

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
