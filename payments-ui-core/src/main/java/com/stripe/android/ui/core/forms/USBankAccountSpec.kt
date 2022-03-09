package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val USBankAccountParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "us_bank_account"
)

internal val usBankAccountNameSection = SectionSpec(
    IdentifierSpec.Name,
    SimpleTextSpec.NAME
)

internal val usBankAccountEmailSection = SectionSpec(
    IdentifierSpec.Email,
    EmailSpec
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val USBankAccountForm = LayoutSpec.create(
    usBankAccountNameSection,
    usBankAccountEmailSection,
)
