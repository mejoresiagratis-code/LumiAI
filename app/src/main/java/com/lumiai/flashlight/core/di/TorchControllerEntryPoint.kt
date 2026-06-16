package com.lumiai.flashlight.core.di

import com.lumiai.flashlight.core.torch.TorchController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets non-injectable components (e.g. AppWidgetProvider) reach the singleton TorchController. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TorchControllerEntryPoint {
    fun torchController(): TorchController
}
