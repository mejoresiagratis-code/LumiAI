package com.lumiai.flashlight.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.lumiai.flashlight.core.data.repository.*
import com.lumiai.flashlight.core.util.StrobeController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lumiai_prefs")

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds @Singleton
    abstract fun bindFlashRepository(impl: FlashRepositoryImpl): FlashRepository

    @Binds @Singleton
    abstract fun bindBillingRepository(impl: BillingRepositoryImpl): BillingRepository

    companion object {
        @Provides @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.dataStore

        @Provides @Singleton
        fun provideStrobeController(): StrobeController = StrobeController()
    }
}
