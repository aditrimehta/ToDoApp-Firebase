
package com.c059assignment.todoappfirebase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import com.google.firebase.database.*

private val Blue = Color(0xFF315B8A)
private val Background = Color(0xFFF7F8FA)
private val Border = Color(0xFFE4E7EC)
private val TextDark = Color(0xFF202939)
private val TextMuted = Color(0xFF667085)

data class Task(
    val title: String = "",
    val priority: String = "Medium"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TodoApp()
            }
        }
    }
}

@Composable
fun TodoApp() {
    val database = remember {
        FirebaseDatabase.getInstance(
            "https://todoappmadproj-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference.child("tasks")
    }

    var tasks by remember { mutableStateOf(listOf<Pair<String, Task>>()) }
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var editing by remember { mutableStateOf<Pair<String, Task>?>(null) }
    var message by remember { mutableStateOf("") }

    DisposableEffect(database) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tasks = snapshot.children.mapNotNull { child ->
                    child.key?.let { key ->
                        child.getValue(Task::class.java)?.let { key to it }
                    }
                }.sortedBy {
                    when (it.second.priority) {
                        "High" -> 0
                        "Medium" -> 1
                        else -> 2
                    }
                }
                message = ""
            }

            override fun onCancelled(error: DatabaseError) {
                message = "Read failed: ${error.message}"
            }
        }

        database.addValueEventListener(listener)
        onDispose { database.removeEventListener(listener) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        // HEADER
        Text(
            "Aditri's Todo App",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(Modifier.height(5.dp))
        Text(
            "Organize your tasks and priorities.",
            fontSize = 14.sp,
            color = TextMuted
        )

        Spacer(Modifier.height(24.dp))


        // ADD TASK
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Add a task",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Task description") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue,
                        unfocusedBorderColor = Border
                    )
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "Priority",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf("High", "Medium", "Low").forEach { p ->
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = priority == p,
                                onClick = { priority = p },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Blue
                                )
                            )
                            Text(p, fontSize = 13.sp, color = TextDark)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            database.push()
                                .setValue(Task(title.trim(), priority))
                                .addOnSuccessListener {
                                    title = ""
                                    priority = "Medium"
                                    message = "Task added successfully."
                                }
                                .addOnFailureListener {
                                    message = "Add failed: ${it.message}"
                                }
                        } else {
                            message = "Please enter a task description."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue
                    )
                ) {
                    Text("Add task", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (message.isNotEmpty()) {
            Text(
                message,
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 12.sp,
                color = if (
                    message.contains("failed", ignoreCase = true) ||
                    message.contains("permission", ignoreCase = true)
                ) Color(0xFFB42318) else Color(0xFF26734D)
            )
        }

        Spacer(Modifier.height(24.dp))

        // TASK LIST HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Your tasks",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${tasks.size} tasks",
                fontSize = 13.sp,
                color = TextMuted
            )
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (tasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No tasks yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Add a task above to get started.",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            items(tasks, key = { it.first }) { item ->
                val (id, task) = item

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            database.child(id).removeValue()
                                .addOnFailureListener {
                                    message = "Delete failed: ${it.message}"
                                }
                            true
                        } else {
                            false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = true,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Color(0xFFFEE4E2),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(end = 16.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                "Delete",
                                color = Color(0xFFB42318),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    task.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDark
                                )
                                Spacer(Modifier.height(8.dp))
                                PriorityBadge(task.priority)
                            }

                            TextButton(onClick = { editing = item }) {
                                Text("Edit", color = Blue)
                            }
                        }
                    }
                }
            }
        }
    }

    // EDIT TASK
    editing?.let { item ->
        var editTitle by remember(item.first) {
            mutableStateOf(item.second.title)
        }
        var editPriority by remember(item.first) {
            mutableStateOf(item.second.priority)
        }

        AlertDialog(
            onDismissRequest = { editing = null },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("Edit task", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Task description") },
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    Text("Priority", fontWeight = FontWeight.Medium)

                    listOf("High", "Medium", "Low").forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = editPriority == p,
                                onClick = { editPriority = p },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Blue
                                )
                            )
                            Text(p)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            database.child(item.first).updateChildren(
                                mapOf(
                                    "title" to editTitle.trim(),
                                    "priority" to editPriority
                                )
                            ).addOnFailureListener {
                                message = "Update failed: ${it.message}"
                            }
                            editing = null
                        }
                    }
                ) {
                    Text("Save", color = Blue)
                }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
fun SummaryCard(
    label: String,
    count: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                label,
                fontSize = 12.sp,
                color = TextMuted
            )
            Spacer(Modifier.height(6.dp))
            Text(
                count,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val color = when (priority) {
        "High" -> Color(0xFFB42318)
        "Medium" -> Color(0xFFB54708)
        else -> Color(0xFF027A48)
    }

    val background = when (priority) {
        "High" -> Color(0xFFFEE4E2)
        "Medium" -> Color(0xFFFEF0C7)
        else -> Color(0xFFD1FADF)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = background
    ) {
        Text(
            priority,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TodoPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(20.dp)
        ) {
            Text(
                "Aditri's Todo App",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                "Organize your tasks and priorities.",
                color = TextMuted
            )
            Spacer(Modifier.height(24.dp))
            SummaryCard("Total tasks", "3")
            Spacer(Modifier.height(12.dp))
            Text("Your tasks", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("Finish assignment  •  High")
            Text("Read chapter  •  Medium")
            Text("Organize notes  •  Low")
        }
    }
}
