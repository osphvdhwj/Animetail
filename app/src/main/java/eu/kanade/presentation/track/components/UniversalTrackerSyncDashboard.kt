package eu.kanade.presentation.track.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.domain.track.service.TrackSyncEngine.SyncProgressState
import eu.kanade.tachiyomi.data.track.Tracker

data class TrackerStatusItem(
    val tracker: Tracker,
    val isLoggedIn: Boolean,
    val syncStatus: SyncItemStatus,
    val lastSyncTime: String? = null,
)

enum class SyncItemStatus {
    IDLE,
    SYNCING,
    SYNCED,
    FAILED,
    NOT_LOGGED_IN,
}

@Composable
fun UniversalTrackerSyncDashboard(
    trackersState: List<TrackerStatusItem>,
    syncProgressState: SyncProgressState,
    onSyncAll: () -> Unit,
    onTrackerClick: (Tracker) -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Universal Multi-Sync Hub",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "MAL • AniList • Kitsu • Simkl & 11 more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Button(
                    onClick = onSyncAll,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync All",
                        modifier = if (syncProgressState is SyncProgressState.Syncing) {
                            Modifier.rotate(rotationAngle)
                        } else {
                            Modifier
                        },
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Sync Now")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sync State Banner
            AnimatedVisibility(
                visible = syncProgressState is SyncProgressState.Syncing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Syncing across platforms...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                }
            }

            // Tracker List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(trackersState, key = { it.tracker.id }) { item ->
                    TrackerStatusRow(
                        item = item,
                        rotationAngle = rotationAngle,
                        onClick = { onTrackerClick(item.tracker) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackerStatusRow(
    item: TrackerStatusItem,
    rotationAngle: Float,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(item.tracker.getLogoColor()))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(item.tracker.getLogo()),
                        contentDescription = item.tracker.name,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.tracker.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (item.isLoggedIn) "Logged In" else "Tap to authenticate",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.isLoggedIn) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    )
                }
            }

            // Status Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (item.syncStatus) {
                            SyncItemStatus.SYNCED -> Color(0xFF1B5E20).copy(alpha = 0.2f)
                            SyncItemStatus.SYNCING -> MaterialTheme.colorScheme.primaryContainer
                            SyncItemStatus.FAILED -> Color(0xFFB71C1C).copy(alpha = 0.2f)
                            SyncItemStatus.IDLE -> MaterialTheme.colorScheme.surfaceContainerHigh
                            SyncItemStatus.NOT_LOGGED_IN -> Color.Gray.copy(alpha = 0.1f)
                        },
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (item.syncStatus) {
                        SyncItemStatus.SYNCED -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Synced",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Synced", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                        SyncItemStatus.SYNCING -> {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Syncing",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .rotate(rotationAngle),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Syncing", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        SyncItemStatus.FAILED -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Failed",
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Failed", fontSize = 11.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        }
                        SyncItemStatus.IDLE -> {
                            Text(text = "Ready", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        SyncItemStatus.NOT_LOGGED_IN -> {
                            Text(text = "Off", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
