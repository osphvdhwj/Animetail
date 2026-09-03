package eu.kanade.presentation.more.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnimetailFeatureHubScreen(
    onNavigateToMultiSync: () -> Unit,
    onNavigateToHabitTracker: () -> Unit,
    onNavigateToForum: () -> Unit,
    onNavigateToWebsitePortal: () -> Unit,
    onNavigateToDownloadManager: () -> Unit,
    onNavigateToLNReader: () -> Unit,
    onNavigateToStaffSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // Hero Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                ),
                            ),
                        )
                        .padding(20.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Animetail Master Feature Hub",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "EverythingMoe (86 Apps), Awesome MAL, MALClient & DailyAL Unified",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }

        // Section 1: Multi-Tracker & Habit Tracking
        item {
            Text(
                text = "Tracking & Community Power Tools",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            FeatureActionCard(
                title = "Universal Multi-Sync Hub",
                subtitle = "Simultaneous auto-sync across 15 platforms (MAL, AniList, Kitsu, Simkl, Trakt)",
                icon = Icons.Default.CloudSync,
                iconContainerColor = Color(0xFF1976D2),
                badgeText = "15 Services",
                onClick = onNavigateToMultiSync,
            )
        }

        item {
            FeatureActionCard(
                title = "Daily Habit & Activity Heatmap",
                subtitle = "30-day activity matrix & fire streak tracker (from DailyAL)",
                icon = Icons.Default.LocalFireDepartment,
                iconContainerColor = Color(0xFFFF6D00),
                badgeText = "Streak Active",
                onClick = onNavigateToHabitTracker,
            )
        }

        item {
            FeatureActionCard(
                title = "MAL Community & Forum Discussions",
                subtitle = "Live episode discussion threads, reviews & recommendations (from MALClient)",
                icon = Icons.Default.Forum,
                iconContainerColor = Color(0xFF2E51A2),
                badgeText = "Live Feed",
                onClick = onNavigateToForum,
            )
        }

        // Section 2: Universal Portal & Media Enhancements
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ecosystem Portal & Player Tools",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            FeatureActionCard(
                title = "21+ External Website Portal",
                subtitle = "Deep links to Anime-Planet, MangaDex, LiveChart, Suwayomi, Komga, Jellyfin",
                icon = Icons.Default.Language,
                iconContainerColor = Color(0xFF388E3C),
                badgeText = "21+ Sites",
                onClick = onNavigateToWebsitePortal,
            )
        }

        item {
            FeatureActionCard(
                title = "Download Speed Meter & Queue Manager",
                subtitle = "Real-time MB/s bandwidth gauge, ETA countdowns & Pause/Resume FABs",
                icon = Icons.Default.Speed,
                iconContainerColor = Color(0xFF7B1FA2),
                badgeText = "Live Meter",
                onClick = onNavigateToDownloadManager,
            )
        }

        item {
            FeatureActionCard(
                title = "Light Novel E-Reader & TTS",
                subtitle = "Custom typography, 5 reader color themes & Text-to-Speech read aloud",
                icon = Icons.Default.Book,
                iconContainerColor = Color(0xFF00796B),
                badgeText = "E-Reader",
                onClick = onNavigateToLNReader,
            )
        }

        item {
            FeatureActionCard(
                title = "Voice Actors & Production Staff Tree",
                subtitle = "Seiyuu profiles, character roles & prequel/sequel relation graphs",
                icon = Icons.Default.Groups,
                iconContainerColor = Color(0xFFC2185B),
                badgeText = "Seiyuu Info",
                onClick = onNavigateToStaffSheet,
            )
        }
    }
}

@Composable
private fun FeatureActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconContainerColor: Color,
    badgeText: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(iconContainerColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = iconContainerColor,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
