package com.yashd.assignment2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.yashd.assignment2.ui.theme.Assignment2Theme
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent { Assignment2Theme { TasksApp() } }
    }
}

internal fun dueLabel(task: PersonalTask): String = task.dueAt?.let {
    (if (task.hasTime) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    else DateFormat.getDateInstance(DateFormat.MEDIUM)).format(Date(it))
}.orEmpty()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksApp() {
    val context = LocalContext.current
    val store = remember { TaskStore(context.applicationContext) }
    DisposableEffect(store) { store.start(); onDispose { store.stop() } }
    var selected by remember { mutableStateOf("my_tasks") }
    var editor by remember { mutableStateOf<PersonalTask?>(null) }
    var listEditor by remember { mutableStateOf(false) }
    var renameList by remember { mutableStateOf<TaskList?>(null) }
    var sort by remember { mutableStateOf("Recently created") }
    var sortMenu by remember { mutableStateOf(false) }
    var listMenu by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val visibleLists = store.lists
    val currentList = store.lists.find { it.id == selected }
    LaunchedEffect(visibleLists, store.loading) {
        if (!store.loading && selected != "priority" && visibleLists.none { it.id == selected }) selected =
            "my_tasks"
    }
    val filtered =
        store.tasks.filter { if (selected == "priority") it.priority > 0 else it.listId == selected }
    val bySort = when (sort) {
        "Due date" -> filtered.sortedBy { it.dueAt ?: Long.MAX_VALUE }
        "Recently created" -> filtered.sortedByDescending { it.createdAt }
        "Alphabetical" -> filtered.sortedBy { it.title.lowercase() }
        else -> filtered.sortedByDescending { it.createdAt }
    }
    val sorted = if (selected == "priority") bySort.sortedByDescending { it.priority } else bySort
    val active = sorted.filter { !it.completed }
    val completed = sorted.filter { it.completed }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = {
                Text(
                    "Tasks",
                    style = MaterialTheme.typography.titleLarge
                )
            })
        },
        floatingActionButton = {
            if (editor == null) FloatingActionButton(onClick = {
                editor = PersonalTask(
                    listId = currentList?.id ?: "my_tasks"
                )
            }) { Icon(Icons.Default.Add, "Add task") }
        }
    ) { insets ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(insets)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selected == "priority",
                    onClick = { selected = "priority" },
                    label = { Text("Priority") },
                    leadingIcon = { Icon(Icons.Default.Star, null, Modifier.size(18.dp)) })
                Spacer(Modifier.width(6.dp))
                visibleLists.forEach { list ->
                    FilterChip(
                        selected = selected == list.id,
                        onClick = { selected = list.id },
                        label = { Text(list.name) })
                    Spacer(Modifier.width(6.dp))
                }
                TextButton(onClick = { renameList = null; listEditor = true }) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Text("New list")
                }
            }
            if (!store.connected) Text(
                "Offline · changes will sync when connected",
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            store.error?.let { message ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            message,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(onClick = { store.error = null }) {
                            Icon(
                                Icons.Default.Close,
                                "Dismiss error"
                            )
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (selected == "priority") "Priority" else currentList?.name ?: "My Tasks",
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { sortMenu = true }) {
                        Icon(
                            Icons.Default.Sort,
                            "Sort tasks"
                        )
                    }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        listOf(
                            "Due date",
                            "Recently created",
                            "Alphabetical"
                        ).forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { sort = option; sortMenu = false },
                                trailingIcon = {
                                    if (sort == option) Icon(
                                        Icons.Default.Check,
                                        null
                                    )
                                })
                        }
                    }
                }
                Box {
                    IconButton(onClick = { listMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            "List actions"
                        )
                    }
                    DropdownMenu(expanded = listMenu, onDismissRequest = { listMenu = false }) {
                        if (currentList == null) DropdownMenuItem(
                            text = { Text("New list") },
                            onClick = { listMenu = false; renameList = null; listEditor = true })
                        if (currentList != null) {
                            if (!currentList.isDefault) {
                                DropdownMenuItem(
                                    text = { Text("Rename list") },
                                    onClick = {
                                        listMenu = false; renameList = currentList; listEditor =
                                        true
                                    })
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Clear completed tasks") },
                                enabled = completed.isNotEmpty(),
                                onClick = {
                                    listMenu = false
                                    confirm =
                                        "Delete all completed tasks in ${currentList.name}?" to {
                                            store.clearCompleted(currentList.id)
                                        }
                                })
                            if (!currentList.isDefault) DropdownMenuItem(text = {
                                Text(
                                    "Delete list",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }, onClick = {
                                listMenu = false
                                confirm = "Delete ${currentList.name} and all its tasks?" to {
                                    store.deleteList(currentList)
                                }
                            })
                        }
                    }
                }
            }
            if (store.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 104.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!store.loading && active.isEmpty()) item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                    ) {
                        Text(
                            if (selected == "priority") "No priority tasks" else "No active tasks",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            if (selected == "priority") "Star a task in any list to mark it as priority." else "Add a task to get started.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(active, key = { it.id }) { task ->
                    TaskRow(
                        task,
                        store.lists.find { it.id == task.listId }?.name.takeIf { selected == "priority" },
                        { store.complete(task) },
                        { level -> store.priority(task, level) },
                        { editor = task },
                        { confirm = "Delete “${task.title}”?" to { store.deleteTask(task) } })
                }
                if (completed.isNotEmpty()) {
                    item {
                        TextButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null
                            )
                            Text(
                                "Completed (${completed.size})",
                                Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            )
                        }
                    }
                    if (expanded) items(completed, key = { it.id }) { task ->
                        TaskRow(
                            task,
                            null,
                            { store.complete(task) },
                            { level -> store.priority(task, level) },
                            { editor = task },
                            { confirm = "Delete “${task.title}”?" to { store.deleteTask(task) } })
                    }
                }
            }
        }
    }
    editor?.let { task ->
        TaskComposer(
            task,
            visibleLists,
            onDismiss = { editor = null },
            onSave = { draft, done -> store.saveTask(draft, done) })
    }
    if (listEditor) ListNameDialog(
        renameList,
        onDismiss = { listEditor = false },
        onSave = { name, done ->
            store.saveList(renameList, name, done)
        })
    confirm?.let { (message, action) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Confirm deletion") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { action(); confirm = null }) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } })
    }
}

@Composable
private fun TaskRow(
    task: PersonalTask, listName: String?, complete: () -> Unit, priority: (Int) -> Unit,
    edit: () -> Unit, delete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.completed, onCheckedChange = { complete() })
            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClick = edit)
                    .padding(horizontal = 4.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    task.title, style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (task.description.isNotBlank()) Text(
                    task.description, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val metadata = listOfNotNull(
                    listName, dueLabel(task).takeIf { it.isNotBlank() }).joinToString(" · ")
                if (metadata.isNotEmpty()) Text(
                    metadata,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PriorityButton(level = task.priority, onSelect = priority)
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        "Task actions"
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit / move") },
                        onClick = { menu = false; edit() })
                    HorizontalDivider()
                    DropdownMenuItem(text = {
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    }, onClick = { menu = false; delete() })
                }
            }
        }
    }
}

@Composable
private fun ListNameDialog(
    list: TaskList?,
    onDismiss: () -> Unit,
    onSave: (String, (String?) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf(list?.name.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (list == null) "New list" else "Rename list") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 100) name = it },
                    singleLine = true,
                    label = { Text("List name") },
                    enabled = !saving
                )
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (saving) Text(
                    "Saving · waiting for Firebase",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && !saving, onClick = {
                saving = true; error = null
                onSave(name.trim()) { message ->
                    saving = false; error = message; if (message == null) onDismiss()
                }
            }) { Text(if (saving) "Saving…" else if (list == null) "Create" else "Save") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } })
}
