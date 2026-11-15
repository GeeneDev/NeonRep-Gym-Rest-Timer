@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.geenedev.gymreps.ui

import androidx.compose.ui.res.stringResource
import com.geenedev.gymreps.R
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import com.geenedev.gymreps.data.GroupStore
import com.geenedev.gymreps.model.GroupAdvance
import com.geenedev.gymreps.model.HistoryEntry
import com.geenedev.gymreps.model.SavedContext
import com.geenedev.gymreps.model.SessionState
import com.geenedev.gymreps.model.Workout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WorkoutScreen(
    vm: WorkoutViewModel,
    onRestDone: () -> Unit,
    modifier: Modifier = Modifier,
    /** Shown next to the exercise field; MainActivity supplies the Android camera button. */
    photoButton: @Composable (exercise: String) -> Unit = {},
    /** Machine photo under the icon on a history card; MainActivity supplies it (nothing when none). */
    historyPhoto: @Composable (entry: HistoryEntry) -> Unit = {},
    /** An edited history entry changed its exercise name; MainActivity carries the photo over. */
    onEntryRenamed: (from: String, to: String) -> Unit = { _, _ -> },
    /** Photo row in the history Edit card; MainActivity supplies it. */
    editPhoto: @Composable (exercise: String, edit: PhotoEdit) -> Unit = { _, _ -> },
    /** Save in the Edit card: apply its photo choice to the entry's (new) exercise name. */
    onPhotoEdited: (exercise: String, edit: PhotoEdit) -> Unit = { _, _ -> },
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val res = LocalContext.current.resources
    var showIconPicker by remember { mutableStateOf(false) }
    var showRestEditor by remember { mutableStateOf(false) }
    // History entry open in the long-press Edit card.
    var editing by remember { mutableStateOf<HistoryEntry?>(null) }
    val celebration = rememberCelebrationState()
    val haptics = LocalHapticFeedback.current
    // Exercise groups: Done saves as usual, then loads the group's next exercise (idle until tapped).
    val appContext = LocalContext.current.applicationContext
    val groups = remember(vm) { GroupsController(GroupStore(appContext), vm) }
    var showGroups by remember { mutableStateOf(false) }
    // Saveable so Info stays open when picking a language restarts the screen.
    var showInfo by rememberSaveable { mutableStateOf(false) }

    val session = vm.session
    val ticking = session.isRunning
    val now by produceState(vm.clock(), ticking) {
        value = vm.clock()
        while (ticking) {
            delay(100)
            value = vm.clock()
        }
    }

    val restEndsAt = session.restEndsAt
    LaunchedEffect(restEndsAt) {
        if (restEndsAt != null) {
            delay((restEndsAt - vm.clock()).coerceAtLeast(0))
            vm.onEndRest(endedAt = restEndsAt)
            onRestDone()
        }
    }

    Box(modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    // Easter egg: tap the app name 3 times in a row to celebrate.
                    Text(
                        "Gym Reps",
                        modifier = Modifier.pointerInput(Unit) {
                            var taps = 0
                            var lastTap = 0L
                            detectTapGestures {
                                val t = System.currentTimeMillis()
                                taps = if (t - lastTap < 600) taps + 1 else 1
                                lastTap = t
                                if (taps >= 3) {
                                    taps = 0
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    celebration.burst(res.getString(R.string.congratulations))
                                }
                            }
                        },
                    )
                },
                actions = {
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.info))
                    }
                    IconButton(onClick = { showGroups = true }) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = stringResource(R.string.groups))
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                // Swipe the message sideways to dismiss it early.
                key(data) {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) data.dismiss()
                            true
                        },
                    )
                    SwipeToDismissBox(state = dismissState, backgroundContent = {}) {
                        Snackbar(data)
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            // Tapping empty space closes the keyboard.
            modifier = Modifier.pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                // Notes belong to an exercise: shown once there is one, hidden (and cleared,
                // see onContextTextChange) when the Exercise field is empty.
                val showNotes = vm.contextText.isNotBlank() || vm.notesText.isNotBlank()
                Column {
                    ContextPicker(
                        text = vm.contextText,
                        iconKey = vm.iconKey,
                        contexts = vm.contexts,
                        onTextChange = vm::onContextTextChange,
                        onPick = vm::onContextPicked,
                        onIconClick = { showIconPicker = true },
                        trailing = { photoButton(vm.contextText) },
                    )
                    AnimatedVisibility(
                        visible = showNotes,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            NotesField(text = vm.notesText, onTextChange = vm::onNotesChange)
                        }
                    }
                }
            }
            item {
                RestPresets(
                    presets = vm.restPresets,
                    selected = vm.selectedRest,
                    onSelect = vm::onSelectRest,
                    onEdit = { showRestEditor = true },
                )
            }
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CyberCore(
                        session = session,
                        now = now,
                        restTotalMs = vm.selectedRest * 1000L,
                        onClick = {
                            focusManager.clearFocus()
                            vm.onBigButton()
                        },
                    )
                }
            }
            item {
                SetActions(
                    session = session,
                    onMinusOne = vm::onMinusOne,
                    onSkip = { vm.onEndRest() },
                    onReset = vm::onReset,
                    onSave = {
                        val sets = session.sets
                        val name = vm.contextText.trim()
                        val groupName = groups.activeGroup?.name
                        val groupSize = groups.activeGroup?.items?.size ?: 0
                        val message = when (vm.onSave(groupName.orEmpty(), groupSize)) {
                            SaveResult.Saved -> {
                                // The name field empties; drop its focus so the saved-exercises
                                // list doesn't pop open (and no keyboard shows).
                                focusManager.clearFocus()
                                val saved = res.getString(R.string.saved_sets_of, res.setsLabel(sets), name)
                                when (groups.onSaved()) {
                                    is GroupAdvance.Next -> res.getString(R.string.saved_next, saved, vm.contextText)
                                    GroupAdvance.Finished -> res.getString(R.string.saved_group_finished, saved, groupName)
                                    null -> saved
                                }
                            }
                            SaveResult.NotSavedNoContext -> res.getString(R.string.type_exercise_then_done)
                            SaveResult.NothingToSave -> null
                        }
                        if (message != null) scope.launch { snackbar.showSnackbar(message) }
                    },
                )
            }
            // Series mini-player sits just above History; expanding it grows downward and
            // pushes the History list down.
            if (groups.activeGroup != null) {
                item(key = "group-run") { GroupRunStrip(groups, unsavedSets = session.sets) }
            }
            item {
                HistoryHeader(
                    count = vm.history.size,
                    totalMs = vm.history.sumOf { it.durationMs },
                    onClear = vm::onClearHistory,
                )
            }
            if (vm.history.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                // One lazy item per day: rows inside a day sit 4dp apart (the list's own
                // 16dp gap only separates days), under a small date label.
                val days = vm.history.groupBy { historyDayKey(it.finishedAt) }.toList()
                items(days, key = { (day, _) -> "day-$day" }) { (_, entries) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // 12dp above the label: the list's 16dp day gap pulled up by 4dp.
                        HistoryDayLabel(
                            entries,
                            Modifier
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints)
                                    val pull = 4.dp.roundToPx()
                                    layout(placeable.width, placeable.height - pull) { placeable.place(0, -pull) }
                                }
                                .padding(bottom = 2.dp),
                        )
                        entries.forEach { entry ->
                            key(entry.id) {
                                HistoryRow(
                                    entry,
                                    photo = { historyPhoto(entry) },
                                    onSelect = {
                                        vm.onHistoryEntryPicked(entry)
                                        scope.launch { listState.animateScrollToItem(0) }
                                    },
                                    onEdit = { editing = entry },
                                    onDelete = { vm.onDeleteEntry(entry.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Groups: a full-screen layer over the workout screen.
    if (showGroups) {
        GroupsScreen(
            controller = groups,
            // Saved exercises from history plus the ones already used in groups.
            savedExercises = (vm.contexts + groups.groups.flatMap { g -> g.items.map { SavedContext(it.name, it.iconKey) } })
                .distinctBy { it.name.lowercase() },
            unsavedSets = session.sets,
            onStarted = {
                showGroups = false
                focusManager.clearFocus()
                scope.launch { listState.animateScrollToItem(0) }
            },
            onDismiss = { showGroups = false },
        )
    }
    if (showInfo) InfoScreen(onDismiss = { showInfo = false })
    // Drawn over everything; it never takes touches, so the button can be spammed.
    CelebrationOverlay(celebration)
    }

    editing?.let { entry ->
        val photoEdit = remember(entry.id) { PhotoEdit() }
        HistoryEditDialog(
            entry = entry,
            photo = { editPhoto(entry.context, photoEdit) },
            onSave = {
                if (it.context != entry.context) onEntryRenamed(entry.context, it.context)
                onPhotoEdited(it.context, photoEdit)
                vm.onUpdateEntry(it)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
    if (showIconPicker) {
        IconPickerDialog(
            selected = vm.iconKey,
            onPick = {
                vm.onIconPicked(it)
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false },
        )
    }
    if (showRestEditor) {
        RestPresetsDialog(
            presets = vm.restPresets,
            onAdd = vm::onAddRestPreset,
            onRemove = vm::onRemoveRestPreset,
            onDismiss = { showRestEditor = false },
        )
    }
}

@Composable
private fun ContextPicker(
    text: String,
    iconKey: String,
    contexts: List<SavedContext>,
    onTextChange: (String) -> Unit,
    onPick: (SavedContext) -> Unit,
    onIconClick: () -> Unit,
    trailing: @Composable () -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    // Opened with the arrow button, without focusing the field (so no keyboard).
    var openedByArrow by remember { mutableStateOf(false) }
    // Closed by tapping outside; reopens on typing or refocusing.
    var dismissed by remember { mutableStateOf(false) }
    val suggestions = Workout.filterContexts(contexts, text)
    val menuOpen = (focused || openedByArrow) && !dismissed && suggestions.isNotEmpty()

    // The menu popup isn't focusable, so Android draws it over the keyboard. Keep it in the
    // space between the field's bottom and the keyboard top (at most ~5 rows; it scrolls).
    val density = LocalDensity.current
    val windowHeightPx = LocalView.current.rootView.height
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    // The menu's window area starts below the status bar; the field's position doesn't.
    val statusTopPx = WindowInsets.statusBars.getTop(density)
    var fieldBottomPx by remember { mutableStateOf(0f) }
    val menuMaxHeight = with(density) {
        // 48dp: the menu's own edge margin; below that it flips over the field instead.
        val spaceBelow = (windowHeightPx - imeBottomPx - statusTopPx - fieldBottomPx).toDp() - 56.dp
        spaceBelow.coerceIn(112.dp, 280.dp)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = onIconClick, modifier = Modifier.size(56.dp)) {
            Icon(ContextIcons.forKey(iconKey), contentDescription = stringResource(R.string.choose_icon))
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f).onGloballyPositioned { fieldBottomPx = it.boundsInWindow().bottom }) {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    onTextChange(it)
                    dismissed = false
                },
                modifier = Modifier.fillMaxWidth().onFocusChanged {
                    focused = it.isFocused
                    onFocusChange(it.isFocused)
                    if (it.isFocused) dismissed = false
                },
                label = { Text(stringResource(R.string.exercise)) },
                placeholder = { Text(stringResource(R.string.exercise_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        // Clearing leaves the keyboard as it was.
                        IconButton(onClick = { onTextChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear))
                        }
                    } else if (contexts.isNotEmpty()) {
                        IconButton(onClick = {
                            if (menuOpen) {
                                openedByArrow = false
                                dismissed = true
                            } else {
                                openedByArrow = true
                                dismissed = false
                            }
                        }) {
                            Icon(
                                if (menuOpen) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = stringResource(R.string.saved_exercises),
                            )
                        }
                    }
                },
            )
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = {
                    openedByArrow = false
                    dismissed = true
                },
                // Not focusable, so the menu never takes focus from the field or pops the keyboard.
                properties = PopupProperties(focusable = false),
                modifier = Modifier.heightIn(max = menuMaxHeight),
            ) {
                suggestions.forEach { context ->
                    DropdownMenuItem(
                        text = { Text(context.name) },
                        leadingIcon = { Icon(ContextIcons.forKey(context.iconKey), contentDescription = null) },
                        onClick = {
                            onPick(context)
                            openedByArrow = false
                            dismissed = true
                            focusManager.clearFocus()
                        },
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

/** One free-text box for the exercise: weights, next set's weight, machine height, anything. */
@Composable
private fun NotesField(text: String, onTextChange: (String) -> Unit) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.notes)) },
        placeholder = { Text(stringResource(R.string.notes_placeholder)) },
        maxLines = 4,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        trailingIcon = {
            if (text.isNotEmpty()) {
                IconButton(onClick = { onTextChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_notes))
                }
            }
        },
    )
}

@Composable
private fun RestPresets(
    presets: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onEdit: () -> Unit,
) {
    Column {
        Text(stringResource(R.string.rest), style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                presets.forEach { seconds ->
                    FilterChip(
                        selected = seconds == selected,
                        onClick = { onSelect(seconds) },
                        label = { Text(Workout.formatRest(seconds)) },
                    )
                }
            }
            // Pinned outside the scrolling chips so it is always visible.
            FilledTonalIconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_rest_times))
            }
        }
    }
}


@Composable
private fun SetActions(
    session: SessionState,
    onMinusOne: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val mod = Modifier.weight(1f)
        if (session.isResting) {
            ActionButton(Icons.Filled.SkipNext, stringResource(R.string.skip), onSkip, mod)
        } else {
            ActionButton(Icons.Filled.Remove, stringResource(R.string.one_set_less), onMinusOne, mod, enabled = session.sets > 0)
        }
        ActionButton(Icons.Filled.RestartAlt, stringResource(R.string.reset), onReset, mod, enabled = session.isRunning)
        ActionButton(Icons.Filled.Check, stringResource(R.string.done), onSave, mod, enabled = session.sets > 0, primary = true)
    }
}

/** Icon above a one-line label, so labels never wrap mid-word on narrow screens or big fonts. */
@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
) {
    val padding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    val content: @Composable RowScope.() -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null)
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    val sized = modifier.heightIn(min = 64.dp)
    if (primary) {
        Button(onClick, sized, enabled, contentPadding = padding, content = content)
    } else {
        FilledTonalButton(onClick, sized, enabled, contentPadding = padding, content = content)
    }
}

@Composable
private fun HistoryHeader(count: Int, totalMs: Long, onClear: () -> Unit) {
    val hasHistory = count > 0
    // Total hours with one decimal, e.g. "(12 - 3.4h)".
    val summary = stringResource(R.string.history_summary, count, totalMs / 3_600_000.0)
    var menuOpen by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.History, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.history), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(8.dp))
        Text(
            summary,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.history_options))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.clear_history)) },
                    leadingIcon = { Icon(Icons.Filled.DeleteSweep, contentDescription = null) },
                    enabled = hasHistory,
                    onClick = {
                        menuOpen = false
                        confirmClear = true
                    },
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_history_q)) },
            text = { Text(stringResource(R.string.clear_history_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onClear()
                    confirmClear = false
                }) { Text(stringResource(R.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    photo: @Composable () -> Unit,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Where the finger last touched the row, so the long-press menu opens right there.
    var pressPos by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val date = remember(entry.finishedAt) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(entry.finishedAt))
    }
    val time = remember(entry.finishedAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.finishedAt))
    }
    Box {
        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            pressPos = awaitFirstDown(requireUnconsumed = false).position
                        }
                    }
                    .combinedClickable(onClick = onSelect, onLongClick = { menuOpen = true })
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HistoryIcon(entry, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                // Line 1: name + time. Line 2: sets / duration / rest, then a 1-line notes preview.
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            entry.context,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            time,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HistoryStat(Icons.Filled.Repeat, pluralRes(R.plurals.sets_count, entry.sets))
                        HistoryStat(Icons.Filled.Timer, Workout.formatDuration(entry.durationMs))
                        if (entry.restSeconds > 0) {
                            HistoryStat(Icons.Filled.Hotel, Workout.formatRest(entry.restSeconds))
                        }
                        Text(
                            entry.notes.replace('\n', ' ').trim(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                // Machine photo thumbnail; collapses to nothing when there is none.
                Box(Modifier.padding(start = 8.dp).sizeIn(maxWidth = 40.dp, maxHeight = 40.dp)) {
                    photo()
                }
            }
        }
        // Zero-size anchor at the touch point: the menu opens just right of / below the finger
        // (and flips automatically when it would go off screen).
        Box(
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset(pressPos.x.toInt(), pressPos.y.toInt()) }
                .size(0.dp),
        ) {
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit)) },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    confirmDelete = true
                },
            )
        }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_entry_q)) },
            text = { Text(stringResource(R.string.delete_entry_body, entry.context, pluralRes(R.plurals.sets_count, entry.sets), date)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun HistoryStat(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 10.dp)) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(3.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

private fun historyDayKey(millis: Long): String =
    SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(millis))

/**
 * Two-row label above each day's history rows:
 *   "11/11/2026 Serie A (7/7)"
 *   "18:28 - 19:19 (51m)"
 * Date order and time style come from the device: the locale's numeric date pattern with a
 * 4-digit year, and the system 12/24-hour setting.
 */
@Composable
private fun HistoryDayLabel(entries: List<HistoryEntry>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val (title, times) = remember(entries, configuration) {
        val locale = configuration.locales[0]
        val datePattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "ddMMyyyy")
        val day = SimpleDateFormat(datePattern, locale).format(Date(entries.first().finishedAt))
        val series = Workout.daySeriesNames(entries).joinToString(", ")
        val count = Workout.dayProgress(entries)?.let { (done, planned) -> "$done/$planned" } ?: "${entries.size}"
        val head = if (series.isEmpty()) "$day ($count)" else "$day $series ($count)"
        val span = Workout.daySpan(entries)?.let { (start, end) ->
            val time = android.text.format.DateFormat.getTimeFormat(context)
            "${time.format(Date(start))} - ${time.format(Date(end))} (${Workout.formatSpan(end - start)})"
        }
        head to span
    }
    Column(modifier.padding(start = 2.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (times != null) {
            Text(
                times,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** The exercise icon in a tinted rounded square, the same size as a history photo. */
@Composable
fun HistoryIcon(entry: HistoryEntry, size: androidx.compose.ui.unit.Dp = 48.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            ContextIcons.forKey(entry.iconKey),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

@Composable
internal fun IconPickerDialog(selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_icon)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                ContextIcons.all.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (key, icon) ->
                            if (key == selected) {
                                FilledTonalIconButton(onClick = { onPick(key) }, modifier = Modifier.size(52.dp)) {
                                    Icon(icon, contentDescription = key)
                                }
                            } else {
                                IconButton(onClick = { onPick(key) }, modifier = Modifier.size(52.dp)) {
                                    Icon(icon, contentDescription = key)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun RestPresetsDialog(
    presets: List<Int>,
    onAdd: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val seconds = input.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rest_times)) },
        text = {
            Column {
                presets.forEach { preset ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Workout.formatRest(preset), modifier = Modifier.weight(1f))
                        IconButton(onClick = { onRemove(preset) }, enabled = presets.size > 1) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.remove_item, Workout.formatRest(preset)))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { new -> input = new.filter(Char::isDigit).take(4) },
                        label = { Text(stringResource(R.string.seconds)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            if (seconds != null) onAdd(seconds)
                            input = ""
                        },
                        enabled = seconds != null && seconds > 0,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_rest_time))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

