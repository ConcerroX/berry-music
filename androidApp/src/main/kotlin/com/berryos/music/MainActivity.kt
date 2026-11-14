package com.berryos.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.berryos.music.app.App
import com.berryos.music.core.auth.AndroidLogtoAuthController
import com.berryos.music.core.datastore.AppSettingsRepository
import com.berryos.music.core.library.LibraryRepository
import com.berryos.music.core.library.mediaStoreChanges
import com.berryos.music.core.playback.PlaybackController
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class MainActivity : ComponentActivity(), KoinComponent {
    private val auth: AndroidLogtoAuthController
        get() = get()

    private val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private val requestedPermissions: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(audioPermission, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(audioPermission)
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { showApp() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth.attach(this)
        val granted =
            ContextCompat.checkSelfPermission(this, audioPermission) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) {
            showApp()
        } else {
            permissionLauncher.launch(requestedPermissions)
        }
    }

    private fun showApp() {
        val repository = get<LibraryRepository>()
        val playbackController = get<PlaybackController>()
        val settings = get<AppSettingsRepository>()
        setContent {
            App(repository, playbackController, auth, mediaStoreChanges(applicationContext), settings)
        }
    }

    override fun onResume() {
        super.onResume()
        auth.attach(this)
        auth.refreshAccount()
    }

    override fun onDestroy() {
        auth.attach(null)
        super.onDestroy()
    }
}
