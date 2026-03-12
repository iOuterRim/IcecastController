package com.example.icecastcontroller

import java.util.UUID

data class StreamSource(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val mimeType: String = "audio/mpeg"
) {
    companion object {
        val MIME_TYPES = listOf("audio/mpeg", "audio/aac", "audio/ogg", "audio/flac")

        fun guessMimeType(url: String): String {
            return when {
                url.contains(".aac", ignoreCase = true) -> "audio/aac"
                url.contains(".ogg", ignoreCase = true) -> "audio/ogg"
                url.contains(".flac", ignoreCase = true) -> "audio/flac"
                else -> "audio/mpeg"
            }
        }

        // Einige Demo-Quellen
        val EXAMPLES = listOf(
            StreamSource(name = "1.FM Costa Del Mar", url = "http://strm112.1.fm/costadelmarchillout_mobile_mp3", mimeType = "audio/mpeg"),
            StreamSource(name = "SWR3", url = "https://liveradio.swr.de/sw890cl/swr3/", mimeType = "audio/mpeg"),
            StreamSource(name = "Bayern 3", url = "https://dispatcher.rndfnk.com/br/br3/live/mp3/low", mimeType = "audio/mpeg"),
            StreamSource(name = "Deutschlandfunk", url = "https://st01.sslstream.dlf.de/dlf/01/128/mp3/stream.mp3", mimeType = "audio/mpeg"),
        )
    }
}
