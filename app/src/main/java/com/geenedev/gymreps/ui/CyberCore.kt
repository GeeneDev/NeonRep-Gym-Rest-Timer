package com.geenedev.gymreps.ui

import androidx.compose.ui.res.stringResource
import com.geenedev.gymreps.R

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.geenedev.gymreps.model.SessionState
import com.geenedev.gymreps.model.Workout
import com.geenedev.gymreps.ui.theme.AlabasterGrey
import com.geenedev.gymreps.ui.theme.Black
import com.geenedev.gymreps.ui.theme.Orange
import com.geenedev.gymreps.ui.theme.PrussianBlue
import com.geenedev.gymreps.ui.theme.White

/** One full charge of the core per minute of work. */
private const val CHARGE_MS = 60_000L

// Palette (see theme/Color.kt): orange = idle/working, alabaster grey = resting.
private val Working = Orange
private val Resting = AlabasterGrey
private val Idle = Orange.copy(alpha = 0.85f)
private val Pip = White
private val CoreDark = Black
private val CoreRim = PrussianBlue

/**
 * The main button sitting inside a neon reactor core.
 * Working: the core charges up (orange) over each minute; every full minute adds a lit pip.
 * Resting: the core drains (grey) as the rest countdown runs out.
 * Idle: the core idles with a slow pulse, waiting for START.
 */
@Composable
fun CyberCore(
    session: SessionState,
    now: Long,
    restTotalMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "core")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "spin",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )

    val resting = session.isResting
    val running = session.isRunning
    val workMs = if (running && !resting) session.workElapsed(now).coerceAtLeast(0L) else 0L
    val restLeft = if (resting) session.restRemaining(now).coerceAtLeast(0L) else 0L

    val color = when {
        resting -> Resting
        running -> Working
        else -> Idle
    }
    val level = when {
        resting -> if (restTotalMs > 0) (restLeft.toFloat() / restTotalMs).coerceIn(0f, 1f) else 0f
        // While working the progress ring stays full and still; only the rest countdown drains it.
        running -> 1f
        else -> 0f
    }
    val fullCharges = (workMs / CHARGE_MS).toInt()

    val totalMs = if (running) session.totalElapsed(now).coerceAtLeast(0L) else 0L

    Box(modifier.size(230.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            // The rings stay still unless a timer (set or rest) is running.
            drawCore(color, level, if (running) spin else 0f, pulse, running, fullCharges, reverseInner = resting)
        }
        Button(
            onClick = onClick,
            shape = CircleShape,
            modifier = Modifier.size(150.dp),
            contentPadding = PaddingValues(0.dp),
            border = BorderStroke(2.dp, color.copy(alpha = 0.6f + 0.4f * pulse)),
            colors = ButtonDefaults.buttonColors(containerColor = CoreDark, contentColor = color),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!running) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(44.dp))
                    CoreText(stringResource(R.string.core_start), 24.sp, FontWeight.Black)
                } else {
                    // Set counter, then what a tap does, then the times.
                    CoreText("${session.sets}", 46.sp, FontWeight.Black)
                    if (resting) {
                        CoreText(Workout.formatDuration(restLeft + 999), 18.sp, FontWeight.Bold)
                    } else {
                        CoreText(stringResource(R.string.core_go), 18.sp, FontWeight.Black)
                    }
                    CoreText(
                        if (resting) stringResource(R.string.core_total, Workout.formatDuration(totalMs))
                        else "${Workout.formatDuration(workMs)} · ${Workout.formatDuration(totalMs)}",
                        11.sp,
                        FontWeight.Normal,
                        color.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CoreText(
    text: String,
    size: TextUnit,
    weight: FontWeight,
    color: Color = Color.Unspecified,
) {
    Text(
        text,
        fontSize = size,
        lineHeight = size,
        fontWeight = weight,
        fontFamily = FontFamily.Monospace,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun DrawScope.drawCore(
    color: Color,
    level: Float,
    spin: Float,
    pulse: Float,
    running: Boolean,
    fullCharges: Int,
    reverseInner: Boolean,
) {
    val c = center
    val outer = size.minDimension / 2f
    val glow = if (running) 0.25f + 0.35f * level else 0.12f * pulse

    // Halo behind everything.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = glow), Color.Transparent),
            center = c,
            radius = outer,
        ),
        radius = outer,
        center = c,
    )

    // Dark housing ring.
    val housingR = outer * 0.82f
    drawCircle(CoreRim, housingR, c, style = Stroke(width = outer * 0.16f))

    // Charge track and charge arc.
    val arcR = outer * 0.82f
    val arcW = outer * 0.075f
    val arcTopLeft = Offset(c.x - arcR, c.y - arcR)
    val arcSize = Size(arcR * 2, arcR * 2)
    drawArc(
        color = color.copy(alpha = 0.15f),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = arcW),
    )
    if (level > 0f) {
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * level,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = arcW, cap = StrokeCap.Round),
        )
    }

    // Segmented outer ring spinning one way...
    val segR = outer * 0.95f
    val segTopLeft = Offset(c.x - segR, c.y - segR)
    val segSize = Size(segR * 2, segR * 2)
    rotate(spin, c) {
        for (i in 0 until 12) {
            drawArc(
                color = color.copy(alpha = if (i % 3 == 0) 0.9f else 0.35f),
                startAngle = i * 30f,
                sweepAngle = 18f,
                useCenter = false,
                topLeft = segTopLeft,
                size = segSize,
                style = Stroke(width = outer * 0.025f),
            )
        }
    }
    // ...and a thin inner ring, faster. While a set runs it turns the same way as
    // everything else, so nothing looks like it is counting backwards; only the
    // rest countdown spins it the other way.
    val innerR = outer * 0.68f
    val innerTopLeft = Offset(c.x - innerR, c.y - innerR)
    val innerSize = Size(innerR * 2, innerR * 2)
    rotate((if (reverseInner) -spin else spin) * 1.5f, c) {
        for (i in 0 until 3) {
            drawArc(
                color = color.copy(alpha = 0.7f),
                startAngle = i * 120f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = innerTopLeft,
                size = innerSize,
                style = Stroke(width = outer * 0.015f),
            )
        }
    }

    // One lit pip per full minute of charge (up to 8), just inside the charge arc.
    val pips = fullCharges.coerceAtMost(8)
    for (i in 0 until 8) {
        val angle = Math.toRadians((-90.0 - 42.0) + i * 12.0)
        val p = Offset(
            c.x + (outer * 0.745f) * kotlin.math.cos(angle).toFloat(),
            c.y + (outer * 0.745f) * kotlin.math.sin(angle).toFloat(),
        )
        if (i < pips) {
            drawCircle(Pip, outer * 0.022f, p)
        }
    }
}
