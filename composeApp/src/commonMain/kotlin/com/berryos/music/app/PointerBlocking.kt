package com.berryos.music.app

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/** Consumes every pointer event so content underneath cannot react while this layer is shown. */
internal fun Modifier.consumeAllPointerInput(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}
