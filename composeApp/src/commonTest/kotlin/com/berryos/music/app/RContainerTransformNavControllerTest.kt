package com.berryos.music.app

import art.rippledesign.compose.rui.navigation3.RContainerTransformNavController
import art.rippledesign.compose.rui.navigation3.RContainerTransformPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RContainerTransformNavControllerTest {
    @Test
    fun phasesFollowTheNavigationSequence() {
        val controller = RContainerTransformNavController<String>()

        controller.request("album")
        assertEquals(RContainerTransformPhase.Entering, controller.phase)
        assertTrue(controller.transitionRunning)
        assertTrue(controller.transformEntering)

        controller.markEntered("album")
        assertEquals(RContainerTransformPhase.Entered, controller.phase)
        assertFalse(controller.transitionRunning)

        controller.beginReturn("album")
        assertEquals(RContainerTransformPhase.Returning, controller.phase)
        assertFalse(controller.transformEntering)

        controller.markLanded("album")
        assertEquals(RContainerTransformPhase.Landed, controller.phase)
        assertTrue(controller.transitionRunning)

        controller.finish("album")
        assertEquals(RContainerTransformPhase.Idle, controller.phase)
    }

    @Test
    fun gestureReturnCanBeCancelledBackToEntered() {
        val controller = RContainerTransformNavController<String>()
        controller.request("album")
        controller.markEntered("album")

        controller.beginGestureReturn("album")
        assertEquals(RContainerTransformPhase.GestureReturning, controller.phase)

        controller.markGestureCancelled("album")
        assertEquals(RContainerTransformPhase.Entered, controller.phase)
        assertFalse(controller.transitionRunning)
    }

    @Test
    fun returningRequiresACompletedEnter() {
        val controller = RContainerTransformNavController<String>()

        controller.request("album")
        controller.updateReturningToSource(true)

        assertNull(controller.returningKey)

        controller.markEntered("album")
        controller.updateReturningToSource(true)

        assertEquals("album", controller.returningKey)
    }

    @Test
    fun gestureReturningRequiresACompletedEnter() {
        val controller = RContainerTransformNavController<String>()

        controller.request("album")
        controller.updateGestureReturningToSource(true)

        assertNull(controller.gestureReturningKey)

        controller.markEntered("album")
        controller.updateGestureReturningToSource(true)

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
