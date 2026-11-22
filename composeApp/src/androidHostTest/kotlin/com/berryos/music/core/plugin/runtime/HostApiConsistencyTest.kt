package com.berryos.music.core.plugin.runtime

import com.berryos.music.quickjs.HostApiBootstrap
import kotlin.test.Test
import kotlin.test.assertEquals

class HostApiConsistencyTest {
    @Test
    fun bootstrapAndDispatcherExposeTheSameApis() {
        val calls = Regex("""hostCall\(\s*"([^"]+)"""")
            .findAll(HostApiBootstrap.source)
            .map { it.groupValues[1] }
            .toList()
        assertEquals(calls.size, calls.toSet().size, "Bootstrap defines the same host call twice")
        assertEquals(QuickJsHostApi().supportedHostApis, calls.toSet())
    }
}
