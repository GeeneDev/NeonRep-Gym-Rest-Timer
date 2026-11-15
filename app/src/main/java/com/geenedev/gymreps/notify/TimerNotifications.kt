package com.geenedev.gymreps.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.RemoteViews
import com.geenedev.gymreps.MainActivity
import com.geenedev.gymreps.R

/** Builds the ongoing timer notification and the "Rest over" alert. */
object TimerNotifications {
    /**
     * Default importance (not low) so phones like Honor keep the timer in the normal list instead
     * of the "Muted" section; it has no sound or vibration of its own. A channel's importance
     * cannot be raised after creation, hence the new ID; the old "workout_timer" is deleted.
     */
    const val CHANNEL_TIMER = "workout_timer_v2"
    private const val OLD_CHANNEL_TIMER = "workout_timer"
    const val CHANNEL_ALERTS = "rest_over"
    /** The app's only notification ID: the ongoing timer and the "Rest over" alert share it. */
    const val ONGOING_ID = 1
    /** Old separate alert ID; only cancelled now, to clear leftovers from earlier versions. */
    const val ALERT_ID = 2

    /**
     * True after "Rest over" was announced in the background, until the app is opened or the
     * next rest starts. The ongoing notification then stays on the alert channel (so its sound
     * is not cut off and the heads-up stays) and carries the badge.
     */
    @Volatile
    var alertActive = false

    /** Notification.EXTRA_REQUEST_PROMOTED_ONGOING; the constant is only public from SDK 36.1. */
    private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"

    // Palette: orange for action (working), alabaster grey for rest.
    private const val ORANGE = 0xFFFCA311.toInt()
    private const val GREY = 0xFFE5E5E5.toInt()

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        // Throws if a foreground service notification still uses it; it is retried next launch.
        runCatching { nm.deleteNotificationChannel(OLD_CHANNEL_TIMER) }
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TIMER, context.getString(R.string.channel_timer), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.channel_timer_desc)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(true)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, context.getString(R.string.channel_rest_over), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_rest_over_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    /** The notification that stays up while a session runs. The system ticks the timer itself. */
    fun ongoing(context: Context, snapshot: TimerSnapshot, alertNow: Boolean = false): Notification {
        val session = snapshot.session
        val name = snapshot.exercise.ifBlank { null }
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        fun wall(elapsed: Long) = nowWall + (elapsed - nowElapsed)
        val alerting = alertActive && session.restEndsAt == null

        val b = Notification.Builder(context, if (alerting) CHANNEL_ALERTS else CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setOngoing(true)
            // Only the post that announces "Rest over" makes a sound; every other update is silent.
            .setOnlyAlertOnce(!alertNow)
            .setCategory(Notification.CATEGORY_STOPWATCH)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openApp(context))
            .setUsesChronometer(true)
            .setShowWhen(true)
            // Android 16 "Live Update": the timer sits at the top of the lock screen and as a chip
            // in the status bar. Promotion needs an ongoing, non-colorized, standard-style notification.
            .addExtras(Bundle().apply { putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true) })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            b.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        val restEndsAt = session.restEndsAt
        if (restEndsAt != null) {
            // The system counts down to restEndsAt itself (header and Live Update chip), so the
            // timer keeps moving even if this process is frozen or asleep and never re-posts.
            // No shortCriticalText here: it would replace the live countdown in the chip with static text.
            b.setContentTitle(context.getString(R.string.notif_set_resting, session.sets))
                .setContentText(name ?: context.getString(R.string.notif_next_up, session.sets + 1))
                .setWhen(wall(restEndsAt))
                .setChronometerCountDown(true)
                .setColor(GREY)
                .addAction(action(context, context.getString(R.string.skip_rest), TimerService.ACTION_SKIP_REST, 1))
        } else {
            val since = session.workStartedAt ?: session.startedAt ?: nowElapsed
            // After a rest: "Set 3 - rest over". Before the first set: "Set 1".
            val label = if (session.sets == 0) context.getString(R.string.notif_set, 1) else context.getString(R.string.notif_set_rest_over, session.sets)
            val next = session.sets + 1
            b.setContentTitle(label)
                .setContentText(
                    when {
                        !alerting -> name ?: context.getString(R.string.notif_working)
                        name != null -> context.getString(R.string.notif_time_for_set_of, next, name)
                        else -> context.getString(R.string.notif_time_for_set, next)
                    },
                )
                .setWhen(wall(since))
                .setChronometerCountDown(false)
                .apply { if (Build.VERSION.SDK_INT >= 36) setShortCriticalText(label) }
                .setColor(ORANGE)
                // Same as tapping the circle on screen.
                .addAction(
                    action(
                        context,
                        context.getString(R.string.rest),
                        TimerService.ACTION_SET_DONE,
                        2,
                    ),
                )
            if (alerting) b.setNumber(1) // badge on the app icon
        }
        if (useCustomTimerView(context)) {
            // Honor/Huawei skins (and phones without Live Updates) never draw the header
            // chronometer, so the timer looked missing. Put a big system-ticked Chronometer in the body.
            val base = restEndsAt ?: session.workStartedAt ?: session.startedAt ?: nowElapsed
            val extras = b.build().extras
            b.setStyle(Notification.DecoratedCustomViewStyle())
                .setCustomContentView(timerView(context, extras, base, countDown = restEndsAt != null))
                .setCustomBigContentView(timerView(context, extras, base, countDown = restEndsAt != null))
        }
        return b.build()
    }

    /**
     * True when the standard notification would not show a running timer: the phone cannot show
     * Android 16 Live Updates (whose chip ticks the countdown), or it is an Honor/Huawei skin,
     * which hides the header chronometer even on Android 16.
     */
    private fun useCustomTimerView(context: Context): Boolean {
        val maker = Build.MANUFACTURER.lowercase()
        if (maker.contains("honor") || maker.contains("huawei")) return true
        if (Build.VERSION.SDK_INT < 36) return true // no Live Updates before Android 16
        return !context.getSystemService(NotificationManager::class.java).canPostPromotedNotifications()
    }

    private fun timerView(context: Context, extras: Bundle, base: Long, countDown: Boolean): RemoteViews =
        RemoteViews(context.packageName, R.layout.notif_timer).apply {
            setTextViewText(R.id.notif_title, extras.getCharSequence(Notification.EXTRA_TITLE))
            setTextViewText(R.id.notif_text, extras.getCharSequence(Notification.EXTRA_TEXT))
            // Base is on the elapsedRealtime clock, which is what Chronometer uses.
            setChronometer(R.id.notif_chrono, base, null, true)
            setChronometerCountDown(R.id.notif_chrono, countDown)
        }

    /**
     * Heads-up "Rest over" with sound and vibration, posted on the same ID as the ongoing timer
     * so it replaces it instead of adding a second notification.
     */
    fun alertRestOver(context: Context, snapshot: TimerSnapshot) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.cancel(ALERT_ID)
        val s = snapshot.session
        if (!s.isRunning || !TimerService.running) {
            // Process was restarted just for the backup alarm: no timer to replace, so post a
            // plain dismissable alert, still on the single ID.
            alertActive = false
            val next = s.sets + 1
            val name = snapshot.exercise.ifBlank { null }
            val n = Notification.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_stat_timer)
                .setContentTitle(context.getString(R.string.notif_set_rest_over, s.sets))
                .setContentText(if (name != null) context.getString(R.string.notif_time_for_set_of, next, name) else context.getString(R.string.notif_time_for_set, next))
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(ORANGE)
                .setNumber(1)
                .setAutoCancel(true)
                .setContentIntent(openApp(context))
                .build()
            nm.notify(ONGOING_ID, n)
            return
        }
        alertActive = true
        // Callers may pass the session still resting; show it as the next set.
        val working = s.restEndsAt?.let { snapshot.copy(session = s.endRest(it)) } ?: snapshot
        nm.notify(ONGOING_ID, ongoing(context, working, alertNow = true))
    }

    /**
     * App came to the foreground: drop the alert styling and the badge, and clear any leftover
     * notification. The ongoing timer itself stays while a session runs.
     */
    fun cancelAlert(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val wasAlerting = alertActive
        alertActive = false
        if (!TimerService.running) {
            nm.cancelAll() // no session, so nothing from the app should be showing
            return
        }
        nm.cancel(ALERT_ID)
        val snapshot = TimerBus.state.value
        if (wasAlerting && snapshot.session.isRunning) nm.notify(ONGOING_ID, ongoing(context, snapshot))
    }

    /** "Set 3 - resting 0:34", counting whole seconds up (0:00 only at the very end). */
    fun restingLabel(sets: Int, remainingMs: Long): String {
        val secs = ((remainingMs.coerceAtLeast(0) + 999) / 1000)
        return "Set $sets - resting %d:%02d".format(secs / 60, secs % 60)
    }



    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun action(context: Context, title: String, intentAction: String, code: Int): Notification.Action {
        val intent = Intent(context, TimerService::class.java).setAction(intentAction)
        val pi = PendingIntent.getService(
            context, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_stat_timer), title, pi)
            .build()
    }
}
