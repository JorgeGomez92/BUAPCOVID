/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.polidea.rxandroidble2.RxBleClient
import dagger.Component
import dagger.Module
import dagger.Provides
import org.joda.time.DateTime
import uk.nhs.nhsx.sonar.android.app.ble.DebugBleEventTracker
import uk.nhs.nhsx.sonar.android.app.ble.NoOpBleEventEmitter
import uk.nhs.nhsx.sonar.android.app.ble.SaveContactWorker
import uk.nhs.nhsx.sonar.android.app.ble.Scanner
import uk.nhs.nhsx.sonar.android.app.di.ApplicationComponent
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.di.module.BluetoothModule
import uk.nhs.nhsx.sonar.android.app.di.module.CryptoModule
import uk.nhs.nhsx.sonar.android.app.di.module.NetworkModule
import uk.nhs.nhsx.sonar.android.app.di.module.PersistenceModule
import uk.nhs.nhsx.sonar.android.app.http.KeyStorage
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.onboarding.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.ActivationCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.registration.TokenRetriever
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.storage.AppDatabase
import uk.nhs.nhsx.sonar.android.app.util.DeviceDetection
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        PersistenceModule::class,
        BluetoothModule::class,
        CryptoModule::class,
        NetworkModule::class,
        TestNotificationsModule::class
    ]
)
interface TestAppComponent : ApplicationComponent {
    fun getSonarIdProvider(): SonarIdProvider
    fun getKeyStorage(): KeyStorage
    fun getAppDatabase(): AppDatabase
    fun getUserStateStorage(): UserStateStorage
    fun getUserInbox(): UserInbox
    fun getOnboardingStatusProvider(): OnboardingStatusProvider
    fun getActivationCodeProvider(): ActivationCodeProvider
    fun getPostCodeProvider(): PostCodeProvider
}

class TestBluetoothModule(
    private val appContext: Context,
    private val rxBleClient: RxBleClient,
    private val currentTimestampProvider: () -> DateTime,
    private val scanIntervalLength: Int = 2
) : BluetoothModule(appContext, scanIntervalLength) {

    override fun provideRxBleClient(): RxBleClient =
        rxBleClient

    override fun provideScanner(
        rxBleClient: RxBleClient,
        saveContactWorker: SaveContactWorker,
        debugBleEventEmitter: DebugBleEventTracker,
        noOpBleEventEmitter: NoOpBleEventEmitter
    ): Scanner =
        Scanner(
            rxBleClient,
            saveContactWorker,
            debugBleEventEmitter,
            currentTimestampProvider,
            scanIntervalLength
        )

    var simulateUnsupportedDevice = false

    var simulateTablet = false

    override fun provideDeviceDetection(): DeviceDetection =
        if (simulateUnsupportedDevice || simulateTablet) {
            val adapter =
                if (simulateUnsupportedDevice) null else BluetoothAdapter.getDefaultAdapter()
            DeviceDetection(adapter, appContext, simulateTablet)
        } else {
            super.provideDeviceDetection()
        }

    fun reset() {
        simulateUnsupportedDevice = false
        simulateTablet = false
    }
}

@Module
class TestNotificationsModule {

    @Provides
    fun provideTokenRetriever(): TokenRetriever =
        object : TokenRetriever {
            override suspend fun retrieveToken() = "test firebase token #010"
        }
}
