/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.di.module

import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationApi
import uk.nhs.nhsx.sonar.android.app.http.HttpClient
import uk.nhs.nhsx.sonar.android.app.http.KeyStorage
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationTokenApi
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeApi
import uk.nhs.nhsx.sonar.android.app.registration.ResidentApi

@Module
class NetworkModule(
    private val baseUrl: String,
    private val sonarHeaderValue: String,
    private val appVersion: String
) {

    @Provides
    fun provideHttpClient(): HttpClient =
        HttpClient(
            sonarHeaderValue = sonarHeaderValue,
            appVersion = appVersion
        )

    @Provides
    fun residentApi(keyStorage: KeyStorage, httpClient: HttpClient): ResidentApi =
        ResidentApi(baseUrl, keyStorage, httpClient)

    @Provides
    fun coLocationApi(keyStorage: KeyStorage, httpClient: HttpClient): CoLocationApi =
        CoLocationApi(baseUrl, keyStorage, httpClient)

    @Provides
    fun referenceCodeApi(keyStorage: KeyStorage, httpClient: HttpClient): ReferenceCodeApi =
        ReferenceCodeApi(baseUrl, keyStorage, httpClient)

    @Provides
    fun notificationTokenApi(keyStorage: KeyStorage, httpClient: HttpClient): NotificationTokenApi =
        NotificationTokenApi(baseUrl, keyStorage, httpClient)
}
