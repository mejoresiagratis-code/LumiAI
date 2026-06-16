package com.lumiai.flashlight

import com.lumiai.flashlight.core.torch.TorchController
import com.lumiai.flashlight.core.torch.TorchHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TorchControllerTest {

    /** In-memory TorchHardware that echoes its state back through the OS-style callback. */
    private class FakeTorchHardware : TorchHardware {
        override val backCameraId = "0"
        override val maxStrengthLevel = 5
        val calls = mutableListOf<Boolean>()
        val strengths = mutableListOf<Int>()
        private var cb: ((String, Boolean) -> Unit)? = null
        override fun setTorchMode(on: Boolean) { calls += on; cb?.invoke("0", on) }
        override fun setTorchStrength(level: Int) { strengths += level; cb?.invoke("0", true) }
        override fun registerCallback(onChanged: (String, Boolean) -> Unit) {
            cb = onChanged
            onChanged("0", false) // OS delivers current state on register
        }
        override fun unregisterCallback() { cb = null }
    }

    @Test
    fun `torchState reflects OS callback, not the request`() = runBlocking {
        val hw = FakeTorchHardware()
        val controller = TorchController(hw, Dispatchers.Unconfined)
        assertEquals(false, controller.torchState.value) // initial, from register
        controller.setEnabled(true)
        assertEquals(true, controller.torchState.value)
        controller.setEnabled(false)
        assertEquals(false, controller.torchState.value)
    }

    @Test
    fun `setEnabled drives hardware exactly once per call`() = runBlocking {
        val hw = FakeTorchHardware()
        val controller = TorchController(hw, Dispatchers.Unconfined)
        controller.setEnabled(true)
        controller.setEnabled(false)
        assertEquals(listOf(true, false), hw.calls)
    }

    @Test
    fun `setStrength scales 0_1 fraction into 1_max level`() = runBlocking {
        val hw = FakeTorchHardware()
        val controller = TorchController(hw, Dispatchers.Unconfined)
        controller.setStrength(1.0f) // 1.0 * 5 = 5
        controller.setStrength(0.0f) // coerced to min 1
        assertEquals(listOf(5, 1), hw.strengths)
    }
}
