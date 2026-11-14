package com.berryos.music.feature.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.berryos.music.core.playback.PlaybackState
import com.berryos.music.resources.Res
import com.berryos.music.resources.close
import com.berryos.music.resources.ic_keyboard_arrow_down
import com.berryos.music.resources.ic_pause
import com.berryos.music.resources.ic_play_arrow
import com.berryos.music.resources.ic_skip_next
import com.berryos.music.resources.ic_skip_previous
import com.berryos.music.resources.next
import com.berryos.music.resources.now_playing
import com.berryos.music.resources.pause
import com.berryos.music.resources.play
import com.berryos.music.resources.playing
import com.berryos.music.resources.previous
import com.berryos.music.resources.queue
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlayingSheet(
    state: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val track = state.currentTrack ?: return
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(Res.drawable.ic_keyboard_arrow_down),
                    contentDescription = stringResource(Res.string.close),
                )
            }
            Text(stringResource(Res.string.now_playing), style = MaterialTheme.typography.subtitle1)
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(track.title, style = MaterialTheme.typography.h5)
            Text(track.artist, style = MaterialTheme.typography.body2)
            Row(horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_skip_previous),
                        contentDescription = stringResource(Res.string.previous),
                    )
                }
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        painter = painterResource(
                            if (state.isPlaying) Res.drawable.ic_pause else Res.drawable.ic_play_arrow,
                        ),
                        contentDescription = stringResource(
                            if (state.isPlaying) Res.string.pause else Res.string.play,
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
        Text(stringResource(Res.string.queue), style = MaterialTheme.typography.subtitle1)
        QueueList(state, onPlayQueueItem, Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun QueueList(state: PlaybackState, onPlayQueueItem: (Int) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        itemsIndexed(state.queue) { index, item ->
            ListItem(
                modifier = Modifier.clickable { onPlayQueueItem(index) },
                text = {
                    Text(
                        text = item.title,
                        color = if (index == state.currentIndex) {
                            MaterialTheme.colors.primary
                        } else {
                            Color.Unspecified
                        },
                    )
                },
                secondaryText = { Text(item.artist) },
                trailing = if (index == state.currentIndex) {
                    { Text(stringResource(Res.string.playing)) }
                } else {
                    null
                },
            )
        }
    }
}
