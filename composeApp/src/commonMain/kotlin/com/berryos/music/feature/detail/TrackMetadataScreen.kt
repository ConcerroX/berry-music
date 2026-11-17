package com.berryos.music.feature.detail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import art.rippledesign.compose.rui.reportPageScrollable
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.core.designsystem.formatDuration
import com.berryos.music.core.model.Track
import com.berryos.music.resources.Res
import com.berryos.music.resources.metadata
import com.berryos.music.resources.metadata_album
import com.berryos.music.resources.metadata_artist
import com.berryos.music.resources.metadata_duration
import com.berryos.music.resources.metadata_title
import com.berryos.music.resources.metadata_track_number
import com.berryos.music.resources.metadata_year
import com.berryos.music.resources.track_not_found
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TrackMetadataScreen(track: Track?) {
    if (track == null) {
        PlaceholderScreen(
            title = stringResource(Res.string.metadata),
            subtitle = stringResource(Res.string.track_not_found),
        )
        return
    }
    val fields = metadataFields(track)
    val listState = rememberLazyListState()
    listState.reportPageScrollable()
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(fields) { field ->
            MetadataItem(field.label, field.value)
        }
    }
}

private class MetadataField(val label: StringResource, val value: String)

private fun metadataFields(track: Track): List<MetadataField> = buildList {
    if (track.title.isNotBlank()) add(MetadataField(Res.string.metadata_title, track.title))
    if (track.artist.isNotBlank()) add(MetadataField(Res.string.metadata_artist, track.artist))
    if (track.album.isNotBlank()) add(MetadataField(Res.string.metadata_album, track.album))
    if (track.trackNumber > 0) add(MetadataField(Res.string.metadata_track_number, track.trackNumber.toString()))
    if (track.year > 0) add(MetadataField(Res.string.metadata_year, track.year.toString()))
    if (track.durationMs > 0) add(MetadataField(Res.string.metadata_duration, formatDuration(track.durationMs)))
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MetadataItem(label: StringResource, value: String) {
    ListItem(
        text = { Text(stringResource(label)) },
        secondaryText = { Text(value) },
    )
}
