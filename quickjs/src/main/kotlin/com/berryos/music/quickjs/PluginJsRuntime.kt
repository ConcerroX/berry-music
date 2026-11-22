package com.berryos.music.quickjs

/*
 * Ported from Lyrico (Apache License 2.0):
 * https://github.com/Replica0110/Lyrico
 */

interface PluginJsRuntime : AutoCloseable {
    fun resetCancellation() {}
    fun cancelInvocation() {}
    fun eval(script: String, filename: String = "<eval>"): String
    fun call(functionName: String, requestJson: String): String
}
