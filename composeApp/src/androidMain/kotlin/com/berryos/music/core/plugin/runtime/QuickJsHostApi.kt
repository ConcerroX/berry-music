package com.berryos.music.core.plugin.runtime

import android.util.Log
import com.berryos.music.core.plugin.PluginJson
import com.berryos.music.core.plugin.i18n.PluginLocalePreferences
import com.berryos.music.core.plugin.i18n.PluginStrings
import com.berryos.music.quickjs.HostCallback
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@Suppress("TooManyFunctions")
class QuickJsHostApi(
    private val appInfo: HostAppInfo = HostAppInfo(),
    private val runtimeInfo: HostRuntimeInfo = HostRuntimeInfo(),
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val pluginId: String = "default",
    private val cacheRootDir: File? = null,
    private val pluginStrings: PluginStrings? = null,
    private val json: Json = PluginJson,
) : HostCallback {
    private val cancelled = AtomicBoolean(false)
    private val activeHttp = AtomicReference<Call?>(null)
    private var invocationDeadline = 0L

    override fun resetCancellation() {
        cancelled.set(false)
    }
    override fun cancelInvocation() {
        cancelled.set(true)
        activeHttp.get()?.cancel()
    }
    private var stringsSnapshot: PluginStrings.Snapshot? = null

    override fun beginInvocation(timeoutMs: Long) {
        invocationDeadline = if (timeoutMs > 0) System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs) else 0L
        stringsSnapshot = pluginStrings?.snapshot(PluginLocalePreferences.preferences.value)
    }
    private companion object {
        const val CACHE_LOG_TAG = "PlatformPluginCache"
        const val MAX_RESPONSE_BYTES = 8L * 1024 * 1024
        const val HTTP_BUFFER_BYTES = 8192
        const val INFLATE_BUFFER_BYTES = 4096
        const val BYTE_MASK = 0xff
        const val DEFAULT_CONNECT_TIMEOUT_MS = 8_000
        const val DEFAULT_READ_TIMEOUT_MS = 12_000
    }

    // Single source of truth for the host API surface; the JS bootstrap and runtime.info derive from it.
    private val handlers: Map<String, (JsonObject) -> String> = mapOf(
        "i18n.getLocale" to { _ -> text(stringsSnapshot?.locale ?: "und") },

        "i18n.t" to { payload ->
            text(
                requireNotNull(stringsSnapshot) { "Plugin has no string resources" }.format(
                    payload.string("key"),
                    (payload["args"] as? JsonArray).orEmpty().map { element ->
                        val primitive = element as? JsonPrimitive
                        if (primitive?.isString == true) primitive.content else primitive?.longOrNull
                    },
                ),
            )
        },

        "app.info" to { _ -> value(appInfo.toJsonObject()) },

        "app.userAgent" to { _ -> text(buildDefaultUserAgent(appInfo)) },

        "runtime.info" to { _ -> value(runtimeInfo.toJsonObject(supportedHostApis)) },

        "cache.get" to { payload -> text(cacheGet(payload.string("key"))) },

        "cache.set" to { payload ->
            cacheSet(
                key = payload.string("key"),
                value = payload.string("value"),
                ttlMs = payload.longOrNull("ttlMs"),
            )
            text("")
        },

        "cache.remove" to { payload ->
            cacheRemove(payload.string("key"))
            text("")
        },

        "cache.clear" to { _ ->
            cacheClear()
            text("")
        },

        "crypto.md5" to { payload -> text(md5(payload.string("text"))) },

        "crypto.aesEcbPkcs5EncryptBase64" to { payload ->
            text(
                aesEcbPkcs5EncryptBase64(
                    text = payload.string("text"),
                    key = payload.string("key"),
                ),
            )
        },

        "crypto.aesEcbPkcs5EncryptHex" to { payload ->
            text(
                aesEcbPkcs5Encrypt(
                    text = payload.string("text"),
                    key = payload.string("key"),
                ).toHex(),
            )
        },

        "crypto.aesEcbPkcs5DecryptBase64ToText" to { payload ->
            text(
                aesEcbPkcs5DecryptBase64ToText(
                    base64 = payload.string("base64"),
                    key = payload.string("key"),
                ),
            )
        },

        "base64.encodeText" to { payload ->
            text(
                Base64.getEncoder().encodeToString(payload.string("text").encodeToByteArray()),
            )
        },

        "base64.decodeText" to { payload ->
            text(
                Base64.getDecoder().decode(payload.string("base64")).decodeToString(),
            )
        },

        "base64.dropBytes" to { payload ->
            text(
                Base64.getEncoder().encodeToString(
                    Base64.getDecoder().decode(payload.string("base64"))
                        .drop(payload.intOrNull("count") ?: 0)
                        .toByteArray(),
                ),
            )
        },

        "base64.decodeBytes" to { payload ->
            bytes(
                Base64.getDecoder().decode(payload.string("base64")),
            )
        },

        "base64.encodeBytes" to { payload ->
            text(
                Base64.getEncoder().encodeToString(payload.bytes("bytes")),
            )
        },

        "base64.encodeUrlText" to { payload ->
            text(
                Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payload.string("text").encodeToByteArray()),
            )
        },

        "base64.decodeUrlText" to { payload ->
            text(
                Base64.getUrlDecoder().decode(payload.string("base64Url")).decodeToString(),
            )
        },

        "base64.encodeUrlBytes" to { payload ->
            text(
                Base64.getUrlEncoder().withoutPadding().encodeToString(payload.bytes("bytes")),
            )
        },

        "base64.decodeUrlBytes" to { payload ->
            bytes(
                Base64.getUrlDecoder().decode(payload.string("base64Url")),
            )
        },

        "base64.toUrl" to { payload ->
            text(
                Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(Base64.getDecoder().decode(payload.string("base64"))),
            )
        },

        "base64.fromUrl" to { payload ->
            text(
                Base64.getEncoder().encodeToString(Base64.getUrlDecoder().decode(payload.string("base64Url"))),
            )
        },

        "bytes.xor" to { payload ->
            bytes(
                xor(
                    bytes = payload.bytes("bytes"),
                    key = payload.bytes("key"),
                ),
            )
        },

        "bytes.xorBase64" to { payload ->
            text(
                Base64.getEncoder().encodeToString(
                    xor(
                        bytes = Base64.getDecoder().decode(payload.string("base64")),
                        key = payload.bytes("key"),
                    ),
                ),
            )
        },

        "compression.inflateBytesToText" to { payload ->
            text(
                inflate(payload.bytes("bytes")),
            )
        },

        "compression.inflateBase64ToText" to { payload ->
            text(
                inflate(Base64.getDecoder().decode(payload.string("base64"))),
            )
        },

        /*
         * Legacy API: returns only the body.
         * Kept for existing source.js / 01_http.js scripts.
         */
        "http.getText" to { payload ->
            text(
                executeHttp(
                    method = "GET",
                    payload = payload,
                    binaryResponse = false,
                ).bodyText,
            )
        },

        "http.postText" to { payload ->
            text(
                executeHttp(
                    method = "POST",
                    payload = payload,
                    binaryResponse = false,
                ).bodyText,
            )
        },

        "http.postBytes" to { payload ->
            text(
                executeHttp(
                    method = "POST",
                    payload = payload,
                    binaryResponse = true,
                ).bodyBase64,
            )
        },

        /*
         * New API: returns the full response object.
         *
         * http.get:
         * {
         *   code: 200,
         *   headers: { "Set-Cookie": ["..."] },
         *   body: "..."
         * }
         *
         * http.getBytes / http.postBytesResponse:
         * {
         *   code: 200,
         *   headers: { "Set-Cookie": ["..."] },
         *   bodyBase64: "..."
         * }
         */
        "http.get" to { payload ->
            value(
                executeHttp(
                    method = "GET",
                    payload = payload,
                    binaryResponse = false,
                ).toJsonObject(),
            )
        },

        "http.post" to { payload ->
            value(
                executeHttp(
                    method = "POST",
                    payload = payload,
                    binaryResponse = false,
                ).toJsonObject(),
            )
        },

        "http.getBytes" to { payload ->
            value(
                executeHttp(
                    method = "GET",
                    payload = payload,
                    binaryResponse = true,
                ).toJsonObject(),
            )
        },

        "http.postBytesResponse" to { payload ->
            value(
                executeHttp(
                    method = "POST",
                    payload = payload,
                    binaryResponse = true,
                ).toJsonObject(),
            )
        },

        "xml.getRootAttributes" to { payload ->
            value(
                HostXmlApi.getRootAttributes(
                    xml = payload.string("xml"),
                ),
            )
        },

        "xml.findElements" to { payload ->
            value(
                HostXmlApi.findElements(
                    xml = payload.string("xml"),
                    query = payload.obj("query") ?: JsonObject(emptyMap()),
                ),
            )
        },

        "xml.replaceChildrenByAttr" to { payload ->
            text(
                HostXmlApi.replaceChildrenByAttr(
                    xml = payload.string("xml"),
                    options = payload.obj("options") ?: JsonObject(emptyMap()),
                ),
            )
        },

        "xml.removeElements" to { payload ->
            text(
                HostXmlApi.removeElements(
                    xml = payload.string("xml"),
                    query = payload.obj("query") ?: JsonObject(emptyMap()),
                ),
            )
        },

        "log.debug" to { payload ->
            Log.d(payload.logTag(), payload.string("message"))
            text("")
        },

        "log.warn" to { payload ->
            Log.w(payload.logTag(), payload.string("message"))
            text("")
        },

        "log.error" to { payload ->
            Log.e(payload.logTag(), payload.string("message"))
            text("")
        },
    )

    internal val supportedHostApis: Set<String>
        get() = handlers.keys

    override fun call(name: String, payloadJson: String): String {
        val payload = runCatching {
            json.parseToJsonElement(payloadJson).jsonObject
        }.getOrDefault(JsonObject(emptyMap()))

        return handlers[name]?.invoke(payload) ?: error("Unsupported host api: $name")
    }

    private fun cacheGet(key: String): String {
        val normalizedKey = key.trim()
        val file = cacheFile(normalizedKey) ?: run {
            logCache("get ignored blank key")
            return ""
        }
        if (!file.isFile) {
            logCache("miss plugin=$pluginId key=$normalizedKey")
            return ""
        }

        val entry = runCatching {
            json.parseToJsonElement(file.readText()).jsonObject
        }.getOrNull() ?: run {
            file.delete()
            logCache("corrupt entry removed plugin=$pluginId key=$normalizedKey")
            return ""
        }

        val expiresAt = entry.longOrNull("expiresAt") ?: 0L
        if (expiresAt > 0L && expiresAt <= System.currentTimeMillis()) {
            file.delete()
            logCache("expired entry removed plugin=$pluginId key=$normalizedKey")
            return ""
        }

        logCache(
            "hit plugin=$pluginId key=$normalizedKey expiresAt=$expiresAt valueLength=${entry.string("value").length}",
        )
        return entry.string("value")
    }

    private fun cacheSet(key: String, value: String, ttlMs: Long?) {
        val normalizedKey = key.trim()
        val file = cacheFile(normalizedKey) ?: run {
            logCache("set ignored blank key")
            return
        }
        val normalizedTtlMs = ttlMs ?: 0L
        val expiresAt = if (normalizedTtlMs > 0L) {
            System.currentTimeMillis() + normalizedTtlMs
        } else {
            0L
        }

        file.parentFile?.mkdirs()
        file.writeText(
            json.encodeToString(
                JsonObject.serializer(),
                buildJsonObject {
                    put("value", value)
                    put("expiresAt", expiresAt)
                },
            ),
        )
        logCache(
            "set plugin=$pluginId key=$normalizedKey ttlMs=$normalizedTtlMs " +
                "expiresAt=$expiresAt valueLength=${value.length}",
        )
    }

    private fun cacheRemove(key: String) {
        val normalizedKey = key.trim()
        val removed = cacheFile(normalizedKey)?.delete() == true
        logCache("remove plugin=$pluginId key=$normalizedKey removed=$removed")
    }

    private fun cacheClear() {
        val cleared = pluginCacheDir()?.deleteRecursively() == true
        logCache("clear plugin=$pluginId cleared=$cleared")
    }

    private fun cacheFile(key: String): File? {
        val normalizedKey = key.trim()
        return pluginCacheDir()
            ?.takeIf { normalizedKey.isNotEmpty() }
            ?.let { File(it, "${md5(normalizedKey)}.json") }
    }

    private fun pluginCacheDir(): File? {
        val root = cacheRootDir ?: return null
        return File(root, md5(pluginId.ifBlank { "default" }))
    }

    private fun logCache(message: String) {
        Log.d(CACHE_LOG_TAG, message)
    }

    private fun executeHttp(method: String, payload: JsonObject, binaryResponse: Boolean): HostHttpResponse {
        val urlText = payload.string("url")
        require(urlText.isNotBlank()) { "HTTP url is blank" }
        val contentType = payload.resolveContentType(binaryResponse)
        val request = buildHttpRequest(method, payload, urlText, contentType)
        val client = buildHttpClient(payload, remainingInvocationMillis())

        val call = client.newCall(request)
        activeHttp.set(call)
        if (cancelled.get()) call.cancel()
        try {
            return call.execute().use { response -> response.toHostResponse(binaryResponse) }
        } finally {
            activeHttp.compareAndSet(call, null)
        }
    }

    private fun JsonObject.resolveContentType(binaryResponse: Boolean): String = string("contentType").ifBlank {
        if (binaryResponse) "application/octet-stream" else "application/json; charset=utf-8"
    }

    private fun buildHttpRequest(method: String, payload: JsonObject, urlText: String, contentType: String): Request {
        val builder = Request.Builder().url(urlText)
        val hasBody = method == "POST" || method == "PUT" || method == "PATCH"
        if (hasBody) builder.header("Content-Type", contentType)

        payload.obj("headers")?.forEach { (key, value) ->
            val headerValue = value.headerString()
            if (key.equals("User-Agent", ignoreCase = true) && headerValue.isBlank()) {
                return@forEach
            }
            builder.header(key, headerValue)
        }

        if (!hasNonBlankHeader(payload.obj("headers"), "User-Agent")) {
            builder.header("User-Agent", buildDefaultUserAgent(appInfo))
        }

        val requestBody = if (hasBody) {
            payload.requestBodyBytes().toRequestBody(contentType.toMediaType())
        } else {
            null
        }
        return builder.method(method, requestBody).build()
    }

    private fun remainingInvocationMillis(): Long {
        check(!cancelled.get()) { "Plugin invocation cancelled" }
        val remainingNanos = invocationDeadline - System.nanoTime()
        check(invocationDeadline == 0L || remainingNanos > 0) { "Plugin invocation timed out" }
        return if (invocationDeadline == 0L) {
            0L
        } else {
            TimeUnit.NANOSECONDS.toMillis(remainingNanos).coerceAtLeast(1L)
        }
    }

    private fun buildHttpClient(payload: JsonObject, remainingMs: Long): OkHttpClient = okHttpClient.newBuilder()
        .callTimeout(remainingMs, TimeUnit.MILLISECONDS)
        .followRedirects(payload.booleanOrNull("followRedirects") ?: true)
        .followSslRedirects(payload.booleanOrNull("followRedirects") ?: true)
        .connectTimeout(
            (payload.intOrNull("connectTimeoutMs") ?: DEFAULT_CONNECT_TIMEOUT_MS).toLong(),
            TimeUnit.MILLISECONDS,
        )
        .readTimeout(
            (payload.intOrNull("readTimeoutMs") ?: DEFAULT_READ_TIMEOUT_MS).toLong(),
            TimeUnit.MILLISECONDS,
        )
        .build()

    private fun Response.toHostResponse(binaryResponse: Boolean): HostHttpResponse {
        val responseBytes = readBodyBytes()
        return HostHttpResponse(
            code = code,
            message = message,
            headers = headers.toMultimap(),
            bodyText = if (binaryResponse) "" else responseBytes.decodeToString(),
            bodyBase64 = if (binaryResponse) Base64.getEncoder().encodeToString(responseBytes) else "",
        )
    }

    private fun Response.readBodyBytes(): ByteArray {
        require(body.contentLength() <= MAX_RESPONSE_BYTES) { "Plugin response is too large" }
        val output = ByteArrayOutputStream()
        body.byteStream().use { input ->
            val buffer = ByteArray(HTTP_BUFFER_BYTES)
            while (true) {
                check(!cancelled.get()) { "Plugin invocation cancelled" }
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size().toLong() + count <= MAX_RESPONSE_BYTES) { "Plugin response is too large" }
                output.write(buffer, 0, count)
            }
        }
        return output.toByteArray()
    }

    private fun hasNonBlankHeader(headers: JsonObject?, targetName: String): Boolean {
        if (headers == null) return false
        return headers.any { (key, value) ->
            key.equals(targetName, ignoreCase = true) && value.headerString().isNotBlank()
        }
    }

    private fun JsonObject.requestBodyBytes(): ByteArray {
        val bodyBase64 = string("bodyBase64")
        if (bodyBase64.isNotBlank()) {
            return Base64.getDecoder().decode(bodyBase64)
        }

        val bodyBytes = this["bodyBytes"] as? JsonArray
        if (bodyBytes != null) {
            return ByteArray(bodyBytes.size) { index ->
                bodyBytes[index].jsonPrimitive.int.toByte()
            }
        }

        return string("body").encodeToByteArray()
    }

    private fun md5(text: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(
            text.encodeToByteArray(),
        )
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun aesEcbPkcs5EncryptBase64(text: String, key: String): String =
        Base64.getEncoder().encodeToString(aesEcbPkcs5Encrypt(text, key))

    private fun aesEcbPkcs5Encrypt(text: String, key: String): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKey = SecretKeySpec(key.encodeToByteArray(), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(text.encodeToByteArray())
    }

    private fun aesEcbPkcs5DecryptBase64ToText(base64: String, key: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKey = SecretKeySpec(key.encodeToByteArray(), "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return cipher.doFinal(Base64.getDecoder().decode(base64)).decodeToString()
    }

    private fun xor(bytes: ByteArray, key: ByteArray): ByteArray {
        if (key.isEmpty()) return bytes
        return ByteArray(bytes.size) { index ->
            (bytes[index].toInt() xor key[index % key.size].toInt()).toByte()
        }
    }

    private fun inflate(bytes: ByteArray): String {
        val inflater = Inflater()
        inflater.setInput(bytes)

        val buffer = ByteArray(INFLATE_BUFFER_BYTES)
        val output = ByteArrayOutputStream()

        try {
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0) break
                output.write(buffer, 0, count)
            }
        } finally {
            inflater.end()
        }

        return output.toByteArray().decodeToString()
    }

    private fun text(value: String): String = value(JsonPrimitive(value))

    private fun bytes(value: ByteArray): String = value(
        JsonArray(
            value.map { byte ->
                JsonPrimitive(byte.toInt() and BYTE_MASK)
            },
        ),
    )

    private fun value(value: JsonElement): String = json.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("value", value)
        },
    )

    private data class HostHttpResponse(
        val code: Int,
        val message: String,
        val headers: Map<String, List<String>>,
        val bodyText: String,
        val bodyBase64: String,
    ) {
        fun toJsonObject(): JsonObject = buildJsonObject {
            put("code", code)
            put("message", message)

            put(
                "headers",
                JsonObject(
                    headers.mapValues { (_, values) ->
                        JsonArray(values.map { JsonPrimitive(it) })
                    },
                ),
            )

            if (bodyText.isNotEmpty()) {
                put("body", bodyText)
            } else {
                put("body", "")
            }

            if (bodyBase64.isNotEmpty()) {
                put("bodyBase64", bodyBase64)
            } else {
                put("bodyBase64", "")
            }
        }
    }
}

data class HostAppInfo(
    val name: String = "Berry Music",
    val packageName: String = "com.berryos.music",
    val versionName: String = "0.0.0",
    val versionCode: Long = 0,
    val buildType: String = "unknown",
    val debug: Boolean = false,
) {
    fun toJsonObject(): JsonObject = buildJsonObject {
        put("name", name)
        put("packageName", packageName)
        put("versionName", versionName)
        put("versionCode", versionCode)
        put("buildType", buildType)
        put("debug", debug)
    }
}

data class HostRuntimeInfo(
    val pluginApiVersion: Int = HostApiRegistry.PLUGIN_PROTOCOL_VERSION,
    val hostApiVersion: Int = HostApiRegistry.PLATFORM_API_VERSION,
    val engine: String = "quickjs",
    val engineVersion: String? = null,
) {
    fun toJsonObject(supportedHostApis: Collection<String>): JsonObject = buildJsonObject {
        put("pluginApiVersion", pluginApiVersion)
        put("hostApiVersion", hostApiVersion)
        put("engine", engine)
        if (engineVersion != null) {
            put("engineVersion", engineVersion)
        } else {
            put("engineVersion", JsonNull)
        }
        put(
            "supportedHostApis",
            JsonArray(supportedHostApis.sorted().map { JsonPrimitive(it) }),
        )
    }
}

fun buildDefaultUserAgent(appInfo: HostAppInfo): String = "${appInfo.name}/${appInfo.versionName}"

private fun JsonObject.intOrNull(key: String): Int? = this[key]?.jsonPrimitive?.int

private fun JsonObject.longOrNull(key: String): Long? = this[key]?.jsonPrimitive?.longOrNull

private fun JsonObject.booleanOrNull(key: String): Boolean? =
    this[key]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()

private fun JsonObject.bytes(key: String): ByteArray {
    val array = this[key]?.jsonArray ?: return ByteArray(0)
    return ByteArray(array.size) { index ->
        array[index].jsonPrimitive.int.toByte()
    }
}

private fun JsonElement.headerString(): String = when (this) {
    is JsonPrimitive -> contentOrNull.orEmpty()

    is JsonArray -> joinToString(", ") {
        it.jsonPrimitive.contentOrNull.orEmpty()
    }

    else -> toString()
}

private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

private fun JsonObject.logTag(): String = string("tag")
    .ifBlank { "PlatformPlugin" }
    .take(MAX_LOG_TAG_LENGTH)

private const val MAX_LOG_TAG_LENGTH = 48
