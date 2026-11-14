package com.berryos.music.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val defaultColorsMutex = Mutex()
private var defaultColors: CoverColors? = null
private var defaultColorsLoaded = false

@Composable
fun rememberDefaultCoverColors(): CoverColors? {
    var colors by remember { mutableStateOf(if (defaultColorsLoaded) defaultColors else null) }
    LaunchedEffect(Unit) {
        defaultColorsMutex.withLock {
            if (!defaultColorsLoaded) {
                defaultColors = defaultCoverColorsOf()
                defaultColorsLoaded = true
            }
        }
        colors = defaultColors
    }
    return colors
}
