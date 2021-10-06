package com.stripe.android.googlepaylauncher.injection

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.Logger
import com.stripe.android.googlepaylauncher.DefaultGooglePayRepository
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Module that provides [GooglePayRepository].
 *
 * Allows delayed setting of configuration parameters by providing a factory, for the case when the
 * configuration is not known at injection time.
 */
@Module
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GooglePayRepositoryModule {

    @Provides
    @Singleton
    fun provideGooglePayRepositoryFactory(
        appContext: Context,
        logger: Logger
    ): (
        environment: GooglePayEnvironment,
        billingAddressParameters: GooglePayJsonFactory.BillingAddressParameters,
        existingPaymentMethodRequired: Boolean
    ) -> GooglePayRepository =
        { environment, billingAddressParameters, existingPaymentMethodRequired ->
            DefaultGooglePayRepository(
                appContext,
                environment,
                billingAddressParameters,
                existingPaymentMethodRequired,
                logger
            )
        }
}
