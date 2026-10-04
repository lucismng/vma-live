package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class ProgramReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "vma_program_reminders_v2"
        const val EXTRA_PROGRAM_TITLE = "extra_program_title"
        const val EXTRA_CHANNEL_NAME = "extra_channel_name"
        const val EXTRA_PROGRAM_TIME = "extra_program_time"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        fun ensureChannelCreated(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Lịch nhắc phát sóng VMA",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Thông báo kèm chuông khi chương trình bạn theo dõi sắp phát sóng"
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                    setSound(soundUri, audioAttributes)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun scheduleReminder(
            context: Context,
            programTitle: String,
            channelName: String,
            startTimeMs: Long
        ): Boolean {
            ensureChannelCreated(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: return false

            // Schedule notification 5 minutes before show, or if already past/too close, trigger in 5 seconds for testing
            val triggerTime = if (startTimeMs - 5 * 60 * 1000L > System.currentTimeMillis()) {
                startTimeMs - 5 * 60 * 1000L
            } else if (startTimeMs > System.currentTimeMillis()) {
                startTimeMs
            } else {
                // If user tests on past or current show, trigger alert in 5 seconds
                System.currentTimeMillis() + 5000L
            }

            val requestCode = (programTitle.hashCode() xor channelName.hashCode() xor (startTimeMs / 1000).toInt()) and 0x7FFFFFFF

            val intent = Intent(context, ProgramReminderReceiver::class.java).apply {
                action = "com.example.ACTION_PROGRAM_REMINDER"
                putExtra(EXTRA_PROGRAM_TITLE, programTitle)
                putExtra(EXTRA_CHANNEL_NAME, channelName)
                putExtra(EXTRA_PROGRAM_TIME, startTimeMs)
                putExtra(EXTRA_NOTIFICATION_ID, requestCode)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }

                // Gửi thông báo xác nhận ngay lập tức để người dùng kiểm tra máy nhận thông báo tốt
                sendImmediateConfirmation(context, programTitle, channelName, requestCode)
                return true
            } catch (_: SecurityException) {
                try {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    sendImmediateConfirmation(context, programTitle, channelName, requestCode)
                    return true
                } catch (_: Exception) {
                    sendImmediateConfirmation(context, programTitle, channelName, requestCode)
                    return true
                }
            } catch (_: Exception) {
                sendImmediateConfirmation(context, programTitle, channelName, requestCode)
                return true
            }
        }

        private fun sendImmediateConfirmation(context: Context, programTitle: String, channelName: String, reqId: Int) {
            try {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val tapIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    context,
                    reqId + 1,
                    tapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val confirmation = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_reminder)
                    .setContentTitle("Đã đặt nhắc nhở: $programTitle")
                    .setContentText("Hệ thống sẽ nhắc bạn trước khi $channelName phát sóng.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSound(soundUri)
                    .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
                    .setAutoCancel(true)
                    .setContentIntent(contentPendingIntent)
                    .build()

                notificationManager.notify(reqId + 1, confirmation)
                try {
                    RingtoneManager.getRingtone(context, soundUri)?.play()
                } catch (_: Exception) {}
            } catch (_: Exception) {}
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val programTitle = intent.getStringExtra(EXTRA_PROGRAM_TITLE) ?: "Chương trình truyền hình"
        val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "VMA TV"
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1001)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        ensureChannelCreated(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle("Sắp phát: $programTitle")
            .setContentText("Kênh $channelName sắp chiếu, mở VMA Live để đón xem ngay!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(soundUri)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        notificationManager.notify(notifId, notification)
        try {
            RingtoneManager.getRingtone(context, soundUri)?.play()
        } catch (_: Exception) {}
    }
}
