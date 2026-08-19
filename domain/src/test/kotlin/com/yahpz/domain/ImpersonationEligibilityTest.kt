package com.yahpz.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImpersonationEligibilityTest {
    private val actor = "actor-1"

    @Test
    fun allowsActiveNonSuperAdminOtherUser() {
        assertTrue(
            canImpersonateTarget(
                actor,
                ImpersonationTarget(id = "user-2", active = true, roles = listOf("responder")),
            ),
        )
    }

    @Test
    fun rejectsSelfInactiveAndSuperAdmin() {
        assertFalse(
            canImpersonateTarget(
                actor,
                ImpersonationTarget(id = actor, active = true, roles = listOf("admin")),
            ),
        )
        assertFalse(
            canImpersonateTarget(
                actor,
                ImpersonationTarget(id = "user-2", active = false, roles = listOf("responder")),
            ),
        )
        assertFalse(
            canImpersonateTarget(
                actor,
                ImpersonationTarget(id = "user-2", active = true, roles = listOf("admin", "super_admin")),
            ),
        )
        assertFalse(
            canImpersonateTarget(
                null,
                ImpersonationTarget(id = "user-2", active = true, roles = listOf("responder")),
            ),
        )
    }
}
