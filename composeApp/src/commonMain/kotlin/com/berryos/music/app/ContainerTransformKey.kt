package com.berryos.music.app

import androidx.compose.ui.graphics.Color
import art.rippledesign.compose.rui.container.RContainerTransformSpec
import com.berryos.music.core.designsystem.cachedCoverColors
import com.berryos.music.core.model.SourceKey

internal data class ContainerTransformKey(val kind: String, val source: SourceKey, val id: String)

internal fun albumTransformKey(source: SourceKey, albumId: String): ContainerTransformKey =
    ContainerTransformKey(kind = "album", source = source, id = albumId)

internal fun artistTransformKey(source: SourceKey, artistId: String): ContainerTransformKey =
    ContainerTransformKey(kind = "artist", source = source, id = artistId)

internal val ContainerTransformSpec = RContainerTransformSpec()

/** Uses the item's own background for the transforming surface instead of the theme surface. */
internal fun RContainerTransformSpec.withContainerColor(color: Color?): RContainerTransformSpec =
    if (color == null) this else copy(containerColor = color)

internal fun cachedCoverColorsOrNull(coverUrl: String?): Color? = coverUrl?.let(::cachedCoverColors)?.background
