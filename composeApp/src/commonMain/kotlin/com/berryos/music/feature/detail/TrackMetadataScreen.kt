package com.berryos.music.feature.detail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import art.rippledesign.compose.rui.reportPageScrollable
import com.berryos.music.core.datastore.AppSettingsRepository
import com.berryos.music.core.designsystem.LibraryLoading
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.core.metadata.TagEntry
import com.berryos.music.core.metadata.TrackTagField
import com.berryos.music.core.metadata.TrackTagInspection
import com.berryos.music.core.model.Track
import com.berryos.music.resources.Res
import com.berryos.music.resources.metadata
import com.berryos.music.resources.metadata_binary
import com.berryos.music.resources.metadata_field_album
import com.berryos.music.resources.metadata_field_album_artist
import com.berryos.music.resources.metadata_field_artist
import com.berryos.music.resources.metadata_field_artwork
import com.berryos.music.resources.metadata_field_composer
import com.berryos.music.resources.metadata_field_date
import com.berryos.music.resources.metadata_field_disc_number
import com.berryos.music.resources.metadata_field_disc_total
import com.berryos.music.resources.metadata_field_genre
import com.berryos.music.resources.metadata_field_language
import com.berryos.music.resources.metadata_field_lyricist
import com.berryos.music.resources.metadata_field_lyrics
import com.berryos.music.resources.metadata_field_rating
import com.berryos.music.resources.metadata_field_title
import com.berryos.music.resources.metadata_field_track_number
import com.berryos.music.resources.metadata_field_track_total
import com.berryos.music.resources.metadata_field_with_key
import com.berryos.music.resources.metadata_no_tag
import com.berryos.music.resources.metadata_only_local
import com.berryos.music.resources.metadata_picture_type
import com.berryos.music.resources.metadata_tag_type
import com.berryos.music.resources.track_not_found
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TrackMetadataScreen(track: Track?, settings: AppSettingsRepository) {
    val viewModel: TrackTagViewModel = koinViewModel()
    LaunchedEffect(track) { viewModel.open(track) }
    val state by viewModel.state.collectAsState()
    val appSettings by settings.settings.collectAsState()
    when (val current = state) {
        TrackTagUiState.Loading -> LibraryLoading()

        TrackTagUiState.Missing -> PlaceholderScreen(
            stringResource(Res.string.metadata),
            stringResource(Res.string.track_not_found),
        )

        TrackTagUiState.NotLocal -> PlaceholderScreen(
            stringResource(Res.string.metadata),
            stringResource(Res.string.metadata_only_local),
        )

        is TrackTagUiState.Error -> PlaceholderScreen(stringResource(Res.string.metadata), current.message)

        is TrackTagUiState.Ready -> TagInspectionList(current.inspection, appSettings.showTechnicalDetails)
    }
}

@Composable
private fun TagInspectionList(inspection: TrackTagInspection, showTechnicalDetails: Boolean) {
    val listState = rememberLazyListState()
    listState.reportPageScrollable()
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        if (showTechnicalDetails) {
            item {
                TagTextField(
                    label = stringResource(Res.string.metadata_tag_type),
                    value = inspection.tagType ?: stringResource(Res.string.metadata_no_tag),
                    italic = true,
                )
            }
        }
        items(inspection.entries) { entry -> EntryRow(entry, showTechnicalDetails) }
    }
}

@Composable
private fun TagTextField(label: String, value: String, modifier: Modifier = Modifier, italic: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        maxLines = FIELD_MAX_LINES,
        textStyle = MaterialTheme.typography.body1.copy(
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        ),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun EntryRow(entry: TagEntry, showTechnicalDetails: Boolean) {
    val picture = entry.picture
    if (picture != null) {
        val typeText = stringResource(Res.string.metadata_picture_type, picture.type)
        val value = listOfNotNull(typeText, picture.mimeType, picture.description).joinToString(" · ")
        TagTextField(label = entryLabel(entry, showTechnicalDetails), value = value, italic = true)
    } else {
        TagTextField(
            label = entryLabel(entry, showTechnicalDetails),
            value = entry.value ?: stringResource(Res.string.metadata_binary),
        )
    }
}

@Composable
private fun entryLabel(entry: TagEntry, showTechnicalDetails: Boolean): String {
    val readable = entry.field?.let { fieldLabel(it) } ?: return entry.key.orEmpty()
    val key = entry.key
    return if (showTechnicalDetails && key != null) {
        stringResource(Res.string.metadata_field_with_key, readable, key)
    } else {
        readable
    }
}

@Composable
private fun fieldLabel(field: TrackTagField): String = stringResource(FIELD_LABELS.getValue(field))

private val FIELD_LABELS = mapOf(
    TrackTagField.TITLE to Res.string.metadata_field_title,
    TrackTagField.ARTIST to Res.string.metadata_field_artist,
    TrackTagField.ALBUM to Res.string.metadata_field_album,
    TrackTagField.ALBUM_ARTIST to Res.string.metadata_field_album_artist,
    TrackTagField.COMPOSER to Res.string.metadata_field_composer,
    TrackTagField.LYRICIST to Res.string.metadata_field_lyricist,
    TrackTagField.GENRE to Res.string.metadata_field_genre,
    TrackTagField.LANGUAGE to Res.string.metadata_field_language,
    TrackTagField.DATE to Res.string.metadata_field_date,
    TrackTagField.RATING to Res.string.metadata_field_rating,
    TrackTagField.TRACK_NUMBER to Res.string.metadata_field_track_number,
    TrackTagField.TRACK_TOTAL to Res.string.metadata_field_track_total,
    TrackTagField.DISC_NUMBER to Res.string.metadata_field_disc_number,
    TrackTagField.DISC_TOTAL to Res.string.metadata_field_disc_total,
    TrackTagField.LYRICS to Res.string.metadata_field_lyrics,
    TrackTagField.ARTWORK to Res.string.metadata_field_artwork,
)

private const val FIELD_MAX_LINES = 4
