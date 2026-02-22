package com.w3nto.app.di

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.w3nto.app.data.repository.AuthRepository
import com.w3nto.app.wallet.WalletManager
import com.w3nto.app.wallet.phantom.PhantomWallet
import com.w3nto.app.wallet.saga.SagaSeedVault
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Provides
    @Singleton
    fun provideWalletManager(
        @ApplicationContext context: Context,
        phantomWallet: PhantomWallet,
        sagaSeedVault: SagaSeedVault,
        authRepository: AuthRepository,
        firestore: FirebaseFirestore
    ): WalletManager = WalletManager(context, phantomWallet, sagaSeedVault, authRepository, firestore)
}
