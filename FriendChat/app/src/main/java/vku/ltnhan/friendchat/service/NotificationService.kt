package vku.ltnhan.friendchat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import vku.ltnhan.friendchat.R
import vku.ltnhan.friendchat.messages.LatestMessagesActivity
import vku.ltnhan.friendchat.models.TokenNotification

class NotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        sendNotification(message.notification?.body.toString())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val tokenNotification = TokenNotification()
        tokenNotification.key = token
        FirebaseDatabase.getInstance().getReference("/token/$token")
            .setValue(tokenNotification)

        val sharedPreferences = getSharedPreferences("token", Context.MODE_PRIVATE)

        val editor:SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString("token_key", token)
        editor.apply()
    }

    private fun sendNotification(message: String){

        val intent = Intent(this, LatestMessagesActivity::class.java)

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val channelId:String = getString(R.string.app_name)

        val defaultSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val n: NotificationCompat.Builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_call)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    resources,
                    R.drawable.ic_launcher_background
                )
            )
            .setContentTitle(channelId)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setContentIntent(pendingIntent)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.sym_call_missed, "Cancel", PendingIntent.getActivity(
                        this, 0, intent,
                        PendingIntent.FLAG_CANCEL_CURRENT
                    )
                )
            )
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.sym_call_outgoing,
                    "OK",
                    PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
                )
            )

        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }



        notificationManager.notify(0, n.build())
    }

}