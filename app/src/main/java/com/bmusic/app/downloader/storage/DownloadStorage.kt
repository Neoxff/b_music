package com.bmusic.app.downloader.storage

/**
 * Step 1: Scoped-Storage targets for completed downloads (step 3 owns the
 * actual MediaStore insertion with IS_PENDING semantics).
 */
object DownloadStorage {
    const val AUDIO_RELATIVE_DIR = "Music/B Music"
    const val VIDEO_RELATIVE_DIR = "Movies/B Music"
}
