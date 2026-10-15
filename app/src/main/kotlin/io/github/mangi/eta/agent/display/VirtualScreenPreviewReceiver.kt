package io.github.mangi.eta.agent.display

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Only an explicit tap on the live entry may bring the floating preview onto the phone screen. */
internal class VirtualScreenPreviewReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != ACTION_SHOW_PREVIEW) return
        val sessionId = intent.getStringExtra(EXTRA_SESSION) ?: return
        val active = VirtualScreenSession.state.value.display?.sessionId
        if (active != sessionId) return
        VirtualScreenPreviewRequest.request(sessionId)
    }

    companion object {
        const val ACTION_SHOW_PREVIEW = "io.github.mangi.eta.action.SHOW_VIRTUAL_SCREEN_PREVIEW"
        const val EXTRA_SESSION = "sessionId"
    }
}
