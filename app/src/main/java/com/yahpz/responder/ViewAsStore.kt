package com.yahpz.responder

import android.content.Context
import android.content.SharedPreferences
import com.yahpz.domain.parseRolePreviewRole

data class ImpersonationStash(
    val actorAccessToken: String,
    val actorRefreshToken: String,
    val actorUserId: String,
    val targetUserId: String,
    val targetFullName: String,
    val targetCallsign: String,
)

object ViewAsStore {
    private const val PREFS = "yahpaz_view_as"
    private const val ROLE = "role"
    private const val ACTOR_ACCESS = "actor_access"
    private const val ACTOR_REFRESH = "actor_refresh"
    private const val ACTOR_ID = "actor_id"
    private const val TARGET_ID = "target_id"
    private const val TARGET_NAME = "target_name"
    private const val TARGET_CALLSIGN = "target_callsign"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    private fun store(): SharedPreferences? = prefs

    fun rolePreviewRaw(): String? = store()?.getString(ROLE, null)

    fun writeRolePreview(role: String) {
        store()?.edit()?.putString(ROLE, role)?.apply()
    }

    fun clearRolePreview() {
        store()?.edit()?.remove(ROLE)?.apply()
    }

    fun readImpersonation(): ImpersonationStash? {
        val access = store()?.getString(ACTOR_ACCESS, null) ?: return null
        val refresh = store()?.getString(ACTOR_REFRESH, null) ?: return null
        val actorId = store()?.getString(ACTOR_ID, null) ?: return null
        val targetId = store()?.getString(TARGET_ID, null) ?: return null
        val name = store()?.getString(TARGET_NAME, null).orEmpty()
        val callsign = store()?.getString(TARGET_CALLSIGN, null).orEmpty()
        if (access.isEmpty() || refresh.isEmpty() || actorId.isEmpty() || targetId.isEmpty()) return null
        return ImpersonationStash(
            actorAccessToken = access,
            actorRefreshToken = refresh,
            actorUserId = actorId,
            targetUserId = targetId,
            targetFullName = name,
            targetCallsign = callsign,
        )
    }

    fun writeImpersonation(stash: ImpersonationStash) {
        store()?.edit()
            ?.putString(ACTOR_ACCESS, stash.actorAccessToken)
            ?.putString(ACTOR_REFRESH, stash.actorRefreshToken)
            ?.putString(ACTOR_ID, stash.actorUserId)
            ?.putString(TARGET_ID, stash.targetUserId)
            ?.putString(TARGET_NAME, stash.targetFullName)
            ?.putString(TARGET_CALLSIGN, stash.targetCallsign)
            ?.apply()
    }

    fun clearImpersonation() {
        store()?.edit()
            ?.remove(ACTOR_ACCESS)
            ?.remove(ACTOR_REFRESH)
            ?.remove(ACTOR_ID)
            ?.remove(TARGET_ID)
            ?.remove(TARGET_NAME)
            ?.remove(TARGET_CALLSIGN)
            ?.apply()
    }

    fun isImpersonating(): Boolean = readImpersonation() != null

    fun isRolePreviewing(): Boolean = parseRolePreviewRole(rolePreviewRaw()) != null

    fun clearAll() {
        store()?.edit()?.clear()?.apply()
    }
}
