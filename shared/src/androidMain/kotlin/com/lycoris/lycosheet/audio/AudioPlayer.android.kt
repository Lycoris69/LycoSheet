package com.lycoris.lycosheet.audio

import android.content.Context
import android.media.MediaPlayer

actual class AudioPlayer(private val context: Context) {
    private var player: MediaPlayer? = null

    actual fun play(uri: String, onComplete: () -> Unit) {
        stop()
        player = MediaPlayer().apply {
            setDataSource(uri)
            prepare()
            setOnCompletionListener { onComplete() }
            start()
        }
    }

    actual fun stop() {
        player?.release()
        player = null
    }

    actual fun release() = stop()
}
