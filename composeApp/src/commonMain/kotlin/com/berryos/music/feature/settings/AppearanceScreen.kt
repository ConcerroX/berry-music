package com.berryos.music.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.berryos.music.core.datastore.AppSettingsRepository
import com.berryos.music.resources.Res
import com.berryos.music.resources.collapse_player_on_pause
import com.berryos.music.resources.metadata_and_lyrics
import com.berryos.music.resources.show_technical_details
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppearanceScreen(settings: AppSettingsRepository) {
    val appSettings by settings.settings.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SwitchItem(
                label = stringResource(Res.string.collapse_player_on_pause),
                checked = appSettings.collapsePlayerOnPause,
                onCheckedChange = settings::setCollapsePlayerOnPause,
            )
        }
        item { SectionHeader(stringResource(Res.string.metadata_and_lyrics)) }
        item {
            SwitchItem(
                label = stringResource(Res.string.show_technical_details),
                checked = appSettings.showTechnicalDetails,
                onCheckedChange = settings::setShowTechnicalDetails,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.subtitle2,
        color = MaterialTheme.colors.primary,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SwitchItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        text = { Text(label) },
        trailing = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    )
}
