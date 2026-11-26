package com.cbdog.ptvbox

import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * Space-bounded LRU cache policy.
 * Avoids arbitrary TTL expiration so infrequently visited works and live logos stay cached
 * until the disk budget is exceeded, then evicts the least-recently-used entries first.
 */
internal object CacheBudget101 {
    const val METADATA_MAX_BYTES = 32L * 1024L * 1024L
    const val ANIME_MAX_BYTES = 16L * 1024L * 1024L
    const val PRESENTATION_MAX_ROWS = 2000

    fun touch(file: File) {
        if (file.isFile) runCatching { file.setLastModified(System.currentTimeMillis()) }
    }

    fun trimDirectory(dir: File, maxBytes: Long = METADATA_MAX_BYTES) {
        if (!dir.isDirectory) return
        val files = dir.listFiles()?.filter { it.isFile }?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        val target = (maxBytes * 4L) / 5L
        if (total <= maxBytes) return
        for (f in files) {
            if (total <= target) break
            val len = f.length()
            if (f.delete()) total -= len
        }
    }

    fun trimFiles(files: List<File>, maxBytes: Long = ANIME_MAX_BYTES) {
        val sorted = files.filter { it.isFile }.sortedBy { it.lastModified() }
        var total = sorted.sumOf { it.length() }
        val target = (maxBytes * 4L) / 5L
        if (total <= maxBytes) return
        for (f in sorted) {
            if (total <= target) break
            val len = f.length()
            if (f.delete()) total -= len
        }
    }

    fun touchPresentation(db: SQLiteDatabase, key: String) {
        runCatching {
            db.execSQL("UPDATE presentation_v5 SET stamp=? WHERE k=?", arrayOf<Any>(System.currentTimeMillis(), key))
        }
    }

    fun trimPresentation(db: SQLiteDatabase, maxRows: Int = PRESENTATION_MAX_ROWS) {
        runCatching {
            db.execSQL(
                "DELETE FROM presentation_v5 WHERE k IN (SELECT k FROM presentation_v5 ORDER BY stamp DESC LIMIT -1 OFFSET ?)",
                arrayOf(maxRows)
            )
        }
    }
}
