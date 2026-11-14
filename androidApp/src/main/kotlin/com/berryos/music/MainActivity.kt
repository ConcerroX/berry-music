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
import com.berryos.music.core.library.LibraryRepository
import com.berryos.music.core.library.LocalLibrarySource
import com.berryos.music.core.playback.MediaSessionPlaybackController

class MainActivity : ComponentActivity() {
    private var playback: MediaSessionPlaybackController? = null
    private var auth: AndroidLogtoAuthController? = null

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
        val repository = LibraryRepository(listOf(LocalLibrarySource(applicationContext)))
        val playbackController = MediaSessionPlaybackController(applicationContext)
        val authController = AndroidLogtoAuthController(this)
        playback = playbackController
        auth = authController
        setContent {
            App(repository, playbackController, authController)
        }
    }

    override fun onResume() {
        super.onResume()
        auth?.refreshAccount()
    }

    override fun onDestroy() {
        auth?.release()
        auth = null
        playback?.release()
        playback = null
        super.onDestroy()
    }
}
