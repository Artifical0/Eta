package io.github.mangi.eta.agent.display

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The floating preview never appears on the phone screen on its own; it shows only after the user
 * taps the live entry for that session.
 */
internal object VirtualScreenPreviewRequest {
    private val lock = Any()
    private val requested = MutableStateFlow<String?>(null)
    val session: StateFlow<String?> = requested.asStateFlow()

    fun request(sessionId: String) {
        if (sessionId.isBlank()) return
        synchronized(lock) { requested.value = sessionId }
    }

    /** Keeps a newer request alive when an older session tears down. */
    fun clear(sessionId: String? = null) {
        synchronized(lock) {
            if (sessionId == null || requested.value == sessionId) requested.value = null
        }
    }

    fun isRequested(sessionId: String?): Boolean =
        sessionId != null && synchronized(lock) { requested.value == sessionId }
}
