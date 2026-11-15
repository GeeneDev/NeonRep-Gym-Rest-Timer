package com.geenedev.gymreps.ui

import androidx.compose.ui.res.stringResource
import com.geenedev.gymreps.R

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geenedev.gymreps.model.Workout
import kotlin.math.PI
import kotlin.math.sin

/** One full jar of lava per minute of work; every extra minute makes the lava hotter. */
private const val LAP_MS = 60_000L
private const val MAX_HEAT_LAPS = 4

private val LavaDeep = Color(0xFFB71C1C)
private val LavaHot = Color(0xFFFF6D00)
private val LavaWhiteHot = Color(0xFFFFE57F)

/**
 * The set timer as a jar filling with lava: the level is the seconds of the current minute,
 * the surface waves and bubbles, and each full minute turns the heat up and adds a glow.
 */
@Composable
fun LavaJarTimer(workMs: Long, totalMs: Long, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "lava")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "wave",
    )
    val bubbleT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "bubbles",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse",
    )

    val laps = (workMs / LAP_MS).toInt()
    val level = (workMs % LAP_MS) / LAP_MS.toFloat()
    val heat = laps.coerceAtMost(MAX_HEAT_LAPS) / MAX_HEAT_LAPS.toFloat()
    val top = lerp(LavaHot, LavaWhiteHot, heat)
    val bottom = lerp(LavaDeep, LavaHot, heat)
    val glass = MaterialTheme.colorScheme.onSurface

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(width = 132.dp, height = 168.dp)) {
            val w = size.width
            val h = size.height
            val neckW = w * 0.5f
            val neckTop = h * 0.06f
            val shoulder = h * 0.16f
            val body = Rect(w * 0.06f, shoulder, w * 0.94f, h)
            val corner = w * 0.2f
            val jar = Path.combine(
                PathOperation.Union,
                Path().apply { addRoundRect(RoundRect(body, CornerRadius(corner))) },
                Path().apply { addRect(Rect((w - neckW) / 2, neckTop, (w + neckW) / 2, shoulder + corner)) },
            )

            // Heat glow behind the jar, stronger with every full minute.
            if (laps > 0) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(top.copy(alpha = 0.25f + 0.25f * heat * pulse), Color.Transparent),
                        center = body.center,
                        radius = w * 0.85f,
                    ),
                    radius = w * 0.85f,
                    center = body.center,
                )
            }

            clipPath(jar) {
                drawRect(glass.copy(alpha = 0.06f))
                val surface = h - (h - neckTop) * level
                val amp = if (level > 0f) 4.dp.toPx() else 0f
                val lava = Path().apply {
                    moveTo(0f, surface)
                    var x = 0f
                    while (x <= w) {
                        lineTo(x, surface + sin(x / w * 3f * PI.toFloat() + phase) * amp)
                        x += 4f
                    }
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(lava, Brush.verticalGradient(listOf(top, bottom), startY = surface, endY = h))

                // Bubbles rising from the bottom to the surface.
                if (level > 0.05f || laps > 0) {
                    for (i in 0 until 7) {
                        val progress = (bubbleT + i / 7f) % 1f
                        val y = h - progress * (h - surface)
                        val x = w * (0.18f + 0.64f * ((i * 0.618f) % 1f)) +
                            sin(progress * 6f + i) * 3.dp.toPx()
                        val r = (2f + (i % 3) * 1.5f).dp.toPx()
                        drawCircle(
                            color = LavaWhiteHot.copy(alpha = 0.7f * (1f - progress)),
                            radius = r,
                            center = Offset(x, y),
                        )
                    }
                }
            }

            drawPath(jar, color = glass.copy(alpha = 0.55f), style = Stroke(width = 3.dp.toPx()))
            // Lid.
            val lidW = neckW * 1.2f
            drawRoundRect(
                color = glass.copy(alpha = 0.7f),
                topLeft = Offset((w - lidW) / 2, 0f),
                size = Size(lidW, neckTop * 1.6f),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            Workout.formatDuration(workMs),
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            color = lerp(LavaDeep, LavaHot, 0.5f + heat / 2),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (laps > 0) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = top,
                    modifier = Modifier.size(18.dp),
                )
                Text("x$laps", fontWeight = FontWeight.Bold, color = top)
                Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                stringResource(R.string.total, Workout.formatDuration(totalMs)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
