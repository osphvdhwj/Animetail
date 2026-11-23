package eu.kanade.presentation.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animetail design language, layered on Material 3 Expressive (M3E) + Glassmorphism.
 *
 * All custom surfaces — Floating Navigation Island, Discover Hub, Browse Portal,
 * Stats Bento Grid, Player Sheets, Local Media Browsers — draw their shape language,
 * motion physics, and glassmorphic surfaces from these tokens.
 *
 * Rules:
 *  - **Cards & tiles** — [radiusCard], container `surfaceContainerHigh`.
 *  - **Bento grids & large panels** — [radiusBento], container `surfaceContainer`.
 *  - **Hero banners** — [radiusHero], primaryContainer→tertiaryContainer brush.
 *  - **Floating island & pills** — [radiusPill], glassIsland modifier.
 *  - **Inner controls** (pills, score badges, small buttons) — [radiusMedium].
 *  - **Thumbnails & avatars** — [radiusSmall] (circles stay [CircleShape]).
 *  - **Elevation** — tonal + soft ambient glass shadow; avoid harsh dark shadows.
 *  - **Motion** — durations/springs from [Motion]; never invent ad-hoc specs.
 */
object DS {

    /** Thumbnails, inline images, avatar frames. */
    val radiusSmall = RoundedCornerShape(8.dp)

    /** Pills, badges, inner controls inside a card. */
    val radiusMedium = RoundedCornerShape(12.dp)

    /** Every standard card, list tile, and quick-action tile. */
    val radiusCard = RoundedCornerShape(16.dp)

    /** Hero banners, full-width highlight surfaces, and modal sheets. */
    val radiusHero = RoundedCornerShape(20.dp)

    /** Large bento containers, section groupings, and modal dialogs. */
    val radiusBento = RoundedCornerShape(24.dp)

    /** Pill capsules, floating islands, and active indicators. */
    val radiusPill = RoundedCornerShape(100.dp)

    /** Border thickness tokens. */
    val strokeHairline = 0.5.dp
    val strokeThin = 1.dp
    val strokeMedium = 1.5.dp
    val strokeThick = 2.dp

    /** Elevation tokens. */
    val elevationNone = 0.dp
    val elevationLow = 2.dp
    val elevationMedium = 6.dp
    val elevationHigh = 10.dp
    val elevationIsland = 8.dp

    /**
     * Motion vocabulary. Expressive spring curves and emphasized easing curves.
     */
    object Motion {
        /** Fast feedback: chips, icon swaps, toggle taps. */
        const val durationFast = 150

        /** Standard transitions: section switches, collapse, tab shifts. */
        const val durationNormal = 250

        /** Emphasized entrances: hero cards, first-in content. */
        const val durationSlow = 350

        /** Paging and full-screen screen transitions. */
        const val durationPaging = 400

        /** Signature easing — accelerate then settle (M3 "emphasized" curve). */
        val easing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

        fun <T> emphasized() = tween<T>(durationNormal, easing = easing)

        fun <T> emphasizedSlow() = tween<T>(durationSlow, easing = easing)

        fun <T> fast() = tween<T>(durationFast, easing = easing)

        /** Interactive pop: presses, selections, icon swaps, bouncy feedback. */
        fun <T> pop() = spring<T>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )

        /** Bouncy spring for vibrant badges and playful UI feedback. */
        fun <T> bouncy() = spring<T>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        )

        /** Smooth positional movement: collapsing headers, indicator slides. */
        fun <T> slide() = spring<T>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )

        /** Gentle settle for background fades and non-jarring state updates. */
        fun <T> gentle() = spring<T>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        )
    }

    /**
     * Glassmorphism color palette and border tokens.
     */
    object Glass {
        /** Default translucent container color respecting dark, light, and AMOLED modes. */
        @Composable
        fun containerColor(alpha: Float = 0.85f): Color {
            return MaterialTheme.colorScheme.surfaceContainer.copy(alpha = alpha)
        }

        /** High-emphasis translucent container color (e.g. for elevated cards and sheets). */
        @Composable
        fun containerColorHigh(alpha: Float = 0.88f): Color {
            return MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)
        }

        /** Highest-emphasis translucent container color (e.g. for dialogs and modals). */
        @Composable
        fun containerColorHighest(alpha: Float = 0.92f): Color {
            return MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha)
        }

        /** Subtle translucent border stroke color for glass highlights. */
        @Composable
        fun borderColor(alpha: Float = 0.35f): Color {
            return MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha)
        }

        /** Primary-tinted border stroke color for active or highlighted elements. */
        @Composable
        fun activeBorderColor(alpha: Float = 0.55f): Color {
            return MaterialTheme.colorScheme.primary.copy(alpha = alpha)
        }
    }
}

/**
 * Applies an M3 Expressive glassmorphic surface treatment:
 * - Subtle translucent container fill.
 * - Soft glass border gradient / stroke highlight.
 * - Optional tonal elevation shadow with ambient and spot colors.
 * - Shape clipping.
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape = DS.radiusCard,
    containerColor: Color = DS.Glass.containerColorHigh(),
    borderColor: Color = DS.Glass.borderColor(),
    borderWidth: Dp = DS.strokeThin,
    shadowElevation: Dp = 0.dp,
): Modifier {
    return this
        .then(
            if (shadowElevation > 0.dp) {
                Modifier.shadow(
                    elevation = shadowElevation,
                    shape = shape,
                    ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                )
            } else {
                Modifier
            },
        )
        .clip(shape)
        .background(containerColor, shape)
        .border(borderWidth, borderColor, shape)
}

/**
 * Floating glass island modifier specifically tuned for bottom navigation bars and floating action surfaces.
 */
@Composable
fun Modifier.glassIsland(
    shape: Shape = DS.radiusPill,
    containerColor: Color = DS.Glass.containerColor(0.88f),
    borderColor: Color = DS.Glass.borderColor(0.40f),
    elevation: Dp = DS.elevationIsland,
): Modifier {
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.16f),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        )
        .clip(shape)
        .background(containerColor, shape)
        .border(DS.strokeThin, borderColor, shape)
}

/**
 * Bento card modifier for 16-24dp rounded cards with subtle glass borders.
 */
@Composable
fun Modifier.glassBento(
    shape: Shape = DS.radiusBento,
    containerColor: Color = DS.Glass.containerColorHigh(),
    borderColor: Color = DS.Glass.borderColor(),
    elevation: Dp = DS.elevationLow,
): Modifier {
    return this.glassSurface(
        shape = shape,
        containerColor = containerColor,
        borderColor = borderColor,
        shadowElevation = elevation,
    )
}

/**
 * Pill badge / capsule modifier for small glass chips, pills, and filter buttons.
 */
@Composable
fun Modifier.glassPill(
    containerColor: Color = DS.Glass.containerColorHigh(0.80f),
    borderColor: Color = DS.Glass.borderColor(0.30f),
): Modifier {
    return this.glassSurface(
        shape = DS.radiusPill,
        containerColor = containerColor,
        borderColor = borderColor,
        borderWidth = DS.strokeHairline,
        shadowElevation = 0.dp,
    )
}
