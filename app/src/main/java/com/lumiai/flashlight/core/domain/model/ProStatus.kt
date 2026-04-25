package com.lumiai.flashlight.core.domain.model

/**
 * Represents the user's Pro purchase state.
 */
sealed class ProStatus {
    object Free       : ProStatus()
    object Pro        : ProStatus()
    object Loading    : ProStatus()
    data class Error(val msg: String) : ProStatus()
}
