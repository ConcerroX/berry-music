package com.berryos.music.quickjs

/*
 * Ported from Lyrico (Apache License 2.0):
 * https://github.com/Replica0110/Lyrico
 */

class QuickJsRuntime(
    memoryLimitBytes: Long = DEFAULT_MEMORY_LIMIT_BYTES,
    stackSizeBytes: Long = DEFAULT_STACK_SIZE_BYTES,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val hostApi: HostCallback? = null
) : PluginJsRuntime {
    @Volatile private var runtimePtr: Long = QuickJsNative.createRuntime(
        memoryLimitBytes = memoryLimitBytes,
        stackSizeBytes = stackSizeBytes,
        timeoutMs = timeoutMs,
        hostApi = hostApi
    )

    init {
        val ptr = runtimePtr
        if (hostApi != null) {
            try {
                QuickJsNative.eval(ptr, HostApiBootstrap.source, "<lyrico-host>")
            } catch (throwable: Throwable) {
                runtimePtr = 0L
                QuickJsNative.closeRuntime(ptr)
                throw throwable
            }
        }
    }

    override fun eval(script: String, filename: String): String {
        hostApi?.beginInvocation(timeoutMs)
        val ptr = runtimePtr
        check(ptr != 0L) { "QuickJS runtime is closed" }
        return QuickJsNative.eval(ptr, script, filename)
    }

    override fun call(functionName: String, requestJson: String): String {
        hostApi?.beginInvocation(timeoutMs)
        val ptr = runtimePtr
        check(ptr != 0L) { "QuickJS runtime is closed" }
        return QuickJsNative.call(ptr, functionName, requestJson)
    }

    @Synchronized
    override fun resetCancellation() {
        if (runtimePtr != 0L) QuickJsNative.setCancelled(runtimePtr, false)
        hostApi?.resetCancellation()
    }

    @Synchronized
    override fun cancelInvocation() {
        if (runtimePtr != 0L) QuickJsNative.setCancelled(runtimePtr, true)
        hostApi?.cancelInvocation()
    }

    @Synchronized
    override fun close() {
        val ptr = runtimePtr
        if (ptr != 0L) {
            runtimePtr = 0L
            QuickJsNative.closeRuntime(ptr)
        }
    }

    private companion object {
        const val DEFAULT_MEMORY_LIMIT_BYTES = 64L * 1024L * 1024L
        const val DEFAULT_STACK_SIZE_BYTES = 2L * 1024L * 1024L
        const val DEFAULT_TIMEOUT_MS = 15_000L
    }
}
