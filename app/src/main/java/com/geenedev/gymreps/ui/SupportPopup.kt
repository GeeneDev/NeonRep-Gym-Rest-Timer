package com.geenedev.gymreps.ui

import androidx.compose.ui.res.stringResource
import com.geenedev.gymreps.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.geenedev.gymreps.model.Freemium
import com.geenedev.gymreps.ui.theme.AlabasterGrey
import com.geenedev.gymreps.ui.theme.NavyHigh
import com.geenedev.gymreps.ui.theme.OnOrange
import com.geenedev.gymreps.ui.theme.Orange
import com.geenedev.gymreps.ui.theme.TextMuted
import kotlinx.coroutines.delay

/**
 * Free-version popup: asks for the one-time unlock. It can't be dismissed by back or tapping
 * outside; "Skip" unlocks after [skipSeconds] (the countdown survives rotation).
 */
@Composable
fun SupportPopup(
    entries: Int,
    skipSeconds: Int,
    price: String?,
    onBuy: () -> Unit,
    onSkip: () -> Unit,
) {
    var left by rememberSaveable { mutableIntStateOf(skipSeconds) }
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1000)
            left--
        }
    }
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        ElevatedCard(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = NavyHigh),
        ) {
            Column(
                Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = Orange, modifier = Modifier.size(40.dp))
                Text(
                    pluralRes(R.plurals.support_title, entries),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AlabasterGrey,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.support_body, Freemium.FREE_ENTRIES),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onBuy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = OnOrange),
                ) {
                    Text(if (price != null) stringResource(R.string.unlock_with_price, price) else stringResource(R.string.unlock), fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick = onSkip,
                    enabled = left == 0,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AlabasterGrey,
                        disabledContentColor = TextMuted.copy(alpha = 0.6f),
                    ),
                ) {
                    Text(if (left > 0) stringResource(R.string.skip_in, left) else stringResource(R.string.skip))
                }
            }
        }
    }
}

