package com.berryos.music.app

import androidx.compose.foundation.layout.Box
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import art.rippledesign.compose.rui.RTopAppBar
import com.berryos.music.resources.Res
import com.berryos.music.resources.back
import com.berryos.music.resources.cover_palette
import com.berryos.music.resources.ic_arrow_back
import com.berryos.music.resources.ic_menu
import com.berryos.music.resources.ic_more_vert
import com.berryos.music.resources.menu
import com.berryos.music.resources.more_options
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
    if (transparent) {
        RTopAppBar(
            title = { title?.let { Text(it) } },
            navigationIcon = navIcon,
            actions = { more?.invoke() },
            backgroundColor = Color.Transparent,
            contentColor = contentColor ?: LocalContentColor.current,
            elevation = 0.dp,
        )
    } else {
        RTopAppBar(
            title = { title?.let { Text(it) } },
            navigationIcon = navIcon,
            actions = { more?.invoke() },
        )
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
