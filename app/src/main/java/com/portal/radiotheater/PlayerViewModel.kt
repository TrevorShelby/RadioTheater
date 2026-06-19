package com.portal.radiotheater

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RadioStatus { OFF, TUNING, PLAYING, PAUSED, STATIC }

/**
 * Browsing and playback are independent: the dial, knob, search, and
 * PREV/NEXT only move [currentIndex] (what the display shows). Nothing
 * plays until PLAY is pressed, which starts the browsed episode and sets
 * [playingIndex]. When an episode ends, the *playing* episode auto-advances;
 * the browse view follows only if it was sitting on the playing episode.
 *
 * The app can host several shows. [currentShow] selects which catalog is
 * loaded; browse position, resume point, and played set are stored per show
 * (prefs keys prefixed with the show id). Volume and the selected show are
 * global. Switching shows stops playback and reloads the new show where the
 * user left it.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("radio", Context.MODE_PRIVATE)

    /** Shows available in the station picker. */
    val shows: List<Show> = ShowRegistry.all

    /** The show currently tuned in. */
    var currentShow by mutableStateOf(ShowRegistry.byId(prefs.getString("showId", ShowRegistry.default.id)))
        private set

    /** The loaded show's catalog. Reassigned when the show changes. */
    var catalog by mutableStateOf<List<Episode>>(emptyList())
        private set

    /** The episode being browsed/shown on the dial display. */
    var currentIndex by mutableIntStateOf(0)
        private set

    /** The episode loaded in the player, or -1 if nothing has been played yet. */
    var playingIndex by mutableIntStateOf(-1)
        private set

    var status by mutableStateOf(RadioStatus.OFF)
        private set

    var volume by mutableFloatStateOf(prefs.getFloat("volume", 0.8f))
        private set

    var searchVisible by mutableStateOf(false)
    var detailVisible by mutableStateOf(false)
    var stationVisible by mutableStateOf(false)

    /** Episode numbers the user has listened to the end of (current show). */
    var playedEps by mutableStateOf<Set<Int>>(emptySet())
        private set

    /** Playback position/duration of the loaded episode (polled twice a second). */
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set

    private val fx = SoundFx(app)

    private var fileMap: Map<Int, String>? = null
    private var triedFallback = false

    // Where the user left off in the current show (one-shot: consumed on first PLAY).
    private var pendingResumeIndex = -1
    private var pendingResumeMs = 0L

    /** Episode shown on the display (browsed). */
    val episode: Episode
        get() {
            val c = catalog
            return if (c.isEmpty()) PLACEHOLDER else c[currentIndex.coerceIn(0, c.size - 1)]
        }

    /** Episode currently loaded in the player, if any. */
    val playingEpisode: Episode? get() = catalog.getOrNull(playingIndex)

    /** Per-show prefs key. */
    private fun key(suffix: String) = "${currentShow.id}_$suffix"

    // Archive.org's mp3s carry no seek index, so ExoPlayer treats them as
    // unseekable (no duration, seeks snap to 0:00) unless constant-bitrate
    // seeking is enabled on the mp3 extractor.
    private val player: ExoPlayer = ExoPlayer.Builder(
        app,
        DefaultMediaSourceFactory(
            app,
            DefaultExtractorsFactory()
                .setConstantBitrateSeekingEnabled(true)
                .setConstantBitrateSeekingAlwaysEnabled(true),
        ),
    ).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            /* handleAudioFocus = */ true,
        )
        volume = this@PlayerViewModel.volume
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> status = RadioStatus.TUNING
                    Player.STATE_READY ->
                        status = if (isPlaying) RadioStatus.PLAYING else RadioStatus.PAUSED
                    Player.STATE_ENDED -> {
                        playingEpisode?.let { markPlayed(it.ep) }
                        prefs.edit().remove(key("resumeIndex")).remove(key("resumePos")).apply()
                        if (playingIndex in 0 until catalog.size - 1) {
                            val follow = currentIndex == playingIndex
                            play(playingIndex + 1)
                            if (follow) browseTo(playingIndex)
                        } else {
                            status = RadioStatus.PAUSED
                        }
                    }
                    Player.STATE_IDLE -> {}
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                if (playbackState == Player.STATE_READY) {
                    status = if (playing) RadioStatus.PLAYING else RadioStatus.PAUSED
                }
                if (!playing) saveResume()
            }

            override fun onPlayerError(error: PlaybackException) {
                // One retry: if the exact-name URL failed, fall back to the
                // constructed filename; if we were already on the fallback (or
                // hit a transient mid-stream error), retry the same URL once.
                // Resume from where playback stopped. Second failure = static.
                val pe = playingEpisode
                if (!triedFallback && pe != null) {
                    triedFallback = true
                    val resumeAt = currentPosition.coerceAtLeast(0L)
                    val fallbackUrl = EpisodeRepository.ARCHIVE_DOWNLOAD +
                        EpisodeRepository.itemFor(currentShow, pe.ep) + "/" + pe.fallbackFile
                    val failedUrl = currentMediaItem?.localConfiguration?.uri?.toString()
                    val retryUrl = if (failedUrl != null && failedUrl != fallbackUrl) fallbackUrl else failedUrl ?: fallbackUrl
                    setMediaItem(MediaItem.fromUri(retryUrl))
                    prepare()
                    if (resumeAt > 0) seekTo(resumeAt)
                    play()
                } else {
                    status = RadioStatus.STATIC
                }
            }
        })
    }

    init {
        migrateLegacyPrefs()
        loadShow(currentShow)

        // Poll playback position for the scrub bar. Guard every read: an
        // uncaught throw here (e.g. a transient player/state read) would escape
        // the coroutine and crash the whole app, so we swallow and retry.
        viewModelScope.launch {
            var tick = 0
            while (true) {
                try {
                    if (playingIndex >= 0) {
                        positionMs = player.currentPosition.coerceAtLeast(0L)
                        durationMs = player.duration.let { if (it > 0) it else 0L }
                        // Persist the resume point every ~5 seconds while loaded.
                        if (++tick % 10 == 0) saveResume()
                    }
                } catch (_: Exception) {
                    // transient; try again next tick
                }
                delay(500)
            }
        }
    }

    /**
     * Load a show's catalog and restore its per-show browse/resume/played state.
     * Sets [currentShow] first so [key] resolves to the new show.
     */
    private fun loadShow(show: Show) {
        currentShow = show
        val app = getApplication<Application>()
        catalog = EpisodeRepository.loadCatalog(app, show)
        val lastIdx = catalog.size - 1
        playedEps = prefs.getStringSet(key("playedEps"), emptySet())!!
            .mapNotNull { it.toIntOrNull() }.toSet()
        pendingResumeIndex = prefs.getInt(key("resumeIndex"), -1)
        pendingResumeMs = prefs.getLong(key("resumePos"), 0L)
        currentIndex = if (pendingResumeIndex in catalog.indices && pendingResumeMs > 0)
            pendingResumeIndex
        else
            prefs.getInt(key("lastIndex"), 0).coerceIn(0, lastIdx.coerceAtLeast(0))

        fileMap = EpisodeRepository.loadCachedFileMap(app, show)
        if (fileMap == null && show.fileRegex != null) {
            viewModelScope.launch {
                val m = withContext(Dispatchers.IO) {
                    EpisodeRepository.fetchAndCacheFileMap(getApplication(), show)
                }
                if (m != null && currentShow.id == show.id) fileMap = m
            }
        }
    }

    /** Tune to a different show: save current state, stop audio, load the new one. */
    fun switchShow(id: String) {
        stationVisible = false
        if (id == currentShow.id) return
        // Persist the outgoing show's browse + resume under its own keys.
        prefs.edit().putInt(key("lastIndex"), currentIndex).apply()
        saveResume()
        // Stop playback; playingIndex is an index into the old catalog.
        player.stop()
        player.clearMediaItems()
        playingIndex = -1
        triedFallback = false
        status = RadioStatus.OFF
        positionMs = 0L
        durationMs = 0L
        // Remember the selection globally and load the new show.
        prefs.edit().putString("showId", id).apply()
        loadShow(ShowRegistry.byId(id))
        fx.click()
    }

    /** One-time copy of pre-multishow prefs into the CBSRMT-prefixed keys. */
    private fun migrateLegacyPrefs() {
        if (prefs.getBoolean("migratedMultishow", false)) return
        val cb = ShowRegistry.CBSRMT.id
        val e = prefs.edit()
        if (prefs.contains("lastIndex")) e.putInt("${cb}_lastIndex", prefs.getInt("lastIndex", 0))
        if (prefs.contains("resumeIndex")) e.putInt("${cb}_resumeIndex", prefs.getInt("resumeIndex", -1))
        if (prefs.contains("resumePos")) e.putLong("${cb}_resumePos", prefs.getLong("resumePos", 0L))
        if (prefs.contains("playedEps"))
            e.putStringSet("${cb}_playedEps", prefs.getStringSet("playedEps", emptySet()))
        e.putBoolean("migratedMultishow", true).apply()
    }

    /** Move the browse view (display + needle) without touching playback. */
    fun browseTo(index: Int) {
        val i = index.coerceIn(0, catalog.size - 1)
        if (i != currentIndex) fx.detent()
        currentIndex = i
        prefs.edit().putInt(key("lastIndex"), currentIndex).apply()
    }

    /**
     * Continuous dial scrub: move the browse index with no per-step detent sound
     * or disk write, so dragging stays smooth even across a large catalog.
     * Persisted once the gesture ends via [commitBrowse].
     */
    fun scrubTo(index: Int) {
        val i = index.coerceIn(0, catalog.size - 1)
        if (i != currentIndex) currentIndex = i
    }

    /** Persist the browse position after a dial scrub gesture finishes. */
    fun commitBrowse() {
        prefs.edit().putInt(key("lastIndex"), currentIndex).apply()
    }

    /** Bakelite button press sound. */
    fun uiClick() = fx.click()

    fun nudge(delta: Int) = browseTo(currentIndex + delta)

    /** Load and play a specific episode. */
    fun play(index: Int) {
        if (catalog.isEmpty()) return
        val i = index.coerceIn(0, catalog.size - 1)
        playingIndex = i
        triedFallback = false
        durationMs = 0L
        try {
            val url = EpisodeRepository.streamUrl(currentShow, catalog[i], fileMap)
            status = RadioStatus.TUNING
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            // Pick up where the user left off last session.
            val resumeAt = if (i == pendingResumeIndex) pendingResumeMs else 0L
            pendingResumeIndex = -1
            if (resumeAt > 3000) {
                player.seekTo(resumeAt)
                positionMs = resumeAt
            } else {
                positionMs = 0L
            }
            player.play()
        } catch (e: Exception) {
            // Don't let a playback-setup failure crash the app.
            status = RadioStatus.STATIC
        }
    }

    /**
     * PLAY button: if the browsed episode is the one already loaded, toggle
     * pause/resume; otherwise start the browsed episode.
     */
    fun playPause() {
        when {
            playingIndex != currentIndex || status == RadioStatus.OFF || status == RadioStatus.STATIC ->
                play(currentIndex)
            player.isPlaying -> player.pause()
            else -> player.play()
        }
    }

    fun next() = nudge(+1)
    fun previous() = nudge(-1)

    /** Jump the browse view back to whatever is on air. */
    fun browseToPlaying() {
        if (playingIndex >= 0) browseTo(playingIndex)
    }

    private fun saveResume() {
        if (playingIndex < 0) return
        prefs.edit()
            .putInt(key("resumeIndex"), playingIndex)
            .putLong(key("resumePos"), player.currentPosition.coerceAtLeast(0L))
            .apply()
    }

    /** Millis to resume at if [index] is the episode left off last session. */
    fun resumeHintFor(index: Int): Long? =
        if (index == pendingResumeIndex && pendingResumeMs > 3000) pendingResumeMs else null

    fun isPlayed(ep: Int) = ep in playedEps

    fun markPlayed(ep: Int) {
        if (ep in playedEps) return
        playedEps = playedEps + ep
        prefs.edit().putStringSet(key("playedEps"), playedEps.map { it.toString() }.toSet()).apply()
    }

    /** Episodes the user has finished in [show] (for the station picker). */
    fun playedCount(show: Show): Int =
        if (show.id == currentShow.id) playedEps.size
        else prefs.getStringSet("${show.id}_playedEps", emptySet())!!.size

    /** Scrub bar: jump to a fraction (0..1) of the loaded episode. */
    fun seekToFraction(f: Float) {
        if (playingIndex < 0 || durationMs <= 0) return
        val t = (f.coerceIn(0f, 1f) * durationMs).toLong()
        player.seekTo(t)
        positionMs = t
    }

    /** Jump buttons: skip forward/back by [ms] (negative = back). */
    fun skip(ms: Long) {
        if (playingIndex < 0) return
        val d = player.duration
        var t = (player.currentPosition + ms).coerceAtLeast(0L)
        if (d > 0) t = t.coerceAtMost(d)
        player.seekTo(t)
        positionMs = t
    }

    fun updateVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        player.volume = volume
    }

    fun persistVolume() {
        prefs.edit().putFloat("volume", volume).apply()
    }

    fun search(query: String, limit: Int = 60): List<Pair<Int, Episode>> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val byNumber = q.toIntOrNull()
        val results = ArrayList<Pair<Int, Episode>>(limit)
        catalog.forEachIndexed { i, e ->
            if (results.size >= limit) return results
            val hit = (byNumber != null && e.ep == byNumber) ||
                e.title.contains(q, ignoreCase = true) ||
                e.writer.contains(q, ignoreCase = true) ||
                e.date.startsWith(q)
            if (hit) results.add(i to e)
        }
        return results
    }

    override fun onCleared() {
        saveResume()
        fx.release()
        player.release()
    }

    private companion object {
        val PLACEHOLDER = Episode(0, "—", "", "", "", "")
    }
}
