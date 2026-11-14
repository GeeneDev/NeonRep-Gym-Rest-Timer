package com.geenedev.gymreps.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.geenedev.gymreps.ui.theme.PrussianBlue

// Same wording as the developer's other app (UAS Log Book), kept verbatim.
private const val FEEDBACK_EMAIL = "biagiodev1701@duck.com"
private const val INFO_DISCLAIMER_1 = "For suggestions and improvement send a mail to $FEEDBACK_EMAIL"
private const val INFO_DISCLAIMER_2 = "I gladly welcome feedbacks and suggestions."
private const val KOFI_DESC_0 = "Liked the application?"
private const val KOFI_DESC_1 = "Buy me a coffee"
private const val KOFI_LINK_0 = "https://ko-fi.com/ebg"

/** Info page: feedback contact and the Ko-fi link. A full-screen layer like Groups. */
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
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                    Text("Info", style = MaterialTheme.typography.titleLarge)
                }
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    InfoCard(
                        icon = Icons.Filled.MailOutline,
                        onClick = {
                            open(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$FEEDBACK_EMAIL")))
                        },
                    ) {
                        Text("$INFO_DISCLAIMER_1\n$INFO_DISCLAIMER_2", style = MaterialTheme.typography.bodyLarge)
                    }
                    InfoCard(
                        icon = Icons.Filled.LocalCafe,
                        onClick = { open(Intent(Intent.ACTION_VIEW, Uri.parse(KOFI_LINK_0))) },
                    ) {
                        Text("$KOFI_DESC_0\n$KOFI_DESC_1", style = MaterialTheme.typography.bodyLarge)
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
