package eu.kanade.tachiyomi.ui.discover.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RotateLeft
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

data class SwipeableMediaCard(
    val id: Long,
    val title: String,
    val coverUrl: String,
    val score: Float = 0f,
    val format: String = "",
    val status: String = "",
    val summary: String = "",
    val sourceName: String = "Anime",
    val isAnime: Boolean = true,
    val genres: List<String> = emptyList(),
    val animeItem: AnimeTrackSearch? = null,
    val mangaItem: MangaTrackSearch? = null,
)

@Composable
fun TinderDiscoveryDeck(
    items: List<SwipeableMediaCard>,
    onCardClick: (SwipeableMediaCard) -> Unit,
    onSaveToLibrary: (SwipeableMediaCard) -> Unit,
    onPass: (SwipeableMediaCard) -> Unit,
    onRefresh: () -> Unit,
    bothSidesDismiss: Boolean = false,
    onSearchInSources: ((SwipeableMediaCard) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var mediaTypeFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Anime, 2: Manga
    val filteredItems = remember(items, mediaTypeFilter) {
        when (mediaTypeFilter) {
            1 -> items.filter { it.isAnime }
            2 -> items.filter { !it.isAnime }
            else -> items
        }
    }

    if (filteredItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "You've reviewed all titles!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Tap below to reload more discovery cards",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledIconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(54.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Reload Deck")
                }
            }
        }
        return
    }

    var currentIndex by remember(filteredItems) { mutableIntStateOf(0) }
    val historyStack = remember { mutableStateListOf<SwipeableMediaCard>() }
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    val currentCard = filteredItems.getOrNull(currentIndex)
    val nextCard = filteredItems.getOrNull(currentIndex + 1)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Top Filter Bar: [ All | 🎬 Anime | 📖 Manga ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .graphicsLayer {
                    val dragDistance = kotlin.math.abs(offsetX.value) + kotlin.math.abs(offsetY.value)
                    alpha = (1f - (dragDistance / 110f)).coerceIn(0f, 1f)
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = mediaTypeFilter == 0,
                onClick = { mediaTypeFilter = 0; currentIndex = 0 },
                label = { Text("All", fontSize = 12.sp) },
                border = null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            FilterChip(
                selected = mediaTypeFilter == 1,
                onClick = { mediaTypeFilter = 1; currentIndex = 0 },
                label = { Text("🎬 Anime", fontSize = 12.sp) },
                border = null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            FilterChip(
                selected = mediaTypeFilter == 2,
                onClick = { mediaTypeFilter = 2; currentIndex = 0 },
                label = { Text("📖 Manga", fontSize = 12.sp) },
                border = null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        // Deck Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            // Background Next Card Preview with subtle depth scaling
            if (nextCard != null) {
                val nextScale = (0.94f + (abs(offsetX.value) / 2000f)).coerceIn(0.94f, 1f)
                DeckCardItem(
                    card = nextCard,
                    dragOffsetX = 0f,
                    bothSidesDismiss = bothSidesDismiss,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 14.dp, start = 10.dp, end = 10.dp)
                        .graphicsLayer {
                            scaleX = nextScale
                            scaleY = nextScale
                        },
                    onClick = {},
                )
            }

            // Top Active Swipe Card
            if (currentCard != null) {
                val rotation = (offsetX.value / 60f).coerceIn(-25f, 25f)

                DeckCardItem(
                    card = currentCard,
                    dragOffsetX = offsetX.value,
                    bothSidesDismiss = bothSidesDismiss,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                        .rotate(rotation)
                        .pointerInput(currentCard.id) {
                            detectDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        if (offsetX.value > 150f) {
                                            // Swiped Right -> Save / Explore
                                            offsetX.animateTo(1000f, tween(200))
                                            historyStack.add(currentCard)
                                            if (bothSidesDismiss) {
                                                onPass(currentCard)
                                            } else {
                                                onSaveToLibrary(currentCard)
                                            }
                                            currentIndex++
                                            offsetX.snapTo(0f)
                                            offsetY.snapTo(0f)
                                        } else if (offsetX.value < -150f) {
                                            // Swiped Left -> Pass / Dismiss
                                            offsetX.animateTo(-1000f, tween(200))
                                            historyStack.add(currentCard)
                                            onPass(currentCard)
                                            currentIndex++
                                            offsetX.snapTo(0f)
                                            offsetY.snapTo(0f)
                                        } else {
                                            // Snap back seamlessly
                                            offsetX.animateTo(0f, spring(dampingRatio = 0.8f))
                                            offsetY.animateTo(0f, spring(dampingRatio = 0.8f))
                                        }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        offsetX.snapTo(offsetX.value + dragAmount.x)
                                        offsetY.snapTo(offsetY.value + dragAmount.y)
                                    }
                                },
                            )
                        },
                    onClick = { onCardClick(currentCard) },
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons Row: Smoothly disappears when card is swiped, appears back when idle!
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .graphicsLayer {
                    val dragDistance = kotlin.math.abs(offsetX.value) + kotlin.math.abs(offsetY.value)
                    alpha = (1f - (dragDistance / 120f)).coerceIn(0f, 1f)
                },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Undo / Rewind Button
            FilledTonalIconButton(
                onClick = {
                    if (currentIndex > 0 && historyStack.isNotEmpty()) {
                        currentIndex--
                        historyStack.removeLastOrNull()
                        scope.launch {
                            offsetX.snapTo(-400f)
                            offsetX.animateTo(0f, spring(dampingRatio = 0.8f))
                        }
                    }
                },
                enabled = currentIndex > 0 && historyStack.isNotEmpty(),
                modifier = Modifier.size(46.dp),
            ) {
                Icon(Icons.Outlined.RotateLeft, contentDescription = "Undo Previous Card", modifier = Modifier.size(22.dp))
            }

            // Dismiss / Pass Left Button
            FilledIconButton(
                onClick = {
                    if (currentCard != null) {
                        scope.launch {
                            offsetX.animateTo(-1000f, tween(200))
                            historyStack.add(currentCard)
                            onPass(currentCard)
                            currentIndex++
                            offsetX.snapTo(0f)
                            offsetY.snapTo(0f)
                        }
                    }
                },
                modifier = Modifier.size(54.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Dismiss", modifier = Modifier.size(26.dp))
            }

            // Quick Search in Extension Sources
            FilledTonalIconButton(
                onClick = {
                    if (currentCard != null) {
                        onSearchInSources?.invoke(currentCard) ?: onSaveToLibrary(currentCard)
                    }
                },
                modifier = Modifier.size(46.dp),
            ) {
                Icon(Icons.Outlined.Search, contentDescription = "Search Sources", modifier = Modifier.size(22.dp))
            }

            // Info / Details Button
            FilledTonalIconButton(
                onClick = {
                    if (currentCard != null) {
                        onCardClick(currentCard)
                    }
                },
                modifier = Modifier.size(46.dp),
            ) {
                Icon(Icons.Outlined.Info, contentDescription = "Details", modifier = Modifier.size(22.dp))
            }

            // Like / Save to Library Right Button
            FilledIconButton(
                onClick = {
                    if (currentCard != null) {
                        scope.launch {
                            offsetX.animateTo(1000f, tween(200))
                            historyStack.add(currentCard)
                            if (bothSidesDismiss) {
                                onPass(currentCard)
                            } else {
                                onSaveToLibrary(currentCard)
                            }
                            currentIndex++
                            offsetX.snapTo(0f)
                            offsetY.snapTo(0f)
                        }
                    }
                },
                modifier = Modifier.size(54.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (bothSidesDismiss) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (bothSidesDismiss) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(
                    if (bothSidesDismiss) Icons.Outlined.Close else Icons.Outlined.BookmarkAdd,
                    contentDescription = if (bothSidesDismiss) "Dismiss" else "Save to Library",
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun DeckCardItem(
    card: SwipeableMediaCard,
    dragOffsetX: Float = 0f,
    bothSidesDismiss: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = card.coverUrl,
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Dynamic Swipe Stamps (SAVE / PASS)
            if (dragOffsetX > 40f) {
                val alpha = ((dragOffsetX - 40f) / 100f).coerceIn(0f, 1f)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = (if (bothSidesDismiss) Color(0xFFE53935) else Color(0xFF43A047)).copy(alpha = 0.92f * alpha),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 56.dp, start = 20.dp)
                        .rotate(-15f),
                ) {
                    Text(
                        text = if (bothSidesDismiss) "DISMISS" else "SAVE",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            } else if (dragOffsetX < -40f) {
                val alpha = ((-dragOffsetX - 40f) / 100f).coerceIn(0f, 1f)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE53935).copy(alpha = 0.92f * alpha),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 56.dp, end = 20.dp)
                        .rotate(15f),
                ) {
                    Text(
                        text = "PASS",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }

            // Gradient Overlay: Seamless fractional stops, NO pixel startY thick lines!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.38f to Color.Transparent,
                                0.65f to Color.Black.copy(alpha = 0.45f),
                                0.85f to Color.Black.copy(alpha = 0.85f),
                                1.0f to Color.Black.copy(alpha = 0.96f),
                            ),
                        ),
                    ),
            )

            // Top Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        text = if (card.isAnime) "ANIME" else "MANGA",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                if (card.score > 0f) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", card.score),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            // Bottom Content Information
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (card.format.isNotBlank() || card.status.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (card.format.isNotBlank()) {
                            Text(
                                text = card.format,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (card.status.isNotBlank()) {
                            Text(
                                text = "•  ${card.status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                    }
                }

                if (card.summary.isNotBlank()) {
                    Text(
                        text = card.summary.replace(Regex("<[^>]*>"), ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

