package eu.kanade.presentation.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class LNThemePreset(
    val displayName: String,
    val backgroundColor: Color,
    val textColor: Color,
) {
    LIGHT("Light", Color(0xFFFAF9F6), Color(0xFF1C1C1C)),
    DARK("Dark", Color(0xFF1E1E1E), Color(0xFFE0E0E0)),
    SEPIA("Sepia", Color(0xFFF4ECD8), Color(0xFF423219)),
    OLED("OLED Black", Color(0xFF000000), Color(0xFFE5E5E5)),
    SOLARIZED("Solarized", Color(0xFF002B36), Color(0xFF839496)),
}

@Composable
fun LightNovelReaderEngine(
    chapterTitle: String,
    contentParagraphs: List<String>,
    onNextChapter: () -> Unit,
    onPrevChapter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var fontSizeSp by remember { mutableFloatStateOf(17f) }
    var lineHeightMultiplier by remember { mutableFloatStateOf(1.5f) }
    var selectedTheme by remember { mutableStateOf(LNThemePreset.DARK) }
    var isTtsPlaying by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(selectedTheme.backgroundColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            // Chapter Title Header
            Text(
                text = chapterTitle,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (fontSizeSp + 4).sp,
                ),
                color = selectedTheme.textColor,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Paragraphs
            contentParagraphs.forEach { paragraph ->
                Text(
                    text = paragraph,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp * lineHeightMultiplier).sp,
                        fontFamily = FontFamily.Serif,
                    ),
                    color = selectedTheme.textColor,
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // Floating Control Bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Font Controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (fontSizeSp > 12f) fontSizeSp -= 1f }) {
                            Text("-A", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "${fontSizeSp.toInt()} pt",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(onClick = { if (fontSizeSp < 32f) fontSizeSp += 1f }) {
                            Text("+A", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                    }

                    // TTS Button
                    IconButton(onClick = { isTtsPlaying = !isTtsPlaying }) {
                        Icon(
                            imageVector = if (isTtsPlaying) Icons.Default.Pause else Icons.Default.VolumeUp,
                            contentDescription = "TTS Read Aloud",
                            tint = if (isTtsPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    // Theme Selector Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LNThemePreset.entries.forEach { theme ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(theme.backgroundColor)
                                    .clickable { selectedTheme = theme },
                            )
                        }
                    }
                }
            }
        }
    }
}
