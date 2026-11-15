package com.geenedev.gymreps.notify

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.geenedev.gymreps.MainActivity
import com.geenedev.gymreps.R
import com.geenedev.gymreps.model.SessionState

/**
 * Makes sure the "Rest over" alert is posted exactly once per rest, even when the screen is
 * locked, the phone is dozing, or the OEM battery manager throttles or kills [TimerService].
 *
 * Three paths can end a rest and all go through [alertOnce]: the service's own timer, the screen
 * ending the rest in its state (which used to cancel the service's timer before it fired), and a
 * backup [AlarmManager] alarm that reaches [RestAlarmReceiver] if the other two never run.
 */
object RestAlarm {
    private const val ACTION_REST_OVER = "com.geenedev.gymreps.action.REST_OVER"
    private const val EXTRA_ENDS_AT = "endsAt"
    private const val EXTRA_SETS = "sets"
    private const val EXTRA_EXERCISE = "exercise"

    /** restEndsAt of the last rest we alerted for, so the three paths never alert twice. */
    @Volatile
    private var alertedFor: Long? = null

    /**
     * True only when the user can actually see the app. With the screen locked while the app was
     * on top, onStop can come late or not at all, so [TimerBus.appVisible] alone is not enough.
     */
    fun userIsLooking(context: Context): Boolean {
        if (!TimerBus.appVisible) return false
        val pm = context.getSystemService(PowerManager::class.java)
        val km = context.getSystemService(KeyguardManager::class.java)
        return pm.isInteractive && !km.isKeyguardLocked
    }

    /**
     * Posts the alert for the rest ending at [endsAt], once, unless the user is looking at the app.
     * [working] is the session after the rest. Returns true if the alert was posted now.
     */
    fun alertOnce(context: Context, working: TimerSnapshot, endsAt: Long): Boolean {
        cancel(context)
        if (alertedFor == endsAt) return false
        alertedFor = endsAt
        if (userIsLooking(context)) return false
        if (TimerService.running) {
            TimerNotifications.alertRestOver(context, working)
        } else {
            postStandalone(context, working)
        }
        return true
    }

    /** The service was killed (OEM battery manager): post a plain, dismissable alert instead. */
    private fun postStandalone(context: Context, working: TimerSnapshot) {
        TimerNotifications.createChannels(context)
        val sets = working.session.sets
        val name = working.exercise.ifBlank { null }
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = Notification.Builder(context, TimerNotifications.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(context.getString(R.string.notif_set_rest_over, sets))
            .setContentText(if (name != null) context.getString(R.string.notif_time_for_set_of, sets + 1, name) else context.getString(R.string.notif_time_for_set, sets + 1))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(TimerNotifications.ONGOING_ID, n) // single app notification ID
    }

    /** Backup alarm at the end of the rest (elapsedRealtime clock, wakes the phone). */
    fun schedule(context: Context, snapshot: TimerSnapshot, endsAt: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context, snapshot, endsAt)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            // An "alarm clock" alarm is the one kind OEM battery managers (Honor PowerGenie) and
            // Doze reliably deliver on time with the screen off; plain exact alarms were sometimes
            // held back. Its time is on the wall clock.
            val wallTime = System.currentTimeMillis() + (endsAt - android.os.SystemClock.elapsedRealtime())
            val show = PendingIntent.getActivity(
                context, 1,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(wallTime, show), pi)
        } else {
            // Without the exact-alarm permission Doze may delay this a little, but it still fires.
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, endsAt, pi)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, null, 0L))
    }

    internal fun onAlarm(context: Context, intent: Intent) {
        val endsAt = intent.getLongExtra(EXTRA_ENDS_AT, 0L)
        val live = TimerBus.state.value
        val session = live.session
        // Process still alive and the rest was skipped / the session ended: nothing to announce.
        // (The alarm is cancelled in those cases; this only guards a race.)
        if (session.isRunning && session.restEndsAt != endsAt && session.workStartedAt != endsAt) return
        val snapshot = if (session.isRunning) {
            live.copy(session = session.endRest(endsAt))
        } else {
            // The process was killed and restarted just for this alarm: rebuild what the alert shows.
            TimerSnapshot(
                SessionState(sets = intent.getIntExtra(EXTRA_SETS, 0)),
                intent.getStringExtra(EXTRA_EXERCISE).orEmpty(),
            )
        }
        alertOnce(context, snapshot, endsAt)
    }

    private fun pendingIntent(context: Context, snapshot: TimerSnapshot?, endsAt: Long): PendingIntent {
        val intent = Intent(context, RestAlarmReceiver::class.java).setAction(ACTION_REST_OVER)
        if (snapshot != null) {
            intent.putExtra(EXTRA_ENDS_AT, endsAt)
                .putExtra(EXTRA_SETS, snapshot.session.sets)
                .putExtra(EXTRA_EXERCISE, snapshot.exercise)
        }
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RestAlarm.onAlarm(com.geenedev.gymreps.data.AppLanguage.wrap(context), intent)
    }
}
