package com.berryos.music.quickjs

/**
 * Native entry points. Function names match the JNI symbols in quickjs_bridge.cpp.
 */
object QuickJsNative {

    init {
        System.loadLibrary("quickjs-ng")
    }

    @JvmStatic
    external fun createRuntime(
        memoryLimitBytes: Long,
        stackSizeBytes: Long,
        timeoutMs: Long,
        hostApi: HostCallback?,
    ): Long

    @JvmStatic
    external fun setCancelled(runtimePtr: Long, cancelled: Boolean)

    @JvmStatic
    external fun eval(runtimePtr: Long, script: String, filename: String?): String

    @JvmStatic
    external fun call(runtimePtr: Long, functionName: String, requestJson: String): String

    @JvmStatic
    external fun closeRuntime(runtimePtr: Long)
}
