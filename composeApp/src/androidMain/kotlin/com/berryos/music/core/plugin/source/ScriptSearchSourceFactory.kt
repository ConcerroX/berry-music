package com.berryos.music.core.plugin.source

import com.berryos.music.core.database.entity.SourcePluginEntity
import com.berryos.music.core.plugin.i18n.PluginStrings
import com.berryos.music.core.plugin.locale.PluginLocalePreferences
import com.berryos.music.core.plugin.model.PluginManifest
import com.berryos.music.core.plugin.pluginJson
import com.berryos.music.core.plugin.registry.displayName
import com.berryos.music.quickjs.PluginJsRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ScriptSearchSourceFactory(private val runtimeFactory: (SourcePluginEntity, PluginStrings) -> PluginJsRuntime) {
    suspend fun create(plugin: SourcePluginEntity): ScriptSearchSource = withContext(Dispatchers.IO) {
        val pluginDir = File(plugin.pluginDir)
        val manifestFile = File(pluginDir, MANIFEST_FILE)
        val manifest = pluginJson.decodeFromString<PluginManifest>(manifestFile.readText())
        val strings = PluginStrings.load(pluginDir, manifest)
        val entryFile = File(pluginDir, plugin.entryFile.ifBlank { manifest.entry })
        val script = buildScript(pluginDir, entryFile, manifest)

        ScriptSearchSource(
            manifest = manifest,
            script = script,
            displayName = plugin.displayName,
            localizedManifest = { strings.snapshot(PluginLocalePreferences.preferences.value).localize(manifest) },
            customName = plugin.customName,
            runtimeFactory = { runtimeFactory(plugin, strings) },
        )
    }

    private companion object {
        const val MANIFEST_FILE = "manifest.json"
    }

    private fun buildScript(pluginDir: File, entryFile: File, manifest: PluginManifest): String {
        val includes = collectIncludes(pluginDir, manifest)
        return buildString {
            append(includeBootstrap(includes))
            append('\n')
            includes.forEach { appendInclude(this, it) }
            appendEntry(this, manifest.entry, entryFile)
        }
    }

    private fun collectIncludes(pluginDir: File, manifest: PluginManifest): List<IncludedScript> = manifest.includeDirs
        .asSequence()
        .map { includeDir -> includeDir to File(pluginDir, includeDir) }
        .filter { (_, dir) -> dir.isDirectory }
        .flatMap { (includeDir, dir) ->
            dir.walkTopDown()
                .filter { it.isFile && it.extension.equals("js", ignoreCase = true) }
                .sortedBy { it.relativeTo(dir).invariantSeparatorsPath }
                .map { file -> includeDir to file }
        }
        .map { (includeDir, file) ->
            val relativePath = file.relativeTo(File(pluginDir, includeDir)).invariantSeparatorsPath
            IncludedScript(
                path = "$includeDir/$relativePath",
                content = file.readText(),
            )
        }
        .toList()

    private fun includeBootstrap(includes: List<IncludedScript>): String {
        val includePathSetJson = pluginJson.encodeToString(includes.map { it.path }.toSet())
        return """
        (function() {
          var __lyricoDeclaredIncludes = $includePathSetJson;
          var __lyricoDeclaredIncludeMap = Object.create(null);

          __lyricoDeclaredIncludes.forEach(function(path) {
            __lyricoDeclaredIncludeMap[path] = true;
          });

          /*
           * All declared include files have already been concatenated into the same
           * script unit before source.js.
           *
           * Keep include(path) for compatibility with plugins that call it manually.
           * It validates the path, then becomes a no-op.
           */
          globalThis.include = function(path) {
            path = String(path || "");
            if (!Object.prototype.hasOwnProperty.call(__lyricoDeclaredIncludeMap, path)) {
              throw new Error("Include path is not declared in includeDirs: " + path);
            }
          };
        })();
        """.trimIndent()
    }

    private fun appendInclude(builder: StringBuilder, source: IncludedScript) {
        builder.append("\n;")
        builder.append("\n// ===== Platform include: ")
        builder.append(source.path)
        builder.append(" =====\n")
        builder.append(source.content)
        builder.append("\n//# sourceURL=")
        builder.append(source.path)
        builder.append('\n')
    }

    private fun appendEntry(builder: StringBuilder, entryName: String, entryFile: File) {
        builder.append("\n;")
        builder.append("\n// ===== Platform entry: ")
        builder.append(entryName)
        builder.append(" =====\n")
        builder.append(entryFile.readText())
        builder.append("\n//# sourceURL=")
        builder.append(entryName)
        builder.append('\n')
    }

    private data class IncludedScript(val path: String, val content: String)
}
