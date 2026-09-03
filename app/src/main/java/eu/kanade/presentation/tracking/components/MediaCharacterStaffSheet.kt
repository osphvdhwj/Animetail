package eu.kanade.presentation.tracking.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

data class CharacterItem(
    val id: Long,
    val name: String,
    val role: String, // Main, Supporting
    val imageUrl: String,
    val voiceActorName: String? = null,
    val voiceActorImageUrl: String? = null,
)

data class StaffItem(
    val id: Long,
    val name: String,
    val role: String, // Director, Character Design, Music
    val imageUrl: String? = null,
)

data class RelationMediaItem(
    val id: Long,
    val title: String,
    val relationType: String, // Prequel, Sequel, Spin-Off
    val coverUrl: String,
)

@Composable
fun MediaCharacterStaffSheet(
    characters: List<CharacterItem>,
    staff: List<StaffItem>,
    relations: List<RelationMediaItem>,
    onRelationClick: (RelationMediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Characters & Voice Actors Section
        if (characters.isNotEmpty()) {
            Column {
                Text(
                    text = "Characters & Voice Actors",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(characters, key = { it.id }) { character ->
                        CharacterCard(character = character)
                    }
                }
            }
        }

        // Staff & Production Section
        if (staff.isNotEmpty()) {
            Column {
                Text(
                    text = "Production Staff",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(staff, key = { it.id }) { staffMember ->
                        StaffCard(staff = staffMember)
                    }
                }
            }
        }

        // Related Media Tree
        if (relations.isNotEmpty()) {
            Column {
                Text(
                    text = "Related & Prequels/Sequels",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(relations, key = { it.id }) { relation ->
                        RelationCard(
                            relation = relation,
                            onClick = { onRelationClick(relation) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterCard(character: CharacterItem) {
    Surface(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AsyncImage(
                model = character.imageUrl,
                contentDescription = character.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = character.name,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = character.role,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (character.voiceActorName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "VA: ${character.voiceActorName}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StaffCard(staff: StaffItem) {
    Surface(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(14.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = staff.name,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = staff.role,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RelationCard(
    relation: RelationMediaItem,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = relation.relationType,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 10.sp,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            AsyncImage(
                model = relation.coverUrl,
                contentDescription = relation.title,
                modifier = Modifier
                    .size(width = 90.dp, height = 120.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = relation.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
