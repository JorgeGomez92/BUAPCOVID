/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit.PactProviderRule
import au.com.dius.pact.consumer.junit.PactVerification
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.core.model.annotations.PactFolder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.StoppedUTCClock
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdSigner
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdentifier
import uk.nhs.nhsx.sonar.android.app.crypto.Cryptogram
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationApi
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationData
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationEvent
import uk.nhs.nhsx.sonar.android.app.diagnose.review.toJson
import uk.nhs.nhsx.sonar.android.app.encodeBase64
import uk.nhs.nhsx.sonar.android.app.http.HttpClient
import uk.nhs.nhsx.sonar.android.app.http.KeyStorage
import uk.nhs.nhsx.sonar.android.app.http.jsonObjectOf
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeApi
import uk.nhs.nhsx.sonar.android.app.generateSecretKey
import uk.nhs.nhsx.sonar.android.app.generateSignature
import uk.nhs.nhsx.sonar.android.app.testQueue
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat
import java.nio.ByteBuffer
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.random.Random

@ExperimentalCoroutinesApi
@PactFolder("pacts")
class ProximityApiPactTest {
    private val sonarId: String = UUID.randomUUID().toString()
    private val utcNow: LocalDateTime = LocalDateTime.now(DateTimeZone.UTC)
    private val timestamp: String = utcNow.toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
    private val secretKey: SecretKey =
        generateSecretKey()

    private val encryptionKeyStorage: KeyStorage = mockk<KeyStorage>(relaxed = true).apply {
        every { provideSecretKey() } returns secretKey
    }

    private val colocationData: CoLocationData = CoLocationData(
        sonarId,
        timestamp,
        listOf(Symptom.NAUSEA, Symptom.ANOSMIA),
        (0..Random.nextInt(3, 10)).map { generateCoLocationEvent() }
    )
    private val getReferenceCodeRequest = jsonObjectOf("sonarId" to sonarId)

    @get:Rule
    val provider = PactProviderRule("Proximity API", this)

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Pact(consumer = "Android App")
    fun getReferenceCodeFragment(builder: PactDslWithProvider): RequestResponsePact {
        return builder
            .given(
                "a confirmed registration",
                mutableMapOf<String, Any>(
                    "id" to sonarId,
                    "key" to encodeBase64(secretKey.encoded)
                )
            )
            .given("the date and time is", mutableMapOf<String, Any>("timestamp" to timestamp))
            // request
            .uponReceiving("a reference code request")
            .path(
                "/api/app-instances/linking-id"
            )
            .method("PUT")
            .matchHeader(
                "Sonar-Request-Timestamp",
                "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z",
                timestamp
            )
            .headers(
                "Sonar-Message-Signature",
                generateSignature(
                    secretKey,
                    timestamp,
                    getReferenceCodeRequest.toString().toByteArray()
                )
            )
            .body(getReferenceCodeRequest)
            // response
            .willRespondWith()
            .body(
                PactDslJsonBody().stringMatcher(
                    "linkingId",
                    "[0-9-abcdefghjkmnpqrstvwxyz]{4,20}",
                    "abcd-efgj"
                )
            )
            .status(HttpStatus.SC_OK)
            .toPact()
    }

    @Pact(consumer = "Android App")
    fun proximityUploadFragment(builder: PactDslWithProvider): RequestResponsePact {
        every { encryptionKeyStorage.provideSecretKey() } returns secretKey
        return builder
            .given(
                "a confirmed registration",
                mutableMapOf<String, Any>(
                    "id" to colocationData.sonarId,
                    "key" to encodeBase64(secretKey.encoded)
                )
            )
            .given("the date and time is", mutableMapOf<String, Any>("timestamp" to timestamp))
            // request
            .uponReceiving("a proximity data submission")
            .path(
                "/api/proximity-events/upload"
            )
            .method("PATCH")
            .matchHeader(
                "Sonar-Request-Timestamp",
                "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z",
                timestamp
            )
            .headers(
                "Sonar-Message-Signature",
                generateSignature(
                    secretKey,
                    timestamp,
                    colocationData.toJson().toString().toByteArray()
                )
            )
            .body(colocationData.toJson())
            // response
            .willRespondWith()
            .status(HttpStatus.SC_NO_CONTENT)
            .toPact()
    }

    @Test
    @PactVerification(fragment = "proximityUploadFragment")
    fun `verifies submission of proximity event data`() {
        val coLocationApi = CoLocationApi(
            provider.url,
            encryptionKeyStorage,
            createHttpClient()
        )

        val uploadRequest = coLocationApi.save(colocationData)
        runBlocking { uploadRequest.toCoroutineUnsafe() }

        assertThat(uploadRequest.isSuccess).isTrue()
    }

    @Test
    @PactVerification(fragment = "getReferenceCodeFragment")
    fun `verifies getting a reference code`() {
        val referenceCodeApi = ReferenceCodeApi(
            provider.url,
            encryptionKeyStorage,
            createHttpClient()
        )

        val referenceCodeRequest = referenceCodeApi.get(sonarId)
        runBlocking { referenceCodeRequest.toCoroutineUnsafe() }

        assertThat(referenceCodeRequest.isSuccess).isTrue()
    }

    private fun generateCoLocationEvent(): CoLocationEvent {
        val bluetoothIdentifier = generateBluetoothIdentifier()
        val numRssiValues = Random.nextInt(3, 10)
        return CoLocationEvent(
            encryptedRemoteContactId = encodeBase64(bluetoothIdentifier.asBytes()),
            rssiValues = encodeBase64(
                (0..numRssiValues).map { Random.nextInt(-100, 100).toByte() }.toByteArray()
            ),
            rssiIntervals = (0..numRssiValues).map { Random.nextInt(0, 100) },
            timestamp = DateTime.now(DateTimeZone.UTC).toUtcIsoFormat(),
            duration = Random.nextInt(0, 1000),
            txPowerInProtocol = randomTxPower().toByte(),
            txPowerAdvertised = randomTxPower().toByte(),
            countryCode = ByteBuffer.wrap("GB".toByteArray()).short,
            transmissionTime = Random.nextInt(0, 50),
            hmacSignature = encodeBase64(bluetoothIdentifier.hmacSignature)
        )
    }

    private fun generateBluetoothIdentifier(): BluetoothIdentifier {
        val cryptogram = Cryptogram.fromBytes(
            Random.Default.nextBytes(Cryptogram.SIZE)
        )

        val txPowerLevel = randomTxPower().toByte()
        val transmissionTime = Random.nextInt(0, 30)
        val countryCode = "GB".toByteArray()

        val signer = BluetoothIdSigner(encryptionKeyStorage)
        val signature = signer.computeHmacSignature(
            countryCode,
            cryptogram.asBytes(),
            txPowerLevel,
            ByteBuffer.wrap(ByteArray(4)).apply {
                putInt(transmissionTime)
            }.array()
        )

        return BluetoothIdentifier(
            countryCode,
            cryptogram,
            txPowerLevel,
            transmissionTime,
            signature
        )
    }

    private fun randomTxPower() = Random.nextInt(-20, -1)

    private fun createHttpClient() = HttpClient(
            queue = testQueue(),
            sonarHeaderValue = "some-header",
            appVersion = "buildInfo",
            utcClock = StoppedUTCClock(utcNow),
            base64enc = ::encodeBase64
        )
}
