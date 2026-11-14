package com.geenedev.gymreps.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.NordicWalking
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector

/** Icons a context can wear. Keys are stored, so never rename one. */
object ContextIcons {
    const val DEFAULT_KEY = "dumbbell"

    val all: List<Pair<String, ImageVector>> = listOf(
        "dumbbell" to Icons.Filled.FitnessCenter,
        "body" to Icons.Filled.Accessibility,
        "stairs" to Icons.Filled.Stairs,
        "bike" to Icons.Filled.PedalBike,
        "walk" to Icons.Filled.NordicWalking,
        "cardio" to Icons.Filled.MonitorHeart,
        "body_weight" to Icons.Filled.MonitorWeight,
        "scale" to Icons.Filled.Scale,
        "speed" to Icons.Filled.Speed,
        "progress" to Icons.AutoMirrored.Filled.TrendingUp,
        "hot" to Icons.Filled.Whatshot,
        "medal" to Icons.Filled.MilitaryTech,
        "badge" to Icons.Filled.WorkspacePremium,
        "finish" to Icons.Filled.SportsScore,
        "whistle" to Icons.Filled.Sports,
        "water" to Icons.Filled.WaterDrop,
        "protein" to Icons.Filled.Egg,
        "recovery" to Icons.Filled.Spa,
        "stopwatch" to Icons.Filled.AvTimer,
        "yoga" to Icons.Filled.SelfImprovement,
        "swim" to Icons.Filled.Pool,
        "hike" to Icons.Filled.Hiking,
        "fire" to Icons.Filled.LocalFireDepartment,
        "bolt" to Icons.Filled.Bolt,
        "heart" to Icons.Filled.Favorite,
        "star" to Icons.Filled.Star,
        "trophy" to Icons.Filled.EmojiEvents,
        "timer" to Icons.Filled.Timer,
    )

    private val byKey = all.toMap()

    fun forKey(key: String): ImageVector = byKey[key] ?: Icons.Filled.FitnessCenter
}
