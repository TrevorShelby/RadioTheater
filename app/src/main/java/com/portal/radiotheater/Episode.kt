package com.portal.radiotheater

import android.content.Context
import android.net.Uri
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

/** One Internet Archive item that hosts a contiguous range of a show's episodes. */
data class ArchiveItem(val id: String, val firstEp: Int, val lastEp: Int)

/**
 * A radio show the app can tune to. Everything show-specific lives here so the
 * player, repository, and UI stay generic.
 *
 * [fileRegex], if non-null, enables runtime resolution of exact mp3 filenames
 * from the Archive.org metadata API. Its capture groups MUST be:
 *   group(1) = the full mp3 filename, group(2) = the episode number.
 * If null, the app trusts each episode's bundled [Episode.fallbackFile] and
 * never hits the metadata API (right for shows with irregular filenames).
 */
data class Show(
    val id: String,            // stable key; prefixes per-show prefs + cache files
    val name: String,          // brand-plate / station-list label (no decoration)
    val catalogAsset: String,  // bundled assets/ json filename
    val episodeCount: Int,     // for the station picker without parsing the catalog
    val items: List<ArchiveItem>,
    val fileRegex: String? = null,
)

/** The shows bundled with the app. Add a show by registering it here. */
object ShowRegistry {

    val CBSRMT = Show(
        id = "cbsrmt",
        name = "RADIO MYSTERY THEATER",
        catalogAsset = "catalog.json",
        episodeCount = 1399,
        items = listOf(
            ArchiveItem("APPBLY_radioMysteryTheater_0001", 1, 1288),
            ArchiveItem("APPBLY_radioMysteryTheater_0002", 1289, 1399),
        ),
        fileRegex = "\"name\"\\s*:\\s*\"(CBS-Radio-Mystery-Theater_\\d{4}_[0-9-]{10}_(\\d{4})_[^\"]*?\\.mp3)\"",
    )

    val XMINUS1 = Show(
        id = "xminus1",
        name = "X MINUS ONE",
        catalogAsset = "catalog_xminus1.json",
        episodeCount = 125,
        items = listOf(
            ArchiveItem("OTRR_X_Minus_One_Singles", 1, 125),
        ),
        // Filenames embed the episode number after the YY-MM-DD date, e.g.
        // XMinusOne55-04-24001NoContact.mp3 -> group(1)=file, group(2)=ep.
        fileRegex = "\"name\"\\s*:\\s*\"(XMinusOne\\d{2}-\\d{2}-\\d{2}(\\d{3})[^\"]*?\\.mp3)\"",
    )

    val DIMENSIONX = Show(
        id = "dimensionx",
        name = "DIMENSION X",
        catalogAsset = "catalog_dimensionx.json",
        episodeCount = 50,
        items = listOf(
            ArchiveItem("OTRR_Dimension_X_Singles", 1, 50),
        ),
        // Filenames embed the episode number after the ISO date + "__", e.g.
        // Dimension_X_1950-04-08__01_OuterLimit.mp3 -> group(1)=file, group(2)=ep.
        fileRegex = "\"name\"\\s*:\\s*\"(Dimension_X_\\d{4}-\\d{2}-\\d{2}__(\\d{2})_[^\"]*?\\.mp3)\"",
    )

    // Lights Out (horror/supernatural). Filenames are irregular (human-readable
    // with spaces, parens, and many unknown dates), so there is no runtime
    // resolution: fileRegex stays null and each episode's exact (URL-encoded)
    // filename is bundled in the catalog's fallbackFile.
    val LIGHTSOUT = Show(
        id = "lightsout",
        name = "LIGHTS OUT",
        catalogAsset = "catalog_lightsout.json",
        episodeCount = 71,
        items = listOf(
            ArchiveItem("lights-out-1943-04-20-29-kill", 1, 71),
        ),
    )

    val QUIETPLEASE = Show(
        id = "quietplease",
        name = "QUIET, PLEASE",
        catalogAsset = "catalog_quietplease.json",
        episodeCount = 91,
        items = listOf(
            ArchiveItem("Quiet_Please", 1, 106),
        ),
        // Filenames embed the episode number after the YYMMDD date, e.g.
        // Quiet_Please_470608_001_Nothing_Behind_the_Door.mp3 (a few are .MP3).
        fileRegex = "\"name\"\\s*:\\s*\"(Quiet_Please_\\d{6}_(\\d{3})_[^\"]*?\\.[Mm][Pp]3)\"",
    )

    val ESCAPE = Show(
        id = "escape",
        name = "ESCAPE",
        catalogAsset = "catalog_escape.json",
        episodeCount = 218,
        items = listOf(
            ArchiveItem("OTRR_Escape_Singles", 1, 228),
        ),
        // Filenames embed the episode number in "-NNN-", e.g.
        // Escape_47-07-07_-001-_The_Man_Who_Would_Be_King_-Raymond_Lawrence-.mp3
        fileRegex = "\"name\"\\s*:\\s*\"(Escape_\\d{2}-\\d{2}-\\d{2}_-(\\d{3})-_[^\"]*?\\.mp3)\"",
    )

    // Yours Truly, Johnny Dollar — the full 1949-1962 run, spread across seven
    // OTRR items by lead actor. The episode number is global and continuous, and
    // each item's filenames embed it: "YTJD 1955-10-03 231 The Macormack Matter - Ep 1.mp3".
    // Runtime resolution fetches all items and maps each ep to its item+file, so
    // the per-item ranges below only steer the offline fallback.
    val JOHNNYDOLLAR = Show(
        id = "johnnydollar",
        name = "JOHNNY DOLLAR",
        catalogAsset = "catalog_johnnydollar.json",
        episodeCount = 809,
        items = listOf(
            ArchiveItem("OTRR_YoursTrulyJohnnyDollar_Singles_Charles_Russell", 1, 105),
            ArchiveItem("OTRR_YoursTrulyJohnnyDollar_Singles_Edmond_OBrien", 106, 186),
            ArchiveItem("OTRR_YoursTrulyJohnnyDollar_Singles_John_Lund", 187, 230),
            ArchiveItem("OTRR_YoursTrulyJohnnyDollar_Singles_Bob_Bailey", 231, 419),
            ArchiveItem("OTRR_YoursTrulyJohnnyDollar_Singles_Bob_Bailey_2", 420, 714),
            ArchiveItem("OTRR_YoursTrulyJohnnyDollar_Singles_Robert_Readick", 715, 742),
            ArchiveItem("OTRR_YoursTrulyJohnnyDollar_Singles_Mandel_Kramer", 743, 809),
        ),
        fileRegex = "\"name\"\\s*:\\s*\"(YTJD \\d{4}-\\d{2}-\\d{2} (\\d+) [^\"]*?\\.[Mm][Pp]3)\"",
    )

    // More shows get registered here in later passes.

    val all: List<Show> = listOf(CBSRMT, XMINUS1, DIMENSIONX, LIGHTSOUT, QUIETPLEASE, ESCAPE, JOHNNYDOLLAR)

    val default: Show = CBSRMT

    fun byId(id: String?): Show = all.firstOrNull { it.id == id } ?: default
}

/**
 * Loads a show's bundled episode catalog and resolves exact Archive.org mp3
 * filenames.
 *
 * The catalog (titles, dates, descriptions) is bundled in assets. For shows
 * with a [Show.fileRegex], exact mp3 names are fetched once from the metadata
 * API, cached in filesDir, and merged at startup; if the fetch fails (or the
 * show has no regex) we fall back to filenames constructed/bundled in the
 * catalog.
 */
object EpisodeRepository {

    const val ARCHIVE_DOWNLOAD = "https://archive.org/download/"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun loadCatalog(context: Context, show: Show): List<Episode> {
        val json = context.assets.open(show.catalogAsset).bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        val list = ArrayList<Episode>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Episode(
                    ep = o.getInt("ep"),
                    title = o.getString("title"),
                    date = o.optString("date", ""),
                    writer = o.optString("writer", ""),
                    desc = o.optString("desc", ""),
                    fallbackFile = o.getString("fallbackFile"),
                )
            )
        }
        list.sortBy { it.ep }
        return list
    }

    private fun cacheFile(context: Context, show: Show) =
        File(context.filesDir, "filemap_${show.id}.json")

    /** Returns map of episode number -> "itemIdentifier/exactFileName.mp3" (no host). */
    fun loadCachedFileMap(context: Context, show: Show): Map<Int, String>? {
        if (show.fileRegex == null) return null
        val f = cacheFile(context, show)
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
    fun fetchAndCacheFileMap(context: Context, show: Show): Map<Int, String>? {
        val rx = show.fileRegex?.let { Regex(it) } ?: return null
        val m = HashMap<Int, String>(show.episodeCount + 64)
        for (item in show.items) {
            try {
                val req = Request.Builder().url("https://archive.org/metadata/${item.id}").build()
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body?.string() ?: return@use
                    for (match in rx.findAll(body)) {
                        val name = match.groupValues[1]
                        val ep = match.groupValues[2].toInt()
                        val derivative = name.endsWith("_64kb.mp3") ||
                            name.contains("_vbr") || name.contains("_esshigh") ||
                            name.contains("_esslow")
                        if (!derivative) m[ep] = "${item.id}/$name"
                        else if (ep !in m) m[ep] = "${item.id}/$name"
                    }
                }
            } catch (_: Exception) {
                // offline or archive.org hiccup; fall through with what we have
            }
        }
        if (m.size < (show.episodeCount / 2).coerceAtLeast(1)) return null // implausible; don't cache garbage
        return try {
            val o = JSONObject()
            m.forEach { (k, v) -> o.put(k.toString(), v) }
            cacheFile(context, show).writeText(o.toString())
            m
        } catch (_: Exception) {
            m
        }
    }

    /** Identifier of the Archive item that hosts [ep] for [show]. */
    fun itemFor(show: Show, ep: Int): String =
        show.items.firstOrNull { ep in it.firstEp..it.lastEp }?.id
            ?: show.items.lastOrNull()?.id ?: ""

    fun streamUrl(show: Show, e: Episode, fileMap: Map<Int, String>?): String {
        // Runtime-resolved names can contain spaces/brackets (e.g. Johnny Dollar),
        // so URL-encode the filename segment. For shows whose filenames are already
        // URL-safe (hyphens/underscores) this is a no-op. The bundled fallbackFile
        // is stored already URL-ready per show, so it is used as-is.
        val mapped = fileMap?.get(e.ep)
        if (mapped != null) {
            val slash = mapped.indexOf('/')
            return if (slash >= 0)
                ARCHIVE_DOWNLOAD + mapped.substring(0, slash) + "/" + Uri.encode(mapped.substring(slash + 1))
            else ARCHIVE_DOWNLOAD + Uri.encode(mapped)
        }
        return ARCHIVE_DOWNLOAD + itemFor(show, e.ep) + "/" + e.fallbackFile
    }
}
