package com.berryos.music.quickjs

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickJsRuntimeTest {

    private class EchoHost : HostCallback {
        @Volatile
        var lastCall: String? = null

        override fun call(name: String, payloadJson: String): String {
            lastCall = name
            return """{"value":"pong"}"""
        }

        override fun beginInvocation(timeoutMs: Long) = Unit

        override fun resetCancellation() = Unit

        override fun cancelInvocation() = Unit
    }

    @Test
    fun evaluatesPrimitives() {
        val runtime = QuickJsRuntime()
        try {
            assertEquals("3", runtime.eval("1 + 2", "<test>"))
        } finally {
            runtime.close()
        }
    }

    @Test
    fun callsGlobalFunctionWithJson() {
        val runtime = QuickJsRuntime()
        try {
            runtime.eval("function add(request) { return request.a + request.b }", "<test>")
            assertEquals("5", runtime.call("add", """{"a":2,"b":3}"""))
        } finally {
            runtime.close()
        }
    }

    @Test
    fun hostCallIsSynchronous() {
        val host = EchoHost()
        val runtime = QuickJsRuntime(hostApi = host)
        try {
            assertEquals("pong", runtime.eval("Platform.app.getInfo()", "<test>"))
            assertEquals("app.info", host.lastCall)
        } finally {
            runtime.close()
        }
    }

    @Test
    fun timeoutInterruptsScript() {
        val runtime = QuickJsRuntime(timeoutMs = 100)
        try {
            assertThrows(IllegalStateException::class.java) {
                runtime.eval("while (true) {}", "<test>")
            }
        } finally {
            runtime.close()
        }
    }
}
