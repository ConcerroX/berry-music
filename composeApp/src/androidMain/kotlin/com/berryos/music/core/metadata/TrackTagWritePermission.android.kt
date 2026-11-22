package com.berryos.music.core.metadata

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberTrackTagWritePermission(): TrackTagWritePermission {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val callback = pending
        pending = null
        if (result.resultCode == Activity.RESULT_OK) callback?.invoke()
    }
    return remember(context, launcher) {
        TrackTagWritePermission { track, onGranted ->
            val mediaId = track.id.toLongOrNull()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && mediaId != null) {
                val request = MediaStore.createWriteRequest(context.contentResolver, listOf(audioUri(mediaId)))
                pending = onGranted
                launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            } else {
                onGranted()
            }
        }
    }
}
