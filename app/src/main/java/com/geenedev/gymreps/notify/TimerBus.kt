package com.geenedev.gymreps.notify

import com.geenedev.gymreps.model.SessionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** What the timer notification shows: the running session and the exercise name. */
data class TimerSnapshot(
    val session: SessionState = SessionState(),
    val exercise: String = "",
)

/** Buttons on the notification. */
enum class TimerAction { SetDone, SkipRest }

/**
 * In-process link between the screen and [TimerService]. The screen publishes its state here,
 * the service turns it into notifications, and notification buttons come back as [actions].
 */
object TimerBus {
    val state = MutableStateFlow(TimerSnapshot())

    private val _actions = MutableSharedFlow<TimerAction>(extraBufferCapacity = 4)
    val actions: SharedFlow<TimerAction> = _actions.asSharedFlow()

    /** True while the app is on screen; alerts and badges are only posted when it is not. */
    @Volatile
    var appVisible: Boolean = false

    fun send(action: TimerAction) {
        _actions.tryEmit(action)
    }
}
