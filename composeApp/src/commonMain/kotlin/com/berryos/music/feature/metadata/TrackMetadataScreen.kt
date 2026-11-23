@file:Suppress("TooManyFunctions")

package com.berryos.music.feature.metadata

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import art.rippledesign.compose.rui.RChipInput
import art.rippledesign.compose.rui.ROutlinedTextField
import art.rippledesign.compose.rui.pager.RHorizontalPager
import com.berryos.music.core.datastore.AppSettingsRepository
import com.berryos.music.core.designsystem.PlaceholderScreen
import com.berryos.music.core.metadata.TagEntry
import com.berryos.music.core.metadata.TrackTagField
import com.berryos.music.core.metadata.rememberTrackTagWritePermission
import com.berryos.music.core.model.Track
import com.berryos.music.resources.Res
import com.berryos.music.resources.default_album_art
import com.berryos.music.resources.metadata
import com.berryos.music.resources.metadata_field_album
import com.berryos.music.resources.metadata_field_album_artist
import com.berryos.music.resources.metadata_field_arranger
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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Suppress("LongMethod")
@Composable
fun TrackMetadataScreen(
    track: Track?,
    settings: AppSettingsRepository,
    artistSuggestions: List<String> = emptyList(),
    startInLyrics: Boolean = false,
    topBar: @Composable (MetadataTopBarState) -> Unit,
) {
    val viewModel: TrackTagViewModel = koinViewModel()
    LaunchedEffect(track) { viewModel.open(track) }
    val state by viewModel.state.collectAsState()
    val appSettings by settings.settings.collectAsState()
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
    val topBarState = MetadataTopBarState(
        onSave = { track?.let { writePermission.request(it) { viewModel.save() } } },
        saveEnabled = canSave,
        tabLabels = tabLabels,
        selectedTabIndex = selectedTab,
        selectedTabPosition = tabPosition,
        onTabSelected = { selectedTab = it },
    )
    Column(modifier = Modifier.fillMaxSize()) {
        topBar(topBarState)
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
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
                    artistSuggestions = artistSuggestions,
                    onFilenameEdit = viewModel::editFilename,
                    onPeopleChange = viewModel::editPeople,
                    onEdit = viewModel::edit,
                )
            }
        }
    }
}

@Composable
private fun TabbedContent(
    state: TrackTagUiState.Ready,
    pagerState: PagerState,
    selectedTab: Int,
    onSelectedTabChange: (Int) -> Unit,
    showTechnicalDetails: Boolean,
    artistSuggestions: List<String>,
    onFilenameEdit: (String) -> Unit,
    onPeopleChange: (TrackTagField, List<String>) -> Unit,
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
    RHorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        key = { it },
    ) { page ->
        when (page) {
            TAB_METADATA -> TagInspectionList(
                state = state,
                showTechnicalDetails = showTechnicalDetails,
                artistSuggestions = artistSuggestions,
                onFilenameEdit = onFilenameEdit,
                onPeopleChange = onPeopleChange,
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
    artistSuggestions: List<String>,
    onFilenameEdit: (String) -> Unit,
    onPeopleChange: (TrackTagField, List<String>) -> Unit,
    onEdit: (TrackTagField, String) -> Unit,
) {
    val inspection = state.inspection
    val listState = rememberLazyListState()
    val entries = remember(inspection.entries) {
        inspection.entries.filterNot { it.field == TrackTagField.LYRICS }
    }
    val rows = remember(entries) { entries.distinctEditableFields().toRows() }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            val filenameState = rememberSyncedTagField(state.filename, onFilenameEdit)
            TagTextField(
                label = stringResource(Res.string.metadata_field_filename),
                state = filenameState,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                lineLimits = TextFieldLineLimits.SingleLine,
            )
        }
        if (showTechnicalDetails) {
            item {
                val tagType = inspection.tagType ?: stringResource(Res.string.metadata_no_tag)
                val tagTypeState = rememberTextFieldState(tagType)
                TagTextField(
                    label = stringResource(Res.string.metadata_tag_type),
                    state = tagTypeState,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    italic = true,
                    readOnly = true,
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
        items(rows, key = { it.key() }) { row ->
            val rowField = (row as? TagEntryRow.Single)?.entry?.field
            val personEntry = (row as? TagEntryRow.Single)?.entry
                ?.takeIf { it.field in PERSON_FIELDS && it.picture == null }
            when {
                personEntry != null && rowField != null -> PersonChipField(
                    label = entryLabel(personEntry, showTechnicalDetails),
                    values = state.people[rowField].orEmpty(),
                    suggestions = artistSuggestions,
                    onValuesChange = { values -> onPeopleChange(rowField, values) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )

                row is TagEntryRow.Single -> EntryRow(
                    entry = row.entry,
                    showTechnicalDetails = showTechnicalDetails,
                    editState = state,
                    onEdit = onEdit,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )

                row is TagEntryRow.Pair -> EntryPairRow(row.first, row.second, showTechnicalDetails, state, onEdit)
            }
        }
    }
}

@Composable
private fun PersonChipField(
    label: String,
    values: List<String>,
    suggestions: List<String>,
    onValuesChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    RChipInput(
        values = values,
        onValuesChange = onValuesChange,
        label = { Text(label) },
        modifier = modifier,
        chipLeadingIcon = {
            Image(
                painter = painterResource(Res.drawable.default_album_art),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        },
        suggestions = { query ->
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                emptyList()
            } else {
                suggestions
                    .filter { it.contains(trimmed, ignoreCase = true) }
                    .filterNot { candidate ->
                        values.any { it.equals(candidate, ignoreCase = true) }
                    }
                    .sortedWith(
                        compareByDescending<String> { it.startsWith(trimmed, ignoreCase = true) }
                            .thenBy { it.lowercase() },
                    )
                    .take(MAX_PERSON_SUGGESTIONS)
            }
        },
        suggestionContent = { suggestion, query ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(Res.drawable.default_album_art),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                )
                Spacer(Modifier.width(24.dp))
                Text(
                    text = highlightedArtistName(suggestion, query),
                    style = MaterialTheme.typography.body1,
                )
            }
        },
    )
}

private fun highlightedArtistName(name: String, query: String): AnnotatedString {
    val trimmed = query.trim()
    val start = if (trimmed.isEmpty()) -1 else name.indexOf(trimmed, ignoreCase = true)
    return if (start < 0) {
        AnnotatedString(name)
    } else {
        buildAnnotatedString {
            append(name)
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, start + trimmed.length)
        }
    }
}

@Composable
private fun TagTextField(
    label: String,
    state: TextFieldState,
    modifier: Modifier = Modifier,
    italic: Boolean = false,
    readOnly: Boolean = false,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = FIELD_MAX_LINES),
) {
    ROutlinedTextField(
        state = state,
        readOnly = readOnly,
        label = { Text(label) },
        lineLimits = lineLimits,
        textStyle = MaterialTheme.typography.body1.copy(
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        ),
        modifier = modifier,
    )
}

/** Keeps a text field's state in sync with the view model's string value. */
@Composable
private fun rememberSyncedTagField(value: String, onValueChange: (String) -> Unit): TextFieldState {
    val state = rememberTextFieldState(value)
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect(onValueChange)
    }
    LaunchedEffect(value) {
        if (state.text.toString() != value) {
            state.setTextAndPlaceCursorAtEnd(value)
        }
    }
    return state
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
    val lyricsState = rememberSyncedTagField(
        value = state.edits[TrackTagField.LYRICS].orEmpty(),
        onValueChange = { onEdit(TrackTagField.LYRICS, it) },
    )
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (state.error != null) {
            Text(text = state.error, color = MaterialTheme.colors.error)
        }
        TagTextField(
            label = label,
            state = lyricsState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            lineLimits = TextFieldLineLimits.MultiLine(),
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
        val textState = rememberTextFieldState(value)
        TagTextField(
            label = entryLabel(entry, showTechnicalDetails),
            state = textState,
            modifier = modifier,
            italic = true,
            readOnly = true,
        )
    } else {
        val field = entry.field
        val textState =
            if (field != null) {
                rememberSyncedTagField(
                    value = editState.edits[field].orEmpty(),
                    onValueChange = { onEdit(field, it) },
                )
            } else {
                rememberTextFieldState(entry.value.orEmpty())
            }
        TagTextField(
            label = entryLabel(entry, showTechnicalDetails),
            state = textState,
            modifier = modifier,
            readOnly = field == null,
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

private fun TagEntryRow.key(): String = when (this) {
    is TagEntryRow.Single -> entry.field?.name ?: entry.key ?: entry.value.orEmpty()
    is TagEntryRow.Pair -> first.field?.name ?: first.key.orEmpty()
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
    TrackTagField.ARRANGER to Res.string.metadata_field_arranger,
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
private const val MAX_PERSON_SUGGESTIONS = 8

private val PERSON_FIELDS = setOf(
    TrackTagField.ARTIST,
    TrackTagField.COMPOSER,
    TrackTagField.ALBUM_ARTIST,
    TrackTagField.LYRICIST,
    TrackTagField.ARRANGER,
)
private const val TAB_METADATA = 0
private const val TAB_LYRICS = 1
private const val TAB_COUNT = 2
