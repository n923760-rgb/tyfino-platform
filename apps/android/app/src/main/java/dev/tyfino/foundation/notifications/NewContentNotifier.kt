package dev.tyfino.foundation.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit
import dev.tyfino.foundation.MainActivity
import dev.tyfino.foundation.R

/** Device-only alerts; no IPTV catalog or viewing information goes to the licensing service. */
internal class NewContentNotifier(context: Context) {
    private val app = context.applicationContext
    private val preferences = app.getSharedPreferences("new_content_alerts", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = preferences.getBoolean("enabled", false)

    fun setEnabled(enabled: Boolean) {
        preferences.edit { putBoolean("enabled", enabled) }
        if (enabled) schedule() else app.getSystemService(JobScheduler::class.java).cancel(JOB_ID)
    }

    fun restoreSchedule() {
        if (isEnabled() && app.getSystemService(JobScheduler::class.java).getPendingJob(JOB_ID) == null) schedule()
    }

    private fun schedule() {
        if (!canNotify()) return
        app.getSystemService(JobScheduler::class.java).schedule(JobInfo.Builder(
            JOB_ID, ComponentName(app, NewContentCheckJob::class.java),
        ).setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
            .setPeriodic(12L * 60L * 60L * 1_000L)
            .setPersisted(true)
            .build())
    }

    fun canNotify(): Boolean = Build.VERSION.SDK_INT < 33 ||
        app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun movies(count: Int) = alert(count, R.string.new_movies_alert, MOVIES_ID)
    fun episodes(count: Int) = alert(count, R.string.new_episodes_alert, EPISODES_ID)

    private fun alert(count: Int, title: Int, id: Int) {
        if (count <= 0 || !isEnabled() || !canNotify()) return
        val manager = app.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL, app.getString(R.string.new_content_channel), NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { lockscreenVisibility = Notification.VISIBILITY_SECRET })
        }
        val intent = PendingIntent.getActivity(app, id, Intent(app, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(app, CHANNEL)
            else Notification.Builder(app)
        manager.notify(id, builder
            .setSmallIcon(R.drawable.ic_app_tyfino)
            .setContentTitle(app.getString(title))
            .setContentText(app.getString(R.string.new_content_count, count))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .build())
    }

    private companion object {
        const val CHANNEL = "new_content"
        const val JOB_ID = 7101
        const val MOVIES_ID = 101
        const val EPISODES_ID = 102
    }
}
