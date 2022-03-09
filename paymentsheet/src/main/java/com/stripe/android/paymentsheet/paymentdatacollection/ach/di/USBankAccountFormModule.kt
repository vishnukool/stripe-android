package com.stripe.android.paymentsheet.paymentdatacollection.ach.di

import android.app.Application
import android.content.Context
import com.stripe.android.BuildConfig
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
internal object USBankAccountFormModule {

    @Provides
    fun providesAppContext(application: Application): Context = application

    @Provides
    @Named(PRODUCT_USAGE)
    fun providesProductUsage(): Set<String> = emptySet()

    @Provides
    @Named(ENABLE_LOGGING)
    fun providesEnableLogging(): Boolean = BuildConfig.DEBUG
}
