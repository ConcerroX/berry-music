package com.berryos.music.quickjs

/**
 * Host side of the JavaScript bridge.
 *
 * [call] is invoked synchronously from the script thread; [beginInvocation],
 * [resetCancellation] and [cancelInvocation] share the invocation deadline with the script runner.
 */
interface HostCallback {
    fun call(name: String, payloadJson: String): String
    fun beginInvocation(timeoutMs: Long)
    fun resetCancellation()
    fun cancelInvocation()
}
