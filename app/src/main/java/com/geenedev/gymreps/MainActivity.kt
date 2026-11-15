package com.geenedev.gymreps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.geenedev.gymreps.billing.Premium
import com.geenedev.gymreps.model.Freemium
import com.geenedev.gymreps.ui.SupportPopup
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geenedev.gymreps.data.GroupStore
import com.geenedev.gymreps.data.PhotoStore
import com.geenedev.gymreps.data.WorkoutStore
import com.geenedev.gymreps.notify.TimerAction
import com.geenedev.gymreps.notify.TimerBus
import com.geenedev.gymreps.notify.TimerNotifications
import com.geenedev.gymreps.notify.TimerService
import com.geenedev.gymreps.notify.TimerSnapshot
import com.geenedev.gymreps.ui.ExercisePhotoButton
import com.geenedev.gymreps.ui.EditPhotoRow
import com.geenedev.gymreps.ui.ExercisePhotoThumb
import com.geenedev.gymreps.ui.WorkoutScreen
import com.geenedev.gymreps.ui.WorkoutViewModel
import com.geenedev.gymreps.ui.theme.GymRepsTheme

class MainActivity : AppCompatActivity() {
    private val premium by lazy { Premium.get(this) }
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { askForExactAlarmsOnce() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark, so keep light status/nav bar icons whatever the phone's mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        TimerNotifications.createChannels(this)
        val store = WorkoutStore(applicationContext)
        val photos = PhotoStore(applicationContext)
        setContent {
            GymRepsTheme {
                // Created once per activity (rotation reuses it). A fresh launch (no saved
                // instance state) opens with an empty exercise unless a workout is running.
                val vm = viewModel {
                    WorkoutViewModel(
                        store,
                        clock = SystemClock::elapsedRealtime,
                        freshLaunch = savedInstanceState == null,
                        groupRunActive = GroupStore(applicationContext).loadRun() != null,
                    )
                }

                // Background timer: mirror the session into the notification service.
                val session = vm.session
                val exercise = vm.contextText.trim()
                LaunchedEffect(session, exercise) {
                    TimerBus.state.value = TimerSnapshot(session, exercise)
                }
                LaunchedEffect(session.isRunning) {
                    if (session.isRunning) {
                        askForNotificationsOnce()
                        TimerService.start(this@MainActivity)
                    } else {
                        TimerService.stop(this@MainActivity)
                    }
                }
                // "Set done" / "Skip rest" pressed on the notification.
                LaunchedEffect(vm) {
                    TimerBus.actions.collect { action ->
                        val s = vm.session
                        when (action) {
                            TimerAction.SetDone -> if (s.isRunning && !s.isResting) vm.onBigButton()
                            TimerAction.SkipRest -> vm.onEndRest()
                        }
                    }
                }

                WorkoutScreen(
                    vm = vm,
                    // In the background the "Rest over" notification vibrates instead.
                    onRestDone = { if (TimerBus.appVisible) buzz() },
                    photoButton = { exercise -> ExercisePhotoButton(exercise, photos) },
                    historyPhoto = { entry -> ExercisePhotoThumb(entry.context, photos) },
                    onEntryRenamed = photos::carryOver,
                    editPhoto = { exercise, edit -> EditPhotoRow(exercise, edit, photos) },
                    onPhotoEdited = { exercise, edit -> photos.applyEdit(exercise, edit.staged, edit.removed) },
                )

                // Free version: past 50 saved exercises, ask for the one-time unlock.
                // Re-checked every minute so it comes back an hour after a skip.
                var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(60_000)
                        now = System.currentTimeMillis()
                    }
                }
                val entries = vm.history.size
                if (Freemium.shouldNag(premium.unlocked, entries, premium.lastSkipAt, now)) {
                    SupportPopup(
                        entries = entries,
                        skipSeconds = Freemium.skipSeconds(entries),
                        price = premium.price,
                        onBuy = {
                            if (!premium.buy(this@MainActivity)) {
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.play_unreachable),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        },
                        onSkip = {
                            premium.lastSkipAt = System.currentTimeMillis()
                            now = premium.lastSkipAt
                        },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        TimerBus.appVisible = true
        TimerNotifications.cancelAlert(this) // clears the badge too
        premium.refresh() // picks up a purchase finished elsewhere, or a refund
    }

    override fun onStop() {
        TimerBus.appVisible = false
        super.onStop()
    }

    /** Asks for notification permission the first time a session starts in this app run. */
    private fun askForNotificationsOnce() {
        if (askedForNotifications) return
        askedForNotifications = true
        // Before Android 13 notifications need no runtime permission.
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else askForExactAlarmsOnce()
    }

    /**
     * Without "Alarms & reminders" the "Rest over" alarm is inexact and, with the phone locked
     * and the app frozen by the battery manager, can arrive up to ~45 s late. Asked once ever.
     */
    private fun askForExactAlarmsOnce() {
        val am = getSystemService(android.app.AlarmManager::class.java)
        val prefs = getSharedPreferences("permissions", MODE_PRIVATE)
        // Before Android 12 exact alarms need no special access.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms() ||
            prefs.getBoolean("askedExactAlarms", false)
        ) {
            askToRunInBackgroundOnce()
            return
        }
        prefs.edit().putBoolean("askedExactAlarms", true).apply()
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.exact_alarm_title)
            .setMessage(R.string.exact_alarm_message)
            .setPositiveButton(R.string.allow) { _, _ ->
                startActivity(
                    android.content.Intent(
                        android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        android.net.Uri.parse("package:$packageName"),
                    ),
                )
            }
            .setNegativeButton(R.string.not_now, null)
            .show()
    }

    /**
     * Honor/Huawei (and other aggressive battery managers) kill the app a few seconds after the
     * screen goes off, and then the "Rest over" alert only sometimes arrives. Asks once ever to
     * let GymReps run in the background, opening the phone's own setting for it.
     */
    private fun askToRunInBackgroundOnce() {
        val pm = getSystemService(android.os.PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        val prefs = getSharedPreferences("permissions", MODE_PRIVATE)
        if (prefs.getBoolean("askedBackground", false)) return
        prefs.edit().putBoolean("askedBackground", true).apply()
        val maker = Build.MANUFACTURER.lowercase()
        val honor = maker.contains("honor") || maker.contains("huawei")
        val steps = if (honor) {
            getString(R.string.background_steps_honor)
        } else {
            getString(R.string.background_steps_other)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.background_title)
            .setMessage(getString(R.string.background_message, steps))
            .setPositiveButton(R.string.open_settings) { _, _ -> openBackgroundSettings(honor) }
            .setNegativeButton(R.string.not_now, null)
            .show()
    }

    private fun openBackgroundSettings(honor: Boolean) {
        val tries = buildList {
            if (honor) {
                add(
                    android.content.Intent().setClassName(
                        "com.hihonor.systemmanager",
                        "com.hihonor.systemmanager.appcontrol.activity.StartupAppControlActivity",
                    ),
                )
                add(
                    android.content.Intent().setClassName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
                    ),
                )
            }
            add(android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            add(
                android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:$packageName"),
                ),
            )
        }
        for (intent in tries) {
            try {
                startActivity(intent)
                return
            } catch (_: Exception) {
                // Not on this phone, or not exported: try the next one.
            }
        }
    }

    /** Two short pulses when the rest countdown ends. */
    private fun buzz() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        } ?: return
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 150, 250), -1))
    }

    private companion object {
        var askedForNotifications = false
    }
}
