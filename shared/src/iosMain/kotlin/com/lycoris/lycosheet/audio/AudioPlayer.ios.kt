package com.lycoris.lycosheet.audio

/** iOS stub — AVFoundation wired here when the iosApp target is added. */
actual class AudioPlayer {
    actual fun play(uri: String, onComplete: () -> Unit) {}
    actual fun stop() {}
    actual fun release() {}
}
