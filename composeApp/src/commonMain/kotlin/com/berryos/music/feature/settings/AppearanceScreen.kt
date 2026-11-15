package com.berryos.music.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.berryos.music.core.datastore.AppSettingsRepository
import com.berryos.music.resources.Res
import com.berryos.music.resources.collapse_player_on_pause
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
    }
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
