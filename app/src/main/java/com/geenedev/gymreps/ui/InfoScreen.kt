package com.geenedev.gymreps.ui

import androidx.compose.ui.res.stringResource
import com.geenedev.gymreps.R

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import com.geenedev.gymreps.data.AppLanguage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.geenedev.gymreps.ui.theme.PrussianBlue

private const val FEEDBACK_EMAIL = "biagiodev1701@duck.com"
private const val KOFI_LINK_0 = "https://ko-fi.com/ebg"

/** Info page: language, feedback contact and the Ko-fi link. A full-screen layer like Groups. */
@Composable
fun InfoScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    fun open(intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            // No app to handle it: nothing to do.
        }
    }

    BackHandler(onBack = onDismiss)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close)) }
                    Text(stringResource(R.string.info), style = MaterialTheme.typography.titleLarge)
                }
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LanguageCard()
                    InfoCard(
                        icon = Icons.Filled.MailOutline,
                        onClick = {
                            open(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$FEEDBACK_EMAIL")))
                        },
                    ) {
                        Text(stringResource(R.string.info_feedback, FEEDBACK_EMAIL), style = MaterialTheme.typography.bodyLarge)
                    }
                    InfoCard(
                        icon = Icons.Filled.LocalCafe,
                        onClick = { open(Intent(Intent.ACTION_VIEW, Uri.parse(KOFI_LINK_0))) },
                    ) {
                        Text(stringResource(R.string.info_kofi), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            KOFI_LINK_0,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, onClick: () -> Unit, content: @Composable () -> Unit) {
    ElevatedCard(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = PrussianBlue),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column { content() }
        }
    }
}

/** Language picker: phone default, English, Italiano, Español. Picking one restarts the screen in it. */
@Composable
private fun LanguageCard() {
    val context = LocalContext.current
    val current = AppLanguage.current()
    val choices = listOf("" to stringResource(R.string.language_system)) + AppLanguage.options
    ElevatedCard(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.elevatedCardColors(containerColor = PrussianBlue),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.padding(top = 4.dp).selectableGroup()) {
                for ((tag, label) in choices) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = tag == current,
                                role = Role.RadioButton,
                                onClick = { if (tag != current) AppLanguage.set(context, tag) },
                            )
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = tag == current, onClick = null, modifier = Modifier.padding(start = 2.dp, end = 12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
