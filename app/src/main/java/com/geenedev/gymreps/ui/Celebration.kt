package com.geenedev.gymreps.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlin.random.Random

/** The one classic festive look of the easter-egg celebration. */
private val PartyEmojis = listOf("🎉", "🎊", "⭐", "✨", "🏆", "💪")
private val PartyConfetti = listOf(
    Color(0xFFFF4081), Color(0xFFFFD600), Color(0xFF00E5FF), Color(0xFF76FF03), Color(0xFF7C4DFF),
)

private const val PER_BURST = 36
private const val MAX_PARTICLES = 400
private const val GRAVITY = 900f // px/s²

internal class Particle(
    var x: Float, // fraction of width
    var y: Float, // px
    val vx: Float, // fraction of width per second
    var vy: Float, // px per second
    var rotation: Float,
    val spin: Float,
    val emoji: String?,
    val color: Color,
    val sizeSp: Int,
    val wobble: Float,
)

/** Falling particles plus a "Congratulations" banner; call [burst] as often as you like. */
class CelebrationState {
    internal val particles = ArrayList<Particle>()
    internal var frame by mutableLongStateOf(0L)
    internal var running by mutableStateOf(false)
    internal var banner by mutableStateOf("")
    internal var bannerId by mutableIntStateOf(0)

    fun burst(message: String) {
        repeat(PER_BURST) {
            val confetti = Random.nextFloat() < 0.55f
            particles += Particle(
                x = Random.nextFloat(),
                y = -40f - Random.nextFloat() * 500f,
                vx = (Random.nextFloat() - 0.5f) * 0.25f,
                vy = 150f + Random.nextFloat() * 350f,
                rotation = Random.nextFloat() * 360f,
                spin = (Random.nextFloat() - 0.5f) * 540f,
                emoji = if (confetti) null else PartyEmojis.random(),
                color = if (confetti) PartyConfetti.random() else Color.Unspecified,
                sizeSp = listOf(24, 28, 32, 36, 40).random(),
                wobble = Random.nextFloat() * 6f,
            )
        }
        while (particles.size > MAX_PARTICLES) particles.removeAt(0)
        banner = message
        bannerId++
        running = true
    }
}

@Composable
fun rememberCelebrationState(): CelebrationState = remember { CelebrationState() }

/** Full-screen, touch-transparent layer that draws the celebration on top of the app. */
@Composable
fun CelebrationOverlay(state: CelebrationState, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val emojiCache = remember { HashMap<Pair<String, Int>, TextLayoutResult>() }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val heightPx = constraints.maxHeight.toFloat()

        LaunchedEffect(state.running) {
            if (!state.running) return@LaunchedEffect
            var last = withFrameNanos { it }
            while (state.particles.isNotEmpty()) {
                withFrameNanos { now ->
                    val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                    last = now
                    val iterator = state.particles.iterator()
                    while (iterator.hasNext()) {
                        val p = iterator.next()
                        p.vy += GRAVITY * dt * 0.35f
                        p.y += p.vy * dt
                        p.x += p.vx * dt
                        p.rotation += p.spin * dt
                        if (p.y > heightPx + 120f) iterator.remove()
                    }
                    state.frame = now
                }
            }
            state.running = false
        }

        Canvas(Modifier.fillMaxSize()) {
            state.frame // redraw every frame while particles move
            for (p in state.particles) {
                val px = p.x * size.width + sin(p.y / 60f + p.wobble) * 12.dp.toPx()
                translate(px, p.y) {
                    rotate(p.rotation, pivot = Offset.Zero) {
                        when {
                            p.emoji != null -> {
                                val layout = emojiCache.getOrPut(p.emoji to p.sizeSp) {
                                    measurer.measure(p.emoji, TextStyle(fontSize = p.sizeSp.sp))
                                }
                                drawText(
                                    layout,
                                    topLeft = Offset(-layout.size.width / 2f, -layout.size.height / 2f),
                                )
                            }
                            else -> drawRect(
                                p.color,
                                topLeft = Offset(-4.dp.toPx(), -7.dp.toPx()),
                                size = Size(8.dp.toPx(), 14.dp.toPx()),
                            )
                        }
                    }
                }
            }
        }

        if (state.bannerId > 0) {
            key(state.bannerId) {
                val scale = remember { Animatable(0.4f) }
                val alpha = remember { Animatable(1f) }
                LaunchedEffect(Unit) {
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    alpha.animateTo(0f, tween(durationMillis = 900, delayMillis = 900))
                }
                Text(
                    state.banner,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = 120.dp, start = 24.dp, end = 24.dp)
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                            this.alpha = alpha.value
                        },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 4f), 8f),
                    ),
                )
            }
        }
    }
}
