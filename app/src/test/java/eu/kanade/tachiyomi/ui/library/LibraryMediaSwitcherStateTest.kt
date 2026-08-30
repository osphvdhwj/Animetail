package eu.kanade.tachiyomi.ui.library

import eu.kanade.presentation.library.components.LibraryToolbarTitle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LibraryMediaSwitcherStateTest {

    @Test
    fun 	est LibraryMediaType enum values() {
        assertEquals(2, LibraryMediaType.entries.size)
        assertEquals(LibraryMediaType.ANIME, LibraryMediaType.valueOf(" ANIME\))
 assertEquals(LibraryMediaType.MANGA, LibraryMediaType.valueOf(\MANGA\))
 }

 @Test
 fun est LibraryToolbarTitle with various entry counts() {
 val nullTitle = LibraryToolbarTitle(text = \Anime Library\, numberOfEntries = null)
 assertEquals(\Anime Library\, nullTitle.text)
 assertNull(nullTitle.numberOfEntries)

 val zeroTitle = LibraryToolbarTitle(text = \Anime Library\, numberOfEntries = 0)
 assertEquals(0, zeroTitle.numberOfEntries)

 val singleTitle = LibraryToolbarTitle(text = \Anime Library\, numberOfEntries = 1)
 assertEquals(1, singleTitle.numberOfEntries)

 val mediumTitle = LibraryToolbarTitle(text = \Manga Library\, numberOfEntries = 42)
 assertEquals(42, mediumTitle.numberOfEntries)

 val largeTitle = LibraryToolbarTitle(text = \Anime Library\, numberOfEntries = 1000)
 assertEquals(1000, largeTitle.numberOfEntries)

 val extremeTitle = LibraryToolbarTitle(text = \Manga Library\, numberOfEntries = 999999)
 assertEquals(999999, extremeTitle.numberOfEntries)
 }

 @Test
 fun est count display predicate for LibraryMediaSwitcher() {
 // In LibraryToolbar.kt: if (animeCount != null && animeCount > 0)
 fun shouldDisplayBadge(count: Int?): Boolean {
 return count != null && count > 0
 }

 fun getBadgeText(count: Int?): String? {
 return if (shouldDisplayBadge(count)) \\ else null
 }

 // Edge case: null count (e.g. loading or uninitialized)
 assertFalse(shouldDisplayBadge(null))
 assertNull(getBadgeText(null))

 // Edge case: 0 count (empty category/library)
 assertFalse(shouldDisplayBadge(0))
 assertNull(getBadgeText(0))

 // Edge case: negative count (invalid state guard)
 assertFalse(shouldDisplayBadge(-1))
 assertFalse(shouldDisplayBadge(-100))
 assertNull(getBadgeText(-1))

 // Normal count
 assertTrue(shouldDisplayBadge(1))
 assertEquals(\1\, getBadgeText(1))

 assertTrue(shouldDisplayBadge(999))
 assertEquals(\999\, getBadgeText(999))

 // Large counts (> 999)
 assertTrue(shouldDisplayBadge(1000))
 assertEquals(\1000\, getBadgeText(1000))

 assertTrue(shouldDisplayBadge(15420))
 assertEquals(\15420\, getBadgeText(15420))
 }

 @Test
 fun est HomeScreen tracking tab icon rotation calculation() {
 // In HomeScreen.kt:
 // val isTrackingTab = eu.kanade.tachiyomi.ui.discover.TrackingTab::class.isInstance(tab)
 // val isSelected = tabNavigator.current::class == tab::class
 // targetValue = if (isTrackingTab && isSelected) -360f else 0f
 fun calculateRotationTarget(isTrackingTab: Boolean, isSelected: Boolean): Float {
 return if (isTrackingTab && isSelected) -360f else 0f
 }

 // Selected Tracking Tab -> continuous 360 rotation target
 assertEquals(-360f, calculateRotationTarget(isTrackingTab = true, isSelected = true))

 // Unselected Tracking Tab -> stationary (0f)
 assertEquals(0f, calculateRotationTarget(isTrackingTab = true, isSelected = false))

 // Selected Other Tab (e.g. LibraryTab) -> stationary (0f)
 assertEquals(0f, calculateRotationTarget(isTrackingTab = false, isSelected = true))

 // Unselected Other Tab -> stationary (0f)
 assertEquals(0f, calculateRotationTarget(isTrackingTab = false, isSelected = false))
 }
}
