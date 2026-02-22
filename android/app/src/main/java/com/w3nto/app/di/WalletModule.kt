package com.w3nto.app.di

import com.w3nto.app.wallet.phantom.PhantomWallet
import com.w3nto.app.wallet.saga.SagaSeedVault
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WalletModule {

    @Provides
    @Singleton
    fun providePhantomWallet(): PhantomWallet = PhantomWallet()

    @Provides
    @Singleton
    fun provideSagaSeedVault(phantomWallet: PhantomWallet): SagaSeedVault =
        SagaSeedVault(phantomWallet)

}
