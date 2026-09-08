Integrate Torrentio addon (initial implementation)

This branch adds a minimal integration scaffold to consume an Addon manifest (e.g. Torrentio) and list/play streams.

What was added
- app/src/main/java/com/akiro/addon/AddonManager.kt
  - Simple helper to download manifest and create a Retrofit API for the addon base URL
- app/src/main/java/com/akiro/addon/AddonApi.kt
  - Retrofit interface for a /stream endpoint (adjust params to addon specifics)
- app/src/main/java/com/akiro/addon/StreamDto.kt
  - Generic DTO for streams returned by addons
- app/src/main/java/com/akiro/ui/StreamsFragment.kt
  - Fragment that downloads the manifest, calls /stream and shows results in a RecyclerView
- app/src/main/java/com/akiro/ui/StreamsAdapter.kt
  - RecyclerView Adapter to show streams
- app/src/main/java/com/akiro/player/PlayerActivity.kt
  - ExoPlayer-based player for playable http/https/HLS/DASH URLs

Notes / Next steps
1) jlibtorrent (torrent -> http streaming)
   - This branch intentionally leaves torrent engine integration as TODO. For magnets/infohashes we recommend using jlibtorrent (FrostWire fork) and a local HTTP server (NanoHTTPD or Ktor netty) that serves the partial file to ExoPlayer.
   - Packaging native .so files (for armeabi-v7a and arm64-v8a) is required. This step can be added in a follow-up commit; let me know if you want me to integrate it.

2) Gradle dependencies
Add the following to your app/module build.gradle dependencies block (example):

implementation "com.squareup.okhttp3:okhttp:4.11.0"
implementation "com.squareup.retrofit2:retrofit:2.9.0"
implementation "com.squareup.retrofit2:converter-moshi:2.9.0"
implementation "com.squareup.moshi:moshi:1.15.0"
implementation "com.google.android.exoplayer:exoplayer:2.19.0"
implementation "androidx.recyclerview:recyclerview:1.3.0"

If you plan to integrate jlibtorrent, follow the instructions of the jlibtorrent project for including native libs and the Java wrapper.

3) Usage
- To show streams for an item, create StreamsFragment.newInstance(manifestUrl, type, id)
  - manifestUrl example: https://torrentio.strem.fun/manifest.json
  - type: "movie" / "series" etc. (depends on addon)
  - id: addon-specific id; Torrentio expects parameters for stream discovery (we will adapt after inspecting manifest / stream response)

4) Testing the addon manifest manually
curl -i https://torrentio.strem.fun/manifest.json

5) Follow-up tasks I can do next (pick one):
- Integrate jlibtorrent + NanoHTTPD and implement magnet -> local http streaming (recommended)
- Adapt /stream parameters and DTOs to match exact Torrentio responses after we inspect a sample /stream response
- Replicate Stremio UI more closely (grid of episodes, artwork, provider badges, etc.)

If you want, I will now adapt the /stream parameters to the exact Torrentio format and implement magnet streaming with jlibtorrent; tell me to continue and I'll add the native libs and the player server.
