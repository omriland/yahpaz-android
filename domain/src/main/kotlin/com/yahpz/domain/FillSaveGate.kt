package com.yahpz.domain

enum class FillSaveKind { EXPLICIT, AUTOSAVE }

/** Lets one fill save (explicit or autosave) run at a time; a second one is refused, not queued. */
class FillSaveGate {
    var isSaving: Boolean = false
        private set

    fun tryBegin(kind: FillSaveKind): Boolean {
        if (isSaving) return false
        isSaving = true
        return true
    }

    fun finish() {
        isSaving = false
    }
}
