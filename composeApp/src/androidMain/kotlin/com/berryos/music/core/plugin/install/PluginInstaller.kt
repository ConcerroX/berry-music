package com.berryos.music.core.plugin.install

import android.content.Context
import android.net.Uri
import android.util.Log
import com.berryos.music.core.database.entity.SourcePluginEntity
import com.berryos.music.core.plugin.contract.HostApiVersions
import com.berryos.music.core.plugin.i18n.PluginStrings
import com.berryos.music.core.plugin.model.PluginManifest
import com.berryos.music.core.plugin.pluginJson
import com.berryos.music.core.plugin.registry.PluginRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

sealed interface PluginInstallResult {
    data class Success(val plugin: SourcePluginEntity, val replaced: Boolean) : PluginInstallResult

    data class Failure(val fileName: String, val reason: String, val cause: Throwable? = null) : PluginInstallResult
}

/**
 * Imports Lyrico source plugins from a local ZIP.
 *
 * The archive is validated and extracted into a staging directory first; each plugin is then
 * published to its own immutable directory before the registry row is written.
 */
@Suppress("TooGenericExceptionCaught")
class PluginInstaller(context: Context, private val registry: PluginRegistry) {
    private val appContext = context.applicationContext
    private val pluginsRoot = File(appContext.filesDir, "plugins/sources")
    private val stagingRoot = File(appContext.cacheDir, "plugin-install")
    private val installMutex = Mutex()

    suspend fun install(uri: Uri): List<PluginInstallResult> = withContext(Dispatchers.IO) {
        installMutex.withLock { installFromUri(uri) }
    }

    private suspend fun installFromUri(uri: Uri): List<PluginInstallResult> {
        val displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "plugin.zip"
        val archive = File.createTempFile("plugin-", ".zip", stagingRoot.apply { mkdirs() })
        return try {
            copyArchive(uri, archive)
            installArchiveLocked(archive, displayName)
        } catch (throwable: Exception) {
            Log.w(TAG, "Plugin import failed: $displayName", throwable)
            listOf(PluginInstallResult.Failure(displayName, throwable.message ?: "Import failed", throwable))
        } finally {
            archive.delete()
        }
    }

    private fun copyArchive(uri: Uri, archive: File) {
        val source = appContext.contentResolver.openInputStream(uri) ?: error("Unable to open archive")
        source.use { input ->
            archive.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var copied = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    copied += read
                    require(copied <= MAX_ARCHIVE_BYTES) { "Archive is too large" }
                    output.write(buffer, 0, read)
                }
            }
        }
    }

    suspend fun installArchive(archive: File, displayName: String): List<PluginInstallResult> =
        withContext(Dispatchers.IO) {
            installMutex.withLock { installArchiveLocked(archive, displayName) }
        }

    private suspend fun installArchiveLocked(archive: File, displayName: String): List<PluginInstallResult> {
        require(archive.length() <= MAX_ARCHIVE_BYTES) { "Archive is too large" }
        val staging = File(stagingRoot, "staging-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            extract(archive, staging)
            val pluginRoots = findPluginRoots(staging)
            return when {
                pluginRoots.isEmpty() -> listOf(
                    PluginInstallResult.Failure(displayName, "No manifest.json found"),
                )

                pluginRoots.size > MAX_PLUGINS -> listOf(
                    PluginInstallResult.Failure(displayName, "Too many plugins in one archive"),
                )

                else -> pluginRoots.map { installPlugin(it, pluginRoots, displayName) }
            }
        } catch (throwable: Exception) {
            Log.w(TAG, "Plugin install failed: $displayName", throwable)
            return listOf(PluginInstallResult.Failure(displayName, throwable.message ?: "Install failed", throwable))
        } finally {
            staging.deleteRecursively()
        }
    }

    private suspend fun installPlugin(root: File, allRoots: List<File>, archiveName: String): PluginInstallResult {
        val manifestFile = File(root, MANIFEST_FILE)
        return try {
            require(manifestFile.length() <= MAX_MANIFEST_BYTES) { "manifest.json is too large" }
            val manifest = pluginJson.decodeFromString<PluginManifest>(manifestFile.readText())
            validate(manifest, root)

            val existing = registry.getPlugin(manifest.id)
            if (existing != null && manifest.versionCode < existing.versionCode) {
                return PluginInstallResult.Failure(
                    "$archiveName:${manifest.id}",
                    "Plugin ${manifest.id} would be downgraded (${existing.versionCode} -> ${manifest.versionCode})",
                )
            }

            val finalDir = File(File(pluginsRoot, manifest.id), UUID.randomUUID().toString())
            try {
                copyPlugin(root, allRoots, finalDir)
                val entity = manifest.toEntity(finalDir, existing)
                registry.upsert(entity)
                if (existing != null) File(existing.pluginDir).deleteRecursively()
                PluginInstallResult.Success(entity, replaced = existing != null)
            } catch (throwable: Exception) {
                finalDir.deleteRecursively()
                throw throwable
            }
        } catch (throwable: Exception) {
            PluginInstallResult.Failure(
                "$archiveName:${root.name}",
                throwable.message ?: "Invalid plugin ${root.name}",
                throwable,
            )
        }
    }

    private fun validate(manifest: PluginManifest, root: File) {
        require(PLUGIN_ID_PATTERN.matches(manifest.id)) { "Invalid plugin id: ${manifest.id}" }
        require(manifest.name.isNotBlank()) { "Plugin name is blank" }
        require(manifest.versionCode >= 1) { "Invalid versionCode" }
        require(manifest.versionName.isNotBlank()) { "Invalid versionName" }
        require(HostApiVersions.supportsPluginApiVersion(manifest.apiVersion)) {
            "Unsupported apiVersion ${manifest.apiVersion}"
        }
        require(HostApiVersions.supportsHostApiVersion(manifest.minHostApiVersion)) {
            "Unsupported minHostApiVersion ${manifest.minHostApiVersion}"
        }
        val entry = safeChild(root, manifest.entry)
        require(entry.isFile && entry.extension == "js" && entry.length() <= MAX_ENTRY_BYTES) {
            "Invalid entry script: ${manifest.entry}"
        }
        manifest.includeDirs.forEach { include ->
            val dir = safeChild(root, include)
            require(dir.isDirectory) { "Missing include directory: $include" }
        }
        manifest.icon?.let { icon ->
            val file = safeChild(root, icon)
            require(file.isFile && file.extension.lowercase() in ICON_EXTENSIONS) { "Invalid icon: $icon" }
        }
        PluginStrings.load(root, manifest)
    }

    private fun safeChild(root: File, relative: String): File {
        require(relative.isNotBlank()) { "Unsafe path: $relative" }
        require(!relative.startsWith('/') && '\\' !in relative) { "Unsafe path: $relative" }
        require(relative.split('/').none { it.isEmpty() || it == ".." || it == "." }) {
            "Unsafe path: $relative"
        }
        val file = File(root, relative).canonicalFile
        require(file.toPath().startsWith(root.canonicalFile.toPath())) { "Path escapes plugin root: $relative" }
        return file
    }

    private fun copyPlugin(root: File, allRoots: List<File>, target: File) {
        val excluded = allRoots.filter {
            it != root && it.canonicalPath.startsWith(
                root.canonicalPath + File.separator,
            )
        }
            .map { it.canonicalPath }
        var total = 0L
        root.walkTopDown().forEach { file ->
            if (!file.isFile) return@forEach
            val path = file.canonicalPath
            if (excluded.any { path.startsWith(it + File.separator) }) return@forEach
            total += file.length()
            require(total <= MAX_PLUGIN_BYTES) { "Plugin directory is too large" }
            val relative = file.relativeTo(root)
            val destination = File(target, relative.path)
            destination.parentFile?.mkdirs()
            file.copyTo(destination, overwrite = true)
        }
        require(target.isDirectory) { "Unable to publish plugin directory" }
    }

    private fun extract(archive: File, target: File) {
        var files = 0
        var total = 0L
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                validateEntry(entry)
                if (entry.isDirectory) {
                    File(target, entry.name).mkdirs()
                } else {
                    files++
                    require(files <= MAX_FILES) { "Too many files in archive" }
                    total += extractEntry(zip, entry, target)
                    require(total <= MAX_ARCHIVE_BYTES) { "Archive is too large" }
                }
            }
        }
    }

    private fun validateEntry(entry: ZipEntry) {
        val name = entry.name
        require(validZipEntry(name)) { "Unsafe archive entry: $name" }
        require(name.split('/').size - 1 <= MAX_DEPTH) { "Archive entry is too deep: $name" }
    }

    private fun extractEntry(zip: ZipInputStream, entry: ZipEntry, target: File): Long {
        val destination = File(target, entry.name).canonicalFile
        require(destination.toPath().startsWith(target.canonicalFile.toPath())) {
            "Archive entry escapes target: ${entry.name}"
        }
        destination.parentFile?.mkdirs()
        var written = 0L
        destination.outputStream().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = zip.read(buffer)
                if (read < 0) break
                written += read
                require(written <= MAX_ARCHIVE_BYTES) { "Archive is too large" }
                output.write(buffer, 0, read)
            }
        }
        return written
    }

    private fun validZipEntry(name: String): Boolean {
        if (name.isBlank() || hasUnsafePathCharacters(name)) return false
        return name.split('/').none { it == ".." }
    }

    private fun hasUnsafePathCharacters(name: String): Boolean =
        '\u0000' in name || '\\' in name || name.startsWith('/')
    private fun findPluginRoots(root: File): List<File> = root.walkTopDown()
        .maxDepth(MAX_DEPTH)
        .filter { it.isFile && it.name == MANIFEST_FILE }
        .mapNotNull { it.parentFile }
        .toList()

    private companion object {
        const val TAG = "PluginInstaller"
        const val MANIFEST_FILE = "manifest.json"
        const val MAX_ARCHIVE_BYTES = 30L * 1024 * 1024
        const val MAX_PLUGIN_BYTES = 5L * 1024 * 1024
        const val MAX_MANIFEST_BYTES = 128L * 1024
        const val MAX_ENTRY_BYTES = 1024L * 1024
        const val MAX_FILES = 1000
        const val MAX_PLUGINS = 20
        const val MAX_DEPTH = 16
        val ICON_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")
        val PLUGIN_ID_PATTERN = Regex("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+")
    }
}

private fun PluginManifest.toEntity(dir: File, existing: SourcePluginEntity?): SourcePluginEntity {
    val now = System.currentTimeMillis()
    return SourcePluginEntity(
        id = id,
        name = name,
        versionCode = versionCode,
        versionName = versionName,
        apiVersion = apiVersion,
        minHostApiVersion = minHostApiVersion,
        author = author,
        description = description,
        pluginDir = dir.absolutePath,
        entryFile = entry,
        capabilitiesJson = pluginJson.encodeToString(capabilities),
        iconPath = icon?.let { File(dir, it).absolutePath },
        customName = existing?.customName,
        metadataEnabled = existing?.metadataEnabled ?: true,
        lyricsEnabled = existing?.lyricsEnabled ?: true,
        coverEnabled = existing?.coverEnabled ?: true,
        metadataSortOrder = existing?.metadataSortOrder ?: 0,
        lyricsSortOrder = existing?.lyricsSortOrder ?: 0,
        coverSortOrder = existing?.coverSortOrder ?: 0,
        installedAt = existing?.installedAt ?: now,
        updatedAt = now,
    )
}
