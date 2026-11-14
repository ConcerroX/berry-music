package com.berryos.music.core.designsystem

import androidx.compose.runtime.Composable
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.allowHardware

@Composable
actual fun coverImageModel(model: Any): Any = ImageRequest.Builder(LocalPlatformContext.current)
    .data(model)
    .allowHardware(false)
    .build()
