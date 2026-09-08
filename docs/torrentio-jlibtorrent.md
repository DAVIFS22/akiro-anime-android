jlibtorrent integration added

This patch adds a basic TorrentEngine using jlibtorrent (via reflection to fail gracefully if the lib is not present) and a LocalHttpServer using NanoHTTPD to serve the partial downloaded file to ExoPlayer.

Files added/updated
- app/src/main/java/com/akiro/torrent/TorrentEngine.kt
- app/src/main/java/com/akiro/torrent/LocalHttpServer.kt
- app/src/main/java/com/akiro/player/PlayerActivity.kt (updated to support magnet links)

Important notes before building
1) Add dependencies to your app/build.gradle (module) to compile and run the torrent features:

// NanoHTTPD
implementation "org.nanohttpd:nanohttpd:2.3.1"

// jlibtorrent (example coordinates; verify the latest and preferred distribution)
implementation "com.frostwire:jlibtorrent:1.2.0.14"

// ExoPlayer and network libs (if not already present)
implementation "com.google.android.exoplayer:exoplayer:2.19.0"
implementation "com.squareup.okhttp3:okhttp:4.11.0"
implementation "com.squareup.retrofit2:retrofit:2.9.0"
implementation "com.squareup.retrofit2:converter-moshi:2.9.0"
implementation "com.squareup.moshi:moshi:1.15.0"

2) jlibtorrent requires native .so libraries for each ABI (armeabi-v7a, arm64-v8a, x86...). Follow the jlibtorrent / frostwire-jlibtorrent documentation to include the native binaries in src/main/jniLibs/<abi>/. The app will crash at runtime if the Java wrapper is present but the native libs are missing.

3) ProGuard/R8
If you use R8, ensure jlibtorrent classes are kept and native symbols are preserved. Add recommended -keep rules from the library docs.

4) Testing
- Run the app on a real device (emulator might lack native compatibility for the jlibtorrent .so).
- Open a content that returns a magnet link. The PlayerActivity will start the torrent engine, wait for metadata and start a local HTTP server, then point ExoPlayer to that local URL.

If you want, eu continuo e:
- adiciono as .so pré-empacotadas no repositório (se você me enviar os binários) ou instruo como obtê-las e onde colocar;
- ajusto tempo de espera/seleção de arquivo (atualmente espera até 60s por metadata);
- melhoro UI indicando progresso do download/peers/health.

Mensagem de commit: "Add jlibtorrent/NanoHTTPD torrent streaming support (magnet -> local HTTP -> ExoPlayer)"
