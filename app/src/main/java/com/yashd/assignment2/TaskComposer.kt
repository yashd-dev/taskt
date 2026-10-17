package com.yashd.assignment2

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskComposer(
    initial: PersonalTask, lists: List<TaskList>, onDismiss: () -> Unit,
    onSave: (PersonalTask, (String?) -> Unit) -> Unit
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var discard by remember { mutableStateOf(false) }
    var listMenu by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }

    fun dismiss() {
        if (!saving) {
            if (draft != initial) discard = true else onDismiss()
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { value -> value != SheetValue.Hidden || (!saving && draft == initial) })
    ModalBottomSheet(
        onDismissRequest = { dismiss() }, sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (initial.id.isEmpty()) "New task" else "Edit task",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = draft.title,
                onValueChange = { if (it.length <= 300) draft = draft.copy(title = it) },
                label = { Text("Task title") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                singleLine = true
            )
            OutlinedTextField(
                value = draft.description,
                onValueChange = { if (it.length <= 5000) draft = draft.copy(description = it) },
                label = { Text("Details (optional)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                maxLines = 4
            )
            PriorityButton(
                level = draft.priority, enabled = !saving, showLabel = true,
                onSelect = { draft = draft.copy(priority = it) })
            Box {
                TextButton(enabled = !saving, onClick = { listMenu = true }) {
                    Icon(
                        Icons.Default.List,
                        null,
                        Modifier.size(20.dp)
                    ); Spacer(Modifier.width(8.dp))
                    Text(lists.find { it.id == draft.listId }?.name ?: "Choose list")
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(expanded = listMenu, onDismissRequest = { listMenu = false }) {
                    lists.forEach { list ->
                        DropdownMenuItem(
                            text = { Text(list.name) },
                            onClick = { draft = draft.copy(listId = list.id); listMenu = false })
                    }
                }
            }
            TextButton(enabled = !saving, onClick = { picker = true }) {
                Icon(
                    Icons.Default.Schedule,
                    null,
                    Modifier.size(20.dp)
                ); Spacer(Modifier.width(8.dp))
                Text(if (draft.dueAt == null) "Due date & time · Not set" else dueLabel(draft))
            }
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (saving) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    "Waiting for Firebase to confirm the save…",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(enabled = !saving, onClick = { dismiss() }) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = !saving && draft.title.isNotBlank() && lists.any { it.id == draft.listId },
                    onClick = {
                        saving = true; error = null
                        onSave(draft.copy(title = draft.title.trim())) { message ->
                            saving = false; error = message
                            if (message == null) onDismiss()
                        }
                    }) { Text(if (saving) "Saving…" else "Save") }
            }
        }
    }
    if (discard) AlertDialog(
        onDismissRequest = { discard = false },
        title = { Text("Discard changes?") },
        text = { Text("Your unsaved changes will be lost.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
    if (picker) SchedulePicker(draft, onDismiss = { picker = false }, onDone = { dueAt, hasTime ->
        draft = draft.copy(dueAt = dueAt, hasTime = hasTime, dueTimezone = TimeZone.getDefault().id)
        picker = false
    })
}

@Composable
private fun SchedulePicker(
    task: PersonalTask,
    onDismiss: () -> Unit,
    onDone: (Long?, Boolean) -> Unit
) {
    val context = LocalContext.current
    var selected by remember {
        mutableStateOf(Calendar.getInstance().apply {
            timeInMillis = task.dueAt ?: System.currentTimeMillis()
            if (task.dueAt == null) {
                set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0)
            }
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        })
    }
    var month by remember {
        mutableStateOf((selected.clone() as Calendar).apply {
            set(
                Calendar.DAY_OF_MONTH,
                1
            )
        })
    }
    var hasTime by remember { mutableStateOf(task.hasTime) }
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Due date & time", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        month = (month.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                    }) {
                        Icon(Icons.Default.ChevronLeft, "Previous month")
                    }
                    Text(
                        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.time),
                        Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = {
                        month = (month.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                    }) {
                        Icon(Icons.Default.ChevronRight, "Next month")
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                        Text(
                            it,
                            Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                val offset = (month.get(Calendar.DAY_OF_WEEK) + 5) % 7
                val days = month.getActualMaximum(Calendar.DAY_OF_MONTH)
                repeat((offset + days + 6) / 7) { week ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { day ->
                            val number = week * 7 + day - offset + 1
                            val valid = number in 1..days
                            val chosen =
                                valid && month.get(Calendar.YEAR) == selected.get(Calendar.YEAR) &&
                                        month.get(Calendar.MONTH) == selected.get(Calendar.MONTH) && selected.get(
                                    Calendar.DAY_OF_MONTH
                                ) == number
                            Box(
                                Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(CircleShape)
                                    .background(if (chosen) colors.primary else colors.surfaceContainerHigh)
                                    .clickable(enabled = valid) {
                                        selected = (selected.clone() as Calendar).apply {
                                            set(Calendar.DAY_OF_MONTH, 1)
                                            set(Calendar.YEAR, month.get(Calendar.YEAR)); set(
                                            Calendar.MONTH,
                                            month.get(Calendar.MONTH)
                                        )
                                            set(Calendar.DAY_OF_MONTH, number)
                                        }
                                    }, contentAlignment = Alignment.Center
                            ) {
                                if (valid) Text(
                                    number.toString(), style = MaterialTheme.typography.bodySmall,
                                    color = if (chosen) colors.onPrimary else colors.onSurface
                                )
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        TimePickerDialog(
                            context,
                            android.app.AlertDialog.THEME_DEVICE_DEFAULT_DARK,
                            { _, hour, minute ->
                                selected = (selected.clone() as Calendar).apply {
                                    set(
                                        Calendar.HOUR_OF_DAY,
                                        hour
                                    ); set(Calendar.MINUTE, minute)
                                }
                                hasTime = true
                            },
                            selected.get(Calendar.HOUR_OF_DAY),
                            selected.get(Calendar.MINUTE),
                            android.text.format.DateFormat.is24HourFormat(context)
                        ).show()
                    }) {
                        Icon(
                            Icons.Default.Schedule,
                            null,
                            Modifier.size(18.dp)
                        ); Spacer(Modifier.width(8.dp))
                        Text(
                            if (hasTime) SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                                selected.time
                            ) else "Set time"
                        )
                    }
                    if (hasTime) IconButton(onClick = {
                        hasTime = false
                    }) { Icon(Icons.Default.Close, "Remove time") }
                }
                TextButton(onClick = { onDone(null, false) }) { Text("Clear due date") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val date = (selected.clone() as Calendar).apply {
                    if (!hasTime) {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    }
                }
                onDone(date.timeInMillis, hasTime)
            }) { Text("Done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
