/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleCustomOperation
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.withContext
import net.lachlanmckee.timberjunit.TimberTestRule
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdentifier
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@ExperimentalCoroutinesApi
class ScannerTest {

    private val bleClient = mockk<RxBleClient>()
    private val scanResult = mockk<ScanResult>()
    private val bleDevice = mockk<RxBleDevice>()
    private val connection = mockk<RxBleConnection>()
    private val saveContactWorker = mockk<SaveContactWorker>()

    private val timestamp = DateTime.now(DateTimeZone.UTC)
    private val rssi = -50
    private val period = 50L
    private val txPower = 47

    private lateinit var identifier: BluetoothIdentifier

    private val coroutineScope = TestCoroutineScope()

    @get:Rule
    val logAllOnFailuresRule: TimberTestRule = TimberTestRule.logAllWhenTestFails()

    val scanner = Scanner(
        bleClient,
        saveContactWorker,
        eventEmitter = DebugBleEventTracker { Base64.getEncoder().encodeToString(it) },
        currentTimestampProvider = { timestamp },
        scanIntervalLength = 1,
        base64Decoder = { Base64.getDecoder().decode(it) },
        base64Encoder = { Base64.getEncoder().encodeToString(it) }
    )

    @Before
    fun setUp() {
        Timber.plant(Timber.DebugTree())
        every { bleClient.observeStateChanges() } returns Observable.empty()
        every { bleClient.state } returns RxBleClient.State.READY
        every {
            bleClient.scanBleDevices(
                any<ScanSettings>(),
                any(),
                any()
            )
        } returns Observable.just(scanResult)

        every { scanResult.bleDevice } returns bleDevice
        every { bleDevice.connectionState } returns RxBleConnection.RxBleConnectionState.DISCONNECTED
        every { scanResult.scanRecord.txPowerLevel } returns txPower
        every { bleDevice.macAddress } returns "00:1B:44:11:3A:B7"

        every { bleDevice.establishConnection(false) } returns Observable.merge(
            Observable.just(connection),
            Observable
                .timer(period + 25, TimeUnit.MILLISECONDS)
                .flatMap {
                    val disconnectException = BleDisconnectedException.adapterDisabled("")
                    Observable.error<RxBleConnection>(disconnectException)
                }
        )
        every { connection.requestMtu(BluetoothIdentifier.SIZE + 2) } returns Single.just(
            BluetoothIdentifier.SIZE + 2
        )
        every { connection.queue(any<RxBleCustomOperation<RxBleConnection>>()) } returns Observable.just(
            connection
        )
        every { connection.readRssi() } returns Single.just(rssi)

        identifier = BluetoothIdentifier.fromBytes(Random.Default.nextBytes(BluetoothIdentifier.SIZE))
        every { connection.readCharacteristic(SONAR_IDENTITY_CHARACTERISTIC_UUID) } returns Single.just(
            identifier.asBytes()
        )

        every {
            saveContactWorker.createOrUpdateContactEvent(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Unit
    }

    @After
    fun tearDown() {
        coroutineScope.cleanupTestCoroutines()
    }

    @Test
    fun connectionWithSingularDevice() =
        runBlocking {
            scanner.start(coroutineScope)
            coroutineScope.advanceTimeBy(1_000)

            try {
                withContext(Dispatchers.Default) {
                    verify(timeout = 3_000) {
                        saveContactWorker.createOrUpdateContactEvent(
                            coroutineScope,
                            identifier,
                            rssi,
                            timestamp,
                            txPower
                        )
                    }
                }
            } finally {
                scanner.stop()
            }
        }

    @Test
    fun cachesIdentifierAgainstMacAddress() = runBlocking {
            scanner.start(coroutineScope)
            coroutineScope.advanceTimeBy(30_000)

            try {
                withContext(Dispatchers.Default) {
                    verify(timeout = 3_000) {
                        saveContactWorker.createOrUpdateContactEvent(
                            coroutineScope,
                            identifier,
                            rssi,
                            timestamp,
                            txPower
                        )
                    }

                    verify(exactly = 1) { connection.readCharacteristic(any<UUID>()) }
                }

                every { bleDevice.macAddress } returns "new-addr"
                coroutineScope.advanceTimeBy(30_000)

                withContext(Dispatchers.Default) {
                    verify(exactly = 2) { connection.readCharacteristic(any<UUID>()) }
                }
            } finally {
                scanner.stop()
            }
        }

    @Test
    fun emitsReadErrors() {
        val exception = Exception("Failed reading remote RSSI")
        every { connection.readRssi() } throws exception
        val eventEmitter = spyk<BleEventEmitter>()

        runBlocking {
            val scan = Scanner(
                bleClient,
                saveContactWorker,
                eventEmitter = eventEmitter,
                currentTimestampProvider = { timestamp },
                scanIntervalLength = 1,
                base64Decoder = { Base64.getDecoder().decode(it) },
                base64Encoder = { Base64.getEncoder().encodeToString(it) }
            )

            scan.start(coroutineScope)
            coroutineScope.advanceTimeBy(1_000)

            try {
                withContext(Dispatchers.Default) {
                    verify(exactly = 0, timeout = 3_000) {
                        saveContactWorker.createOrUpdateContactEvent(
                            any(),
                            any(),
                            any(),
                            any(),
                            any()
                        )
                    }

                    verify { eventEmitter.errorEvent("00:1B:44:11:3A:B7", exception) }
                }
            } finally {
                scan.stop()
            }
        }
    }

    @Test
    fun emitsConnectionErrors() {
        val exception = Exception("Connection failed")
        every { bleDevice.establishConnection(any()) } returns Observable.error(exception)
        val eventEmitter = spyk<BleEventEmitter>()

        runBlocking {
            val scan = Scanner(
                bleClient,
                saveContactWorker,
                eventEmitter = eventEmitter,
                currentTimestampProvider = { timestamp },
                scanIntervalLength = 1,
                base64Decoder = { Base64.getDecoder().decode(it) },
                base64Encoder = { Base64.getEncoder().encodeToString(it) }
            )

            scan.start(coroutineScope)
            coroutineScope.advanceTimeBy(1_000)

            try {
                withContext(Dispatchers.Default) {
                    verify(exactly = 0, timeout = 3_000) {
                        saveContactWorker.createOrUpdateContactEvent(
                            any(),
                            any(),
                            any(),
                            any(),
                            any()
                        )
                    }

                    verify { eventEmitter.errorEvent("00:1B:44:11:3A:B7", exception) }
                }
            } finally {
                scan.stop()
            }
        }
    }
}
