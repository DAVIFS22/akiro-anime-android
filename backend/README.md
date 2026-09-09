# Akiro API v2.2

Backend próprio do Akiro Anime: PostgreSQL + catálogo automático + episódios + fontes + autenticação + histórico.

## Catálogo automático

O endpoint administrativo `/v1/admin/sync/catalog` usa a API oficial do TMDB para descobrir séries de TV do gênero animação e grava/atualiza:

- animes
- temporadas
- episódios
- pôsteres/backdrops
- sinopse, avaliação, ano e estúdios

Configure `TMDB_API_KEY` (chave v3) ou `TMDB_API_READ_ACCESS_TOKEN` no Render. A sincronização usa o servidor, portanto a credencial nunca vai para o APK.

## Fontes de reprodução

`/v1/admin/sync/sources` pode consumir um **feed de fontes que você tenha autorização para usar**, configurado em `AKIRO_SOURCE_FEED_URL`. O feed recebe `episodeId`, `tmdbId`, `season` e `episode` e responde:

```json
{"sources":[{"provider":"meu-provedor","name":"1080p","url":"https://exemplo.com/video.m3u8","type":"hls","quality":"1080p","language":"pt-BR","subtitles":[],"playableInBrowser":true,"priority":100}]}
```

A API não copia credenciais nem usa API privada de terceiros. Para fontes de torrent/streaming, use somente provedores e conteúdo que você esteja autorizado a disponibilizar.

## Sincronizar tudo

`POST /v1/admin/sync/all` atualiza o catálogo e depois tenta importar as fontes configuradas.

O workflow `.github/workflows/sync-akiro-catalog.yml` pode chamar isso diariamente. Configure no GitHub:

- `AKIRO_API_BASE_URL` = `https://akiro-api.onrender.com`
- `AKIRO_ADMIN_TOKEN` = o mesmo valor do `ADMIN_TOKEN` do Render

Também é possível executar manualmente pelo GitHub Actions.

## Render

- Runtime: Node
- Root Directory: `backend`
- Build Command: `npm install`
- Start Command: `npm start`
- Health: `/health`

Variáveis: `DATABASE_URL`, `JWT_SECRET`, `ADMIN_TOKEN`, `CORS_ORIGIN`, `TMDB_API_KEY` e, opcionalmente, `AKIRO_SOURCE_FEED_URL`/`AKIRO_SOURCE_FEED_TOKEN`.


### Sincronização automática
`POST /v1/admin/sync/all` sincroniza TMDB e, quando habilitado, o adaptador público Torrentio. O workflow GitHub Actions executa essa rota diariamente.

## Administração automática

O administrador não precisa cadastrar episódios manualmente. O fluxo é:

1. `POST /v1/admin/sync/catalog` atualiza animes, temporadas e episódios via TMDB.
2. `POST /v1/admin/sync/sources` atualiza fontes configuradas.
3. `POST /v1/admin/sync/all` executa catálogo + fontes + Torrentio.
4. O GitHub Actions chama `sync/all` automaticamente a cada 6 horas e também permite execução manual.
5. `GET /v1/admin/status` mostra contagens e o estado do catálogo/fontes.

### Administração manual

```bash
curl -H "Authorization: Bearer $AKIRO_ADMIN_TOKEN" \
  https://SEU-ENDERECO.onrender.com/v1/admin/status

curl -X POST \
  -H "Authorization: Bearer $AKIRO_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  https://SEU-ENDERECO.onrender.com/v1/admin/sync/all \
  -d '{"pages":5,"perPage":20,"language":"pt-BR","sourceLimit":1000}'
```

Nunca coloque `ADMIN_TOKEN` no aplicativo Android ou em código público. Use o segredo no Render e o mesmo valor em `AKIRO_ADMIN_TOKEN` no GitHub Actions.

### Torrentio

O adaptador usa o protocolo público de addon do ecossistema Stremio/Torrentio. Ele consulta as streams para o identificador Kitsu resolvido pelo catálogo e grava as fontes encontradas em `episode_sources`. A sincronização deve ser habilitada com `AKIRO_ENABLE_TORRENTIO_SYNC=true`.

Use apenas fontes/conteúdo que você tenha autorização para acessar ou distribuir.
