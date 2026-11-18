package com.berryos.music.app

import art.rippledesign.compose.rui.navigation3.RContainerTransformNavController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RContainerTransformNavControllerTest {
    @Test
    fun returningRequiresACompletedEnter() {
        val controller = RContainerTransformNavController<String>()

        controller.request("album")
        controller.setReturningToSource(true)

        assertNull(controller.returningKey)

        controller.markEntered("album")
        controller.setReturningToSource(true)

        assertEquals("album", controller.returningKey)
    }

    @Test
    fun gestureReturningRequiresACompletedEnter() {
        val controller = RContainerTransformNavController<String>()

        controller.request("album")
        controller.setGestureReturningToSource(true)

        assertNull(controller.gestureReturningKey)

        controller.markEntered("album")
        controller.setGestureReturningToSource(true)

        assertEquals("album", controller.gestureReturningKey)
    }

    @Test
    fun detachKeepsTheAssociationWhileTheRouteIsStillInTheStack() {
        val controller = RContainerTransformNavController<String>()
        controller.request("album")
        controller.markEntered("album")

        controller.detach("album")

        assertEquals("album", controller.activeKey)
        assertTrue(controller.hasEntered("album"))
        assertFalse(controller.transitionRunning)
    }

    @Test
    fun finishClearsTheAssociation() {
        val controller = RContainerTransformNavController<String>()
        controller.request("album")
        controller.markEntered("album")

        controller.finish("album")

        assertNull(controller.activeKey)
        assertFalse(controller.hasEntered("album"))
        assertFalse(controller.transitionRunning)
    }
}
