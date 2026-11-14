package com.berryos.music.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun rememberDefaultCoverColors(): CoverColors? {
    var colors by remember { mutableStateOf<CoverColors?>(null) }
    LaunchedEffect(Unit) {
        colors = defaultCoverColorsOf()
    }
    return colors
}
