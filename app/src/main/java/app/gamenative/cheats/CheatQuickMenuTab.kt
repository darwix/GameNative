package app.gamenative.cheats

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun CheatQuickMenuTab(
    cheats: List<CheatDefinition>,
    lockedIds: Set<String>,
    onAction: (CheatDefinition, CheatEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val grouped = cheats.groupBy { it.section }
        grouped.forEach { (section, sectionCheats) ->
            if (section.isNotEmpty()) {
                Text(
                    text = section,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
                HorizontalDivider(thickness = 0.5.dp)
            }
            sectionCheats.forEach { cheat ->
                when (val type = (cheat.action as CheatAction.InputCommand).type) {
                    is InputCommandType.Toggle -> ToggleRow(
                        label = cheat.label,
                        locked = cheat.id in lockedIds,
                        onToggle = { enabled -> onAction(cheat, CheatEvent.Toggle(enabled)) },
                    )
                    is InputCommandType.FreeText -> FreeTextRow(
                        label = cheat.label,
                        hint = type.hint,
                        onExecute = { value -> onAction(cheat, CheatEvent.Execute(value)) },
                    )
                    is InputCommandType.Dropdown -> DropdownRow(
                        label = cheat.label,
                        options = type.options,
                        onExecute = { value -> onAction(cheat, CheatEvent.Execute(value)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, locked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            if (locked) PulsingDot()
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = locked, onCheckedChange = onToggle)
    }
}

@Composable
private fun FreeTextRow(label: String, hint: String, onExecute: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(hint, style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) { onExecute(text); text = "" } }),
            modifier = Modifier.width(120.dp),
            textStyle = MaterialTheme.typography.bodySmall,
        )
        Button(
            onClick = { if (text.isNotBlank()) { onExecute(text); text = "" } },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text("Go", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownRow(label: String, options: List<Pair<String, String>>, onExecute: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(options.firstOrNull()) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selected?.first ?: "",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().width(130.dp),
                textStyle = MaterialTheme.typography.bodySmall,
                singleLine = true,
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.first, style = MaterialTheme.typography.bodySmall) },
                        onClick = { selected = option; expanded = false },
                    )
                }
            }
        }
        Button(
            onClick = { selected?.second?.let { onExecute(it) } },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text("Go", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PulsingDot() {
    val alpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "dot_alpha",
    )
    Icon(
        imageVector = Icons.Default.Circle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
        modifier = Modifier.size(8.dp),
    )
}
