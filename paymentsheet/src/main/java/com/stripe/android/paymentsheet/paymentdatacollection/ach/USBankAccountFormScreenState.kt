package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.payments.paymentlauncher.PaymentResult

sealed class USBankAccountFormScreenState {
    object NameAndEmailCollection : USBankAccountFormScreenState()
    data class MandateCollection(
        val intentId: String,
        val linkedAccountId: String,
        val bankName: String?,
        val displayName: String?,
        val last4: String?
    ) : USBankAccountFormScreenState()
    data class VerifyWithMicrodeposits(
        val intentId: String,
        val linkedAccountId: String,
        val bankName: String?,
        val displayName: String?,
        val last4: String?
    ) : USBankAccountFormScreenState()
    data class ProcessPayment(val result: PaymentResult): USBankAccountFormScreenState()
}
