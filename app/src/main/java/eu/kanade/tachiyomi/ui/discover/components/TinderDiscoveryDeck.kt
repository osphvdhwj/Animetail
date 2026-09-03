package eu.kanade.tachiyomi.ui.discover.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
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

    var currentIndex by remember(items) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    val currentCard = items.getOrNull(currentIndex)
    val nextCard = items.getOrNull(currentIndex + 1)
    val isDraggingUp = offsetY.value < -40f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Deck Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            // Background Next Card Preview with depth scaling
            if (nextCard != null) {
                val nextScale = (0.94f + (kotlin.math.abs(offsetX.value) / 2000f)).coerceIn(0.94f, 1f)
                DeckCardItem(
                    card = nextCard,
                    dragOffsetX = 0f,
                    bothSidesDismiss = bothSidesDismiss,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp, start = 12.dp, end = 12.dp)
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
                                            // Swiped Right
                                            offsetX.animateTo(1000f, tween(200))
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
                                            onPass(currentCard)
                                            currentIndex++
                                            offsetX.snapTo(0f)
                                            offsetY.snapTo(0f)
                                        } else {
                                            // Snap back
                                            offsetX.animateTo(0f, tween(150))
                                            offsetY.animateTo(0f, tween(150))
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

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Row (Pass, Info, Like) - Automatically hides when dragging up
        AnimatedVisibility(
            visible = !isDraggingUp,
            enter = fadeIn(tween(150)) + expandVertically(tween(150)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Pass / Dismiss Left Button
                FilledIconButton(
                    onClick = {
                        if (currentCard != null) {
                            scope.launch {
                                offsetX.animateTo(-1000f, tween(200))
                                onPass(currentCard)
                                currentIndex++
                                offsetX.snapTo(0f)
                                offsetY.snapTo(0f)
                            }
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Dismiss", modifier = Modifier.size(28.dp))
                }

                // Info / Details Button
                FilledIconButton(
                    onClick = {
                        if (currentCard != null) {
                            onCardClick(currentCard)
                        }
                    },
                    modifier = Modifier.size(46.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = "Details", modifier = Modifier.size(22.dp))
                }

                // Like / Save or Dismiss Right Button
                FilledIconButton(
                    onClick = {
                        if (currentCard != null) {
                            scope.launch {
                                offsetX.animateTo(1000f, tween(200))
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
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (bothSidesDismiss) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (bothSidesDismiss) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        if (bothSidesDismiss) Icons.Outlined.Close else Icons.Outlined.Favorite,
                        contentDescription = if (bothSidesDismiss) "Dismiss" else "Explore",
                        modifier = Modifier.size(28.dp),
                    )
                }
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
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = card.coverUrl,
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Dynamic Swipe Stamps (SAVE / PASS / DISMISS)
            if (dragOffsetX > 40f) {
                val alpha = ((dragOffsetX - 40f) / 100f).coerceIn(0f, 1f)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = (if (bothSidesDismiss) Color(0xFFE53935) else Color(0xFF43A047)).copy(alpha = 0.9f * alpha),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 64.dp, start = 20.dp)
                        .rotate(-15f),
                ) {
                    Text(
                        text = if (bothSidesDismiss) "DISMISS" else "SAVE",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            } else if (dragOffsetX < -40f) {
                val alpha = ((-dragOffsetX - 40f) / 100f).coerceIn(0f, 1f)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE53935).copy(alpha = 0.9f * alpha),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 64.dp, end = 20.dp)
                        .rotate(15f),
                ) {
                    Text(
                        text = "PASS",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.95f),
                            ),
                            startY = 300f,
                        ),
                    ),
            )

            // Top Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                        color = Color.Black.copy(alpha = 0.7f),
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.headlineSmall,
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
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                fontWeight = FontWeight.SemiBold,
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
