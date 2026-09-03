package eu.kanade.tachiyomi.ui.discover.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.util.system.toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MediaDetailsBottomSheet(
    anime: AnimeTrackSearch? = null,
    manga: MangaTrackSearch? = null,
    onDismiss: () -> Unit,
    onSearchInSources: (String, Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uriHandler = LocalUriHandler.current

    val title = anime?.title ?: manga?.title ?: ""
    val coverUrl = anime?.cover_url ?: manga?.cover_url ?: ""
    val summary = anime?.summary ?: manga?.summary ?: ""
    val format = anime?.publishing_type ?: manga?.publishing_type ?: ""
    val status = anime?.publishing_status ?: manga?.publishing_status ?: ""
    val trackingUrl = anime?.tracking_url ?: manga?.tracking_url ?: ""
    val rawScore = anime?.score ?: manga?.score ?: -1.0
    val formattedScore = if (rawScore > 0) {
        if (rawScore > 10) {
            String.format(java.util.Locale.US, "%.1f", rawScore / 10f)
        } else {
            String.format(java.util.Locale.US, "%.1f", rawScore)
        }
    } else null
    val totalCount = (anime?.total_episodes ?: manga?.total_chapters ?: 0).toInt()
    val isAnime = anime != null
    val authors = anime?.authors ?: manga?.authors ?: emptyList()

    val context = LocalContext.current
    var isSummaryExpanded by remember { mutableStateOf(false) }
    var userStatus by remember { mutableStateOf(if (isAnime) "Watching" else "Reading") }
    var userProgress by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            // Header with Cover Art, Title, Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Cover Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .width(115.dp)
                        .aspectRatio(0.70f),
                ) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // Metadata Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // Badges Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (format.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ) {
                                Text(
                                    text = format.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }

                        if (formattedScore != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = formattedScore,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    if (status.isNotBlank()) {
                        Text(
                            text = "Status: ${status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (totalCount > 0) {
                        Text(
                            text = if (isAnime) "$totalCount Episodes" else "$totalCount Chapters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (authors.isNotEmpty()) {
                        Text(
                            text = if (isAnime) "Studio: ${authors.joinToString()}" else "Author: ${authors.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Watch / Read in Animetail Bridge Button
                Button(
                    onClick = {
                        onDismiss()
                        onSearchInSources(title, isAnime)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = if (isAnime) Icons.Outlined.PlayArrow else Icons.Outlined.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAnime) "Watch in App" else "Read in App",
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Open in Browser
                if (trackingUrl.isNotBlank()) {
                    FilledTonalButton(
                        onClick = { uriHandler.openUri(trackingUrl) },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = "Open Web",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Tracking Entry Status & Progress Manager (Custom-made for all tracking sites)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Sync & Tracking Status",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    // Status Filter Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val statusList = if (isAnime) {
                            listOf("Watching", "Plan to Watch", "Completed", "On Hold", "Dropped")
                        } else {
                            listOf("Reading", "Plan to Read", "Completed", "On Hold", "Dropped")
                        }
                        statusList.forEach { statusOption ->
                            val isSel = userStatus == statusOption
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    userStatus = statusOption
                                    context.toast("Tracking status set to $statusOption")
                                },
                                label = { Text(statusOption, fontSize = 12.sp) },
                                leadingIcon = if (isSel) {
                                    { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }

                    // Progress Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isAnime) "Episodes Progress" else "Chapters Progress",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            IconButton(
                                onClick = {
                                    if (userProgress > 0) {
                                        userProgress--
                                        context.toast("Progress: $userProgress")
                                    }
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Outlined.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                            }

                            Text(
                                text = if (totalCount > 0) "$userProgress / $totalCount" else "$userProgress",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )

                            IconButton(
                                onClick = {
                                    if (totalCount == 0 || userProgress < totalCount) {
                                        userProgress++
                                        context.toast("Progress: $userProgress")
                                    }
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Synopsis / Summary
            if (summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Synopsis",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isSummaryExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { isSummaryExpanded = !isSummaryExpanded },
                    )
                    Text(
                        text = if (isSummaryExpanded) "Show less" else "Read more...",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable { isSummaryExpanded = !isSummaryExpanded },
                    )
                }
            }
        }
    }
}
