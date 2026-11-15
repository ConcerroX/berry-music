package com.berryos.music.core.designsystem

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import art.rippledesign.compose.rui.RTile
import art.rippledesign.compose.rui.RTileMode
import art.rippledesign.compose.rui.RTilePosition
import com.berryos.music.resources.Res
import com.berryos.music.resources.ic_more_vert
import com.berryos.music.resources.more_options
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Cover tile shared by the album and artist grids. */
@Composable
fun CoverTile(coverUrl: String?, title: String, text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val coverColors = rememberAlbumCoverColors(coverUrl)
    RTile(
        media = {
            AlbumCover(coverUrl, coverColors::onImage)
        },
        modifier = modifier,
        backgroundColor = coverColors.colors?.background ?: Color.Transparent,
        contentColor = coverColors.colors?.contentColor,
        supportingContentColor = coverColors.colors?.supportingColor,
        onClick = onClick,
        mode = RTileMode.Outside,
        position = RTilePosition.Footer,
        title = title,
        text = text,
        action = {
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(Res.drawable.ic_more_vert),
                    contentDescription = stringResource(Res.string.more_options),
                )
            }
        },
    )
}
