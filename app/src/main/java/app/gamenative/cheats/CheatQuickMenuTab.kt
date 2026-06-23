package app.gamenative.cheats

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun CheatQuickMenuTab(
    cheats: List<CheatDefinition>,
    lockedIds: Set<String>,
    onToggle: (CheatDefinition, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        cheats.forEach { cheat ->
            CheatRow(
                cheat = cheat,
                locked = cheat.id in lockedIds,
                onToggle = { enabled -> onToggle(cheat, enabled) },
            )
        }
    }
}

@Composable
private fun CheatRow(
    cheat: CheatDefinition,
    locked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            if (locked) PulsingDot()
            Text(
                text = stringResource(cheat.labelResId),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Switch(checked = locked, onCheckedChange = onToggle)
    }
}

@Composable
private fun PulsingDot() {
    val alpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot_alpha",
    )
    Icon(
        imageVector = Icons.Default.Circle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
        modifier = Modifier.size(8.dp),
    )
}
