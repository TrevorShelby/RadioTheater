package com.portal.radiotheater

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Skeuomorphic radio sounds: mechanical detent ticks as episodes click past
 * and a Bakelite button thunk. Synthesized, played through a SoundPool.
 */
class SoundFx(context: Context) {

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val loaded = HashSet<Int>()
    private val clickId = pool.load(context, R.raw.click, 1)
    private val detentId = pool.load(context, R.raw.detent, 1)

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded.add(sampleId)
        }
    }

    fun click() {
        if (clickId in loaded) pool.play(clickId, 0.7f, 0.7f, 2, 0, 1f)
    }

    fun detent() {
        if (detentId in loaded) pool.play(detentId, 0.5f, 0.5f, 1, 0, 1f)
    }

    fun release() = pool.release()
}
