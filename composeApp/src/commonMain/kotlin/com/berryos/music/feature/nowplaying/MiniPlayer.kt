package com.berryos.music.feature.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.berryos.music.core.playback.PlaybackState
import com.berryos.music.resources.Res
import com.berryos.music.resources.ic_pause
import com.berryos.music.resources.ic_play_arrow
import com.berryos.music.resources.ic_skip_next
import org.jetbrains.compose.resources.painterResource

@Composable
fun MiniPlayer(state: PlaybackState, onClick: () -> Unit, onTogglePlayPause: () -> Unit, onNext: () -> Unit) {
    val track = state.currentTrack ?: return
    Surface(elevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.subtitle1, maxLines = 1)
                Text(track.artist, style = MaterialTheme.typography.caption, maxLines = 1)
            }
            IconButton(onClick = onTogglePlayPause) {
                Icon(
                    painter = painterResource(
                        if (state.isPlaying) Res.drawable.ic_pause else Res.drawable.ic_play_arrow,
                    ),
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    painter = painterResource(Res.drawable.ic_skip_next),
                    contentDescription = "Next",
                )
            }
        }
    }
}
