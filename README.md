# Akiro Anime

Projeto Android Kotlin/Jetpack Compose.

## Build no GitHub Actions

O workflow em `.github/workflows/build-apk.yml` instala Gradle 8.7 e gera o APK Debug automaticamente.

Se usar TMDB, configure o secret `TMDB_API_KEY` no GitHub em:
Settings → Secrets and variables → Actions.

## Player

O app aceita fontes HTTP/HTTPS e possui um motor genérico para reprodução de torrents via `magnet:` usando libtorrent4j + servidor HTTP local com Range para o Media3/ExoPlayer.

Use o motor de torrent apenas com conteúdo que você tenha direito de reproduzir.
