package com.stripe.android.paymentsheet.injection

import android.content.res.Resources
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
@Component
internal interface FormViewModelComponent {
    val viewModel: FormViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun layout(layoutSpec: LayoutSpec): Builder

        @BindsInstance
        fun saveForFutureUseValue(
            @Named(SAVE_FOR_FUTURE_USE_INITIAL_VALUE) saveForFutureUseValue: Boolean
        ): Builder

        @BindsInstance
        fun saveForFutureUseVisibility(
            @Named(SAVE_FOR_FUTURE_USE_INITIAL_VISIBILITY) saveForFutureUseVisibility: Boolean
        ): Builder

        @BindsInstance
        fun merchantName(merchantName: String): Builder

        @BindsInstance
        fun resources(resources: Resources): Builder

        @BindsInstance
        fun resourceLoadingContext(workContext: CoroutineContext): Builder

        fun build(): FormViewModelComponent
    }
}
