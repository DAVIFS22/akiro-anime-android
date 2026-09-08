package com.akiro.addon

import com.squareup.moshi.Json

/**
 * Generic DTO for streams returned by addons. Fields may vary by addon implementation; adjust when you inspect the actual addon responses.
 */
data class StreamDto(
    val title: String? = null,
    val name: String? = null,
    val infoHash: String? = null,
    val magnet: String? = null,
    val url: String? = null,
    val quality: String? = null,
    val size: Long? = null,
    val provider: String? = null
)
