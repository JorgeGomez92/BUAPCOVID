/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.edgecases

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.TabletNotSupportedRobot

class TabletNotSupportedActivityTest : EspressoTest() {

    private val robot = TabletNotSupportedRobot()

    @Test
    fun displaysExpectedViews() {
        startTestActivity<TabletNotSupportedActivity>()

        robot.checkScreenIsDisplayed()
    }
}
