/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.receivers

import android.content.Intent
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verifyAll
import org.junit.Before
import org.junit.Test
import testsupport.TestIntent
import testsupport.mockContextWithMockedAppComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.ReminderScheduler
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider

class BootCompletedReceiverTest {

    private val sonarIdProvider = mockk<SonarIdProvider>()
    private val reminderScheduler = mockk<ReminderScheduler>()
    private val context = mockContextWithMockedAppComponent()

    private val receiver = BootCompletedReceiver().also {
        it.sonarIdProvider = sonarIdProvider
        it.reminderScheduler = reminderScheduler
    }

    @Before
    fun setUp() {
        mockkObject(BluetoothService)

        every { reminderScheduler.reschedulePendingReminder() } returns Unit
        every { BluetoothService.start(any()) } returns Unit
    }

    @Test
    fun `onReceive - with unknown intent action`() {
        val intent = TestIntent("SOME_OTHER_ACTION")

        receiver.onReceive(context, intent)

        verifyAll {
            sonarIdProvider wasNot Called
            BluetoothService wasNot Called
            reminderScheduler wasNot Called
        }
    }

    @Test
    fun `onReceive - with sonarId`() {

        every { sonarIdProvider.hasProperSonarId() } returns true

        receiver.onReceive(context, TestIntent(Intent.ACTION_BOOT_COMPLETED))

        verifyAll {
            BluetoothService.start(context)
            reminderScheduler.reschedulePendingReminder()
        }
    }

    @Test
    fun `onReceive - without sonarId`() {
        every { sonarIdProvider.hasProperSonarId() } returns false

        receiver.onReceive(context, TestIntent(Intent.ACTION_BOOT_COMPLETED))

        verifyAll {
            BluetoothService wasNot Called
            reminderScheduler.reschedulePendingReminder()
        }
    }
}
