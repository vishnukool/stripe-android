package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.FormElement
import com.stripe.android.paymentsheet.SectionFieldElement
import com.stripe.android.paymentsheet.elements.CountryConfig
import com.stripe.android.paymentsheet.elements.DropdownFieldController
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.IbanConfig
import com.stripe.android.paymentsheet.elements.SaveForFutureUseController
import com.stripe.android.paymentsheet.elements.SectionController
import com.stripe.android.paymentsheet.elements.SimpleDropdownConfig
import com.stripe.android.paymentsheet.elements.SimpleTextFieldConfig
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.specifications.BankRepository
import com.stripe.android.paymentsheet.specifications.FormItemSpec
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec

/**
 * Transform a [LayoutSpec] data object into an Element, which
 * has a controller and identifier.  With only a single field in a section the section
 * controller will be a pass through the field controller.
 */
internal fun List<FormItemSpec>.transform(
    merchantName: String,
    bankRepository: BankRepository
): List<FormElement> =
    this.map {
        when (it) {
            is FormItemSpec.SaveForFutureUseSpec -> it.transform(merchantName)
            is FormItemSpec.SectionSpec -> it.transform(bankRepository)
            is FormItemSpec.MandateTextSpec -> it.transform(merchantName)
        }
    }

private fun FormItemSpec.SectionSpec.transform(bankRepository: BankRepository): FormElement.SectionElement {
    val fieldElements = this.fields.transform(bankRepository)

    // The controller of the section element will be the same as the field element
    // as there is only a single field in a section
    return FormElement.SectionElement(
        identifier = this.identifier,
        fieldElements,
        SectionController(
            this.title,
            fieldElements.map { it.controller }
        )
    )
}

/**
 * This function will transform a list of specs into a list of elements
 */
internal fun List<SectionFieldSpec>.transform(bankRepository: BankRepository? = null) = this.map {
    when (it) {
        is SectionFieldSpec.Email -> it.transform()
        is SectionFieldSpec.Iban -> it.transform()
        is SectionFieldSpec.Country -> it.transform()
        is SectionFieldSpec.SimpleDropdown -> it.transform(bankRepository)
        is SectionFieldSpec.SimpleText -> it.transform()
    }
}

private fun SectionFieldSpec.SimpleText.transform(): SectionFieldElement =
    SectionFieldElement.SimpleText(
        this.identifier,
        TextFieldController(
            SimpleTextFieldConfig(
                label = this.label,
                capitalization = this.capitalization,
                keyboard = this.keyboardType
            ),
            showOptionalLabel = this.showOptionalLabel
        )
    )

private fun FormItemSpec.MandateTextSpec.transform(merchantName: String) =
// It could be argued that the static text should have a controller, but
    // since it doesn't provide a form field we leave it out for now
    FormElement.MandateTextElement(
        this.identifier,
        this.stringResId,
        this.color,
        merchantName
    )

private fun SectionFieldSpec.Email.transform() =
    SectionFieldElement.Email(
        this.identifier,
        TextFieldController(EmailConfig()),
    )

private fun SectionFieldSpec.Iban.transform() =
    SectionFieldElement.Iban(
        this.identifier,
        TextFieldController(IbanConfig())
    )

private fun SectionFieldSpec.Country.transform() =
    SectionFieldElement.Country(
        this.identifier,
        DropdownFieldController(CountryConfig(this.onlyShowCountryCodes))
    )

private fun SectionFieldSpec.SimpleDropdown.transform(bankRepository: BankRepository?) =
    SectionFieldElement.SimpleDropdown(
        this.identifier,
        DropdownFieldController(
            SimpleDropdownConfig(
                label,
                bankRepository?.get(this.bankType)!!
            )
        )
    )

private fun FormItemSpec.SaveForFutureUseSpec.transform(merchantName: String) =
    FormElement.SaveForFutureUseElement(
        this.identifier,
        SaveForFutureUseController(
            this.identifierRequiredForFutureUse.map { element ->
                element.identifier
            }
        ),
        merchantName
    )
