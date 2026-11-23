package eu.kanade.tachiyomi.ui.more

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import eu.kanade.presentation.design.DS
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.PlayerPermissionHelper
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import logcat.logcat
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.secondaryItemAlpha

/** A video (or audio file) indexed by MediaStore that can be opened in the player. */
data class LocalVideoItem(
    val uri: Uri,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val filePath: String?,
)

/**
 * Lists video files indexed by MediaStore so they can be played with the built-in
 * player, plus a SAF picker as a fallback for files MediaStore doesn't index.
 */
object LocalVideoScreen : Screen() {

    private val VIDEO_PROJECTION = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.DATA,
    )

    private fun queryLocalVideos(context: android.content.Context): List<LocalVideoItem> {
        val items = mutableListOf<LocalVideoItem>()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            VIDEO_PROJECTION,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = "${MediaStore.Video.Media.EXTERNAL_CONTENT_URI}/$id".toUri()
                items += LocalVideoItem(
                    uri = uri,
                    title = cursor.getString(nameCol) ?: "Video $id",
                    durationMs = cursor.getLong(durationCol),
                    sizeBytes = cursor.getLong(sizeCol),
                    filePath = cursor.getString(dataCol),
                )
            }
        }
        return items
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        val permissionRequiredText = stringResource(TLMR.strings.local_video_permission_required)

        var hasPermission by remember {
            mutableStateOf(PlayerPermissionHelper.hasVideoPlaybackPermission(context))
        }
        var refreshKey by remember { mutableStateOf(0) }
        var pendingPickAfterPermission by remember { mutableStateOf(false) }

        val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                context.startActivity(PlayerActivity.newFileIntent(context, uri))
            }
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            if (permissions.values.any { it }) {
                hasPermission = true
                if (pendingPickAfterPermission) {
                    pendingPickAfterPermission = false
                    filePicker.launch(arrayOf("video/*", "audio/*", "application/octet-stream"))
                }
            } else {
                context.toast(permissionRequiredText)
            }
        }

        val requestPermissions: (pickAfter: Boolean) -> Unit = { pickAfter ->
            pendingPickAfterPermission = pickAfter
            permissionLauncher.launch(PlayerPermissionHelper.getVideoPlaybackPermissions())
        }

        val videos by produceState<List<LocalVideoItem>?>(initialValue = null, hasPermission, refreshKey) {
            value = if (!hasPermission) {
                emptyList()
            } else {
                withContext(Dispatchers.IO) {
                    runCatching { queryLocalVideos(context) }
                        .onFailure { logcat(LogPriority.ERROR) { "Failed to query local videos: ${it.message}" } }
                        .getOrDefault(emptyList())
                }
            }
        }

        Scaffold(
            topBarScrollBehavior = scrollBehavior,
            topBar = { behavior ->
                AppBar(
                    title = stringResource(TLMR.strings.local_video_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = behavior,
                    actions = {
                        IconButton(onClick = { filePicker.launch(arrayOf("video/*", "audio/*", "application/octet-stream")) }) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = stringResource(TLMR.strings.local_video_pick_file))
                        }
                        IconButton(onClick = { refreshKey++ }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = stringResource(TLMR.strings.local_video_refresh))
                        }
                    },
                )
            },
        ) { contentPadding ->
            when {
                !hasPermission -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                            .padding(MaterialTheme.padding.large),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(MaterialTheme.padding.medium))
                        Text(
                            text = stringResource(TLMR.strings.local_video_permission_required),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(MaterialTheme.padding.medium))
                        Button(onClick = { requestPermissions(false) }) {
                            Text(stringResource(TLMR.strings.local_video_grant_permission))
                        }
                    }
                }

                videos == null -> {
                    LoadingScreen(modifier = Modifier.padding(contentPadding))
                }

                videos!!.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                            .padding(MaterialTheme.padding.large),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(TLMR.strings.local_video_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(MaterialTheme.padding.medium))
                        Button(onClick = { filePicker.launch(arrayOf("video/*", "audio/*", "application/octet-stream")) }) {
                            Text(stringResource(TLMR.strings.local_video_pick_file))
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                    ) {
                        items(videos!!, key = { it.uri.toString() }) { video ->
                            LocalVideoRow(video = video)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun LocalVideoRow(video: LocalVideoItem) {
        val context = LocalContext.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    context.startActivity(PlayerActivity.newFileIntent(context, video.uri, video.title))
                }
                .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            VideoThumbnail(video = video)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        if (video.durationMs > 0) {
                            append(DateUtils.formatElapsedTime(video.durationMs / 1000))
                            if (video.sizeBytes > 0) append("  •  ")
                        }
                        if (video.sizeBytes > 0) {
                            append(Formatter.formatShortFileSize(context, video.sizeBytes))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.secondaryItemAlpha(),
                )
            }
        }
    }

    @Composable
    private fun VideoThumbnail(video: LocalVideoItem) {
        val context = LocalContext.current
        val thumbnail by produceState<Bitmap?>(initialValue = null, video.uri) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        context.contentResolver.loadThumbnail(video.uri, Size(320, 180), null)
                    } else {
                        // Thumbnail generation below Q needs direct file access; skip it
                        null
                    }
                }.getOrNull()
            }
        }

        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 54.dp)
                .clip(DS.radiusSmall)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = thumbnail
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.VideoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
