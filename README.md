# Akiro Anime v1.4.0

Revisão técnica focada em reprodução real, Torrentio e P2P.

## O que foi reforçado

- Resolução Torrentio com timeout, retry e cache do ID Kitsu.
- Fallback de IDs Kitsu com e sem temporada.
- Fallback IMDb quando disponível.
- Seleção automática de fonte com prioridade por qualidade e exclusão de arquivos ruins/sample/cam.
- Conversão de `infoHash` para magnet quando Torrentio não entrega URL.
- HTTP bridge local com suporte a `Range`/`HEAD`.
- P2P usa `TorrentHandle.fileProgress()` para não tratar arquivo pré-alocado como se já estivesse baixado.
- Player recebe o localhost somente depois que o bridge está pronto.
- Indicador de progresso P2P no player.
- Timeouts de rede para evitar telas infinitas de carregamento.

## Fluxo esperado

1. Usuário toca em um episódio.
2. Akiro resolve o ID Kitsu/IMDb.
3. Akiro consulta Torrentio.
4. Torrentio retorna fontes.
5. Akiro escolhe a melhor fonte disponível.
6. Se a fonte for magnet, libtorrent obtém os metadados e prioriza somente o vídeo.
7. Um servidor HTTP local com Range expõe o arquivo.
8. ExoPlayer reproduz `localhost` enquanto o torrent continua baixando.

## Build

O workflow `.github/workflows/build-apk.yml` usa JDK 17 e Gradle 8.7. O segredo `TMDB_API_KEY` precisa estar configurado no GitHub Actions.

## Limitação importante

A disponibilidade das fontes depende da resposta atual do Torrentio e dos provedores configurados nele. O aplicativo não pode garantir que todo episódio terá uma fonte.
