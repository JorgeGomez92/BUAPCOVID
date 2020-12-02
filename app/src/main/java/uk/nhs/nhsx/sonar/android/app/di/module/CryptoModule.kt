/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.app.http.AndroidSecretKeyStorage
import uk.nhs.nhsx.sonar.android.app.http.DelegatingKeyStore
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdProvider
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdSigner
import uk.nhs.nhsx.sonar.android.app.crypto.CryptogramProvider
import uk.nhs.nhsx.sonar.android.app.crypto.CryptogramStorage
import uk.nhs.nhsx.sonar.android.app.crypto.Encrypter
import uk.nhs.nhsx.sonar.android.app.http.KeyStorage
import uk.nhs.nhsx.sonar.android.app.http.PublicKeyStorage
import uk.nhs.nhsx.sonar.android.app.http.SecretKeyStorage
import uk.nhs.nhsx.sonar.android.app.http.SharedPreferencesPublicKeyStorage
import java.security.KeyStore
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider

@Module
class CryptoModule(
    private val context: Context,
    private val keyStore: KeyStore
) {
    @Provides
    fun provideEncryptionKeyStorage(
        secretKeyStorage: SecretKeyStorage,
        publicKeyStorage: PublicKeyStorage
    ): KeyStorage = DelegatingKeyStore(secretKeyStorage, publicKeyStorage)

    @Provides
    fun provideSecretKeyStorage(): SecretKeyStorage = AndroidSecretKeyStorage(keyStore, context)

    @Provides
    fun providePublicKeyStorage(): PublicKeyStorage = SharedPreferencesPublicKeyStorage(context)

    @Provides
    fun provideCryptogramStorage(context: Context): CryptogramStorage =
        CryptogramStorage(context)

    @Provides
    fun provideCryptogramProvider(
        sonarIdProvider: SonarIdProvider,
        encrypter: Encrypter,
        cryptogramStorage: CryptogramStorage
    ): CryptogramProvider =
        CryptogramProvider(sonarIdProvider, encrypter, cryptogramStorage)

    @Provides
    fun provideBluetoothIdentitymProvider(
        cryptogramProvider: CryptogramProvider,
        signer: BluetoothIdSigner
    ): BluetoothIdProvider =
        BluetoothIdProvider(cryptogramProvider, signer)
}
