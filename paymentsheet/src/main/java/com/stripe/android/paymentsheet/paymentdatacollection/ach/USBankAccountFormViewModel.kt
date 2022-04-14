package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.PaymentConfiguration
import com.stripe.android.connections.model.BankAccount
import com.stripe.android.connections.model.LinkedAccount
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.paymentsheet.paymentdatacollection.ach.di.DaggerUSBankAccountFormComponent
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.SectionFieldElement
import com.stripe.android.ui.core.elements.SimpleTextSpec
import dagger.Lazy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class USBankAccountFormViewModel @Inject internal constructor(
    private val application: Application,
    private val stripeRepository: StripeRepository,
    private val lazyPaymentConfig: Lazy<PaymentConfiguration>,
) : ViewModel() {
    private val _currentScreenState: MutableStateFlow<USBankAccountFormScreenState> =
        MutableStateFlow(USBankAccountFormScreenState.NameAndEmailCollection)
    val currentScreenState: StateFlow<USBankAccountFormScreenState>
        get() = _currentScreenState

    val nameElement: SectionFieldElement = SimpleTextSpec.NAME.transform()
    val name: StateFlow<String> = nameElement.getFormFieldValueFlow().map { formFieldsList ->
        formFieldsList.firstOrNull()?.second?.value ?: ""
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    val emailElement: SectionFieldElement = EmailSpec.transform("")
    val email: StateFlow<String> = emailElement.getFormFieldValueFlow().map { formFieldsList ->
        // formFieldsList contains only one element, for the email. Take the second value of
        // the pair, which is the FormFieldEntry containing the value entered by the user.
        formFieldsList.firstOrNull()?.second?.takeIf { it.isComplete }?.value ?: ""
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    val requiredFields = name
        .map { it.isNotEmpty() }
        .combine(
            emailElement.getFormFieldValueFlow().map { formFieldsList ->
                formFieldsList.firstOrNull()?.second?.isComplete ?: false
            }
        ) { validName, validEmail ->
            validName && validEmail
        }

    private var paymentLauncher: PaymentLauncher? = null
    private var collectBankAccountLauncher: CollectBankAccountLauncher? = null

    fun registerFragment(fragment: Fragment) {
        paymentLauncher = PaymentLauncher.create(
            fragment,
            lazyPaymentConfig.get().publishableKey,
            lazyPaymentConfig.get().stripeAccountId
        ) { paymentResult ->
            _currentScreenState.tryEmit(
                USBankAccountFormScreenState.ProcessPayment(paymentResult)
            )
        }

        collectBankAccountLauncher = CollectBankAccountLauncher.create(fragment) { result ->
            when (result) {
                is CollectBankAccountResult.Completed -> {
                    when (val paymentAccount = result.response.paymentAccount) {
                        is BankAccount -> {
                            result.response.intent.id?.let { intentId ->
                                _currentScreenState.tryEmit(
                                    USBankAccountFormScreenState.VerifyWithMicrodeposits(
                                        intentId = intentId,
                                        linkedAccountId = result.response.linkedAccountSessionId,
                                        bankName = paymentAccount.bankName,
                                        displayName = application.getString(R.string.us_bank_account_payment_sheet_default_display_name),
                                        last4 = paymentAccount.last4
                                    )
                                )
                            }
                        }
                        is LinkedAccount -> {
                            result.response.intent.id?.let { intentId ->
                                _currentScreenState.tryEmit(
                                    USBankAccountFormScreenState.MandateCollection(
                                        intentId = intentId,
                                        linkedAccountId = result.response.linkedAccountSessionId,
                                        bankName = paymentAccount.institutionName,
                                        displayName = paymentAccount.displayName,
                                        last4 = paymentAccount.last4
                                    )
                                )
                            }
                        }
                        null -> {
                            // handle error
                        }
                    }
                }
                is CollectBankAccountResult.Failed -> {
                    // handle error
                }
                is CollectBankAccountResult.Cancelled -> {
                    // handle cancel
                }
            }
        }
    }

    fun collectBankAccount(clientSecret: ClientSecret) {
        when (clientSecret) {
            is PaymentIntentClientSecret -> {
                collectBankAccountLauncher?.presentWithPaymentIntent(
                    lazyPaymentConfig.get().publishableKey,
                    clientSecret.value,
                    CollectBankAccountConfiguration.USBankAccount(
                        name.value,
                        email.value
                    )
                )
            }
            is SetupIntentClientSecret -> {
                collectBankAccountLauncher?.presentWithSetupIntent(
                    lazyPaymentConfig.get().publishableKey,
                    clientSecret.value,
                    CollectBankAccountConfiguration.USBankAccount(
                        name.value,
                        email.value
                    )
                )
            }
        }
    }

    fun confirm(
        clientSecret: ClientSecret,
        intentId: String,
        linkedAccountId: String
    ) {
        viewModelScope.launch {
            when (clientSecret) {
                is PaymentIntentClientSecret -> {
                    stripeRepository.attachLinkAccountSessionToPaymentIntent(
                        clientSecret.value,
                        intentId,
                        linkedAccountId,
                        ApiRequest.Options(
                            apiKey = lazyPaymentConfig.get().publishableKey,
                            stripeAccount = lazyPaymentConfig.get().stripeAccountId
                        )
                    )

                    val confirmPaymentIntentParams = ConfirmPaymentIntentParams.create(
                        clientSecret = clientSecret.value,
                        paymentMethodType = PaymentMethod.Type.USBankAccount
                    )

                    paymentLauncher?.confirm(confirmPaymentIntentParams)
                }
                is SetupIntentClientSecret -> {
                    stripeRepository.attachLinkAccountSessionToSetupIntent(
                        clientSecret.value,
                        intentId,
                        linkedAccountId,
                        ApiRequest.Options(
                            apiKey = lazyPaymentConfig.get().publishableKey,
                            stripeAccount = lazyPaymentConfig.get().stripeAccountId
                        )
                    )

                    val confirmSetupIntentParams = ConfirmSetupIntentParams.create(
                        clientSecret = clientSecret.value,
                        paymentMethodType = PaymentMethod.Type.USBankAccount
                    )

                    paymentLauncher?.confirm(confirmSetupIntentParams)
                }
            }
        }
    }

    fun clearBank() {
        _currentScreenState.tryEmit(USBankAccountFormScreenState.NameAndEmailCollection)
    }

    fun onDestroy() {
        paymentLauncher = null
        collectBankAccountLauncher = null
    }

    class Factory(
        private val applicationSupplier: () -> Application,
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle? = null
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            savedStateHandle: SavedStateHandle
        ): T {
            return DaggerUSBankAccountFormComponent.builder()
                .application(applicationSupplier())
                .build()
                .viewModel as T
        }
    }
}
