package io.github.mangi.eta.agent.display

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtualScreenFloatingPreviewTest {
    @After
    fun tearDown() = VirtualScreenPreviewRequest.clear()

    @Test
    fun previewStaysHiddenUntilTheUserAsksForIt() {
        assertFalse(VirtualScreenPreviewRequest.isRequested("session-a"))
        assertFalse(visible(requested = null))
    }

    @Test
    fun explicitRequestShowsThePreviewForThatSessionOnly() {
        VirtualScreenPreviewRequest.request("session-a")
        assertEquals("session-a", VirtualScreenPreviewRequest.session.value)
        assertTrue(visible(requested = "session-a"))
        assertFalse(visible(session = "session-b", requested = "session-a"))
    }

    @Test
    fun dismissingOrOpeningTheViewerHidesThePreviewAgain() {
        assertFalse(visible(requested = "session-a", dismissedRun = "run-1"))
        assertFalse(visible(requested = "session-a", fullViewerVisible = true))
        assertFalse(visible(requested = "session-a", keyguardLocked = true))
        assertFalse(visible(requested = "session-a", previewEnabled = false))
    }

    @Test
    fun clearingKeepsANewerSessionRequest() {
        VirtualScreenPreviewRequest.request("session-b")
        VirtualScreenPreviewRequest.clear("session-a")
        assertEquals("session-b", VirtualScreenPreviewRequest.session.value)
        VirtualScreenPreviewRequest.clear("session-b")
        assertNull(VirtualScreenPreviewRequest.session.value)
    }

    private fun visible(
        session: String? = "session-a",
        requested: String? = null,
        dismissedRun: String? = null,
        fullViewerVisible: Boolean = false,
        keyguardLocked: Boolean = false,
        previewEnabled: Boolean = true,
    ) = VirtualScreenFloatingPreviewPolicy.visible(
        virtualScreenEnabled = true,
        previewEnabled = previewEnabled,
        canDrawOverlays = true,
        interactive = true,
        keyguardLocked = keyguardLocked,
        agentControlling = true,
        runId = "run-1",
        dismissedRun = dismissedRun,
        fullViewerVisible = fullViewerVisible,
        sessionId = session,
        requestedSessionId = requested,
    )
}
