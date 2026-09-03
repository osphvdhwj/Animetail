package eu.kanade.tachiyomi.ui.stats.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.more.settings.screen.SettingsTrackingScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
fun Screen.profileStatsTab(): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = rememberScreenModel { ProfileScreenModel() }
    val state by screenModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current

    return TabContent(
        titleRes = MR.strings.pref_category_tracking,
        content = { contentPadding, _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                when (val currentState = state) {
                    is ProfileState.Loading -> {
                        LoadingScreen()
                    }
                    is ProfileState.NotLoggedIn -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Trackers Not Connected",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Login to your AniList, MyAnimeList, Kitsu, or other tracker accounts under Tracking Settings to view your profile and sync tracking data.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { navigator.push(SettingsTrackingScreen) }) {
                                Text(text = "Go to Tracking Settings")
                            }
                        }
                    }
                    is ProfileState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Failed to load profile",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { screenModel.loadProfile() }) {
                                Text(text = "Retry")
                            }
                        }
                    }
                    is ProfileState.Success -> {
                        val stats = currentState.anilistStats
                        val accounts = currentState.connectedAccounts

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Header Banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            ) {
                                if (stats?.bannerImage?.isNotBlank() == true) {
                                    AsyncImage(
                                        model = stats.bannerImage,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.secondary
                                                    )
                                                )
                                            )
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.35f))
                                )

                                // Main Profile Details Overlay
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (stats?.avatar?.large?.isNotBlank() == true) {
                                        AsyncImage(
                                            model = stats.avatar?.large,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountCircle,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        val primaryName = stats?.name ?: accounts.firstOrNull()?.username ?: "Tracker User"
                                        Text(
                                            text = primaryName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text(
                                            text = "${accounts.size} Connected Tracker(s)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Connected Trackers List Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Connected Tracking Accounts",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        accounts.forEach { account ->
                                            val profileUrl = getTrackerProfileUrl(account.tracker.id, account.username)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = account.tracker.getLogo()),
                                                    contentDescription = account.trackerName,
                                                    modifier = Modifier.size(24.dp),
                                                    tint = androidx.compose.ui.graphics.Color.Unspecified
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = account.trackerName,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    if (account.username.isNotBlank()) {
                                                        Text(
                                                            text = "@${account.username}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                if (profileUrl != null) {
                                                    IconButton(
                                                        onClick = { uriHandler.openUri(profileUrl) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.OpenInNew,
                                                            contentDescription = "Open ${account.trackerName} Profile",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Outlined.CheckCircle,
                                                        contentDescription = "Connected",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // If AniList stats are present, show deep analytics
                                if (stats != null) {
                                    stats.statistics?.anime?.let { animeStats ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(
                                                    text = "Anime Statistics",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    StatItem("Total Anime", animeStats.count.toString(), Modifier.weight(1f))
                                                    StatItem("Episodes", animeStats.episodesWatched.toString(), Modifier.weight(1f))
                                                }
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    val days = (animeStats.minutesWatched / 1440f)
                                                    StatItem("Time Spent", String.format("%.1f Days", days), Modifier.weight(1f))
                                                    StatItem("Mean Score", String.format("%.1f", animeStats.meanScore), Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }

                                    stats.statistics?.manga?.let { mangaStats ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(
                                                    text = "Manga Statistics",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    StatItem("Total Manga", mangaStats.count.toString(), Modifier.weight(1f))
                                                    StatItem("Chapters", mangaStats.chaptersRead.toString(), Modifier.weight(1f))
                                                }
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    StatItem("Volumes", mangaStats.volumesRead.toString(), Modifier.weight(1f))
                                                    StatItem("Mean Score", String.format("%.1f", mangaStats.meanScore), Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { uriHandler.openUri("https://anilist.co/user/${stats.name}") },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "View AniList Web Profile")
                                    }
                                }

                                OutlinedButton(
                                    onClick = { navigator.push(SettingsTrackingScreen) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.Sync, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Manage Tracking Accounts")
                                }
                            }
                        }
                    }
                }
            }
        },
        navigateUp = navigator::pop,
    )
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun getTrackerProfileUrl(trackerId: Long, username: String): String? {
    if (username.isBlank()) return null
    return when (trackerId) {
        1L -> "https://myanimelist.net/profile/$username"
        2L -> "https://anilist.co/user/$username"
        3L -> "https://kitsu.app/users/$username"
        4L -> "https://shikimori.one/$username"
        5L -> "https://bgm.tv/user/$username"
        7L -> "https://www.mangaupdates.com/members.html?id=$username"
        101L -> "https://simkl.com/$username"
        201L -> "https://trakt.tv/users/$username"
        else -> null
    }
}
