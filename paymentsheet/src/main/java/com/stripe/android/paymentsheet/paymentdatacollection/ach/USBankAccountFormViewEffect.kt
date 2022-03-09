package com.stripe.android.paymentsheet.paymentdatacollection.ach

internal sealed class USBankAccountFormViewEffect {
    object Init : USBankAccountFormViewEffect()
    data class RequiredFieldsCollected(
        val name: String
    ) : USBankAccountFormViewEffect()
    data class BankAccountCollected(
        val paymentIntentId: String,
        val linkedAccountId: String
    ) : USBankAccountFormViewEffect()
    data class Error(
        val message: String?
    ) : USBankAccountFormViewEffect()
}
