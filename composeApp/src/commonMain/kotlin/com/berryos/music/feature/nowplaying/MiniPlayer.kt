package com.berryos.music.feature.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.berryos.music.core.designsystem.AlbumCover
import com.berryos.music.core.designsystem.MEDIUM_ALPHA
import com.berryos.music.core.model.Track
import com.berryos.music.core.playback.PlaybackPosition
import com.berryos.music.core.playback.PlaybackState
import com.berryos.music.core.playback.progress
import com.berryos.music.resources.Res
import com.berryos.music.resources.ic_pause
import com.berryos.music.resources.ic_play_arrow
import com.berryos.music.resources.ic_skip_next
import com.berryos.music.resources.next
import com.berryos.music.resources.pause
import com.berryos.music.resources.play
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MiniPlayer(
    state: PlaybackState,
    coverUrl: String?,
    playbackPosition: StateFlow<PlaybackPosition>,
    onClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    val track = state.currentTrack ?: return
    val position by playbackPosition.collectAsState()
    Surface(elevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MINI_PLAYER_HEIGHT)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumCover(coverUrl, Modifier.size(MINI_PLAYER_HEIGHT))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                LinearProgressIndicator(
                    progress = position.progress,
                    modifier = Modifier.fillMaxWidth(),
                )
                MiniPlayerBody(
                    track,
                    state.isPlayRequested,
                    onTogglePlayPause,
                    onNext,
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerBody(
    track: Track,
    isPlayRequested: Boolean,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = MEDIUM_ALPHA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onTogglePlayPause) {
            Icon(
                painter = painterResource(
                    if (isPlayRequested) Res.drawable.ic_pause else Res.drawable.ic_play_arrow,
                ),
                contentDescription = stringResource(
                    if (isPlayRequested) Res.string.pause else Res.string.play,
                ),
            )
        }
        IconButton(onClick = onNext) {
            Icon(
                painter = painterResource(Res.drawable.ic_skip_next),
                contentDescription = stringResource(Res.string.next),
            )
        }
    }
}

private val MINI_PLAYER_HEIGHT = 64.dp
