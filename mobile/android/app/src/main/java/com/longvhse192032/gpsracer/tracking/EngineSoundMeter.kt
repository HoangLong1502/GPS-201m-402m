package com.longvhse192032.gpsracer.tracking

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class EngineSoundMeter(private val scope: CoroutineScope) {
    private val _activeLeds = MutableStateFlow(0)
    val activeLeds: StateFlow<Int> = _activeLeds.asStateFlow()

    private var job: Job? = null
    private var audioRecord: AudioRecord? = null
    private var smoothLevel = 0f

    fun setEnabled(enabled: Boolean, context: android.content.Context) {
        if (!enabled) {
            stop()
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            _activeLeds.value = 0
            return
        }
        start()
    }

    private fun start() {
        stop()
        val sampleRate = 44_100
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) return

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer * 2,
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) return

        audioRecord = record
        record.startRecording()
        job = scope.launch(Dispatchers.Default) {
            val buffer = ShortArray(minBuffer)
            while (isActive) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    var sum = 0.0
                    for (i in 0 until read) {
                        val sample = buffer[i].toDouble()
                        sum += sample * sample
                    }
                    val rms = kotlin.math.sqrt(sum / read)
                    val db = if (rms > 0) 20 * log10(rms / Short.MAX_VALUE) else -60.0
                    val normalized = ((db + 60) / 60).coerceIn(0.0, 1.0).toFloat()
                    val alpha = 0.28f
                    smoothLevel += alpha * (normalized - smoothLevel)
                    val next = (smoothLevel * 12).roundToInt().coerceIn(0, 12)
                    _activeLeds.value = next
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }
        audioRecord?.release()
        audioRecord = null
        smoothLevel = 0f
        _activeLeds.value = 0
    }
}
