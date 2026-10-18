package com.yahpz.responder

import androidx.activity.ComponentActivity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/** Play-signed installs may only update through Play In-App Updates. */
class PlayFlexibleUpdate(private val activity: ComponentActivity) {
    private val manager = AppUpdateManagerFactory.create(activity)
    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            manager.completeUpdate()
        }
    }

    init {
        manager.registerListener(listener)
    }

    /**
     * Checks if an update is available.
     * If [forceImmediate] is true, launches the full-screen blocking immediate update flow.
     * Otherwise, triggers a background flexible update.
     */
    fun startIfAvailable(forceImmediate: Boolean = false, onImmediateFallback: () -> Unit = {}) {
        if (!PlayInstallSource.installedFromPlay(activity)) return
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                manager.completeUpdate()
                return@addOnSuccessListener
            }
            val availability = info.updateAvailability()
            val available = availability == UpdateAvailability.UPDATE_AVAILABLE ||
                availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS

            if (!available) {
                if (forceImmediate) onImmediateFallback()
                return@addOnSuccessListener
            }

            if (forceImmediate || availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    runCatching {
                        manager.startUpdateFlowForResult(
                            info,
                            activity,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                            REQUEST_CODE_IMMEDIATE,
                        )
                    }.onFailure { onImmediateFallback() }
                } else {
                    onImmediateFallback()
                }
            } else if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                runCatching {
                    manager.startUpdateFlowForResult(
                        info,
                        activity,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        REQUEST_CODE_FLEXIBLE,
                    )
                }
            }
        }.addOnFailureListener {
            if (forceImmediate) onImmediateFallback()
        }
    }

    fun startImmediateUpdate(onFallback: () -> Unit = {}) {
        startIfAvailable(forceImmediate = true, onImmediateFallback = onFallback)
    }

    fun resumeIfInProgress() {
        if (!PlayInstallSource.installedFromPlay(activity)) return
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                manager.completeUpdate()
            } else if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    runCatching {
                        manager.startUpdateFlowForResult(
                            info,
                            activity,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                            REQUEST_CODE_IMMEDIATE,
                        )
                    }
                }
            }
        }
    }

    fun stop() {
        runCatching { manager.unregisterListener(listener) }
    }

    companion object {
        const val REQUEST_CODE_FLEXIBLE = 9173
        const val REQUEST_CODE_IMMEDIATE = 9174
    }
}

typealias PlayInAppUpdate = PlayFlexibleUpdate
