/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.inbox

import org.joda.time.DateTime

// Test Info
data class TestInfo(
    val result: TestResult,
    val date: DateTime
)

enum class TestResult {
    NEGATIVE,
    POSITIVE,
    INVALID
}
