package com.yahpz.responder

import androidx.activity.ComponentActivity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/** Play-signed installs may only update through Play. Sideload APK replacement is a policy violation. */
class PlayFlexibleUpdate(private val activity: ComponentActivity) {
    private val manager = AppUpdateManagerFactory.create(activity)
    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            manager.completeUpdate()
        }
    }

    fun startIfAvailable() {
        if (!PlayInstallSource.installedFromPlay(activity)) return
        manager.registerListener(listener)
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                manager.completeUpdate()
                return@addOnSuccessListener
            }
            val available =
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                    info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            if (available && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                runCatching {
                    manager.startUpdateFlowForResult(
                        info,
                        activity,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        REQUEST_CODE,
                    )
                }
            }
        }
    }

    fun stop() {
        runCatching { manager.unregisterListener(listener) }
    }

    companion object {
        private const val REQUEST_CODE = 9173
    }
}
