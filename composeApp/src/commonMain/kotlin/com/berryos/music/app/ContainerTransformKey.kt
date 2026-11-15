package com.berryos.music.app

import art.rippledesign.compose.rui.container.RContainerTransformSpec
import com.berryos.music.core.model.SourceKey

internal data class ContainerTransformKey(val kind: String, val source: SourceKey, val id: String)

internal fun albumTransformKey(source: SourceKey, albumId: String): ContainerTransformKey =
    ContainerTransformKey(kind = "album", source = source, id = albumId)

internal fun artistTransformKey(source: SourceKey, artistId: String): ContainerTransformKey =
    ContainerTransformKey(kind = "artist", source = source, id = artistId)

internal val ContainerTransformSpec = RContainerTransformSpec()
