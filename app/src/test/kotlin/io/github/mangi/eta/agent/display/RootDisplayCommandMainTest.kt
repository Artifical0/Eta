package io.github.mangi.eta.agent.display

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootDisplayCommandMainTest {
    @Test
    fun displayScopedWakeLockRequiresSecondaryDisplayGroup() {
        assertTrue(RootDisplayCommandMain.recoveryWakelockScopeAvailable(12, 2))
    }

    @Test
    fun defaultGroupOrPrimaryDisplayCannotScopeTheWakeLock() {
        assertFalse(RootDisplayCommandMain.recoveryWakelockScopeAvailable(18, 0))
        assertFalse(RootDisplayCommandMain.recoveryWakelockScopeAvailable(0, 3))
        assertFalse(RootDisplayCommandMain.recoveryWakelockScopeAvailable(0, 0))
    }
}
