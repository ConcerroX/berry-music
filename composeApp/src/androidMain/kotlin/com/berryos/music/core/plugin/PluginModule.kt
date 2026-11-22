package com.berryos.music.core.plugin

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.berryos.music.core.plugin.install.PluginInstaller
import com.berryos.music.core.plugin.locale.PluginLocalePreferences
import com.berryos.music.core.plugin.registry.PluginRegistry
import com.berryos.music.core.plugin.registry.RoomPluginRegistry
import com.berryos.music.core.plugin.runtime.HostAppInfo
import com.berryos.music.core.plugin.runtime.QuickJsHostApi
import com.berryos.music.core.plugin.source.PluginSourceProvider
import com.berryos.music.core.plugin.source.ScriptSearchSourceFactory
import com.berryos.music.quickjs.QuickJsRuntime
import okhttp3.OkHttpClient
import org.koin.dsl.module
import java.io.File

val pluginModule = module {
    single<PluginRegistry> { RoomPluginRegistry(get()) }

    single {
        val context: Context = get()
        val okHttpClient: OkHttpClient = get()
        PluginLocalePreferences.initialize(context)
        ScriptSearchSourceFactory { plugin, strings ->
            QuickJsRuntime(
                hostApi = QuickJsHostApi(
                    appInfo = hostAppInfo(context),
                    okHttpClient = okHttpClient,
                    pluginId = plugin.id,
                    pluginStrings = strings,
                    cacheRootDir = File(context.cacheDir, "plugin_cache"),
                ),
            )
        }
    }

    single { PluginSourceProvider(registry = get(), factory = get()) }

    single { PluginInstaller(context = get(), registry = get()) }
}

private fun hostAppInfo(context: Context): HostAppInfo {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    return HostAppInfo(
        name = context.applicationInfo.loadLabel(context.packageManager).toString(),
        packageName = context.packageName,
        versionName = info.versionName ?: "0.0.0",
        versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        },
        buildType = if (debuggable) "debug" else "release",
        debug = debuggable,
    )
}
