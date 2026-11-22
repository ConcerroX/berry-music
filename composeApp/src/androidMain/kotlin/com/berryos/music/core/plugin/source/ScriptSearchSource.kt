package com.berryos.music.core.plugin.source

import android.util.Log
import com.berryos.music.core.database.entity.SourcePluginEntity
import com.berryos.music.core.plugin.PluginJson
import com.berryos.music.core.plugin.contract.PluginGetLyricsRequest
import com.berryos.music.core.plugin.contract.PluginJsonParser
import com.berryos.music.core.plugin.contract.PluginSearchCoversRequest
import com.berryos.music.core.plugin.contract.PluginSearchSongsRequest
import com.berryos.music.core.plugin.contract.PluginSongRequest
import com.berryos.music.core.plugin.model.LyricsCandidateResult
import com.berryos.music.core.plugin.model.LyricsResult
import com.berryos.music.core.plugin.model.PluginCapability
import com.berryos.music.core.plugin.model.PluginConfigField
import com.berryos.music.core.plugin.model.PluginManifest
import com.berryos.music.core.plugin.model.SongSearchResult
import com.berryos.music.core.plugin.model.SourceRuntimeConfig
import com.berryos.music.core.plugin.model.normalizedPluginCapabilities
import com.berryos.music.quickjs.PluginJsRuntime
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource

@Suppress("TooGenericExceptionCaught")
class ScriptSearchSource(
    private val manifest: PluginManifest,
    private val script: String,
    private val displayName: String = manifest.name,
    private val runtimeFactory: () -> PluginJsRuntime,
    private val localizedManifest: (() -> PluginManifest)? = null,
    private val customName: String? = null,
) : SearchSource,
    AutoCloseable {
    override val id: String = manifest.id
    override val name: String
        get() = customName?.trim()?.takeIf { it.isNotEmpty() } ?: localizedManifest?.invoke()?.name ?: displayName
    override val apiVersion: Int = manifest.apiVersion
    override val minHostApiVersion: Int = manifest.minHostApiVersion
    override val capabilities: Set<PluginCapability> =
        manifest.capabilities.normalizedPluginCapabilities()
    override val configFields: List<PluginConfigField>
        get() = localizedManifest?.invoke()?.configFields ?: manifest.configFields
    private val lifecycleLock = Any()
    private var closing = false
    private var activeSessions = 0
    private var executionContext: RuntimeExecutionContext? = null

    private fun executionContext(): RuntimeExecutionContext = synchronized(lifecycleLock) {
        check(activeSessions > 0) { "Plugin execution requires an active session" }
        executionContext ?: run {
            val executor = Executors.newSingleThreadExecutor { runnable ->
                Thread(null, runnable, "QuickJS-$id", 4L * 1024L * 1024L)
            }
            RuntimeExecutionContext(executor, executor.asCoroutineDispatcher()).also { executionContext = it }
        }
    }
    private val parser = PluginJsonParser(PluginJson)

    @Volatile private var config = SourceRuntimeConfig()
    private var runtimeInstance: PluginJsRuntime? = null
    private val runtime: PluginJsRuntime
        get() = runtimeInstance ?: runtimeFactory().also { runtimeInstance = it }
    private var scriptInitialized = false

    override fun applyConfig(config: SourceRuntimeConfig) {
        this.config = config
    }

    override suspend fun searchSongs(
        keyword: String,
        page: Int,
        separator: String,
        pageSize: Int,
    ): List<SongSearchResult> {
        if (PluginCapability.SEARCH_SONGS !in capabilities) return emptyList()
        return executeCall(
            operation = "search",
            errorMessage = "Plugin song search failed",
            errorDetail = { "plugin=$id\nname=$name\nkeyword=$keyword\npage=$page\npageSize=$pageSize" },
        ) {
            val started = TimeSource.Monotonic.markNow()
            val request = PluginSearchSongsRequest(
                keyword = keyword,
                page = page,
                pageSize = pageSize,
                separator = separator,
                config = config.values,
            )
            val raw = callRuntime(FUNCTION_SEARCH_SONGS, PluginJson.encodeToString(request))
            val results = parser.parseSongResults(
                rawJson = raw,
                pluginId = id,
                pluginName = name,
            )
            logPluginCall(
                message = "Plugin song search returned ${results.size} result(s)",
                detail = buildString {
                    appendLine("plugin=$id")
                    appendLine("name=$name")
                    appendLine("version=${manifest.versionName}(${manifest.versionCode})")
                    appendLine("keyword=$keyword")
                    appendLine("page=$page")
                    appendLine("pageSize=$pageSize")
                    appendLine("resultCount=${results.size}")
                    appendLine("top=${results.take(RESULT_PREVIEW_COUNT).map { "${it.id}:${it.title}" }}")
                    val fieldKeys = results.take(RESULT_PREVIEW_COUNT)
                        .map { it.id to it.normalizedFields().keys }
                    appendLine("fieldKeys=$fieldKeys")
                    appendLine("responseChars=${raw.length}")
                    appendLine("elapsedMs=${started.elapsedNow().inWholeMilliseconds}")
                },
            )
            results
        }
    }

    override suspend fun getLyrics(song: SongSearchResult): LyricsResult? =
        getLyricsCandidates(song).firstOrNull()?.lyrics

    override suspend fun getLyricsCandidates(
        song: SongSearchResult,
        page: Int,
        pageSize: Int,
    ): List<LyricsCandidateResult> {
        if (PluginCapability.GET_LYRICS !in capabilities) return emptyList()
        return executeCall(
            operation = "getLyrics",
            errorMessage = "Plugin lyrics call failed",
            errorDetail = { "plugin=$id\nname=$name\nsong=${song.id}:${song.title}\npage=$page\npageSize=$pageSize" },
        ) {
            val started = TimeSource.Monotonic.markNow()
            val request = PluginGetLyricsRequest(
                song = song.toPluginSongRequest(),
                page = page,
                pageSize = pageSize,
                config = config.values,
            )
            val raw = callRuntime(FUNCTION_GET_LYRICS, PluginJson.encodeToString(request))
            val candidates = parser.parseLyricsCandidates(
                rawJson = raw,
                pluginId = id,
                pluginName = name,
                fallbackSong = song,
                enforceApi4Contract = apiVersion >= 4,
            )
            logPluginCall(
                message = if (candidates.isEmpty()) {
                    "Plugin lyrics call returned no usable lyrics"
                } else {
                    "Plugin lyrics call returned ${candidates.size} candidate(s)"
                },
                detail = buildString {
                    appendLine("plugin=$id")
                    appendLine("name=$name")
                    appendLine("song=${song.id}:${song.title}")
                    appendLine("page=$page")
                    appendLine("pageSize=$pageSize")
                    appendLine("candidateCount=${candidates.size}")
                    appendLine("payloadTypes=${candidates.map { it.lyrics.payloadType }}")
                    appendLine("responseChars=${raw.length}")
                    appendLine("elapsedMs=${started.elapsedNow().inWholeMilliseconds}")
                },
            )
            candidates
        }
    }

    override suspend fun searchCovers(keyword: String, page: Int, pageSize: Int): List<SongSearchResult> {
        if (PluginCapability.SEARCH_COVERS !in capabilities) return emptyList()
        return executeCall(
            operation = "searchCover",
            errorMessage = "Plugin cover search failed",
            errorDetail = { "plugin=$id\nname=$name\nkeyword=$keyword\npage=$page\npageSize=$pageSize" },
        ) {
            val started = TimeSource.Monotonic.markNow()
            val request = PluginSearchCoversRequest(
                keyword = keyword,
                page = page,
                pageSize = pageSize,
                config = config.values,
            )
            val raw = callRuntime(FUNCTION_SEARCH_COVERS, PluginJson.encodeToString(request))
            val results = parser.parseCoverResults(
                rawJson = raw,
                pluginId = id,
                pluginName = name,
                enforceApi4Contract = apiVersion >= 4,
            )
            logPluginCall(
                message = "Plugin cover search returned ${results.size} result(s)",
                detail = "plugin=$id\nname=$name\nkeyword=$keyword\npage=$page\npageSize=$pageSize\n" +
                    "coverCount=${results.count {
                        it.picUrl.isNotBlank()
                    }}\nresponseChars=${raw.length}\nelapsedMs=${started.elapsedNow().inWholeMilliseconds}",
            )
            results
        }
    }

    override suspend fun searchCovers(song: SongSearchResult, page: Int, pageSize: Int): List<SongSearchResult> {
        if (PluginCapability.SEARCH_COVERS !in capabilities) return emptyList()
        return executeCall(
            operation = "searchCover",
            errorMessage = "Plugin cover search failed",
            errorDetail = { "plugin=$id\nname=$name\nsong=${song.id}:${song.title}\npage=$page\npageSize=$pageSize" },
        ) {
            val started = TimeSource.Monotonic.markNow()
            val request = PluginSearchCoversRequest(
                keyword = listOf(song.title, song.artist).filter { it.isNotBlank() }.joinToString(" "),
                song = song.toPluginSongRequest(),
                page = page,
                pageSize = pageSize,
                config = config.values,
            )
            val raw = callRuntime(FUNCTION_SEARCH_COVERS, PluginJson.encodeToString(request))
            val results = parser.parseCoverResults(
                rawJson = raw,
                pluginId = id,
                pluginName = name,
                enforceApi4Contract = apiVersion >= 4,
            )
            logPluginCall(
                message = "Plugin cover search returned ${results.size} result(s)",
                detail = "plugin=$id\nname=$name\nsong=${song.id}:${song.title}\npage=$page\npageSize=$pageSize\n" +
                    "coverCount=${results.count {
                        it.picUrl.isNotBlank()
                    }}\nresponseChars=${raw.length}\nelapsedMs=${started.elapsedNow().inWholeMilliseconds}",
            )
            results
        }
    }

    private suspend fun callRuntime(function: String, request: String): String {
        val instance = runtime
        instance.resetCancellation()
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { instance.cancelInvocation() }
            if (continuation.isActive) {
                continuation.resumeWith(
                    runCatching {
                        if (!scriptInitialized) {
                            try {
                                instance.eval(script, manifest.entry)
                                scriptInitialized = true
                            } catch (error: Throwable) {
                                // An interrupted initializer may have left partial globals behind.
                                runtimeInstance = null
                                instance.close()
                                throw error
                            }
                        }
                        instance.call(function, request)
                    },
                )
            }
        }
    }

    internal fun snapshot(plugin: SourcePluginEntity): SearchSource {
        val localized = localizedManifest?.invoke() ?: manifest
        val name = plugin.customName?.trim()?.takeIf { it.isNotEmpty() } ?: localized.name
        return SourceSnapshot(this, plugin, name, localized.configFields)
    }

    override suspend fun <T> withSession(block: suspend SearchSource.() -> T): T = lease { block(this) }

    private suspend fun <T> executeCall(
        operation: String,
        errorMessage: String,
        errorDetail: () -> String,
        block: suspend () -> T,
    ): T = execute {
        try {
            block()
        } catch (throwable: Exception) {
            if (throwable is CancellationException) throw throwable
            Log.w(TAG, "$operation failed for plugin $id ($name)", throwable)
            logPluginException(message = errorMessage, throwable = throwable, detail = errorDetail())
            throw throwable
        }
    }

    private suspend fun <T> execute(block: suspend () -> T): T = lease {
        withContext(executionContext().dispatcher) { block() }
    }

    private suspend fun <T> lease(block: suspend () -> T): T {
        val held = currentCoroutineContext()[RuntimeSessions]?.sources.orEmpty()
        if (this in held) return block()
        synchronized(lifecycleLock) {
            check(!closing) { "Plugin instance has been retired" }
            activeSessions++
        }
        try {
            return withContext(RuntimeSessions(held + this)) { block() }
        } finally {
            synchronized(lifecycleLock) {
                activeSessions--
                disposeIfIdle()
            }
        }
    }

    override fun close() = synchronized(lifecycleLock) {
        closing = true
        disposeIfIdle()
    }

    private fun disposeIfIdle() {
        if (!closing || activeSessions != 0) return
        val context = executionContext ?: return
        executionContext = null
        context.executor.execute {
            try {
                runtimeInstance?.close()
                runtimeInstance = null
            } finally {
                context.dispatcher.close()
                context.executor.shutdown()
            }
        }
    }

    private class RuntimeSessions(val sources: Set<ScriptSearchSource>) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<RuntimeSessions>
    }

    private data class SourceSnapshot(
        val source: ScriptSearchSource,
        val plugin: SourcePluginEntity,
        override val name: String,
        override val configFields: List<PluginConfigField>,
    ) : SearchSource by source {
        override val iconPath: String? = plugin.iconPath
        override val metadataEnabled: Boolean = plugin.metadataEnabled
        override val lyricsEnabled: Boolean = plugin.lyricsEnabled
        override val coverEnabled: Boolean = plugin.coverEnabled
        override val metadataSortOrder: Int = plugin.metadataSortOrder
        override val lyricsSortOrder: Int = plugin.lyricsSortOrder
        override val coverSortOrder: Int = plugin.coverSortOrder

        override suspend fun <T> withSession(block: suspend SearchSource.() -> T): T =
            source.withSession { block(this@SourceSnapshot) }
    }

    private fun SongSearchResult.toPluginSongRequest(): PluginSongRequest = PluginSongRequest(
        id = id,
        title = title,
        artist = artist,
        album = album,
        date = date,
        duration = duration,
        sourceId = pluginId,
        pluginId = pluginId,
        fields = fields,
        internal = internal,
    )

    private fun logPluginCall(message: String, detail: String) {
        Log.d(TAG, "$message\n${detail.take(LOG_DETAIL_PREVIEW_LIMIT)}")
    }

    private fun logPluginException(message: String, throwable: Throwable, detail: String) {
        Log.w(TAG, "$message\n${detail.take(LOG_DETAIL_PREVIEW_LIMIT)}", throwable)
    }

    private data class RuntimeExecutionContext(
        val executor: ExecutorService,
        val dispatcher: ExecutorCoroutineDispatcher,
    )

    private companion object {
        const val FUNCTION_SEARCH_SONGS = "searchSongs"
        const val FUNCTION_GET_LYRICS = "getLyrics"
        const val FUNCTION_SEARCH_COVERS = "searchCovers"
        const val TAG = "PlatformPlugin"
        const val LOG_DETAIL_PREVIEW_LIMIT = 4_000
        const val RESULT_PREVIEW_COUNT = 5
    }
}
