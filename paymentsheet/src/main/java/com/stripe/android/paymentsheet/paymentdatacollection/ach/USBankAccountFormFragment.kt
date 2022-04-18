package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.paymentsheet.PaymentSheetActivity
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.elements.H6Text
import com.stripe.android.ui.core.elements.Html
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.SectionCard
import com.stripe.android.ui.core.elements.SectionController
import com.stripe.android.ui.core.elements.SectionElement
import com.stripe.android.ui.core.elements.SectionElementUI

/**
 * Fragment that displays a form for us_bank_account payment data collection
 */
internal class USBankAccountFormFragment : Fragment() {

    private val viewModelFactory: ViewModelProvider.Factory = PaymentSheetViewModel.Factory(
        { requireActivity().application },
        {
            requireNotNull(
                requireArguments().getParcelable(PaymentSheetActivity.EXTRA_STARTER_ARGS)
            )
        },
        (activity as? AppCompatActivity) ?: this,
        (activity as? AppCompatActivity)?.intent?.extras
    )

    private val sheetViewModel by activityViewModels<PaymentSheetViewModel> {
        viewModelFactory
    }

    private val viewModel by viewModels<USBankAccountFormViewModel> {
        USBankAccountFormViewModel.Factory(
            { requireActivity().application },
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.registerFragment(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(inflater.context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        lifecycleScope.launchWhenStarted {
            viewModel.requiredFields.collect {
                sheetViewModel.updateBuyButtonEnabled(it)
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.currentScreenState.collect { screenState ->
                when (screenState) {
                    is USBankAccountFormScreenState.NameAndEmailCollection -> {
                        sheetViewModel.updateNotesVisible(false)
                        sheetViewModel.updateBuyButtonState(
                            PrimaryButton.State.Ready(
                                getString(R.string.us_bank_account_payment_sheet_primary_button_continue)
                            )
                        )
                        sheetViewModel.updateBuyButtonAction {
                            viewModel.collectBankAccount(sheetViewModel.args.clientSecret)
                        }
                        setContent {
                            PaymentsTheme {
                                NameAndEmailCollectionScreen()
                            }
                        }
                    }
                    is USBankAccountFormScreenState.MandateCollection -> {
                        sheetViewModel.updateNotesVisible(true)
                        sheetViewModel.updateNotesContent {
                            Html(
                                html = stringResource(R.string.us_bank_account_payment_sheet_mandate),
                                imageGetter = emptyMap(),
                                color = PaymentsTheme.colors.subtitle,
                                style = PaymentsTheme.typography.body1,
                            )
                        }
                        sheetViewModel.updateBuyButtonState(PrimaryButton.State.Ready())
                        sheetViewModel.updateBuyButtonAction {
                            sheetViewModel.updateBuyButtonState(PrimaryButton.State.StartProcessing)
                            viewModel.confirm(
                                sheetViewModel.args.clientSecret,
                                screenState.intentId,
                                screenState.linkedAccountId,
                            )
                        }
                        setContent {
                            PaymentsTheme {
                                MandateCollectionScreen(
                                    screenState.bankName,
                                    screenState.displayName,
                                    screenState.last4
                                )
                            }
                        }
                    }
                    is USBankAccountFormScreenState.VerifyWithMicrodeposits -> {
                        val formattedMerchantName = sheetViewModel.args.config?.merchantDisplayName?.trimEnd { it == '.' }
                        formattedMerchantName?.let {
                            sheetViewModel.updateNotesVisible(true)
                            sheetViewModel.updateNotesContent {
                                Html(
                                    html = stringResource(
                                        R.string.us_bank_account_payment_sheet_mandate_verify_with_microdeposit,
                                        formattedMerchantName
                                    ),
                                    imageGetter = emptyMap(),
                                    color = PaymentsTheme.colors.subtitle,
                                    style = PaymentsTheme.typography.body1,
                                )
                            }
                        }
                        sheetViewModel.updateBuyButtonState(
                            PrimaryButton.State.Ready(
                                getString(R.string.us_bank_account_payment_sheet_primary_button_verify_account)
                            )
                        )
                        sheetViewModel.updateBuyButtonAction {
                            sheetViewModel.updateBuyButtonState(PrimaryButton.State.StartProcessing)
                            viewModel.confirm(
                                sheetViewModel.args.clientSecret,
                                screenState.intentId,
                                screenState.linkedAccountId,
                            )
                        }
                        setContent {
                            PaymentsTheme {
                                VerifyWithMicrodepositsScreen(
                                    screenState.bankName,
                                    screenState.displayName,
                                    screenState.last4
                                )
                            }
                        }
                    }
                    is USBankAccountFormScreenState.ProcessPayment -> {
                        sheetViewModel.onPaymentResult(screenState.result)
                    }
                }
            }
        }
    }

    override fun onDetach() {
        viewModel.onDestroy()
        sheetViewModel.resetViewState(null)
        super.onDetach()
    }

    @Composable
    private fun NameAndEmailCollectionScreen() {
        Column(Modifier.fillMaxWidth()) {
            NameAndEmailForm()
        }
    }

    @Composable
    private fun MandateCollectionScreen(
        bankName: String?,
        displayName: String?,
        last4: String?
    ) {
        Column(Modifier.fillMaxWidth()) {
            NameAndEmailForm()
            AccountDetailsForm(bankName, displayName, last4)
        }
    }

    @Composable
    private fun VerifyWithMicrodepositsScreen(
        bankName: String?,
        displayName: String?,
        last4: String?
    ) {
        Column(Modifier.fillMaxWidth()) {
            NameAndEmailForm()
            AccountDetailsForm(bankName, displayName, last4)
        }
    }

    @Composable
    private fun NameAndEmailForm() {
        Column(Modifier.fillMaxWidth()) {
            H6Text(
                text = stringResource(R.string.us_bank_account_payment_sheet_title),
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                SectionElementUI(
                    enabled = true,
                    element = SectionElement(
                        identifier = IdentifierSpec.Name,
                        fields = listOf(viewModel.nameElement),
                        controller = SectionController(
                            null,
                            listOf(viewModel.nameElement.sectionFieldErrorController())
                        )
                    ),
                    emptyList(),
                    viewModel.nameElement.identifier
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                SectionElementUI(
                    enabled = true,
                    element = SectionElement(
                        identifier = IdentifierSpec.Email,
                        fields = listOf(viewModel.emailElement),
                        controller = SectionController(
                            null,
                            listOf(viewModel.emailElement.sectionFieldErrorController())
                        )
                    ),
                    emptyList(),
                    viewModel.emailElement.identifier
                )
            }
        }
    }

    @Composable
    private fun AccountDetailsForm(
        bankName: String?,
        displayName: String?,
        last4: String?
    ) {
        val openDialog = remember { mutableStateOf(false) }
        val bankIcon = TransformToBankIcon(bankName ?: "")

        Column(
            Modifier.fillMaxWidth()
        ) {
            H6Text(
                text = stringResource(R.string.us_bank_account_payment_sheet_bank_account),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(bankIcon ?: R.drawable.stripe_ic_bank),
                            contentDescription = null,
                            modifier = Modifier
                                .height(40.dp)
                                .width(56.dp)
                        )
                        Text(text = "$displayName ••••$last4", fontWeight = FontWeight.Bold)
                    }
                    Image(
                        painter = painterResource(R.drawable.stripe_ic_clear),
                        contentDescription = null,
                        modifier = Modifier
                            .height(20.dp)
                            .width(20.dp)
                            .clickable {
                                openDialog.value = true
                            }
                    )
                }
            }
        }
        if (openDialog.value) {
            AlertDialog(
                onDismissRequest = {
                    openDialog.value = false
                },
                confirmButton = {
                    Button(onClick = {
                        openDialog.value = false
                        viewModel.clearBank()
                    }) {
                        Text(stringResource(id = R.string.us_bank_account_payment_sheet_alert_remove))
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        openDialog.value = false
                    }) {
                        Text(stringResource(id = R.string.us_bank_account_payment_sheet_alert_cancel))
                    }
                },
                title = {
                    Text(stringResource(id = R.string.us_bank_account_payment_sheet_alert_title))
                },
                text = {
                    last4?.let {
                        Text(text = stringResource(id = R.string.us_bank_account_payment_sheet_alert_text, last4))
                    }
                }
            )
        }
    }

    @Composable
    private fun textFieldColors() =
        TextFieldDefaults.textFieldColors(
            backgroundColor = MaterialTheme.colors.background,
            focusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        )
}
