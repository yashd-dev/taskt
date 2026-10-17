package com.yashd.assignment2

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

@Composable
internal fun PriorityButton(
    level: Int,
    onSelect: (Int) -> Unit,
    enabled: Boolean = true,
    showLabel: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val semantics = Modifier.semantics {
        contentDescription = "Change priority"
        stateDescription = priorityLabel(level)
    }
    Box {
        if (showLabel) TextButton(
            enabled = enabled,
            onClick = { expanded = true },
            modifier = semantics
        ) {
            PriorityStars(level)
            Spacer(Modifier.width(8.dp))
            Text("Priority · ${priorityLabel(level)}")
        } else IconButton(
            enabled = enabled,
            onClick = { expanded = true },
            modifier = semantics.size(48.dp)
        ) {
            PriorityStars(level)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (0..2).forEach { option ->
                DropdownMenuItem(
                    text = { Text(priorityLabel(option)) },
                    leadingIcon = { PriorityStars(option) },
                    trailingIcon = { if (option == level) Icon(Icons.Default.Check, "Selected") },
                    onClick = { expanded = false; onSelect(option) }
                )
            }
        }
    }
}

@Composable
private fun PriorityStars(level: Int) {
    Row {
        repeat(if (level == 2) 2 else 1) {
            Icon(
                if (level == 0) Icons.Default.StarBorder else Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(if (level == 2) 18.dp else 22.dp),
                tint = if (level == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            )
        }
    }
}
