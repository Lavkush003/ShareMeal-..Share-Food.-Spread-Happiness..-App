package com.sharemeal.app.di

import com.sharemeal.app.core.config.AppConfig
import com.sharemeal.app.core.notification.AndroidLocalNotifier
import com.sharemeal.app.core.notification.LocalNotifier
import com.sharemeal.app.core.push.NoOpPushNotificationProvider
import com.sharemeal.app.core.push.PushNotificationProvider
import com.sharemeal.app.core.location.AndroidLocationPermissionChecker
import com.sharemeal.app.core.location.AndroidLocationProvider
import com.sharemeal.app.core.location.LocationPermissionChecker
import com.sharemeal.app.core.location.LocationProvider
import com.sharemeal.app.core.media.ImageRepository
import com.sharemeal.app.core.preferences.AppPreferencesRepository
import com.sharemeal.app.core.preferences.DataStoreAppPreferencesRepository
import com.sharemeal.app.data.auth.DataStoreMockAccountStore
import com.sharemeal.app.data.auth.MockAccountStore
import com.sharemeal.app.data.auth.MockAuthenticationRepository
import com.sharemeal.app.data.auth.UnavailableAuthenticationRepository
import com.sharemeal.app.data.donation.MockDonationRepository
import com.sharemeal.app.data.donation.UnavailableDonationRepository
import com.sharemeal.app.data.location.DataStoreSearchLocationRepository
import com.sharemeal.app.data.media.LocalPreviewImageRepository
import com.sharemeal.app.data.notification.DataStoreNotificationPreferencesRepository
import com.sharemeal.app.data.notification.MockNotificationRepository
import com.sharemeal.app.data.notification.UnavailableNotificationRepository
import com.sharemeal.app.data.pickup.MockPickupRepository
import com.sharemeal.app.data.pickup.UnavailablePickupRepository
import com.sharemeal.app.data.request.MockDonationRequestRepository
import com.sharemeal.app.data.request.UnavailableDonationRequestRepository
import com.sharemeal.app.data.user.MockUserRepository
import com.sharemeal.app.data.user.UnavailableUserRepository
import com.sharemeal.app.domain.repository.AuthenticationRepository
import com.sharemeal.app.data.offline.RoomDonationDraftRepository
import com.sharemeal.app.domain.repository.DonationDraftRepository
import com.sharemeal.app.domain.repository.DonationRepository
import com.sharemeal.app.domain.repository.DonationRequestRepository
import com.sharemeal.app.domain.repository.NotificationPreferencesRepository
import com.sharemeal.app.domain.repository.NotificationRepository
import com.sharemeal.app.domain.repository.PickupRepository
import com.sharemeal.app.domain.repository.SearchLocationRepository
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

    @Binds
    @Singleton
    abstract fun bindDonationDraftRepository(
        implementation: RoomDonationDraftRepository
    ): DonationDraftRepository

    @Binds
    @Singleton
    abstract fun bindLocationPermissionChecker(
        implementation: AndroidLocationPermissionChecker
    ): LocationPermissionChecker

    @Binds
    @Singleton
    abstract fun bindLocationProvider(
        implementation: AndroidLocationProvider
    ): LocationProvider

    @Binds
    @Singleton
    abstract fun bindSearchLocationRepository(
        implementation: DataStoreSearchLocationRepository
    ): SearchLocationRepository

    @Binds
    @Singleton
    abstract fun bindNotificationPreferencesRepository(
        implementation: DataStoreNotificationPreferencesRepository
    ): NotificationPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindLocalNotifier(
        implementation: AndroidLocalNotifier
    ): LocalNotifier

    @Binds
    @Singleton
    abstract fun bindPushNotificationProvider(
        implementation: NoOpPushNotificationProvider
    ): PushNotificationProvider
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
        queued: com.sharemeal.app.data.offline.QueuedUserRepository,
        unavailable: UnavailableUserRepository
    ): UserRepository = if (appConfig.isMockMode) queued else unavailable

    @Provides
    @Singleton
    fun provideDonationRepository(
        appConfig: AppConfig,
        queued: com.sharemeal.app.data.offline.QueuedDonationRepository,
        unavailable: UnavailableDonationRepository
    ): DonationRepository = if (appConfig.isMockMode) queued else unavailable

    @Provides
    @Singleton
    fun provideNgoRepository(
        appConfig: AppConfig,
        mock: com.sharemeal.app.data.ngo.MockNgoRepository,
        unavailable: com.sharemeal.app.data.ngo.UnavailableNgoRepository
    ): com.sharemeal.app.domain.repository.NgoRepository = if (appConfig.isMockMode) mock else unavailable

    @Provides
    @Singleton
    fun provideFoodDiscoveryRepository(
        appConfig: AppConfig,
        networkAware: com.sharemeal.app.data.offline.NetworkAwareFoodDiscoveryRepository,
        cached: com.sharemeal.app.data.offline.CachedFoodDiscoveryRepository
    ): com.sharemeal.app.domain.repository.FoodDiscoveryRepository =
        if (appConfig.isMockMode) networkAware else cached

    @Provides
    @Singleton
    fun provideDonationRequestRepository(
        appConfig: AppConfig,
        queued: com.sharemeal.app.data.offline.QueuedDonationRequestRepository,
        unavailable: UnavailableDonationRequestRepository
    ): DonationRequestRepository = if (appConfig.isMockMode) queued else unavailable

    @Provides
    @Singleton
    fun providePickupRepository(
        appConfig: AppConfig,
        queued: com.sharemeal.app.data.offline.QueuedPickupRepository,
        unavailable: UnavailablePickupRepository
    ): PickupRepository = if (appConfig.isMockMode) queued else unavailable

    @Provides
    @Singleton
    fun provideNotificationRepository(
        appConfig: AppConfig,
        mock: MockNotificationRepository,
        unavailable: UnavailableNotificationRepository
    ): NotificationRepository = if (appConfig.isMockMode) mock else unavailable
}
