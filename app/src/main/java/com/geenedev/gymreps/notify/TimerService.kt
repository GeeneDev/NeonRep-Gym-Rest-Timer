package com.geenedev.gymreps.notify

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import androidx.core.app.ServiceCompat
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Runs while a session is going so the timer survives the screen turning off or the app
 * going to the background. It mirrors [TimerBus.state] into the ongoing notification, keeps
 * the CPU awake during a rest countdown, and posts the "Rest over" alert when the app is
 * not on screen.
 */
class TimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var restOver: Runnable? = null
    private var tick: Runnable? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var inForeground = false
    /** restEndsAt of the rest being counted down, until it is alerted or cancelled. */
    private var pendingRestEndsAt: Long? = null

    override fun onBind(intent: Intent?): IBinder? = null

    // Notification text in the in-app language (Android 12 and older need this).
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(com.geenedev.gymreps.data.AppLanguage.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        running = true
        TimerNotifications.createChannels(this)
        scope.launch { TimerBus.state.collect { render(it) } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SET_DONE -> TimerBus.send(TimerAction.SetDone)
            ACTION_SKIP_REST -> TimerBus.send(TimerAction.SkipRest)
        }
        if (!inForeground) {
            // ServiceCompat leaves the type out on Android versions that don't take one.
            ServiceCompat.startForeground(
                this,
                TimerNotifications.ONGOING_ID,
                TimerNotifications.ongoing(this, TimerBus.state.value),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                },
            )
            inForeground = true
        }
        return START_NOT_STICKY
    }

    /** Swiping the app away from Recents ends the session, so the timer goes too. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
    }

    override fun onDestroy() {
        running = false
        cancelRestTimer()
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        // Session over: no "Rest over" or other leftover notification may stay behind.
        TimerNotifications.alertActive = false
        notifications().cancelAll()
        super.onDestroy()
    }

    private fun render(snapshot: TimerSnapshot) {
        val pending = pendingRestEndsAt
        cancelRestTimer()
        // The screen ends the rest in its own state when the countdown hits zero (workStartedAt ==
        // restEndsAt). That used to cancel our timer before it fired, losing the alert: announce it
        // here instead. A skip uses the current time, so it does not match.
        if (pending != null && snapshot.session.restEndsAt != pending &&
            snapshot.session.isRunning && snapshot.session.workStartedAt == pending
        ) {
            RestAlarm.alertOnce(this, snapshot, pending)
        }
        if (!snapshot.session.isRunning) return // the activity stops the service
        // A new rest replaces the previous "Rest over" in place (same ID, back on the quiet channel).
        if (snapshot.session.restEndsAt != null) TimerNotifications.alertActive = false
        notifications().notify(TimerNotifications.ONGOING_ID, TimerNotifications.ongoing(this, snapshot))

        val endsAt = snapshot.session.restEndsAt ?: return
        val wait = endsAt - SystemClock.elapsedRealtime()
        // Without this the phone can sleep with the screen off and the countdown would stall.
        acquireWakeLock(wait.coerceAtLeast(0) + 15_000)
        pendingRestEndsAt = endsAt
        // Backup in case Doze or the battery manager stalls this handler or kills the service.
        RestAlarm.schedule(this, snapshot, endsAt)
        val r = Runnable { onRestOver(snapshot) }
        restOver = r
        // No per-second re-post: the notification counts down by itself (system chronometer), so
        // a frozen or sleeping process can no longer leave a stuck "resting 0:34" on screen.
        handler.postDelayed(r, wait.coerceAtLeast(0))
    }

    private fun onRestOver(snapshot: TimerSnapshot) {
        restOver = null
        tick?.let(handler::removeCallbacks)
        tick = null
        val endsAt = snapshot.session.restEndsAt ?: return
        // Show the next set right away; the screen ends the rest in its own state at the same moment.
        // In the background the same notification also becomes the audible "Rest over" alert.
        val working = snapshot.copy(session = snapshot.session.endRest(endsAt))
        // RestAlarm: alerts once per rest, and also when the screen locked with the app on top.
        pendingRestEndsAt = null
        if (!RestAlarm.alertOnce(this, working, endsAt)) {
            notifications().notify(TimerNotifications.ONGOING_ID, TimerNotifications.ongoing(this, working))
        }
    }

    private fun cancelRestTimer() {
        restOver?.let(handler::removeCallbacks)
        restOver = null
        tick?.let(handler::removeCallbacks)
        tick = null
        if (pendingRestEndsAt != null) RestAlarm.cancel(this)
        pendingRestEndsAt = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun acquireWakeLock(timeoutMs: Long) {
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GymReps:rest").apply {
            setReferenceCounted(false)
            acquire(timeoutMs)
        }
    }

    private fun notifications() = getSystemService(NotificationManager::class.java)

    companion object {
        const val ACTION_SET_DONE = "com.geenedev.gymreps.action.SET_DONE"
        const val ACTION_SKIP_REST = "com.geenedev.gymreps.action.SKIP_REST"

        @Volatile
        var running = false
            private set

        /** Call from the foreground only (Android blocks starting it from the background). */
        fun start(context: Context) {
            if (!running) context.startForegroundService(Intent(context, TimerService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TimerService::class.java))
        }
    }
}
