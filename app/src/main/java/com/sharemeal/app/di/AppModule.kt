package com.sharemeal.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.sharemeal.app.core.common.DispatcherProvider
import com.sharemeal.app.core.config.AppConfig
import com.sharemeal.app.core.network.ConnectivityNetworkMonitor
import com.sharemeal.app.core.network.NetworkMonitor
import com.sharemeal.app.data.local.database.ShareMealDatabase
import com.sharemeal.app.data.local.database.ShareMealDao
import com.sharemeal.app.data.sync.MockSyncRemoteDataSource
import com.sharemeal.app.data.sync.RetrofitSyncRemoteDataSource
import com.sharemeal.app.data.sync.SyncApiService
import com.sharemeal.app.data.sync.SyncRemoteDataSource
import com.sharemeal.app.data.sync.SyncWorkScheduler
import com.sharemeal.app.data.sync.UnconfiguredSyncRemoteDataSource
import com.sharemeal.app.data.sync.WorkManagerSyncWorkScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DispatcherProvider(
        io = Dispatchers.IO,
        default = Dispatchers.Default,
        main = Dispatchers.Main.immediate
    )

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("sharemeal_preferences")
    }

    @Provides
    @Singleton
    fun provideShareMealDatabase(
        @ApplicationContext context: Context
    ): ShareMealDatabase = Room.databaseBuilder(
        context,
        ShareMealDatabase::class.java,
        ShareMealDatabase.DATABASE_NAME
    )
        .addMigrations(ShareMealDatabase.MIGRATION_1_2)
        .build()

    @Provides
    @Singleton
    fun provideShareMealDao(database: ShareMealDatabase): ShareMealDao = database.shareMealDao()

    @Provides
    @Singleton
    fun provideNetworkMonitor(implementation: ConnectivityNetworkMonitor): NetworkMonitor = implementation

    @Provides
    @Singleton
    fun provideSyncWorkScheduler(
        implementation: WorkManagerSyncWorkScheduler
    ): SyncWorkScheduler = implementation

    @Provides
    @Singleton
    fun provideSyncApiService(retrofit: Retrofit): SyncApiService =
        retrofit.create(SyncApiService::class.java)

    @Provides
    @Singleton
    fun provideSyncRemoteDataSource(
        appConfig: AppConfig,
        mock: MockSyncRemoteDataSource,
        retrofit: RetrofitSyncRemoteDataSource,
        unconfigured: UnconfiguredSyncRemoteDataSource
    ): SyncRemoteDataSource = when {
        appConfig.isMockMode -> mock
        appConfig.apiBaseUrl.isBlank() -> unconfigured
        else -> retrofit
    }
}
