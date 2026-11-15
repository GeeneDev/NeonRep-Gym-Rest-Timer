package com.geenedev.gymreps.ui

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.geenedev.gymreps.R

/** Quantity string from resources, e.g. "1 set" / "3 serie". */
@Composable
@ReadOnlyComposable
fun pluralRes(@PluralsRes id: Int, count: Int): String {
    LocalConfiguration.current // re-read when the language changes
    return LocalContext.current.resources.getQuantityString(id, count, count)
}

/** "1 set" / "3 sets" in the app's language, for code outside composition. */
fun Resources.setsLabel(n: Int): String = getQuantityString(R.plurals.sets_count, n, n)
