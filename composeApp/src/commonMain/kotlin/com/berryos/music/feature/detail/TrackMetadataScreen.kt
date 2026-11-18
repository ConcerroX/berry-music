@file:Suppress("TooManyFunctions")

package com.berryos.music.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import art.rippledesign.compose.rui.LocalPageOwner
import art.rippledesign.compose.rui.reportPageScrollable
import com.berryos.music.core.datastore.AppSettingsRepository
import com.berryos.music.core.designsystem.LocalPageChromeRegistry
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.core.metadata.TagEntry
import com.berryos.music.core.metadata.TrackTagField
import com.berryos.music.core.metadata.rememberTrackTagWritePermission
import com.berryos.music.core.model.Track
import com.berryos.music.resources.Res
import com.berryos.music.resources.metadata
import com.berryos.music.resources.metadata_field_album
import com.berryos.music.resources.metadata_field_album_artist
import com.berryos.music.resources.metadata_field_artist
import com.berryos.music.resources.metadata_field_artwork
import com.berryos.music.resources.metadata_field_composer
import com.berryos.music.resources.metadata_field_date
import com.berryos.music.resources.metadata_field_disc_number
import com.berryos.music.resources.metadata_field_disc_total
import com.berryos.music.resources.metadata_field_filename
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

@Suppress("LongMethod")
@Composable
fun TrackMetadataScreen(track: Track?, settings: AppSettingsRepository, startInLyrics: Boolean = false) {
    val viewModel: TrackTagViewModel = koinViewModel()
    LaunchedEffect(track) { viewModel.open(track) }
    val state by viewModel.state.collectAsState()
    val appSettings by settings.settings.collectAsState()
    val appBarRegistry = LocalPageChromeRegistry.current
    val currentOwner = LocalPageOwner.current
    val owner = remember { currentOwner }
    val appBarState = remember(appBarRegistry, owner) {
        if (appBarRegistry == null || owner == null) null else appBarRegistry.state(owner).actions
    }
    val writePermission = rememberTrackTagWritePermission()
    var selectedTab by rememberSaveable {
        mutableIntStateOf(if (startInLyrics) TAB_LYRICS else TAB_METADATA)
    }
    val pagerState = rememberPagerState(initialPage = selectedTab, pageCount = { TAB_COUNT })
    val tabLabels = listOf(
        stringResource(Res.string.metadata),
        stringResource(Res.string.metadata_field_lyrics),
    )
    val canSave = (state as? TrackTagUiState.Ready)?.let { it.dirty && !it.saving } == true
    val tabPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
    val saveState = rememberUpdatedState<() -> Unit>({
        track?.let { writePermission.request(it) { viewModel.save() } }
    })
    DisposableEffect(appBarState) {
        appBarState?.onSave = { saveState.value() }
        appBarState?.onTabSelected = { selectedTab = it }
        onDispose {
            appBarState?.onSave = null
            appBarState?.saveEnabled = false
            appBarState?.tabLabels = emptyList()
            appBarState?.selectedTabIndex = 0
            appBarState?.selectedTabPosition = 0f
            appBarState?.onTabSelected = {}
        }
    }
    SideEffect {
        val bar = appBarState ?: return@SideEffect
        bar.saveEnabled = canSave
        bar.tabLabels = tabLabels
        bar.selectedTabIndex = selectedTab
        bar.selectedTabPosition = tabPosition
    }
    when (val current = state) {
        TrackTagUiState.Loading -> Box(Modifier.fillMaxSize())

        TrackTagUiState.Missing -> PlaceholderScreen(
            stringResource(Res.string.metadata),
            stringResource(Res.string.track_not_found),
        )

        TrackTagUiState.NotLocal -> PlaceholderScreen(
            stringResource(Res.string.metadata),
            stringResource(Res.string.metadata_only_local),
        )

        is TrackTagUiState.Error -> PlaceholderScreen(stringResource(Res.string.metadata), current.message)

        is TrackTagUiState.Ready -> TabbedContent(
            state = current,
            pagerState = pagerState,
            selectedTab = selectedTab,
            onSelectedTabChange = { selectedTab = it },
            showTechnicalDetails = appSettings.showTechnicalDetails,
            onFilenameEdit = viewModel::editFilename,
            onEdit = viewModel::edit,
        )
    }
}

@Composable
private fun TabbedContent(
    state: TrackTagUiState.Ready,
    pagerState: PagerState,
    selectedTab: Int,
    onSelectedTabChange: (Int) -> Unit,
    showTechnicalDetails: Boolean,
    onFilenameEdit: (String) -> Unit,
    onEdit: (TrackTagField, String) -> Unit,
) {
    LaunchedEffect(pagerState.currentPage) {
        onSelectedTabChange(pagerState.currentPage)
    }
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        key = { it },
    ) { page ->
        when (page) {
            TAB_METADATA -> TagInspectionList(
                state = state,
                showTechnicalDetails = showTechnicalDetails,
                onFilenameEdit = onFilenameEdit,
                onEdit = onEdit,
            )

            TAB_LYRICS -> LyricsEditor(
                state = state,
                showTechnicalDetails = showTechnicalDetails,
                onEdit = onEdit,
            )
        }
    }
}

@Composable
private fun TagInspectionList(
    state: TrackTagUiState.Ready,
    showTechnicalDetails: Boolean,
    onFilenameEdit: (String) -> Unit,
    onEdit: (TrackTagField, String) -> Unit,
) {
    val inspection = state.inspection
    val listState = rememberLazyListState()
    listState.reportPageScrollable()
    val entries = remember(inspection.entries) {
        inspection.entries.filterNot { it.field == TrackTagField.LYRICS }
    }
    val rows = remember(entries) { entries.distinctEditableFields().toRows() }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            TagTextField(
                label = stringResource(Res.string.metadata_field_filename),
                value = state.filename,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                editable = true,
                maxLines = 1,
                onValueChange = onFilenameEdit,
            )
        }
        if (showTechnicalDetails) {
            item {
                TagTextField(
                    label = stringResource(Res.string.metadata_tag_type),
                    value = inspection.tagType ?: stringResource(Res.string.metadata_no_tag),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    italic = true,
                )
            }
        }
        if (state.error != null) {
            item {
                Text(
                    text = state.error,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        items(rows) { row ->
            when (row) {
                is TagEntryRow.Single -> EntryRow(
                    entry = row.entry,
                    showTechnicalDetails = showTechnicalDetails,
                    editState = state,
                    onEdit = onEdit,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )

                is TagEntryRow.Pair -> EntryPairRow(row.first, row.second, showTechnicalDetails, state, onEdit)
            }
        }
    }
}

@Composable
private fun TagTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    italic: Boolean = false,
    editable: Boolean = false,
    maxLines: Int = FIELD_MAX_LINES,
    onValueChange: (String) -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = !editable,
        label = { Text(label) },
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.body1.copy(
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        ),
        modifier = modifier,
    )
}

@Composable
private fun LyricsEditor(
    state: TrackTagUiState.Ready,
    showTechnicalDetails: Boolean,
    onEdit: (TrackTagField, String) -> Unit,
) {
    val entry = state.inspection.entries.firstOrNull { it.field == TrackTagField.LYRICS }
    val label = entry?.let { entryLabel(it, showTechnicalDetails) }
        ?: stringResource(Res.string.metadata_field_lyrics)
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (state.error != null) {
            Text(text = state.error, color = MaterialTheme.colors.error)
        }
        TagTextField(
            label = label,
            value = state.edits[TrackTagField.LYRICS].orEmpty(),
            modifier = Modifier.fillMaxWidth().weight(1f),
            editable = true,
            maxLines = Int.MAX_VALUE,
            onValueChange = { onEdit(TrackTagField.LYRICS, it) },
        )
    }
}

@Composable
private fun EntryRow(
    entry: TagEntry,
    showTechnicalDetails: Boolean,
    editState: TrackTagUiState.Ready,
    onEdit: (TrackTagField, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val picture = entry.picture
    if (picture != null) {
        val typeText = stringResource(Res.string.metadata_picture_type, picture.type)
        val value = listOfNotNull(typeText, picture.mimeType, picture.description).joinToString(" · ")
        TagTextField(label = entryLabel(entry, showTechnicalDetails), value = value, modifier = modifier, italic = true)
    } else {
        val field = entry.field
        val editable = field != null
        TagTextField(
            label = entryLabel(entry, showTechnicalDetails),
            value = field?.let { editState.edits[it] } ?: entry.value.orEmpty(),
            modifier = modifier,
            editable = editable,
            onValueChange = { value -> field?.let { onEdit(it, value) } },
        )
    }
}

@Composable
private fun EntryPairRow(
    first: TagEntry,
    second: TagEntry,
    showTechnicalDetails: Boolean,
    editState: TrackTagUiState.Ready,
    onEdit: (TrackTagField, String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        EntryRow(first, showTechnicalDetails, editState, onEdit, Modifier.weight(1f))
        EntryRow(second, showTechnicalDetails, editState, onEdit, Modifier.weight(1f))
    }
}

private sealed interface TagEntryRow {
    data class Single(val entry: TagEntry) : TagEntryRow
    data class Pair(val first: TagEntry, val second: TagEntry) : TagEntryRow
}

private fun List<TagEntry>.toRows(): List<TagEntryRow> {
    val rows = ArrayList<TagEntryRow>(size)
    var index = 0
    while (index < size) {
        val entry = this[index]
        val partner = entry.field?.pairField()
        val next = getOrNull(index + 1)
        if (partner != null && next?.field == partner) {
            rows += TagEntryRow.Pair(entry, next)
            index += 2
        } else {
            rows += TagEntryRow.Single(entry)
            index += 1
        }
    }
    return rows
}

/** Editable fields already show one combined value from the edit state. */
private fun List<TagEntry>.distinctEditableFields(): List<TagEntry> {
    val seen = mutableSetOf<TrackTagField>()
    return filter { entry ->
        val field = entry.field
        field == null || entry.picture != null || seen.add(field)
    }
}

private fun TrackTagField.pairField(): TrackTagField? = when (this) {
    TrackTagField.TRACK_NUMBER -> TrackTagField.TRACK_TOTAL
    TrackTagField.DISC_NUMBER -> TrackTagField.DISC_TOTAL
    else -> null
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
private const val TAB_METADATA = 0
private const val TAB_LYRICS = 1
private const val TAB_COUNT = 2
