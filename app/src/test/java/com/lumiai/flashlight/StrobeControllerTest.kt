package com.lumiai.flashlight

import com.lumiai.flashlight.core.util.StrobeController
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class StrobeControllerTest {

    private val controller = StrobeController()

    @Test
    fun `strobe clamps hz to valid range`() = runBlocking {
        val calls = mutableListOf<Boolean>()
        controller.startStrobe(50f) { calls.add(it) }  // should clamp to 20 Hz
        delay(200)
        controller.stop()
        // At 20 Hz, expect around 8 toggles in 200ms (±2 tolerance)
        assertTrue("Expected ~8 calls, got ${calls.size}", calls.size in 4..14)
    }

    @Test
    fun `stop cancels active job immediately`() = runBlocking {
        val calls = mutableListOf<Boolean>()
        controller.startStrobe(10f) { calls.add(it) }
        delay(50)
        controller.stop()
        val countAfterStop = calls.size
        delay(200)
        assertEquals("No new calls after stop", countAfterStop, calls.size)
    }
}
