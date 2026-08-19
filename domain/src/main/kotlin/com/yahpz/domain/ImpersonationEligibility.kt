package com.yahpz.domain

data class ImpersonationTarget(
    val id: String,
    val active: Boolean,
    val roles: List<String>,
)

/** Client rules for who a Super Admin may become. Matches web `impersonationEligibility.ts`. */
fun canImpersonateTarget(actorUserId: String?, target: ImpersonationTarget): Boolean {
    if (actorUserId.isNullOrEmpty()) return false
    if (!target.active) return false
    if (target.id == actorUserId) return false
    if (hasSuperAdminRole(target.roles)) return false
    return true
}
