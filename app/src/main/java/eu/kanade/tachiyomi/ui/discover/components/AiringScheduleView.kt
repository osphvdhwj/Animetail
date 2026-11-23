package eu.kanade.tachiyomi.ui.discover.components

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import tachiyomi.presentation.core.i18n.stringResource
import eu.kanade.tachiyomi.data.track.model.AiringEpisode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import eu.kanade.presentation.design.DS

/**
 * Enumeration representing selectable days or schedules for anime broadcasts.
 *
 * Fully backwards-compatible with legacy offset presets ([TODAY], [TOMORROW], etc.)
 * while incorporating the 8 DailyAL weekday selector tabs: [MONDAY], [TUESDAY],
 * [WEDNESDAY], [THURSDAY], [FRIDAY], [SATURDAY], [SUNDAY], and [OTHERS].
 *
 * @property displayName Human-readable label for this day.
 * @property dayOffset Relative day offset from today (0 for today, 1 for tomorrow, etc.).
 */
enum class AiringDay(val displayName: String, val dayOffset: Int) {
    TODAY("Today", 0),
    TOMORROW("Tomorrow", 1),
    DAY_AFTER("In 2 Days", 2),
    DAY_4("In 3 Days", 3),
    DAY_5("In 4 Days", 4),
    DAY_6("In 5 Days", 5),
    DAY_7("In 6 Days", 6),
    WEEK("This Week", 7),
    MONDAY("Monday", 0),
    TUESDAY("Tuesday", 1),
    WEDNESDAY("Wednesday", 2),
    THURSDAY("Thursday", 3),
    FRIDAY("Friday", 4),
    SATURDAY("Saturday", 5),
    SUNDAY("Sunday", 6),
    OTHERS("Others", 0);

    /**
     * Computes a dynamic, localized label for this day choice.
     *
     * @return Formatted label suitable for chip or tab text.
     */
    fun getComputedLabel(): String {
        if (this == WEEK) return "This Week"
        if (this == OTHERS) return "Others"
        if (this in WEEKDAY_TABS) return displayName

        return try {
            val date = LocalDate.now().plusDays(dayOffset.toLong())
            val dayName = date.dayOfWeek.getDisplayName(
                java.time.format.TextStyle.SHORT,
                Locale.getDefault(),
            )
            when (dayOffset) {
                0 -> "Today ($dayName)"
                1 -> "Tomorrow ($dayName)"
                else -> dayName
            }
        } catch (_: Throwable) {
            displayName
        }
    }

    companion object {
        /**
         * List of the 8 canonical weekday tabs matching DailyAL's `WeeklySchedulePage`.
         */
        val WEEKDAY_TABS: List<AiringDay> = listOf(
            MONDAY,
            TUESDAY,
            WEDNESDAY,
            THURSDAY,
            FRIDAY,
            SATURDAY,
            SUNDAY,
            OTHERS,
        )

        /**
         * Maps a standard [DayOfWeek] to its corresponding [AiringDay] weekday representation.
         *
         * @param dayOfWeek Java time [DayOfWeek].
         * @return Matching [AiringDay] enum constant.
         */
        fun fromDayOfWeek(dayOfWeek: DayOfWeek): AiringDay {
            return when (dayOfWeek) {
                DayOfWeek.MONDAY -> MONDAY
                DayOfWeek.TUESDAY -> TUESDAY
                DayOfWeek.WEDNESDAY -> WEDNESDAY
                DayOfWeek.THURSDAY -> THURSDAY
                DayOfWeek.FRIDAY -> FRIDAY
                DayOfWeek.SATURDAY -> SATURDAY
                DayOfWeek.SUNDAY -> SUNDAY
            }
        }
    }
}

/**
 * Interactive Airing Schedule & Timetable view for Animetail, replicating DailyAL's
 * `AnimeCalendarWidget` (`anime_calendar.dart`) and `WeeklySchedulePage` (`weeklyanime.dart`).
 *
 * Core Features:
 * 1. **Weekday Selector Row**: 8 scrollable weekday tabs (Monday (X), Tuesday (Y), ..., Sunday (W), Others),
 *    defaulting to current day of the week with active item counts.
 * 2. **Status Filter Row**: Quick filter chips `[ All | Watching | Plan to Watch ]` with mutual exclusion logic.
 * 3. **Day Accordions**: Expandable day sections for Monday through Sunday displaying `${DayOfWeek} (${itemCount})`,
 *    expanded by default with smooth collapse/expand animations and "TODAY" badges.
 * 4. **Airing List Tiles**: DailyAL row layout with 60x60 rounded cover avatar + capsule airing time pill (`14:30`),
 *    bold title, live countdown badge (`Ep X in Yd Zh Zm` or `Airing Now` during 30-min window), score badge,
 *    and quick actions (Play/Watch button + Add to Calendar alert button).
 * 5. **Live 1-Second Countdown Ticker**: Reactive coroutine ticker updating countdowns every second.
 * 6. **Current Time Divider Marker**: Injected into today's timeline between past and upcoming broadcasts:
 *    2dp divider card containing real-time clock e.g. `14:25` + timezone offset `(GMT +05:30)`. Tapping
 *    displays next scheduled show preview in snackbar.
 * 7. **Add to Calendar Sheet**: Action sheet to add broadcast event to Android device calendar via
 *    [CalendarContract.Events.CONTENT_URI] or export/share RFC 5545 `.ics` details.
 *
 * @param airingList List of scheduled [AiringEpisode] items to display in the timetable.
 * @param selectedDay Currently active [AiringDay] preset. Defaults to [AiringDay.TODAY].
 * @param onSelectDay Callback invoked when a day tab or filter is selected.
 * @param onEpisodeClick Callback when an anime episode tile is clicked (opens media details).
 * @param onWatchClick Optional callback when the quick play/watch button is clicked.
 * @param modifier Composable modifier for the container layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiringScheduleView(
    airingList: List<AiringEpisode>,
    selectedDay: AiringDay = AiringDay.TODAY,
    onSelectDay: ((AiringDay) -> Unit)? = null,
    onEpisodeClick: (AiringEpisode) -> Unit,
    onWatchClick: ((AiringEpisode) -> Unit)? = null,
    modifier: Modifier = Modifier,
    libraryOnly: Boolean = false,
    onToggleLibraryOnly: (() -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()

    // 1. Live 1-Second Reactive Countdown Ticker
    var currentTimeSec by remember { mutableLongStateOf(System.currentTimeMillis() / 1000L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTimeSec = System.currentTimeMillis() / 1000L
        }
    }

    // Timezone and current date
    val systemZone = remember { ZoneId.systemDefault() }
    val todayDate = remember(currentTimeSec / 86400L) { LocalDate.now(systemZone) }
    val todayDayOfWeek = todayDate.dayOfWeek // DayOfWeek.MONDAY..SUNDAY
    val todayWeekdayIndex = todayDayOfWeek.value - 1 // 0 = Mon, 6 = Sun

    // 2. Status Filter State [ All | Watching | Plan to Watch ]
    var isAllFilterActive by remember { mutableStateOf(true) }
    var isWatchingFilterActive by remember { mutableStateOf(false) }
    var isPlanToWatchFilterActive by remember { mutableStateOf(false) }

    val filteredEpisodes = remember(
        airingList,
        isAllFilterActive,
        isWatchingFilterActive,
        isPlanToWatchFilterActive,
    ) {
        if (isAllFilterActive) {
            airingList
        } else {
            airingList.filter { episode ->
                val status = episode.userStatus?.lowercase(Locale.ROOT).orEmpty()
                val matchWatching = isWatchingFilterActive &&
                    (status.contains("watch") || status.contains("current"))
                val matchPlan = isPlanToWatchFilterActive &&
                    (status.contains("plan") || status.contains("planning"))
                matchWatching || matchPlan
            }
        }
    }

    // 3. Group episodes into 8 days (Monday = 1..Sunday = 7, Others = 8)
    val dayGroups = remember(filteredEpisodes, systemZone, todayDayOfWeek) {
        val buckets = Array(8) { mutableListOf<AiringEpisode>() }
        for (episode in filteredEpisodes) {
            val dow = episode.getDayOfWeek(systemZone)
            if (dow != null) {
                buckets[dow.value - 1].add(episode)
            } else {
                buckets[7].add(episode)
            }
        }
        // Sort each day chronologically by airingAt
        for (bucket in buckets) {
            bucket.sortBy { it.airingAt }
        }

        listOf(
            DayScheduleGroup(0, "Monday", todayDayOfWeek == DayOfWeek.MONDAY, buckets[0]),
            DayScheduleGroup(1, "Tuesday", todayDayOfWeek == DayOfWeek.TUESDAY, buckets[1]),
            DayScheduleGroup(2, "Wednesday", todayDayOfWeek == DayOfWeek.WEDNESDAY, buckets[2]),
            DayScheduleGroup(3, "Thursday", todayDayOfWeek == DayOfWeek.THURSDAY, buckets[3]),
            DayScheduleGroup(4, "Friday", todayDayOfWeek == DayOfWeek.FRIDAY, buckets[4]),
            DayScheduleGroup(5, "Saturday", todayDayOfWeek == DayOfWeek.SATURDAY, buckets[5]),
            DayScheduleGroup(6, "Sunday", todayDayOfWeek == DayOfWeek.SUNDAY, buckets[6]),
            DayScheduleGroup(7, "Others", false, buckets[7]),
        )
    }

    // 4. Weekday Selector Tab State (defaulting to current day)
    var selectedWeekdayTabIndex by remember { mutableIntStateOf(todayWeekdayIndex) }

    // 5. Day Accordions Expanded State (all expanded by default, isOpen = true)
    val accordionExpandedState = remember {
        mutableStateMapOf<Int, Boolean>().apply {
            for (i in 0..7) put(i, true)
        }
    }

    // Add to Calendar Bottom Sheet Target
    var calendarTargetEpisode by remember { mutableStateOf<AiringEpisode?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Top Section: Weekday Selector Row (8 scrollable tabs)
            WeekdaySelectorRow(
                dayGroups = dayGroups,
                selectedTabIndex = selectedWeekdayTabIndex,
                todayIndex = todayWeekdayIndex,
                onSelectTab = { tabIndex ->
                    selectedWeekdayTabIndex = tabIndex
                    val targetDay = AiringDay.WEEKDAY_TABS.getOrElse(tabIndex) { AiringDay.TODAY }
                    onSelectDay?.invoke(targetDay)

                    // Ensure clicked day accordion is expanded and scroll to it
                    accordionExpandedState[tabIndex] = true
                    coroutineScope.launch {
                        val scrollTargetIndex = tabIndex * 2
                        lazyListState.animateScrollToItem(scrollTargetIndex.coerceAtLeast(0))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // Status Filter Row [ All | Watching | Plan to Watch ]
            StatusFilterRow(
                isAllSelected = isAllFilterActive,
                isWatchingSelected = isWatchingFilterActive,
                isPlanToWatchSelected = isPlanToWatchFilterActive,
                onToggleAll = {
                    isAllFilterActive = true
                    isWatchingFilterActive = false
                    isPlanToWatchFilterActive = false
                },
                onToggleWatching = {
                    val next = !isWatchingFilterActive
                    isWatchingFilterActive = next
                    if (next) {
                        isAllFilterActive = false
                    } else if (!isPlanToWatchFilterActive) {
                        isAllFilterActive = true
                    }
                },
                onTogglePlanToWatch = {
                    val next = !isPlanToWatchFilterActive
                    isPlanToWatchFilterActive = next
                    if (next) {
                        isAllFilterActive = false
                    } else if (!isWatchingFilterActive) {
                        isAllFilterActive = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )

            // "My library" filter chip (only shown when the caller can supply ids)
            if (onToggleLibraryOnly != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    FilterChip(
                        selected = libraryOnly,
                        onClick = onToggleLibraryOnly,
                        label = { Text(stringResource(tachiyomi.i18n.tail.TLMR.strings.airing_filter_my_library)) },
                    )
                }
            }

            // Main Timetable Content: Day Accordions
            if (filteredEpisodes.isEmpty()) {
                EmptyScheduleView(
                    isFilterActive = !isAllFilterActive,
                    onResetFilter = {
                        isAllFilterActive = true
                        isWatchingFilterActive = false
                        isPlanToWatchFilterActive = false
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                )
            } else {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                ) {
                    dayGroups.forEach { group ->
                        val isExpanded = accordionExpandedState[group.dayIndex] ?: true

                        // Accordion Header Item
                        item(key = "accordion_header_${group.dayIndex}") {
                            DayAccordionHeader(
                                title = "${group.dayName} (${group.episodes.size})",
                                isToday = group.isToday,
                                isExpanded = isExpanded,
                                onToggle = {
                                    accordionExpandedState[group.dayIndex] = !isExpanded
                                },
                            )
                        }

                        // Accordion Content Item
                        item(key = "accordion_content_${group.dayIndex}") {
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                DayAccordionContent(
                                    group = group,
                                    currentTimeSec = currentTimeSec,
                                    systemZone = systemZone,
                                    onEpisodeClick = onEpisodeClick,
                                    onWatchClick = onWatchClick,
                                    onCalendarClick = { episode ->
                                        calendarTargetEpisode = episode
                                    },
                                    onDividerTap = { nextEpisode ->
                                        coroutineScope.launch {
                                            val message = if (nextEpisode != null) {
                                                "Next Show: ${nextEpisode.title} at ${nextEpisode.getFormattedAiringTime(systemZone)}"
                                            } else {
                                                "No more scheduled broadcasts today"
                                            }
                                            val result = snackbarHostState.showSnackbar(
                                                message = message,
                                                actionLabel = if (nextEpisode != null) "View" else null,
                                                duration = SnackbarDuration.Short,
                                            )
                                            if (result == SnackbarResult.ActionPerformed && nextEpisode != null) {
                                                onEpisodeClick(nextEpisode)
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add to Calendar Action Bottom Sheet
    calendarTargetEpisode?.let { episode ->
        AddToCalendarSheet(
            episode = episode,
            systemZone = systemZone,
            onDismiss = { calendarTargetEpisode = null },
            onCalendarAdded = {
                calendarTargetEpisode = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Broadcast event added to calendar")
                }
            },
        )
    }
}

/**
 * Data structure grouping scheduled airing episodes by day index.
 *
 * @property dayIndex Zero-based weekday index (0 = Mon .. 6 = Sun, 7 = Others).
 * @property dayName Capitalized name of the weekday.
 * @property isToday True if this group matches the current device date.
 * @property episodes Chronologically sorted list of episodes for this day.
 */
private data class DayScheduleGroup(
    val dayIndex: Int,
    val dayName: String,
    val isToday: Boolean,
    val episodes: List<AiringEpisode>,
)

/**
 * 8-Weekday scrollable selector row mirroring DailyAL's `WeeklySchedulePage`.
 *
 * @param dayGroups List of 8 [DayScheduleGroup] instances.
 * @param selectedTabIndex Currently selected tab index (0..7).
 * @param todayIndex Today's weekday index (0..6).
 * @param onSelectTab Callback when a tab is selected.
 * @param modifier Composable layout modifier.
 */
@Composable
private fun WeekdaySelectorRow(
    dayGroups: List<DayScheduleGroup>,
    selectedTabIndex: Int,
    todayIndex: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dayGroups.forEach { group ->
            val isSelected = group.dayIndex == selectedTabIndex
            val isToday = group.dayIndex == todayIndex

            Surface(
                onClick = { onSelectTab(group.dayIndex) },
                shape = DS.radiusHero,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                tonalElevation = if (isSelected) 3.dp else 0.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isToday) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(6.dp),
                        ) {}
                    }

                    Text(
                        text = "${group.dayName} (${group.episodes.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/**
 * Quick status filter row replicating DailyAL's `_Filter` chips: `[ All | Watching | Plan to Watch ]`.
 *
 * @param isAllSelected Whether "All" filter is active.
 * @param isWatchingSelected Whether "Watching" filter is active.
 * @param isPlanToWatchSelected Whether "Plan to Watch" filter is active.
 * @param onToggleAll Callback to activate "All".
 * @param onToggleWatching Callback to toggle "Watching".
 * @param onTogglePlanToWatch Callback to toggle "Plan to Watch".
 * @param modifier Composable layout modifier.
 */
@Composable
private fun StatusFilterRow(
    isAllSelected: Boolean,
    isWatchingSelected: Boolean,
    isPlanToWatchSelected: Boolean,
    onToggleAll: () -> Unit,
    onToggleWatching: () -> Unit,
    onTogglePlanToWatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = isAllSelected,
            onClick = onToggleAll,
            label = { Text("All", fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal) },
            leadingIcon = if (isAllSelected) {
                { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )

        FilterChip(
            selected = isWatchingSelected,
            onClick = onToggleWatching,
            label = { Text("Watching", fontWeight = if (isWatchingSelected) FontWeight.Bold else FontWeight.Normal) },
            leadingIcon = if (isWatchingSelected) {
                { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )

        FilterChip(
            selected = isPlanToWatchSelected,
            onClick = onTogglePlanToWatch,
            label = { Text("Plan to Watch", fontWeight = if (isPlanToWatchSelected) FontWeight.Bold else FontWeight.Normal) },
            leadingIcon = if (isPlanToWatchSelected) {
                { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
    }
}

/**
 * Accordion header for a single day section, displaying title, item count, "TODAY" pill, and chevron.
 *
 * @param title Header title formatted as `${DayOfWeek} (${itemCount})`.
 * @param isToday True if this day matches the current device date.
 * @param isExpanded True if the accordion is currently open.
 * @param onToggle Callback to toggle open/collapsed state.
 * @param modifier Composable layout modifier.
 */
@Composable
private fun DayAccordionHeader(
    title: String,
    isToday: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "accordion_arrow_rotation",
    )

    Card(
        shape = DS.radiusCard,
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(DS.radiusCard)
            .clickable(onClick = onToggle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (isToday) {
                    Surface(
                        shape = DS.radiusSmall,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(
                            text = "TODAY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier
                    .size(22.dp)
                    .rotate(rotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Inner content container of an expanded day accordion.
 *
 * For today's timeline, automatically injects the [CurrentTimeDividerMarker]
 * between concluded past broadcasts and upcoming broadcasts.
 *
 * @param group The [DayScheduleGroup] for this day.
 * @param currentTimeSec Current epoch seconds from the live ticker.
 * @param systemZone System time zone for formatting times.
 * @param onEpisodeClick Callback when an episode tile is clicked.
 * @param onWatchClick Optional callback for play/watch shortcut.
 * @param onCalendarClick Callback to open Add to Calendar sheet.
 * @param onDividerTap Callback when the real-time clock divider is tapped.
 */
@Composable
private fun DayAccordionContent(
    group: DayScheduleGroup,
    currentTimeSec: Long,
    systemZone: ZoneId,
    onEpisodeClick: (AiringEpisode) -> Unit,
    onWatchClick: ((AiringEpisode) -> Unit)?,
    onCalendarClick: (AiringEpisode) -> Unit,
    onDividerTap: (AiringEpisode?) -> Unit,
) {
    if (group.episodes.isEmpty()) {
        Card(
            shape = DS.radiusMedium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            Text(
                text = "No scheduled broadcasts for ${group.dayName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!group.isToday) {
            // Non-today schedule: render episodes sequentially
            group.episodes.forEach { episode ->
                DailyALAiringListTile(
                    episode = episode,
                    currentTimeSec = currentTimeSec,
                    systemZone = systemZone,
                    onClick = { onEpisodeClick(episode) },
                    onWatchClick = onWatchClick?.let { { it(episode) } },
                    onCalendarClick = { onCalendarClick(episode) },
                )
            }
        } else {
            // Today's schedule: inject Current Time Divider Marker between past and upcoming shows
            val pastEpisodes = group.episodes.filter { it.airingAt > 0L && it.airingAt < currentTimeSec }
            val upcomingEpisodes = group.episodes.filter { it.airingAt <= 0L || it.airingAt >= currentTimeSec }
            val nextUpcomingShow = upcomingEpisodes.firstOrNull { it.airingAt >= currentTimeSec }

            // Render past shows
            pastEpisodes.forEach { episode ->
                DailyALAiringListTile(
                    episode = episode,
                    currentTimeSec = currentTimeSec,
                    systemZone = systemZone,
                    onClick = { onEpisodeClick(episode) },
                    onWatchClick = onWatchClick?.let { { it(episode) } },
                    onCalendarClick = { onCalendarClick(episode) },
                )
            }

            // Injected Current Time Divider Marker
            CurrentTimeDividerMarker(
                currentTimeSec = currentTimeSec,
                systemZone = systemZone,
                nextEpisode = nextUpcomingShow,
                onTap = { onDividerTap(nextUpcomingShow) },
            )

            // Render upcoming shows
            upcomingEpisodes.forEach { episode ->
                DailyALAiringListTile(
                    episode = episode,
                    currentTimeSec = currentTimeSec,
                    systemZone = systemZone,
                    onClick = { onEpisodeClick(episode) },
                    onWatchClick = onWatchClick?.let { { it(episode) } },
                    onCalendarClick = { onCalendarClick(episode) },
                )
            }
        }
    }
}

/**
 * Real-time timeline divider marker injected into today's schedule, mirroring DailyAL's `_buildCurrentDayTile`.
 *
 * Consists of a 2dp horizontal line centered with an elevated card displaying a live clock
 * e.g. `14:25` and timezone offset e.g. `(GMT +05:30)`. Tapping reveals the next upcoming show preview.
 *
 * @param currentTimeSec Current Unix epoch timestamp in seconds.
 * @param systemZone Target device time zone.
 * @param nextEpisode Next scheduled episode broadcast today, if any.
 * @param onTap Callback invoked when the divider card is tapped.
 * @param modifier Composable layout modifier.
 */
@Composable
fun CurrentTimeDividerMarker(
    currentTimeSec: Long,
    systemZone: ZoneId,
    nextEpisode: AiringEpisode?,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val liveClockText = remember(currentTimeSec) {
        val instant = Instant.ofEpochSecond(currentTimeSec)
        val localTime = instant.atZone(systemZone).toLocalTime()
        DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(localTime)
    }

    val timezoneOffsetString = remember(currentTimeSec, systemZone) {
        try {
            val offset = systemZone.rules.getOffset(Instant.ofEpochSecond(currentTimeSec))
            val totalSeconds = offset.totalSeconds
            val hours = totalSeconds / 3600
            val minutes = Math.abs((totalSeconds % 3600) / 60)
            val sign = if (totalSeconds >= 0) "+" else "-"
            "GMT %s%02d:%02d".format(sign, Math.abs(hours), minutes)
        } catch (_: Exception) {
            "GMT"
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 2dp horizontal divider line
        HorizontalDivider(
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth(),
        )

        // Centered real-time clock pill card
        Card(
            shape = DS.radiusHero,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .clip(DS.radiusHero)
                .clickable(onClick = onTap),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = "Current Time",
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text = liveClockText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = "($timezoneOffsetString)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        }
    }
}

/**
 * Airing list tile implementing DailyAL's row layout (`_buildAnimeListTile`).
 *
 * Features:
 * - Left column: 60x60 rounded cover avatar + capsule airing time pill (`14:30`) in capsule/shadow style.
 * - Right column: 16sp bold title, format pill, score badge, live countdown badge (`Ep X in Yd Zh Zm` or
 *   `Airing Now` during 30-min window), and quick actions (Play/Watch button + Add to Calendar alert button).
 *
 * @param episode The [AiringEpisode] broadcast item.
 * @param currentTimeSec Current epoch timestamp in seconds for reactive countdowns.
 * @param systemZone Device time zone for time formatting.
 * @param onClick Callback when the tile is clicked.
 * @param onWatchClick Optional callback for the play/watch button.
 * @param onCalendarClick Callback when the Add to Calendar alert button is clicked.
 * @param modifier Composable layout modifier.
 */
@Composable
fun DailyALAiringListTile(
    episode: AiringEpisode,
    currentTimeSec: Long,
    systemZone: ZoneId,
    onClick: () -> Unit,
    onWatchClick: (() -> Unit)?,
    onCalendarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val countdownText = episode.formatCountdown(currentTimeSec)
    val isAiringNow = episode.isAiringNow(currentTimeSec)
    val isAired = countdownText.startsWith("Aired", true)

    Card(
        shape = DS.radiusCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(DS.radiusCard)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Left Column: 60x60 rounded cover avatar + Airing time pill (14:30)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Card(
                    shape = DS.radiusMedium,
                    modifier = Modifier.size(60.dp),
                ) {
                    AsyncImage(
                        model = episode.coverUrl,
                        contentDescription = episode.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // Airing time pill in capsule / shadow style
                Surface(
                    shape = DS.radiusSmall,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 3.dp,
                    shadowElevation = 1.dp,
                ) {
                    Text(
                        text = episode.getFormattedAiringTime(systemZone),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            // Right Column: Title, Metadata, Countdown, and Quick Action Buttons
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Metadata Badges Row: Format, Score, User Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (episode.score > 0.0) {
                        Surface(
                            shape = DS.radiusSmall,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ) {
                            Text(
                                text = "★ ${String.format(Locale.US, "%.1f", if (episode.score > 10.0) episode.score / 10.0 else episode.score)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                        }
                    }

                    if (episode.format.isNotBlank()) {
                        Surface(
                            shape = DS.radiusSmall,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ) {
                            Text(
                                text = episode.format,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                        }
                    }

                    episode.userStatus?.let { status ->
                        Surface(
                            shape = DS.radiusSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ) {
                            Text(
                                text = status.replace("_", " ").uppercase(Locale.ROOT),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                // Countdown Badge and Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Live Countdown Badge
                    Surface(
                        shape = DS.radiusSmall,
                        color = when {
                            isAiringNow -> Color(0xFF2E7D32)
                            isAired -> MaterialTheme.colorScheme.surfaceContainerHigh
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = when {
                            isAiringNow -> Color.White
                            isAired -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    ) {
                        Text(
                            text = if (isAiringNow) "● Airing Now" else countdownText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }

                    // Quick Actions Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Quick Watch / Search Button
                        IconButton(
                            onClick = { onWatchClick?.invoke() },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = "Watch Episode",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }

                        // Add to Calendar Alert Button
                        IconButton(
                            onClick = onCalendarClick,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Add to Calendar",
                                modifier = Modifier.size(19.dp),
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Backwards-compatible item card composable for [AiringEpisode].
 *
 * @param episode The episode model.
 * @param onClick Tile click listener.
 * @param onWatchClick Quick watch button listener.
 * @param modifier Composable layout modifier.
 */
@Composable
fun AiringEpisodeItemCard(
    episode: AiringEpisode,
    onClick: () -> Unit,
    onWatchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DailyALAiringListTile(
        episode = episode,
        currentTimeSec = System.currentTimeMillis() / 1000L,
        systemZone = ZoneId.systemDefault(),
        onClick = onClick,
        onWatchClick = onWatchClick,
        onCalendarClick = onClick,
        modifier = modifier,
    )
}

/**
 * Add to Calendar action bottom sheet allowing the user to export the broadcast event
 * to the Android system calendar via [CalendarContract.Events.CONTENT_URI] or share
 * RFC 5545 `.ics` details.
 *
 * @param episode Broadcast [AiringEpisode] target.
 * @param systemZone Current time zone.
 * @param onDismiss Dismissal callback.
 * @param onCalendarAdded Confirmation callback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToCalendarSheet(
    episode: AiringEpisode,
    systemZone: ZoneId,
    onDismiss: () -> Unit,
    onCalendarAdded: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isWeeklyRecurring by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Add to Calendar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            }

            // Target Media Preview Card
            Card(
                shape = DS.radiusMedium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Card(
                        shape = DS.radiusSmall,
                        modifier = Modifier.size(50.dp),
                    ) {
                        AsyncImage(
                            model = episode.coverUrl,
                            contentDescription = episode.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = episode.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (episode.episode > 0) "Episode ${episode.episode}" else "Upcoming Release",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${episode.getFormattedAirDate(systemZone)} at ${episode.getFormattedAiringTime(systemZone)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Recurring Option Toggle
            Surface(
                shape = DS.radiusMedium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Weekly Recurring Event",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Repeat weekly for subsequent episodes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Switch(
                        checked = isWeeklyRecurring,
                        onCheckedChange = { isWeeklyRecurring = it },
                    )
                }
            }

            // Action 1: Add to System Calendar via Intent
            Button(
                onClick = {
                    insertCalendarEvent(
                        context = context,
                        episode = episode,
                        isRecurring = isWeeklyRecurring,
                        systemZone = systemZone,
                    )
                    onCalendarAdded()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = DS.radiusMedium,
            ) {
                Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add to Device Calendar", fontWeight = FontWeight.Bold)
            }

            // Action 2: Share standard RFC 5545 .ics Calendar Details
            OutlinedButton(
                onClick = {
                    shareIcsCalendar(
                        context = context,
                        episode = episode,
                        isRecurring = isWeeklyRecurring,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = DS.radiusMedium,
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share iCalendar (.ics)")
            }

            // Action 3: Copy Calendar Details to Clipboard
            OutlinedButton(
                onClick = {
                    val summaryText = buildString {
                        appendLine("Anime Broadcast: ${episode.title}")
                        if (episode.episode > 0) appendLine("Episode: ${episode.episode}")
                        appendLine("Date: ${episode.getFormattedAirDate(systemZone)}")
                        appendLine("Time: ${episode.getFormattedAiringTime(systemZone)}")
                        if (episode.summary.isNotBlank()) appendLine("Summary: ${episode.summary}")
                    }
                    clipboardManager.setText(AnnotatedString(summaryText))
                    onCalendarAdded()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = DS.radiusMedium,
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Broadcast Info")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Creates and launches an Android [Intent.ACTION_INSERT] to register the episode broadcast
 * in the device's default calendar application.
 */
private fun insertCalendarEvent(
    context: Context,
    episode: AiringEpisode,
    isRecurring: Boolean,
    systemZone: ZoneId,
) {
    val startMillis = if (episode.airingAt > 0L) {
        episode.airingAt * 1000L
    } else {
        System.currentTimeMillis()
    }
    val endMillis = startMillis + (AiringEpisode.AIRING_WINDOW_SECONDS * 1000L)
    val eventTitle = if (episode.episode > 0) {
        "Ep ${episode.episode} - ${episode.title}"
    } else {
        episode.title
    }

    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, eventTitle)
        putExtra(
            CalendarContract.Events.DESCRIPTION,
            "Broadcast reminder for $eventTitle\n\n${episode.summary}",
        )
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        putExtra(CalendarContract.Events.EVENT_TIMEZONE, systemZone.id)

        if (isRecurring) {
            val remainingEps = if (episode.totalEpisodes != null && episode.totalEpisodes > episode.episode) {
                episode.totalEpisodes - episode.episode + 1
            } else {
                12 // Standard anime cour length
            }
            putExtra(CalendarContract.Events.RRULE, "FREQ=WEEKLY;COUNT=$remainingEps")
        }
    }

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback if no calendar app is available
    }
}

/**
 * Shares standard RFC 5545 `.ics` content for this episode broadcast via Android's share chooser.
 */
private fun shareIcsCalendar(
    context: Context,
    episode: AiringEpisode,
    isRecurring: Boolean,
) {
    val recurringCount = if (isRecurring) {
        if (episode.totalEpisodes != null && episode.totalEpisodes > episode.episode) {
            episode.totalEpisodes - episode.episode + 1
        } else {
            12
        }
    } else null

    val icsContent = episode.generateIcsContent(recurringCount)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/calendar"
        putExtra(Intent.EXTRA_SUBJECT, "Calendar Event: ${episode.title}")
        putExtra(Intent.EXTRA_TEXT, icsContent)
    }

    try {
        context.startActivity(Intent.createChooser(sendIntent, "Share Airing Broadcast"))
    } catch (_: Exception) {
        // Ignored
    }
}

/**
 * Empty schedule view displayed when no episodes match current filter criteria.
 */
@Composable
private fun EmptyScheduleView(
    isFilterActive: Boolean,
    onResetFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isFilterActive) {
                "No airing broadcasts match the active filter"
            } else {
                "No airing episodes scheduled for this timetable"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (isFilterActive) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onResetFilter) {
                Text("Show All Broadcasts")
            }
        }
    }
}

