package com.lumiai.flashlight.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.lumiai.flashlight.core.data.repository.*
import com.lumiai.flashlight.core.util.AiModeController
import com.lumiai.flashlight.core.util.StrobeController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lumiai_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideAiModeController(
        @ApplicationContext context: Context,
    ): AiModeController = AiModeController(context)

    @Provides @Singleton
    fun provideStrobeController(): StrobeController = StrobeController()

    @Provides @Singleton
    fun provideFlashRepositoryImpl(
        @ApplicationContext context: Context,
        strobeController: StrobeController,
        aiController: AiModeController,
    ): FlashRepositoryImpl = FlashRepositoryImpl(context, strobeController, aiController)

    /** Expose as interface for use cases that depend on FlashRepository */
    @Provides @Singleton
    fun provideFlashRepository(impl: FlashRepositoryImpl): FlashRepository = impl

    @Provides @Singleton
    fun provideBillingRepository(
        @ApplicationContext context: Context,
    ): BillingRepository = BillingRepositoryImpl(context)

    /** Expose BillingRepositoryImpl directly for any direct injection */
    @Provides @Singleton
    fun provideBillingRepositoryImpl(
        @ApplicationContext context: Context,
    ): BillingRepositoryImpl = BillingRepositoryImpl(context)

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}
