/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.status.Symptom.ANOSMIA
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf
import uk.nhs.nhsx.sonar.android.app.util.toUtcNormalized

class UserStateTransitionsTest {
    private val transitions = UserStateTransitions()

    private val today = LocalDate(2020, 4, 10)
    private val symptomsWithoutTemperature = nonEmptySetOf(COUGH)
    private val symptomsWithTemperature = nonEmptySetOf(TEMPERATURE)

    @Before
    fun setUp() {
        DateTimeUtils.setCurrentMillisFixed(DateTime.now().millis)
    }

    @Test
    fun `diagnose - when symptoms date is 7 days ago or more, and no temperature`() {
        val sevenDaysAgoOrMore = today.minusDays(7)

        val state = transitions.diagnose(DefaultState, sevenDaysAgoOrMore, symptomsWithoutTemperature, today)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `diagnose - when symptoms date is 7 days ago or more, no temperature, and current state is Exposed`() {
        val exposedState = buildExposedState()
        val sevenDaysAgoOrMore = today.minusDays(7)

        val state = transitions.diagnose(exposedState, sevenDaysAgoOrMore, symptomsWithoutTemperature, today)

        assertThat(state).isEqualTo(exposedState)
    }

    @Test
    fun `diagnose - when symptoms date is 7 days ago or more, with temperature`() {
        val sevenDaysAgoOrMore = today.minusDays(7)
        val sevenDaysAfterSymptoms = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = transitions.diagnose(
            DefaultState,
            sevenDaysAgoOrMore,
            symptomsWithTemperature,
            today
        )

        assertThat(state).isEqualTo(
            SymptomaticState(
                sevenDaysAgoOrMore.toUtcNormalized(),
                sevenDaysAfterSymptoms,
                symptomsWithTemperature
            )
        )
    }

    @Test
    fun `diagnose - when symptoms date is less than 7 days ago, and no temperature`() {
        val lessThanSevenDaysAgo = today.minusDays(6)
        val sevenDaysAfterSymptoms = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = transitions.diagnose(
            DefaultState,
            lessThanSevenDaysAgo,
            symptomsWithoutTemperature,
            today
        )

        assertThat(state).isEqualTo(
            SymptomaticState(
                lessThanSevenDaysAgo.toUtcNormalized(),
                sevenDaysAfterSymptoms,
                symptomsWithoutTemperature
            )
        )
    }

    @Test
    fun `diagnose - when symptoms date is less than 7 days ago, with temperature`() {
        val lessThanSevenDaysAgo = today.minusDays(6)
        val sevenDaysAfterSymptoms = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = transitions.diagnose(
            DefaultState,
            lessThanSevenDaysAgo,
            symptomsWithTemperature,
            today
        )

        assertThat(state).isEqualTo(
            SymptomaticState(
                lessThanSevenDaysAgo.toUtcNormalized(),
                sevenDaysAfterSymptoms,
                symptomsWithTemperature
            )
        )
    }

    @Test
    fun `diagnose - when current state is exposed`() {
        val symptomDate = today.minusDays(6)

        val exposed = UserState.exposed(today)

        val state = transitions.diagnose(
            exposed,
            symptomDate,
            symptomsWithTemperature,
            today
        )

        assertThat(state)
            .isEqualTo(
                ExposedSymptomaticState(
                    since = symptomDate.toUtcNormalized(),
                    until = exposed.until,
                    exposedAt = exposed.since,
                    symptoms = symptomsWithTemperature
                )
            )
    }

    @Test
    fun `diagnoseForCheckin - with temperature`() {
        val tomorrow = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val currentState = UserState.symptomatic(today, nonEmptySetOf(COUGH))

        val state = transitions.diagnoseForCheckin(currentState, setOf(TEMPERATURE), today)

        assertThat(state).isEqualTo(
            SymptomaticState(
                currentState.since,
                tomorrow,
                nonEmptySetOf(TEMPERATURE)
            )
        )
    }

    @Test
    fun `diagnoseForCheckin - with cough and temperature`() {
        val aDateTime = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)
        val tomorrow = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val currentState = UserState.positive(aDateTime)

        val state = transitions.diagnoseForCheckin(currentState, setOf(COUGH, TEMPERATURE), today)

        assertThat(state).isEqualTo(
            PositiveState(
                currentState.since,
                tomorrow,
                nonEmptySetOf(COUGH, TEMPERATURE)
            )
        )
    }

    @Test
    fun `diagnoseForCheckin - with cough`() {
        val aDateTime = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)
        val currentState = UserState.positive(aDateTime)

        val state = transitions.diagnoseForCheckin(currentState, setOf(COUGH), today)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `diagnoseForCheckin - with anosmia`() {
        val currentState = UserState.symptomatic(today, nonEmptySetOf(TEMPERATURE))

        val state = transitions.diagnoseForCheckin(currentState, setOf(ANOSMIA), today)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `diagnoseForCheckin - with no symptoms`() {
        val aDateTime = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)
        val currentState = UserState.positive(aDateTime)

        val state = transitions.diagnoseForCheckin(currentState, emptySet(), today)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `test transitionOnContactAlert passes exposure date`() {
        val exposureDate = DateTime.parse("2020-04-21T16:00Z")
        val newState = transitions.transitionOnExposure(DefaultState, exposureDate)

        assertThat(newState).isInstanceOf(ExposedState::class.java)
        val exposedState = newState as ExposedState

        assertThat(exposedState.since.toLocalDate()).isEqualTo(exposureDate.toLocalDate())
    }

    @Test
    fun `test transitionOnContactAlert does not change any state other than default`() {

        buildExposedState().let {
            assertThat(transitions.transitionOnExposure(it, DateTime.now())).isEqualTo(it)
        }

        buildSymptomaticState().let {
            assertThat(transitions.transitionOnExposure(it, DateTime.now())).isEqualTo(it)
        }

        buildExposedSymptomaticState().let {
            assertThat(transitions.transitionOnExposure(it, DateTime.now())).isEqualTo(it)
        }

        buildPositiveState().let {
            assertThat(transitions.transitionOnExposure(it, DateTime.now())).isEqualTo(it)
        }
    }

    @Test
    fun `test expireExposedState`() {
        val exposedState = buildExposedState()
        val symptomaticState = buildSymptomaticState()

        val expiredExposedState = buildExposedState(until = DateTime.now().minusSeconds(1))
        val expiredSymptomaticState = buildSymptomaticState(until = DateTime.now().minusSeconds(1))

        assertThat(transitions.transitionOnExpiredExposedState(DefaultState)).isEqualTo(DefaultState)
        assertThat(transitions.transitionOnExpiredExposedState(exposedState)).isEqualTo(exposedState)
        assertThat(transitions.transitionOnExpiredExposedState(symptomaticState)).isEqualTo(symptomaticState)
        assertThat(transitions.transitionOnExpiredExposedState(expiredSymptomaticState)).isEqualTo(
            expiredSymptomaticState
        )

        assertThat(transitions.transitionOnExpiredExposedState(expiredExposedState)).isEqualTo(DefaultState)
    }

    @After
    fun tearDown() {
        DateTimeUtils.setCurrentMillisSystem()
    }
}
