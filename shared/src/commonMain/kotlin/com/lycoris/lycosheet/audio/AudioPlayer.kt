package com.lycoris.lycosheet.audio

/** Platform-specific audio playback. Registered in androidModule (needs Context).
 *  iOS stub does nothing — AVFoundation wired later when iosApp is added. */
expect class AudioPlayer {
    /** Play the audio at [uri] (absolute file path or content:// URI).
     *  [onComplete] is called when playback finishes naturally. */
    fun play(uri: String, onComplete: () -> Unit)
    fun stop()
    fun release()
}
