package io.github.mangi.eta.agent.display

/**
 * The floating preview stays off the phone screen until the user asks for it from the live entry.
 * Every other condition only describes whether showing it is technically possible.
 */
internal object VirtualScreenFloatingPreviewPolicy {
    fun visible(
        virtualScreenEnabled: Boolean,
        previewEnabled: Boolean,
        canDrawOverlays: Boolean,
        interactive: Boolean,
        keyguardLocked: Boolean,
        agentControlling: Boolean,
        runId: String?,
        dismissedRun: String?,
        fullViewerVisible: Boolean,
        sessionId: String?,
        requestedSessionId: String?,
    ): Boolean = virtualScreenEnabled && previewEnabled && canDrawOverlays && interactive && !keyguardLocked &&
        agentControlling && runId != null && dismissedRun != runId && !fullViewerVisible &&
        sessionId != null && sessionId == requestedSessionId
}
