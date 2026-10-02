package org.cmf.churchconnect

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

class ChurchMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (FirebaseAuth.getInstance().currentUser != null) {
            FirebaseFunctions.getInstance("asia-southeast1")
                .getHttpsCallable("registerDeviceToken")
                .call(mapOf("token" to token))
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val localized = AppLocale.wrap(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, localized.getString(R.string.church_updates), NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(message.notification?.title ?: localized.getString(R.string.app_name))
            .setContentText(message.notification?.body ?: localized.getString(R.string.new_updates))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.notification?.body ?: localized.getString(R.string.new_updates)))
            .setAutoCancel(true)
            .build()
        val notificationId = message.data["notificationId"]?.takeIf { it.isNotBlank() }
        if (notificationId != null) manager.notify(notificationId, 0, notification)
        else manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    companion object { private const val CHANNEL_ID = "church_updates" }
}
