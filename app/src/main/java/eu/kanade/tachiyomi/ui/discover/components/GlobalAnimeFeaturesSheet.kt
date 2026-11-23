package eu.kanade.tachiyomi.ui.discover.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Global Anime & Manga Power Features Hub (aggregating iconic features from top anime apps around the world:
 * DailyAL, Dantotsu, Saikou, Kitsu, AniList Mobile, MALClient, Crunchyroll).
 *
 * Triggered on Double-Tap of the Discover navigation bar tab, Discover top bar, or more options menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalAnimeFeaturesSheet(
    onDismissRequest: () -> Unit,
    onSelectMode: (Int) -> Unit, // 0: Anime, 1: Airing, 2: Manga, 3: Genres, 4: Swipe Deck, 6: Multi-Sync
    onRandomRoll: () -> Unit,
    onTop100: () -> Unit,
    onSeasonalChart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uriHandler = LocalUriHandler.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "✨ Discover Power Hub",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = "GLOBAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = "Features from top anime & manga apps worldwide",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                FilledTonalIconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Nav Features Grid (2 columns)
            Text(
                text = "DISCOVER MODES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HubFeatureCard(
                    title = "Lucky Random Roll",
                    subtitle = "Roll a surprise hit",
                    icon = Icons.Filled.Casino,
                    gradient = listOf(Color(0xFFE91E63), Color(0xFFFF4081)),
                    onClick = {
                        onDismissRequest()
                        onRandomRoll()
                    },
                    modifier = Modifier.weight(1f),
                )
                HubFeatureCard(
                    title = "Tinder Swipe Deck",
                    subtitle = "Swipe to discover",
                    icon = Icons.Outlined.Style,
                    gradient = listOf(Color(0xFFFF5722), Color(0xFFFF9800)),
                    onClick = {
                        onDismissRequest()
                        onSelectMode(4)
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HubFeatureCard(
                    title = "Airing Schedule",
                    subtitle = "Day-by-day countdowns",
                    icon = Icons.Outlined.CalendarMonth,
                    gradient = listOf(Color(0xFF2196F3), Color(0xFF03A9F4)),
                    onClick = {
                        onDismissRequest()
                        onSelectMode(1)
                    },
                    modifier = Modifier.weight(1f),
                )
                HubFeatureCard(
                    title = "Top 100 Leaderboards",
                    subtitle = "All-time highest rated",
                    icon = Icons.Filled.EmojiEvents,
                    gradient = listOf(Color(0xFFFFB300), Color(0xFFFFD54F)),
                    onClick = {
                        onDismissRequest()
                        onTop100()
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HubFeatureCard(
                    title = "Seasonal Chart",
                    subtitle = "Spring 2026 releases",
                    icon = Icons.Filled.AutoAwesome,
                    gradient = listOf(Color(0xFF9C27B0), Color(0xFFBA68C8)),
                    onClick = {
                        onDismissRequest()
                        onSeasonalChart()
                    },
                    modifier = Modifier.weight(1f),
                )
                HubFeatureCard(
                    title = "Universal Multi-Sync",
                    subtitle = "MAL, AniList, Kitsu",
                    icon = Icons.Outlined.Sync,
                    gradient = listOf(Color(0xFF009688), Color(0xFF4DB6AC)),
                    onClick = {
                        onDismissRequest()
                        onSelectMode(6)
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // 🎵 Trending Anime Soundtracks & Theme Songs (Dantotsu / Saikou feature)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Headphones,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "TRENDING THEME SONGS & OSTS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val soundtracks = listOf(
                SoundtrackItem("Bling-Bang-Bang-Born", "Creepy Nuts", "Mashle Season 2 OP", "https://www.youtube.com/results?search_query=Creepy+Nuts+Bling-Bang-Bang-Born"),
                SoundtrackItem("Idol (アイドル)", "YOASOBI", "Oshi no Ko OP", "https://www.youtube.com/results?search_query=YOASOBI+Idol"),
                SoundtrackItem("SPECIALZ", "King Gnu", "Jujutsu Kaisen Season 2 OP", "https://www.youtube.com/results?search_query=King+Gnu+SPECIALZ"),
                SoundtrackItem("Kura Kura", "Ado", "Spy x Family Season 2 OP", "https://www.youtube.com/results?search_query=Ado+Kura+Kura"),
                SoundtrackItem("Ao no Sumika (青のすみか)", "Tatsuya Kitani", "Jujutsu Kaisen Season 2 OP1", "https://www.youtube.com/results?search_query=Tatsuya+Kitani+Ao+no+Sumika"),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                soundtracks.forEach { ost ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { uriHandler.openUri(ost.searchUrl) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ost.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${ost.artist} • ${ost.anime}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.PlayCircle,
                                contentDescription = "Play Preview",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // 🎙️ Legendary Voice Actors & Seiyuu (DailyAL / MAL feature)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "POPULAR SEIYUU & VOICE TALENT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val voiceActors = listOf(
                Pair("Kenjiro Tsuda", "Nanami (JJK), Kishibe (Chainsaw Man)"),
                Pair("Hiroshi Kamiya", "Levi (AOT), Trafalgar Law (One Piece)"),
                Pair("Saori Hayami", "Yor Forger (Spy x Family), Shinobu (Demon Slayer)"),
                Pair("Mamoru Miyano", "Light Yagami (Death Note), Dazai (Bungou)"),
                Pair("Rie Takahashi", "Ai Hoshino (Oshi no Ko), Megumin (Konosuba)"),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                voiceActors.forEach { (name, roles) ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .width(200.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                uriHandler.openUri("https://myanimelist.net/search/all?q=" + java.net.URLEncoder.encode(name, "UTF-8"))
                            },
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = roles,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class SoundtrackItem(
    val title: String,
    val artist: String,
    val anime: String,
    val searchUrl: String,
)

@Composable
private fun HubFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
