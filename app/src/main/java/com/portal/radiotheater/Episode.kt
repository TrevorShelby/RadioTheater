package com.portal.radiotheater

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class Episode(
    val ep: Int,
    val title: String,
    val date: String, // ISO yyyy-MM-dd
    val writer: String,
    val desc: String,
    val fallbackFile: String,
)

/**
 * Loads the bundled episode catalog and resolves exact Archive.org mp3 filenames.
 *
 * The catalog (titles, dates, descriptions) is bundled in assets. Exact mp3 file
 * names live in two Archive.org items (episodes 1-1288 and 1289-1399). They are
 * fetched once from the metadata API, cached in filesDir, and merged at startup.
 * If the fetch fails we fall back to filenames constructed from the catalog,
 * which are correct for ~97% of episodes.
 */
object EpisodeRepository {

    const val ARCHIVE_DOWNLOAD = "https://archive.org/download/"
    private val ITEMS = listOf("APPBLY_radioMysteryTheater_0001", "APPBLY_radioMysteryTheater_0002")
    private val FILE_RX = Regex("\"name\"\\s*:\\s*\"(CBS-Radio-Mystery-Theater_\\d{4}_[0-9-]{10}_(\\d{4})_[^\"]*?\\.mp3)\"")

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun loadCatalog(context: Context): List<Episode> {
        val json = context.assets.open("catalog.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        val list = ArrayList<Episode>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Episode(
                    ep = o.getInt("ep"),
                    title = o.getString("title"),
                    date = o.getString("date"),
                    writer = o.optString("writer", ""),
                    desc = o.optString("desc", ""),
                    fallbackFile = o.getString("fallbackFile"),
                )
            )
        }
        list.sortBy { it.ep }
        return list
    }

    private fun cacheFile(context: Context) = File(context.filesDir, "filemap.json")

    /** Returns map of episode number -> "itemIdentifier/exactFileName.mp3" (no host). */
    fun loadCachedFileMap(context: Context): Map<Int, String>? {
        val f = cacheFile(context)
        if (!f.exists()) return null
        return try {
            val o = JSONObject(f.readText())
            val m = HashMap<Int, String>(o.length())
            o.keys().forEach { k -> m[k.toInt()] = o.getString(k) }
            if (m.isEmpty()) null else m
        } catch (_: Exception) {
            null
        }
    }

    /** Blocking network fetch; call from a background dispatcher. */
    fun fetchAndCacheFileMap(context: Context): Map<Int, String>? {
        val m = HashMap<Int, String>(1500)
        for (item in ITEMS) {
            try {
                val req = Request.Builder().url("https://archive.org/metadata/$item").build()
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body?.string() ?: return@use
                    for (match in FILE_RX.findAll(body)) {
                        val name = match.groupValues[1]
                        val ep = match.groupValues[2].toInt()
                        val derivative = name.endsWith("_64kb.mp3") ||
                            name.contains("_vbr") || name.contains("_esshigh") ||
                            name.contains("_esslow")
                        if (!derivative) m[ep] = "$item/$name"
                        else if (ep !in m) m[ep] = "$item/$name"
                    }
                }
            } catch (_: Exception) {
                // offline or archive.org hiccup; fall through with what we have
            }
        }
        if (m.size < 100) return null // implausible; don't cache garbage
        return try {
            val o = JSONObject()
            m.forEach { (k, v) -> o.put(k.toString(), v) }
            cacheFile(context).writeText(o.toString())
            m
        } catch (_: Exception) {
            m
        }
    }

    fun streamUrl(e: Episode, fileMap: Map<Int, String>?): String {
        val path = fileMap?.get(e.ep)
            ?: (if (e.ep <= 1288) ITEMS[0] else ITEMS[1]) + "/" + e.fallbackFile
        return ARCHIVE_DOWNLOAD + path
    }
}
