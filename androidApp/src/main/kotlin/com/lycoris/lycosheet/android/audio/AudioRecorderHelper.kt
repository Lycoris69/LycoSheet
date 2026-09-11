package com.lycoris.lycosheet.android.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

/** Records audio to app-internal storage (no external-storage permission needed).
 *  Files are saved under [Context.filesDir]/audio/<uuid>.m4a and persist until the
 *  card is deleted — there is intentionally no automatic cleanup. */
class AudioRecorderHelper(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentPath: String? = null

    /** Start recording. Returns the absolute path that will be written to. */
    fun startRecording(): String {
        val dir = File(context.filesDir, "audio").also { it.mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.m4a")
        currentPath = file.absolutePath

        recorder = newRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(currentPath)
            prepare()
            start()
        }
        return currentPath!!
    }

    /** Stop recording. Returns the saved file path, or null if nothing was recorded. */
    fun stopRecording(): String? {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        return currentPath
    }

    fun release() {
        recorder?.release()
        recorder = null
    }

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
        else MediaRecorder()
}
