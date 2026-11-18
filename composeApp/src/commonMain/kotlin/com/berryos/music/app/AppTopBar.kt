package com.berryos.music.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import art.rippledesign.compose.rui.RTab
import art.rippledesign.compose.rui.RTabRow
import art.rippledesign.compose.rui.RText
import art.rippledesign.compose.rui.RTopAppBar
import com.berryos.music.resources.Res
import com.berryos.music.resources.back
import com.berryos.music.resources.cover_palette
import com.berryos.music.resources.ic_arrow_back
import com.berryos.music.resources.ic_menu
import com.berryos.music.resources.ic_more_vert
import com.berryos.music.resources.ic_save
import com.berryos.music.resources.menu
import com.berryos.music.resources.more_options
import com.berryos.music.resources.save
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AppTopBar(
    title: String?,
    onBack: (() -> Unit)?,
    onMenu: (() -> Unit)?,
    transparent: Boolean = false,
    contentColor: Color? = null,
    onCoverPalette: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    saveEnabled: Boolean = true,
    tabLabels: List<String> = emptyList(),
    selectedTabIndex: Int = 0,
    selectedTabPosition: Float = selectedTabIndex.toFloat(),
    onTabSelected: (Int) -> Unit = {},
    backIcon: DrawableResource = Res.drawable.ic_arrow_back,
    backLabel: StringResource = Res.string.back,
) {
    val back = onBack
    val menu = onMenu
    val navIcon: (@Composable () -> Unit)? = when {
        back != null -> {
            {
                IconButton(onClick = back) {
                    Icon(
                        painter = painterResource(backIcon),
                        contentDescription = stringResource(backLabel),
                    )
                }
            }
        }

        menu != null -> {
            {
                IconButton(onClick = menu) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_menu),
                        contentDescription = stringResource(Res.string.menu),
                    )
                }
            }
        }

        else -> null
    }
    val more: (@Composable () -> Unit)? = onCoverPalette?.let { openPalette ->
        { AppMoreMenu(onCoverPalette = openPalette) }
    }
    val hasTabs = tabLabels.isNotEmpty()
    val barElevation = if (hasTabs) 0.dp else AppBarDefaults.TopAppBarElevation
    val barBackground = if (transparent) Color.Transparent else MaterialTheme.colors.primarySurface
    val shadow = if (hasTabs && !transparent) {
        Modifier.shadow(AppBarDefaults.TopAppBarElevation)
    } else {
        Modifier
    }
    Column(modifier = shadow.background(barBackground)) {
        Toolbar(title, navIcon, onSave, saveEnabled, more, transparent, contentColor, barElevation)
        if (hasTabs) {
            TopBarTabs(tabLabels, selectedTabIndex, selectedTabPosition, onTabSelected)
        }
    }
}

@Composable
private fun Toolbar(
    title: String?,
    navigationIcon: @Composable (() -> Unit)?,
    onSave: (() -> Unit)?,
    saveEnabled: Boolean,
    more: (@Composable () -> Unit)?,
    transparent: Boolean,
    contentColor: Color?,
    elevation: Dp,
) {
    if (transparent) {
        RTopAppBar(
            title = { title?.let { Text(it) } },
            navigationIcon = navigationIcon,
            actions = {
                SaveAction(onSave, saveEnabled)
                more?.invoke()
            },
            backgroundColor = Color.Transparent,
            contentColor = contentColor ?: LocalContentColor.current,
            elevation = 0.dp,
        )
    } else {
        RTopAppBar(
            title = { title?.let { Text(it) } },
            navigationIcon = navigationIcon,
            actions = {
                SaveAction(onSave, saveEnabled)
                more?.invoke()
            },
            elevation = elevation,
        )
    }
}

@Composable
private fun TopBarTabs(labels: List<String>, selectedIndex: Int, selectedPosition: Float, onSelected: (Int) -> Unit) {
    RTabRow(
        selectedTabIndex = selectedIndex,
        selectedPosition = selectedPosition,
        modifier = Modifier.fillMaxWidth().clipToBounds(),
    ) {
        labels.forEachIndexed { index, label ->
            RTab(
                selected = index == selectedIndex,
                onClick = { onSelected(index) },
                text = { RText(label) },
            )
        }
    }
}

@Composable
private fun SaveAction(onSave: (() -> Unit)?, enabled: Boolean) {
    if (onSave == null) return
    IconButton(onClick = onSave, enabled = enabled) {
        CompositionLocalProvider(
            LocalContentAlpha provides if (enabled) ContentAlpha.high else ContentAlpha.disabled,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_save),
                contentDescription = stringResource(Res.string.save),
            )
        }
    }
}

@Composable
private fun AppMoreMenu(onCoverPalette: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(Res.drawable.ic_more_vert),
                contentDescription = stringResource(Res.string.more_options),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    onCoverPalette()
                },
            ) {
                Text(stringResource(Res.string.cover_palette))
            }
        }
    }
}
