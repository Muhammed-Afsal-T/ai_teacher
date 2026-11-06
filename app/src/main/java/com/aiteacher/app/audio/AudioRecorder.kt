package com.aiteacher.app.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    fun startRecording(): File? {
        val outputFile = File(context.cacheDir, "recording_${System.currentTimeMillis()}.m4a")
        currentOutputFile = outputFile

        recorder = createRecorder().apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
                Log.d("AudioRecorder", "Recording started -> ${outputFile.absolutePath}")
            } catch (e: IOException) {
                Log.e("AudioRecorder", "prepare() failed: ${e.message}", e)
                release()
                return null
            } catch (e: Exception) {
                Log.e("AudioRecorder", "start() failed: ${e.message}", e)
                release()
                return null
            }
        }
        return outputFile
    }

    fun stopRecording(): File? {
        return try {
            recorder?.apply {
                stop()
                reset()
                release()
            }
            recorder = null
            Log.d("AudioRecorder", "Recording stopped successfully.")
            currentOutputFile
        } catch (e: Exception) {
            Log.e("AudioRecorder", "stopRecording failed: ${e.message}", e)
            release()
            null
        }
    }

    fun cancelRecording() {
        try {
            recorder?.apply {
                reset()
                release()
            }
            recorder = null
            currentOutputFile?.delete()
            currentOutputFile = null
            Log.d("AudioRecorder", "Recording cancelled.")
        } catch (e: Exception) {
            Log.e("AudioRecorder", "cancelRecording error: ${e.message}", e)
        }
    }

    private fun release() {
        try {
            recorder?.release()
        } catch (_: Exception) {}
        recorder = null
    }
}
