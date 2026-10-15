package io.github.mangi.eta.agent.runtime

/** MainActivity and RuntimeService share the main process. Track instances, not a single flag,
 * so an old Activity's onStop cannot hide its replacement during recreation.
 */
internal class StartedActivityTracker {
    private val instances = mutableSetOf<Any>()
    @Synchronized fun started(token: Any) { instances.add(token) }
    @Synchronized fun stopped(token: Any) { instances.remove(token) }
    val visible: Boolean @Synchronized get() = instances.isNotEmpty()
}

internal object EtaMainVisibility {
    val tracker = StartedActivityTracker()
    val visible: Boolean get() = tracker.visible
}
