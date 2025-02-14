package si.maev.twa

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.androidbrowserhelper.trusted.DelegationService


class NotificationDelegationService : DelegationService() {
    override fun onNotifyNotificationWithChannel(
        platformTag: String,
        platformId: Int,
        notification: Notification,
        channelName: String
    ): Boolean {
        val mNotificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var notification = notification

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder: Notification.Builder = Notification.Builder.recoverBuilder(this, notification)
            builder.setChannelId(channelName)
            val channel = NotificationChannel(
                channelName, channelName, NotificationManager.IMPORTANCE_HIGH
            )
            mNotificationManager.createNotificationChannel(channel)
            notification = builder.setSubText("").build()
        }
        mNotificationManager.notify(platformTag, platformId, notification);
        return true
    }
}