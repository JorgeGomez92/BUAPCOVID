/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.joda.time.LocalDate
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.diagnose.review.DiagnoseReviewActivity
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.Symptom.values
import uk.nhs.nhsx.sonar.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.DiagnoseReviewRobot

class DiagnoseReviewActivityTest : EspressoTest() {

    private val diagnoseReviewRobot = DiagnoseReviewRobot()

    private val allSymptoms = values().toSet()
    private val someSymptoms = setOf(COUGH, TEMPERATURE)
    private val noSymptoms = emptySet<Symptom>()

    @Test
    fun displayingYesAnswers() {
        startTestActivity<DiagnoseReviewActivity> {
            putSymptoms(allSymptoms)
        }

        onView(withId(R.id.review_answer_temperature)).check(matches(withText(R.string.i_do_temperature)))
        onView(withId(R.id.review_answer_cough)).check(matches(withText(R.string.i_do_cough)))
        onView(withId(R.id.review_answer_anosmia)).check(matches(withText(R.string.i_do_anosmia)))
        onView(withId(R.id.review_answer_sneeze)).check(matches(withText(R.string.i_do_sneeze)))
        onView(withId(R.id.review_answer_stomach)).check(matches(withText(R.string.i_do_stomach)))
    }

    @Test
    fun displayingNoAnswers() {
        startTestActivity<DiagnoseReviewActivity> {
            putSymptoms(noSymptoms)
        }

        onView(withId(R.id.review_answer_temperature)).check(matches(withText(R.string.i_do_not_temperature)))
        onView(withId(R.id.review_answer_cough)).check(matches(withText(R.string.i_do_not_cough)))
        onView(withId(R.id.review_answer_anosmia)).check(matches(withText(R.string.i_do_not_anosmia)))
        onView(withId(R.id.review_answer_sneeze)).check(matches(withText(R.string.i_do_not_sneeze)))
        onView(withId(R.id.review_answer_stomach)).check(matches(withText(R.string.i_do_not_stomach)))
    }

    @Test
    fun submittingWithoutDate() {
        startTestActivity<DiagnoseReviewActivity> {
            putSymptoms(someSymptoms)
        }

        diagnoseReviewRobot.checkNoDateSelected()
        diagnoseReviewRobot.submit()
        diagnoseReviewRobot.checkDateErrorIsDisplayed()
    }

    @Test
    fun showingCalendarAndCanceling() {
        startTestActivity<DiagnoseReviewActivity> {
            putSymptoms(someSymptoms)
        }

        diagnoseReviewRobot.openCalendar()
        diagnoseReviewRobot.cancelCalendar()
        diagnoseReviewRobot.checkNoDateSelected()
    }

    @Test
    fun selectingTodayFromCalendar() {
        startTestActivity<DiagnoseReviewActivity> {
            putSymptoms(someSymptoms)
        }

        diagnoseReviewRobot.openCalendar()
        diagnoseReviewRobot.okCalendar()
        diagnoseReviewRobot.checkSelectedDate(LocalDate.now())
    }

    @Test
    fun selectingYesterdayFromSpinner() {
        startTestActivity<DiagnoseReviewActivity> {
            putSymptoms(someSymptoms)
        }

        diagnoseReviewRobot.selectYesterday()
        diagnoseReviewRobot.checkSelectedDate(R.string.yesterday)
    }
}
