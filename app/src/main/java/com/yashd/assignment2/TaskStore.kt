package com.yashd.assignment2

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.database.*
import java.util.TimeZone
import java.util.UUID

data class TaskList(
    val id: String,
    val name: String,
    val isDefault: Boolean = false,
    val createdAt: Long = 0
)

data class PersonalTask(
    val id: String = "",
    val listId: String = "my_tasks",
    val title: String = "",
    val description: String = "",
    val completed: Boolean = false,
    val priority: Int = 0,
    val dueAt: Long? = null,
    val hasTime: Boolean = false,
    val dueTimezone: String = TimeZone.getDefault().id,
    val createdAt: Long = 0
)

private val database by lazy {
    FirebaseDatabase.getInstance("https://assignment2-be688-default-rtdb.firebaseio.com").apply {
        setPersistenceEnabled(true)
    }
}

class TaskStore(context: Context) {
    val deviceId: String =
        context.getSharedPreferences("device_identity", Context.MODE_PRIVATE).let { prefs ->
            prefs.getString("id", null) ?: UUID.randomUUID().toString().also {
                prefs.edit().putString("id", it).commit()
            }
        }
    private val root = database.getReference("devices").child(deviceId)
    private val subscriptions = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()
    var lists by mutableStateOf<List<TaskList>>(emptyList())
        private set
    var tasks by mutableStateOf<List<PersonalTask>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var connected by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
    private var listsLoaded = false
    private var tasksLoaded = false

    private fun listen(ref: DatabaseReference, receive: (DataSnapshot) -> Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) = receive(snapshot)
            override fun onCancelled(e: DatabaseError) {
                loading = false
                error = e.message
            }
        }
        subscriptions.add(ref to listener)
        ref.addValueEventListener(listener)
    }

    fun start() {
        listen(database.getReference(".info/connected")) {
            connected = it.getValue(Boolean::class.java) == true
        }
        listen(root.child("lists")) { snapshot ->
            lists = snapshot.children.map { child ->
                TaskList(
                    child.key!!, child.child("name").getValue(String::class.java).orEmpty(),
                    child.child("isDefault").getValue(Boolean::class.java) == true,
                    child.child("createdAt").getValue(Long::class.java) ?: 0
                )
            }.sortedWith(compareByDescending<TaskList> { it.isDefault }.thenBy { it.createdAt })
            listsLoaded = true
            loading = !(listsLoaded && tasksLoaded)
        }
        listen(root.child("tasks")) { snapshot ->
            tasks = snapshot.children.mapNotNull { child ->
                val title =
                    child.child("title").getValue(String::class.java) ?: return@mapNotNull null
                PersonalTask(
                    id = child.key!!,
                    listId = child.child("listId").getValue(String::class.java) ?: "my_tasks",
                    title = title,
                    description = child.child("description").getValue(String::class.java).orEmpty(),
                    completed = child.child("completed").getValue(Boolean::class.java) == true,
                    priority = readPriority(child.child("priority").value),
                    dueAt = child.child("dueAt").getValue(Long::class.java),
                    hasTime = child.child("hasTime").getValue(Boolean::class.java) == true,
                    dueTimezone = child.child("dueTimezone").getValue(String::class.java)
                        ?: TimeZone.getDefault().id,
                    createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0
                )
            }
            tasksLoaded = true
            loading = !(listsLoaded && tasksLoaded)
        }
        root.child("lists/my_tasks").runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                if (data.value == null) data.value = mapOf(
                    "name" to "My Tasks",
                    "isDefault" to true,
                    "createdAt" to ServerValue.TIMESTAMP,
                    "updatedAt" to ServerValue.TIMESTAMP
                )
                return Transaction.success(data)
            }

            override fun onComplete(
                e: DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?
            ) {
                if (e != null) error = e.message
            }
        })
    }

    fun stop() {
        subscriptions.forEach { (ref, listener) -> ref.removeEventListener(listener) }
        subscriptions.clear()
    }

    private fun write(values: Map<String, Any?>, done: (String?) -> Unit = {}) {
        root.updateChildren(values).addOnCompleteListener { result ->
            val message = if (result.isSuccessful) null else result.exception?.localizedMessage
                ?: "Write failed"
            if (message != null) error = message
            done(message)
        }
    }

    fun saveTask(task: PersonalTask, done: (String?) -> Unit) {
        val id = task.id.ifEmpty { root.child("tasks").push().key!! }
        val record = mapOf(
            "listId" to task.listId,
            "title" to task.title.trim(),
            "description" to task.description.trim(),
            "completed" to task.completed,
            "priority" to task.priority.coerceIn(0, 2),
            "dueAt" to task.dueAt,
            "hasTime" to task.hasTime,
            "dueTimezone" to task.dueTimezone,
            "createdAt" to if (task.createdAt == 0L) ServerValue.TIMESTAMP else task.createdAt,
            "updatedAt" to ServerValue.TIMESTAMP
        )
        write(record.mapKeys { "tasks/$id/${it.key}" }, done)
    }

    fun priority(task: PersonalTask, level: Int) = write(
        mapOf(
            "tasks/${task.id}/priority" to level.coerceIn(0, 2),
            "tasks/${task.id}/updatedAt" to ServerValue.TIMESTAMP
        )
    )

    fun complete(task: PersonalTask) {
        val changes = mutableMapOf<String, Any?>(
            "tasks/${task.id}/completed" to !task.completed,
            "tasks/${task.id}/completedAt" to if (task.completed) null else ServerValue.TIMESTAMP,
            "tasks/${task.id}/updatedAt" to ServerValue.TIMESTAMP
        )
        write(changes)
    }

    fun deleteTask(task: PersonalTask) = write(mapOf("tasks/${task.id}" to null))
    fun clearCompleted(listId: String) = write(tasks.filter { it.listId == listId && it.completed }
        .associate { "tasks/${it.id}" to null })

    fun saveList(list: TaskList?, name: String, done: (String?) -> Unit) {
        val id = list?.id ?: root.child("lists").push().key!!
        val fields = mutableMapOf<String, Any?>(
            "lists/$id/name" to name.trim(),
            "lists/$id/updatedAt" to ServerValue.TIMESTAMP
        )
        if (list == null) fields.putAll(
            mapOf(
                "lists/$id/isDefault" to false,
                "lists/$id/createdAt" to ServerValue.TIMESTAMP
            )
        )
        write(fields, done)
    }

    fun deleteList(list: TaskList) {
        if (list.isDefault) return
        val changes = tasks.filter { it.listId == list.id }.associate { "tasks/${it.id}" to null }
            .toMutableMap<String, Any?>()
        changes["lists/${list.id}"] = null
        write(changes)
    }

}
