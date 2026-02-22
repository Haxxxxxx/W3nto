package com.w3nto.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.w3nto.app.data.remote.FirestoreDataSource
import com.w3nto.app.data.remote.StorageDataSource
import com.w3nto.app.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFirestoreDataSource(firestore: FirebaseFirestore): FirestoreDataSource =
        FirestoreDataSource(firestore)

    @Provides
    @Singleton
    fun provideStorageDataSource(storage: FirebaseStorage): StorageDataSource =
        StorageDataSource(storage)

    @Provides
    @Singleton
    fun provideProfileRepository(firestoreDataSource: FirestoreDataSource): ProfileRepository =
        ProfileRepository(firestoreDataSource)

    @Provides
    @Singleton
    fun providePageRepository(firestoreDataSource: FirestoreDataSource): PageRepository =
        PageRepository(firestoreDataSource)

    @Provides
    @Singleton
    fun provideBlockRepository(firestoreDataSource: FirestoreDataSource): BlockRepository =
        BlockRepository(firestoreDataSource)

    @Provides
    @Singleton
    fun provideStorageRepository(storageDataSource: StorageDataSource): StorageRepository =
        StorageRepository(storageDataSource)

    @Provides
    @Singleton
    fun provideAuthRepository(auth: FirebaseAuth): AuthRepository =
        AuthRepository(auth)

    @Provides
    @Singleton
    fun provideDiscoverRepository(firestoreDataSource: FirestoreDataSource): DiscoverRepository =
        DiscoverRepository(firestoreDataSource)

    @Provides
    @Singleton
    fun provideFavoritesRepository(firestoreDataSource: FirestoreDataSource): FavoritesRepository =
        FavoritesRepository(firestoreDataSource)
}
