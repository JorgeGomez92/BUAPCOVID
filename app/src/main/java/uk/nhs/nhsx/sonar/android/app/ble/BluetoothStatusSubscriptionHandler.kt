/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import uk.nhs.nhsx.sonar.android.app.notifications.BluetoothNotificationHelper

class BluetoothStatusSubscriptionHandler(
    private val delegate: Delegate,
    private val notifications: BluetoothNotificationHelper
) {

    interface Delegate {
        fun startGattAndAdvertise()
        fun stopGattAndAdvertise()
        fun startScan()
        fun stopScanner()
    }

    data class CombinedStatus(
        val isBleClientInReadyState: Boolean,
        val isBluetoothEnabled: Boolean,
        val isLocationEnabled: Boolean
    )

    fun handle(status: CombinedStatus) {
        if (status.isLocationEnabled) {
            notifications.hideLocationIsDisabled()
        } else {
            notifications.showLocationIsDisabled()
        }

        if (status.isBluetoothEnabled) {
            notifications.hideBluetoothIsDisabled()
        } else {
            delegate.stopScanner()
            delegate.stopGattAndAdvertise()
            notifications.showBluetoothIsDisabled()
        }

        if (status.isBleClientInReadyState) {
            delegate.startGattAndAdvertise()
            delegate.startScan()
        }
    }
}
