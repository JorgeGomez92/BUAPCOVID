/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult

class UserStateTransitionsOnInvalidResultTest {

    private val transitions = UserStateTransitions()

    @Test
    fun `default remains default`() {
        val testInfo = TestInfo(TestResult.INVALID, DateTime.now())

        val state = transitions.transitionOnTestResult(DefaultState, testInfo)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `symptomatic remains symptomatic`() {
        val symptomatic = buildSymptomaticState()
        val testInfo = TestInfo(TestResult.INVALID, DateTime.now())

        val state = transitions.transitionOnTestResult(symptomatic, testInfo)

        assertThat(state).isEqualTo(symptomatic)
    }

    @Test
    fun `positive remains positive`() {
        val positive = buildPositiveState()
        val testInfo = TestInfo(TestResult.INVALID, DateTime.now())

        val state = transitions.transitionOnTestResult(positive, testInfo)

        assertThat(state).isEqualTo(positive)
    }

    @Test
    fun `exposed remains exposed`() {
        val exposed = buildExposedState()
        val testInfo = TestInfo(TestResult.INVALID, DateTime.now())

        val state = transitions.transitionOnTestResult(exposed, testInfo)

        assertThat(state).isEqualTo(exposed)
    }
}
