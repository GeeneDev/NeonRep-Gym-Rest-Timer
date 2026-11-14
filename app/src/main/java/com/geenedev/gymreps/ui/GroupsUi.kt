@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.geenedev.gymreps.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.zIndex
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.abs
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.geenedev.gymreps.model.ExerciseGroup
import com.geenedev.gymreps.model.GroupItem
import com.geenedev.gymreps.model.Groups
import com.geenedev.gymreps.model.SavedContext
import com.geenedev.gymreps.model.Workout

private fun exercisesLabel(n: Int) = if (n == 1) "1 exercise" else "$n exercises"
private fun setsWord(n: Int) = if (n == 1) "1 set" else "$n sets"

/**
 * Full-screen layer listing the groups: start one, edit, delete, or make a new one.
 * [unsavedSets] > 0 asks before starting a group, because starting clears the counter.
 */
@Composable
fun GroupsScreen(
    controller: GroupsController,
    savedExercises: List<SavedContext>,
    unsavedSets: Int,
    onStarted: (ExerciseGroup) -> Unit,
    onDismiss: () -> Unit,
) {
    // null: the list. A group (id 0 for a new one): the editor.
    var editing by remember { mutableStateOf<ExerciseGroup?>(null) }
    var confirmStart by remember { mutableStateOf<ExerciseGroup?>(null) }
    var confirmDelete by remember { mutableStateOf<ExerciseGroup?>(null) }

    fun start(group: ExerciseGroup) {
        controller.start(group)
        onStarted(group)
    }

    // A full-screen layer over the workout screen (not a Dialog: on some phones a Dialog
    // window ignores the system bar insets and hides the bottom button under the nav bar).
    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize()) {
        // Surface also swallows touches so nothing underneath reacts.
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.safeDrawingPadding()) {
                val draft = editing
                if (draft != null) {
                    GroupEditor(
                        initial = draft,
                        savedExercises = savedExercises,
                        onSave = {
                            controller.saveGroup(it)
                            editing = null
                        },
                        onCancel = { editing = null },
                    )
                } else {
                    GroupList(
                        groups = controller.groups,
                        activeId = controller.activeGroup?.id,
                        onStart = { if (unsavedSets > 0) confirmStart = it else start(it) },
                        onStop = controller::stop,
                        onEdit = { editing = it },
                        onDelete = { confirmDelete = it },
                        onNew = { editing = ExerciseGroup(0, "", emptyList()) },
                        onClose = onDismiss,
                    )
                }
            }
        }

        confirmStart?.let { group ->
            AlertDialog(
                onDismissRequest = { confirmStart = null },
                title = { Text("Start ${group.name}?") },
                text = { Text("Your ${setsWord(unsavedSets)} not saved yet will be cleared.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmStart = null
                        start(group)
                    }) { Text("Start") }
                },
                dismissButton = { TextButton(onClick = { confirmStart = null }) { Text("Cancel") } },
            )
        }
        confirmDelete?.let { group ->
            AlertDialog(
                onDismissRequest = { confirmDelete = null },
                title = { Text("Delete ${group.name}?") },
                text = { Text("The group is removed. Your history is kept.") },
                confirmButton = {
                    TextButton(onClick = {
                        controller.deleteGroup(group.id)
                        confirmDelete = null
                    }) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
            )
        }
    }
}

@Composable
private fun GroupList(
    groups: List<ExerciseGroup>,
    activeId: Long?,
    onStart: (ExerciseGroup) -> Unit,
    onStop: () -> Unit,
    onEdit: (ExerciseGroup) -> Unit,
    onDelete: (ExerciseGroup) -> Unit,
    onNew: () -> Unit,
    onClose: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
            Text("Groups", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (groups.isEmpty()) {
                item {
                    Text(
                        "Put exercises together in a group, like \"Push day\". Start it and Done takes you " +
                            "from one exercise to the next.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(groups, key = { it.id }) { group ->
                GroupCard(
                    group = group,
                    active = group.id == activeId,
                    onStart = { onStart(group) },
                    onStop = onStop,
                    onEdit = { onEdit(group) },
                    onDelete = { onDelete(group) },
                )
            }
        }
        Button(
            onClick = onNew,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("New group")
        }
    }
}

@Composable
private fun GroupCard(
    group: ExerciseGroup,
    active: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    ScrollingName(
                        group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (active) "${exercisesLabel(group.items.size)} · in progress" else exercisesLabel(group.items.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit ${group.name}") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete ${group.name}") }
            }
            if (group.items.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                // Up to 5 per column, as many columns as fit; more scroll sideways.
                ColumnFlow(
                    items = group.items,
                    minColumnWidth = 140.dp,
                    maxRows = 5,
                    modifier = Modifier.padding(end = 8.dp),
                ) { index, item -> CompactExerciseRow(index + 1, item) }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.End) {
                if (active) {
                    // Leaves the group; the current exercise stays in the field.
                    OutlinedButton(onClick = onStop) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Stop")
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Button(onClick = onStart, enabled = group.items.isNotEmpty()) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (active) "Restart" else "Start")
                }
            }
        }
    }
}

/** Name plus an ordered list of exercises, picked from saved ones or typed on the fly. */
@Composable
private fun GroupEditor(
    initial: ExerciseGroup,
    savedExercises: List<SavedContext>,
    onSave: (ExerciseGroup) -> Unit,
    onCancel: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var name by remember { mutableStateOf(initial.name) }
    var items by remember { mutableStateOf(initial.items) }
    var newName by remember { mutableStateOf("") }
    var newIcon by remember { mutableStateOf(ContextIcons.DEFAULT_KEY) }
    // Which icon the picker changes: -1 the new exercise, otherwise a row of the list.
    var pickingIconFor by remember { mutableStateOf<Int?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val dirty = name != initial.name || items != initial.items || newName.isNotBlank()
    val canSave = name.isNotBlank() && items.isNotEmpty()

    fun cancel() {
        if (dirty) confirmDiscard = true else onCancel()
    }
    fun addTyped() {
        // A typed name that matches a saved exercise takes that exercise's icon unless one was chosen.
        val match = savedExercises.find { it.name.equals(newName.trim(), ignoreCase = true) }
        val icon = if (newIcon == ContextIcons.DEFAULT_KEY && match != null && match.iconKey.isNotEmpty()) match.iconKey else newIcon
        items = Groups.addItem(items, match?.name ?: newName, icon)
        newName = ""
        newIcon = ContextIcons.DEFAULT_KEY
    }

    BackHandler { cancel() }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { cancel() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(
                if (initial.id == 0L) "New group" else "Edit group",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { onSave(initial.copy(name = name.trim(), items = items)) },
                enabled = canSave,
            ) { Text("Save") }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Group name") },
                    placeholder = { Text("e.g. Push day") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )
            }
            item {
                Text(
                    "Exercises",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (items.isEmpty()) {
                item {
                    Text(
                        "Add exercises below. They run in this order.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (items.isNotEmpty()) {
                if (items.size > 1) {
                    item {
                        Text(
                            "Hold an exercise and drag it to change the order.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    ReorderableGroupItems(
                        items = items,
                        onMove = { from, to -> items = Groups.move(items, from, to - from) },
                        onIconClick = { pickingIconFor = it },
                        onRemove = { index -> items = items.toMutableList().apply { removeAt(index) } },
                    )
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { pickingIconFor = -1 }, modifier = Modifier.size(56.dp)) {
                        Icon(ContextIcons.forKey(newIcon), contentDescription = "Choose icon")
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Add exercise") },
                        placeholder = { Text("Type a new one or pick below") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { addTyped() }),
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { addTyped() }, enabled = newName.isNotBlank()) {
                        Icon(Icons.Filled.Add, contentDescription = "Add exercise")
                    }
                }
            }
            val suggestions = Workout.filterContexts(savedExercises, newName)
            if (suggestions.isNotEmpty()) {
                item {
                    Text(
                        "Your exercises",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { saved ->
                            AssistChip(
                                onClick = {
                                    items = Groups.addItem(items, saved.name, saved.iconKey.ifEmpty { ContextIcons.DEFAULT_KEY })
                                    newName = ""
                                },
                                label = { Text(saved.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingIcon = {
                                    Icon(
                                        ContextIcons.forKey(saved.iconKey),
                                        contentDescription = null,
                                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    pickingIconFor?.let { target ->
        IconPickerDialog(
            selected = if (target < 0) newIcon else items.getOrNull(target)?.iconKey ?: ContextIcons.DEFAULT_KEY,
            onPick = { key ->
                if (target < 0) newIcon = key
                else items = items.mapIndexed { i, it -> if (i == target) it.copy(iconKey = key) else it }
                pickingIconFor = null
            },
            onDismiss = { pickingIconFor = null },
        )
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("What you changed in this group will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onCancel()
                }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
}

/**
 * The editor's exercise list. Hold a row and drag it up or down to change the order;
 * the other rows make room as it passes the middle of each.
 */
@Composable
private fun ReorderableGroupItems(
    items: List<GroupItem>,
    onMove: (from: Int, to: Int) -> Unit,
    onIconClick: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val spacing = 8.dp
    val spacingPx = with(LocalDensity.current) { spacing.toPx() }
    val heights = remember { mutableMapOf<Int, Int>() }
    var dragging by remember { mutableStateOf<Int?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val currentSize by rememberUpdatedState(items.size)
    val currentOnMove by rememberUpdatedState(onMove)

    fun indexAt(y: Float): Int? {
        var top = 0f
        for (i in 0 until currentSize) {
            val h = heights[i] ?: return null
            if (y < top + h + spacingPx / 2) return i
            top += h + spacingPx
        }
        return null
    }
    fun swapHeights(a: Int, b: Int) {
        val ha = heights[a]
        val hb = heights[b]
        if (ha != null && hb != null) { heights[a] = hb; heights[b] = ha }
    }

    Column(
        Modifier.fillMaxWidth().pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { pos ->
                    indexAt(pos.y)?.let {
                        dragging = it
                        offset = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
                onDrag = { change, amount ->
                    val d = dragging ?: return@detectDragGesturesAfterLongPress
                    change.consume()
                    offset += amount.y
                    if (offset > 0 && d < currentSize - 1) {
                        val step = (heights[d + 1] ?: 0) + spacingPx
                        if (offset > step / 2) {
                            swapHeights(d, d + 1)
                            currentOnMove(d, d + 1)
                            dragging = d + 1
                            offset -= step
                        }
                    } else if (offset < 0 && d > 0) {
                        val step = (heights[d - 1] ?: 0) + spacingPx
                        if (-offset > step / 2) {
                            swapHeights(d, d - 1)
                            currentOnMove(d, d - 1)
                            dragging = d - 1
                            offset += step
                        }
                    }
                },
                onDragEnd = { dragging = null; offset = 0f },
                onDragCancel = { dragging = null; offset = 0f },
            )
        },
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        items.forEachIndexed { index, item ->
            val lifted = index == dragging
            Box(
                Modifier
                    .onSizeChanged { heights[index] = it.height }
                    .zIndex(if (lifted) 1f else 0f)
                    .graphicsLayer {
                        if (lifted) {
                            translationY = offset
                            scaleX = 1.03f
                            scaleY = 1.03f
                        }
                    },
            ) {
                GroupItemRow(
                    position = index + 1,
                    item = item,
                    lifted = lifted,
                    onIconClick = { onIconClick(index) },
                    onRemove = { onRemove(index) },
                )
            }
        }
    }
}

@Composable
private fun GroupItemRow(
    position: Int,
    item: GroupItem,
    lifted: Boolean,
    onIconClick: () -> Unit,
    onRemove: () -> Unit,
) {
    OutlinedCard(
        Modifier.fillMaxWidth(),
        colors = if (lifted) CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        else CardDefaults.outlinedCardColors(),
        border = if (lifted) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else CardDefaults.outlinedCardBorder(),
    ) {
        Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.DragHandle,
                contentDescription = null,
                tint = if (lifted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp).size(20.dp),
            )
            Text(
                "$position",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(20.dp),
            )
            IconButton(onClick = onIconClick) {
                Icon(ContextIcons.forKey(item.iconKey), contentDescription = "Change icon")
            }
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove ${item.name}")
            }
        }
    }
}

/**
 * A vertical list that flows into columns, read top to bottom then left to right.
 * As many columns of at least [minColumnWidth] as fit the width are shown. With [maxRows]
 * a column holds at most that many; the extra columns then scroll sideways.
 * Without it the rows split evenly over the columns that fit.
 */
@Composable
private fun <T> ColumnFlow(
    items: List<T>,
    minColumnWidth: androidx.compose.ui.unit.Dp,
    maxRows: Int?,
    modifier: Modifier = Modifier,
    horizontalSpacing: androidx.compose.ui.unit.Dp = 16.dp,
    verticalSpacing: androidx.compose.ui.unit.Dp = 4.dp,
    content: @Composable (index: Int, item: T) -> Unit,
) {
    if (items.isEmpty()) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val fit = ((maxWidth + horizontalSpacing) / (minColumnWidth + horizontalSpacing)).toInt().coerceAtLeast(1)
        val evenRows = (items.size + fit - 1) / fit
        val rows = if (maxRows != null) evenRows.coerceAtMost(maxRows) else evenRows
        val columns = items.chunked(rows)
        val columnWidth = (maxWidth - horizontalSpacing * (fit - 1)) / fit
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        ) {
            columns.forEachIndexed { c, column ->
                Column(Modifier.width(columnWidth), verticalArrangement = Arrangement.spacedBy(verticalSpacing)) {
                    column.forEachIndexed { r, item -> content(c * rows + r, item) }
                }
            }
        }
    }
}

/** One line of a group card's exercise list: position, icon, name. */
@Composable
private fun CompactExerciseRow(position: Int, item: GroupItem) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$position",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            ContextIcons.forKey(item.iconKey),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            item.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Longest a chip label grows before its name ends in "…". */
private val NameMaxWidth = 150.dp

/** Green tick of an exercise already cleared (saved with Done) in the running group. */
private val ClearedGreen = Color(0xFF4ADE80)

/**
 * One line of text that scrolls slowly sideways (marquee) when it does not fit,
 * so long group and exercise names can still be read in full.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollingName(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
) {
    Text(
        text,
        style = style,
        fontWeight = fontWeight,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.basicMarquee(
            iterations = Int.MAX_VALUE,
            initialDelayMillis = 1500,
            delayMillis = 2000,
            velocity = 30.dp,
        ),
    )
}

/**
 * Mini-player shown above the Exercise field while a group runs, like a music player's bar.
 * Collapsed: the current exercise, the group with its progress, and a Next button.
 * Swipe left for the next exercise, right for the previous one. Tap to expand it:
 * then it shows Previous / Next buttons, a chip per exercise (tap one to jump there)
 * and the X that leaves the group, keeping the current exercise in the field.
 */
@Composable
fun GroupRunStrip(
    controller: GroupsController,
    unsavedSets: Int,
) {
    val group = controller.activeGroup ?: return
    val run = controller.run ?: return
    var confirmJump by remember { mutableStateOf<Int?>(null) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val size = group.items.size
    val current = group.items[run.index]

    // Next (button or swipe left) goes where Done would: the next exercise still to do after
    // this one, wrapping to skipped ones at the end. Previous (swipe right) steps back one place.
    val upcoming = Groups.nextIndex(run, size)
    val nextIndex = upcoming
    val prevIndex = (run.index - 1).takeIf { it >= 0 }
    val currentCleared = run.index in run.done

    fun go(index: Int?) {
        if (index == null || index == run.index) return
        if (unsavedSets > 0) confirmJump = index else controller.jumpTo(index)
    }

    val scope = rememberCoroutineScope()
    val swipeThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    val dragX = remember { Animatable(0f) }
    val orange = MaterialTheme.colorScheme.primary
    // Width of the swipeable row: how far the text travels to leave the bar completely.
    var rowWidth by remember { mutableStateOf(0f) }
    val sliding = remember { mutableStateOf(false) }

    // Each swipe or ⏮/⏭ press queues one step (-1 = next, slides left; +1 = previous, slides right).
    // Steps play one after another, so a fast swipe never cuts a slide off halfway or gets lost.
    val steps = remember { Channel<Int>(Channel.UNLIMITED) }
    val unsavedState = rememberUpdatedState(unsavedSets)
    LaunchedEffect(steps) {
        for (dir in steps) {
            val r = controller.run ?: continue
            val g = controller.activeGroup ?: continue
            val target = if (dir < 0) Groups.nextIndex(r, g.items.size) else (r.index - 1).takeIf { it >= 0 }
            if (target == null || target !in g.items.indices) {
                dragX.animateTo(0f, spring())
                continue
            }
            if (unsavedState.value > 0 || rowWidth <= 0f) {
                // Unsaved sets: ask first (see confirmJump), no slide.
                confirmJump = target
                dragX.animateTo(0f, spring())
                continue
            }
            sliding.value = true
            try {
                dragX.animateTo(dir * rowWidth, tween(140))
                controller.jumpTo(target)
                dragX.snapTo(-dir * rowWidth)
                dragX.animateTo(0f, tween(200))
            } finally {
                withContext(NonCancellable) {
                    if (controller.run?.index != target) controller.jumpTo(target)
                    dragX.snapTo(0f)
                }
                sliding.value = false
            }
        }
    }

    /** Like a music player changing track: the text slides fully out and the next one in. */
    fun slideTo(index: Int?, dir: Int) {
        if (index == null && !sliding.value) {
            scope.launch { dragX.animateTo(0f, spring()) }
            return
        }
        steps.trySend(dir)
    }

    val slideState = rememberUpdatedState(::slideTo)
    val nextState = rememberUpdatedState(nextIndex)
    val prevState = rememberUpdatedState(prevIndex)

    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, orange.copy(alpha = 0.5f)),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = if (expanded) "Collapse" else "Expand") { expanded = !expanded }
                    .pointerInput(Unit) {
                        // Raw finger travel decides the swipe; dragX only draws it.
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = {
                                when {
                                    // While a slide plays, the swipe is still counted and queued.
                                    total <= -swipeThreshold -> slideState.value(if (sliding.value) 0 else nextState.value, -1)
                                    total >= swipeThreshold -> slideState.value(if (sliding.value) 0 else prevState.value, +1)
                                    !sliding.value -> scope.launch { dragX.animateTo(0f, spring()) }
                                }
                                total = 0f
                            },
                            onDragCancel = {
                                total = 0f
                                if (!sliding.value) scope.launch { dragX.animateTo(0f, spring()) }
                            },
                        ) { change, amount ->
                            change.consume()
                            total += amount
                            // The finger only moves the text when no slide is playing.
                            if (sliding.value) return@detectHorizontalDragGestures
                            // Rubber band when there is nothing that way.
                            val blocked = (total < 0 && nextState.value == null) || (total > 0 && prevState.value == null)
                            val shown = if (blocked) total * 0.25f else total
                            scope.launch { dragX.snapTo(shown) }
                        }
                    }
                    .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .onSizeChanged { rowWidth = it.width.toFloat() }
                        .graphicsLayer {
                            translationX = dragX.value
                            alpha = 1f - (abs(dragX.value) / rowWidth.coerceAtLeast(1f)).coerceIn(0f, 0.85f)
                        },
                ) {
                    // "Cover": the exercise icon on an orange tile.
                    Box(
                        Modifier.size(44.dp).clip(MaterialTheme.shapes.small).background(orange),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            ContextIcons.forKey(current.iconKey),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        // key: restart the marquee from the start when the exercise changes.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentCleared) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "done",
                                    tint = ClearedGreen,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            key(run.index) {
                                ScrollingName(
                                    current.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    // A cleared exercise reads grayed out.
                                    color = if (currentCleared) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else Color.Unspecified,
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ScrollingName(
                                group.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Text(
                                "  ·  ${run.index + 1}/$size",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
                if (expanded) {
                    IconButton(onClick = controller::stop) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop group")
                    }
                } else {
                    IconButton(onClick = { slideTo(nextIndex, -1) }, enabled = nextIndex != null) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next exercise", modifier = Modifier.size(30.dp))
                    }
                }
            }

            if (expanded) {
                // Transport row: Previous · what comes next · Next.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                ) {
                    FilledTonalIconButton(onClick = { slideTo(prevIndex, +1) }, enabled = prevIndex != null) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous exercise")
                    }
                    Text(
                        upcoming?.let { "Next: ${group.items[it].name}" } ?: "Last one",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    FilledIconButton(onClick = { slideTo(nextIndex, -1) }, enabled = nextIndex != null) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next exercise")
                    }
                }
                // Keeps the chip of the exercise up now in view.
                val chipsState = rememberLazyListState(initialFirstVisibleItemIndex = (run.index - 1).coerceAtLeast(0))
                LaunchedEffect(run.index) { chipsState.animateScrollToItem((run.index - 1).coerceAtLeast(0)) }
                LazyRow(
                    state = chipsState,
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(group.items) { index, item ->
                        val isCurrent = index == run.index
                        val done = index in run.done
                        FilterChip(
                            selected = isCurrent,
                            onClick = { go(index) },
                            label = {
                                Text(
                                    item.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = NameMaxWidth),
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (done && !isCurrent) Icons.Filled.Check else ContextIcons.forKey(item.iconKey),
                                    contentDescription = if (done) "done" else null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                // Cleared exercises: green tick, grayed-out name.
                                labelColor = if (done) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                iconColor = if (done) ClearedGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                // The exercise up now wears the action orange.
                                selectedContainerColor = orange,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
            }

            // Progress along the group, like a track's progress line.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(run.done.size.toFloat() / size)
                        .height(3.dp)
                        .background(orange),
                )
            }
        }
    }

    confirmJump?.let { index ->
        AlertDialog(
            onDismissRequest = { confirmJump = null },
            title = { Text("Switch to ${group.items[index].name}?", maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = { Text("Your ${setsWord(unsavedSets)} not saved yet will be cleared. Press Done first to keep them.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmJump = null
                    controller.jumpTo(index)
                }) { Text("Switch") }
            },
            dismissButton = { TextButton(onClick = { confirmJump = null }) { Text("Cancel") } },
        )
    }
}
