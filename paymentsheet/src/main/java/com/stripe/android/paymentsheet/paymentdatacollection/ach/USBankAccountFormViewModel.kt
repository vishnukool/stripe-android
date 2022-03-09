package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.connections.model.BankAccount
import com.stripe.android.connections.model.LinkedAccount
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import com.stripe.android.paymentsheet.paymentdatacollection.ach.di.DaggerUSBankAccountFormComponent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

internal class USBankAccountFormViewModel @Inject internal constructor(
    private val stripeRepository: StripeRepository,
) : ViewModel() {
    private val _currentScreenState: MutableStateFlow<USBankAccountFormScreenState> =
        MutableStateFlow(USBankAccountFormScreenState.NameAndEmailCollection)
    val currentScreenState: StateFlow<USBankAccountFormScreenState>
        get() = _currentScreenState

    private val _name = MutableStateFlow("")
    val name: StateFlow<String>
        get() = _name

    private val _email = MutableStateFlow("")
    val email: StateFlow<String>
        get() = _email

    fun updateScreenState(state: USBankAccountFormScreenState) {
        _currentScreenState.tryEmit(state)
    }

    fun updateName(name: String) {
        _name.tryEmit(name)
    }

    fun updateEmail(email: String) {
        _email.tryEmit(email)
    }

    fun handleCollectBankAccountResult(result: CollectBankAccountResult) {
        when (result) {
            is CollectBankAccountResult.Completed -> {
                val paymentAccount = result.response.paymentAccount
                val bankName = (paymentAccount as? LinkedAccount)?.institutionName
                    ?: (paymentAccount as? BankAccount)?.bankName
                val displayName = (paymentAccount as? LinkedAccount)?.displayName
                    ?: (paymentAccount as? BankAccount)?.routingNumber
                val last4 = (paymentAccount as? LinkedAccount)?.last4
                    ?: (paymentAccount as? BankAccount)?.last4

                updateScreenState(
                    USBankAccountFormScreenState.MandateCollection(
                        paymentIntentId = result.response.intent.id!!,
                        linkedAccountId = result.response.linkedAccountSessionId,
                        bankName = bankName,
                        displayName = displayName,
                        last4 = last4
                    )
                )
            }
            is CollectBankAccountResult.Failed -> {}
            is CollectBankAccountResult.Cancelled -> {}
        }
    }

    fun primaryButtonPressed(state: USBankAccountFormScreenState) {
        when (state) {
            is USBankAccountFormScreenState.NameAndEmailCollection -> {}
            is USBankAccountFormScreenState.MandateCollection -> {
            }
        }
    }

    class Factory(
        private val applicationSupplier: () -> Application,
        private val publishableKeyProvider: () -> String,
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
                .viewEffect(MutableSharedFlow())
                .publishableKeyProvider(publishableKeyProvider)
                .build()
                .viewModel as T
        }
    }
}
