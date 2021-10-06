package com.stripe.android.paymentsheet

import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher

internal fun PaymentSheet.GooglePayConfiguration.BillingAddressConfig.convertToLauncherParams() =
    GooglePayPaymentMethodLauncher.BillingAddressConfig(
        isRequired,
        when (format) {
            PaymentSheet.GooglePayConfiguration.BillingAddressConfig.Format.Min ->
                GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Min
            PaymentSheet.GooglePayConfiguration.BillingAddressConfig.Format.Full ->
                GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full
        },
        isPhoneNumberRequired
    )

internal fun PaymentSheet.GooglePayConfiguration.BillingAddressConfig.convertToRepositoryParams() =
    GooglePayJsonFactory.BillingAddressParameters(
        isRequired,
        when (format) {
            PaymentSheet.GooglePayConfiguration.BillingAddressConfig.Format.Min -> GooglePayJsonFactory.BillingAddressParameters.Format.Min
            PaymentSheet.GooglePayConfiguration.BillingAddressConfig.Format.Full -> GooglePayJsonFactory.BillingAddressParameters.Format.Full
        },
        isPhoneNumberRequired
    )
