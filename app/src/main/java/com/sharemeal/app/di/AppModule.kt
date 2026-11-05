package com.sharemeal.app.di

import com.sharemeal.app.core.common.DispatcherProvider
import kotlinx.coroutines.Dispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
}
