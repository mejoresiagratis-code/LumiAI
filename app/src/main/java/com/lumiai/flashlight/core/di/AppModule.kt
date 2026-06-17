package com.lumiai.flashlight.core.di

import com.lumiai.flashlight.core.data.repository.BillingRepositoryImpl
import com.lumiai.flashlight.core.data.repository.BillingRepository
import com.lumiai.flashlight.core.data.repository.BatteryRepository
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.lumiai.flashlight.core.data.repository.*
import com.lumiai.flashlight.core.util.AiModeController
import com.lumiai.flashlight.core.torch.Camera2TorchHardware
import com.lumiai.flashlight.core.torch.TorchController
import com.lumiai.flashlight.core.torch.TorchHardware
import com.lumiai.flashlight.service.NotificationFlashController
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
    fun provideNotificationFlashController(
        torchController: TorchController,
    ): NotificationFlashController = NotificationFlashController(torchController)

    @Provides @Singleton
    fun provideAiModeController(
        @ApplicationContext context: Context,
    ): AiModeController = AiModeController(context)

    @Provides @Singleton
    fun provideTorchHardware(
        @ApplicationContext context: Context,
    ): TorchHardware = Camera2TorchHardware(context)

    @Provides @Singleton
    fun provideTorchController(
        hardware: TorchHardware,
    ): TorchController = TorchController(hardware)

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
    ): BillingRepository {
        // Single instance shared as both BillingRepository (interface) and impl.
        // BillingRepositoryImpl is NOT provided separately to avoid a second Singleton.
        return BillingRepositoryImpl(context)
    }

    @Provides @Singleton
    fun provideBatteryRepository(
        @ApplicationContext context: Context,
    ): BatteryRepository = BatteryRepository(context)

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides @Singleton
    fun provideRewardedProRepository(
        dataStore: DataStore<Preferences>,
    ): com.lumiai.flashlight.core.data.repository.RewardedProRepository =
        com.lumiai.flashlight.core.data.repository.RewardedProRepository(dataStore)
}
