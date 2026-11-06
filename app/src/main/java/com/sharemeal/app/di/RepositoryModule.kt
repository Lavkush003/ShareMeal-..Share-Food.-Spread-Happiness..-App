package com.sharemeal.app.di

import com.sharemeal.app.core.config.AppConfig
import com.sharemeal.app.core.media.ImageRepository
import com.sharemeal.app.core.preferences.AppPreferencesRepository
import com.sharemeal.app.core.preferences.DataStoreAppPreferencesRepository
import com.sharemeal.app.data.auth.DataStoreMockAccountStore
import com.sharemeal.app.data.auth.MockAccountStore
import com.sharemeal.app.data.auth.MockAuthenticationRepository
import com.sharemeal.app.data.auth.UnavailableAuthenticationRepository
import com.sharemeal.app.data.donation.MockDonationRepository
import com.sharemeal.app.data.donation.UnavailableDonationRepository
import com.sharemeal.app.data.media.LocalPreviewImageRepository
import com.sharemeal.app.data.user.MockUserRepository
import com.sharemeal.app.data.user.UnavailableUserRepository
import com.sharemeal.app.domain.repository.AuthenticationRepository
import com.sharemeal.app.domain.repository.DonationRepository
import com.sharemeal.app.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAppPreferencesRepository(
        implementation: DataStoreAppPreferencesRepository
    ): AppPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindMockAccountStore(
        implementation: DataStoreMockAccountStore
    ): MockAccountStore

    @Binds
    @Singleton
    abstract fun bindImageRepository(
        implementation: LocalPreviewImageRepository
    ): ImageRepository
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryProviderModule {

    @Provides
    @Singleton
    fun provideAuthenticationRepository(
        appConfig: AppConfig,
        mock: MockAuthenticationRepository,
        unavailable: UnavailableAuthenticationRepository
    ): AuthenticationRepository = if (appConfig.isMockMode) mock else unavailable

    @Provides
    @Singleton
    fun provideUserRepository(
        appConfig: AppConfig,
        mock: MockUserRepository,
        unavailable: UnavailableUserRepository
    ): UserRepository = if (appConfig.isMockMode) mock else unavailable

    @Provides
    @Singleton
    fun provideDonationRepository(
        appConfig: AppConfig,
        mock: MockDonationRepository,
        unavailable: UnavailableDonationRepository
    ): DonationRepository = if (appConfig.isMockMode) mock else unavailable
}
