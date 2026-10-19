package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.floor
import kotlin.math.sin

/**
 * Manages SoundPool procedural audio and Vibrator haptics.
 * Implementations per Section C (Rules 12, 13, 14, 15).
 */
class AudioHapticsManager(private val context: Context) {

    // Kyun: User requirement C.13 — Single SupervisorJob scope jo release par cancel ho jaye
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isReleased = false

    // Kyun: User requirement C.15 — Mute flag (sound band, haptics chalu)
    var isMuted: Boolean = false

    // Kyun: User requirement C.12 — Thread-safe set of loaded sound IDs
    private val loadedSoundIds = Collections.synchronizedSet(HashSet<Int>())

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build().apply {
            // Kyun: User requirement C.12 — Track loaded sounds before playback
            setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    loadedSoundIds.add(sampleId)
                }
            }
        }

    private var soundSelectId = 0
    private var soundDropId = 0
    private var soundPopId = 0
    private var soundDenyId = 0
    private var soundDoneId = 0
    private val winNoteIds = mutableListOf<Int>()

    init {
        // Kyun: User requirement C.13 — Single scope ke andar sound generation aur loading
        scope.launch(Dispatchers.IO) {
            try {
                loadSounds()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadSounds() {
        if (isReleased) return
        val soundDir = File(context.cacheDir, "watersort_sfx").apply { mkdirs() }

        // Kyun: User requirement C.14 — WAV files sirf tab generate karo jab cache me na hon
        val selectFile = File(soundDir, "select.wav")
        if (!selectFile.exists() || selectFile.length() == 0L) {
            generateSweepWav(selectFile, 520.0, 760.0, 0.08, WaveType.SINE, 0.25)
        }
        soundSelectId = soundPool.load(selectFile.absolutePath, 1)

        val dropFile = File(soundDir, "drop.wav")
        if (!dropFile.exists() || dropFile.length() == 0L) {
            generateSweepWav(dropFile, 900.0, 230.0, 0.13, WaveType.SINE, 0.35)
        }
        soundDropId = soundPool.load(dropFile.absolutePath, 1)

        val popFile = File(soundDir, "pop.wav")
        if (!popFile.exists() || popFile.length() == 0L) {
            generateSweepWav(popFile, 660.0, 1180.0, 0.09, WaveType.TRIANGLE, 0.3)
        }
        soundPopId = soundPool.load(popFile.absolutePath, 1)

        val denyFile = File(soundDir, "deny.wav")
        if (!denyFile.exists() || denyFile.length() == 0L) {
            generateSweepWav(denyFile, 180.0, 110.0, 0.16, WaveType.SAWTOOTH, 0.28)
        }
        soundDenyId = soundPool.load(denyFile.absolutePath, 1)

        val doneFile = File(soundDir, "done.wav")
        if (!doneFile.exists() || doneFile.length() == 0L) {
            generateChordWav(doneFile, listOf(660.0, 880.0, 1320.0), listOf(0.0, 0.07, 0.14), 0.38, WaveType.SINE)
        }
        soundDoneId = soundPool.load(doneFile.absolutePath, 1)

        val winFrequencies = listOf(523.25, 659.25, 783.99, 1046.5, 1318.5)
        winFrequencies.forEachIndexed { idx, freq ->
            val noteFile = File(soundDir, "win_$idx.wav")
            if (!noteFile.exists() || noteFile.length() == 0L) {
                generateToneWav(noteFile, freq, 0.45, WaveType.TRIANGLE, 0.3)
            }
            val id = soundPool.load(noteFile.absolutePath, 1)
            winNoteIds.add(id)
        }
    }

    private fun playSound(soundId: Int, volume: Float, rate: Float) {
        if (isReleased || isMuted) return
        if (soundId != 0 && loadedSoundIds.contains(soundId)) {
            soundPool.play(soundId, volume, volume, 1, 0, rate)
        }
    }

    fun playSelect() {
        if (isReleased) return
        playSound(soundSelectId, 0.8f, 1.0f)
        buzz(12)
    }

    fun playDrop(pitch: Float) {
        if (isReleased) return
        val rate = pitch.coerceIn(0.5f, 2.0f)
        playSound(soundDropId, 0.9f, rate)
        buzz(8)
    }

    fun playPop(pitch: Float) {
        if (isReleased) return
        val rate = pitch.coerceIn(0.5f, 2.0f)
        playSound(soundPopId, 0.85f, rate)
        buzz(7)
    }

    fun playDeny() {
        if (isReleased) return
        playSound(soundDenyId, 0.7f, 1.0f)
        buzzPattern(longArrayOf(0, 15, 35, 15))
    }

    fun playDone() {
        if (isReleased) return
        playSound(soundDoneId, 1.0f, 1.0f)
    }

    fun playWin() {
        if (isReleased) return
        // Kyun: User requirement C.13 — playWin scope me launch hota hai
        scope.launch {
            winNoteIds.forEach { soundId ->
                if (!isReleased && !isMuted && loadedSoundIds.contains(soundId)) {
                    soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
                }
                delay(90)
            }
        }
        buzzPattern(longArrayOf(0, 30, 40, 50, 40, 30, 40, 60))
    }

    private fun buzz(ms: Long) {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(ms)
            }
        }
    }

    private fun buzzPattern(timings: LongArray) {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(timings, -1)
            }
        }
    }

    // Kyun: User requirement C.13 — Release calls cancel scope and mark isReleased
    fun release() {
        isReleased = true
        scope.cancel()
        soundPool.release()
    }

    private enum class WaveType { SINE, TRIANGLE, SAWTOOTH }

    private fun generateSweepWav(
        file: File,
        startFreq: Double,
        endFreq: Double,
        durationSeconds: Double,
        waveType: WaveType,
        amplitude: Double
    ) {
        val sampleRate = 44100
        val numSamples = (durationSeconds * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * PI * freq / sampleRate
            if (phase > 2.0 * PI) phase -= 2.0 * PI

            val envelope = when {
                progress < 0.1 -> progress / 0.1
                else -> (1.0 - progress) / 0.9
            }.coerceIn(0.0, 1.0)

            val rawSample = sampleWave(phase, waveType) * amplitude * envelope
            buffer[i] = (rawSample * 32767).toInt().coerceIn(-32768, 32767).toShort()
        }
        writeWavFile(file, sampleRate, buffer)
    }

    private fun generateToneWav(
        file: File,
        freq: Double,
        durationSeconds: Double,
        waveType: WaveType,
        amplitude: Double
    ) {
        val sampleRate = 44100
        val numSamples = (durationSeconds * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            phase += 2.0 * PI * freq / sampleRate
            if (phase > 2.0 * PI) phase -= 2.0 * PI

            val envelope = when {
                progress < 0.05 -> progress / 0.05
                else -> (1.0 - progress) / 0.95
            }.coerceIn(0.0, 1.0)

            val rawSample = sampleWave(phase, waveType) * amplitude * envelope
            buffer[i] = (rawSample * 32767).toInt().coerceIn(-32768, 32767).toShort()
        }
        writeWavFile(file, sampleRate, buffer)
    }

    private fun generateChordWav(
        file: File,
        freqs: List<Double>,
        delays: List<Double>,
        durationSeconds: Double,
        waveType: WaveType
    ) {
        val sampleRate = 44100
        val numSamples = (durationSeconds * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        val phases = DoubleArray(freqs.size)
        val noteDuration = 0.22

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var mixedSample = 0.0

            freqs.forEachIndexed { idx, freq ->
                val start = delays[idx]
                if (t >= start && t <= start + noteDuration) {
                    val noteProgress = (t - start) / noteDuration
                    phases[idx] += 2.0 * PI * freq / sampleRate
                    if (phases[idx] > 2.0 * PI) phases[idx] -= 2.0 * PI

                    val envelope = when {
                        noteProgress < 0.05 -> noteProgress / 0.05
                        else -> (1.0 - noteProgress) / 0.95
                    }.coerceIn(0.0, 1.0)

                    mixedSample += sampleWave(phases[idx], waveType) * 0.28 * envelope
                }
            }
            buffer[i] = (mixedSample * 32767).toInt().coerceIn(-32768, 32767).toShort()
        }
        writeWavFile(file, sampleRate, buffer)
    }

    private fun sampleWave(phase: Double, type: WaveType): Double {
        return when (type) {
            WaveType.SINE -> sin(phase)
            WaveType.TRIANGLE -> (2.0 / PI) * asin(sin(phase))
            WaveType.SAWTOOTH -> (2.0 * (phase / (2.0 * PI) - floor(phase / (2.0 * PI) + 0.5)))
        }
    }

    private fun writeWavFile(file: File, sampleRate: Int, samples: ShortArray) {
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val dataSize = samples.size * 2
        val chunkSize = 36 + dataSize

        FileOutputStream(file).use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(chunkSize)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16)
            header.putShort(1)
            header.putShort(numChannels.toShort())
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort((numChannels * bitsPerSample / 8).toShort())
            header.putShort(bitsPerSample.toShort())
            header.put("data".toByteArray())
            header.putInt(dataSize)

            out.write(header.array())

            val sampleBytes = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in samples) {
                sampleBytes.putShort(sample)
            }
            out.write(sampleBytes.array())
        }
    }
}
