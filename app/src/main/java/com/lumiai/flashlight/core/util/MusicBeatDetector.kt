package com.lumiai.flashlight.core.util

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlin.math.sqrt

/**
 * Real-time beat detection via microphone using energy-based RMS analysis.
 *
 * Algorithm (based on BeatBlink open-source approach):
 * 1. Capture 44.1 kHz 16-bit PCM mono via AudioRecord
 * 2. Compute RMS energy for each buffer chunk (~23ms window)
 * 3. Maintain a rolling history of 43 energy values (~1 second window)
 * 4. Detect beat when current energy > average history * THRESHOLD
 * 5. Enforce minimum interval between beats (300ms = max 200 BPM)
 * 6. Fire [onBeat] callback on each detected beat
 *
 * Requires RECORD_AUDIO permission — checked before calling start().
 */
class MusicBeatDetector(
    private val onBeat: () -> Unit,
    private val threshold: Float = 1.5f,
    private val minIntervalMs: Long = 300L,
    private val minEnergy: Float = 50f,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    // Tuning constants
    private val SAMPLE_RATE          = 44100
    private val BUFFER_FRAMES        = 1024   // ~23ms at 44.1kHz
    private val HISTORY_SIZE         = 43     // ~1 second of energy history
    private val BEAT_THRESHOLD       get() = threshold
    private val MIN_BEAT_INTERVAL_MS get() = minIntervalMs
    private val MIN_ENERGY           get() = minEnergy

    fun start() {
        job = scope.launch {
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            val bufSize = maxOf(minBuf, BUFFER_FRAMES * 2)
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize,
            )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                return@launch
            }

            val buffer       = ShortArray(BUFFER_FRAMES)
            val energyHistory = ArrayDeque<Float>(HISTORY_SIZE)
            var lastBeatMs   = 0L

            recorder.startRecording()
            try {
                while (isActive) {
                    val read = recorder.read(buffer, 0, BUFFER_FRAMES)
                    if (read <= 0) continue

                    // RMS energy
                    var sum = 0.0
                    for (i in 0 until read) sum += buffer[i].toDouble() * buffer[i]
                    val rms = sqrt(sum / read).toFloat()

                    // Maintain rolling history
                    energyHistory.addLast(rms)
                    if (energyHistory.size > HISTORY_SIZE) energyHistory.removeFirst()

                    if (energyHistory.size < HISTORY_SIZE / 2) continue // warm up

                    val avgEnergy = energyHistory.average().toFloat()

                    // Beat detection: energy spike above threshold, above silence floor
                    val now = System.currentTimeMillis()
                    if (rms > avgEnergy * BEAT_THRESHOLD
                        && rms > MIN_ENERGY
                        && now - lastBeatMs > MIN_BEAT_INTERVAL_MS
                    ) {
                        lastBeatMs = now
                        withContext(Dispatchers.Main) { onBeat() }
                    }
                }
            } finally {
                recorder.stop()
                recorder.release()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun isRunning() = job?.isActive == true
}
