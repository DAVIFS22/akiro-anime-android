# Akiro API v2

Backend próprio do Akiro Anime, desenhado a partir da arquitetura observada em um app de streaming analisado pelo proprietário, mas implementado de forma independente. Não contém credenciais, tokens privados ou código proprietário de terceiros.

## Arquitetura

`Akiro Android -> Akiro API -> PostgreSQL (Supabase)`

O backend centraliza catálogo, temporadas, episódios, fontes, autenticação, favoritos, histórico, downloads, notificações, gamificação e endpoints administrativos. O player continua no Android; a API entrega metadados e fontes autorizadas cadastradas pelo administrador.

## Render

- Runtime: Node
- Root Directory: `backend`
- Build Command: `npm install`
- Start Command: `npm start`
- Health: `/health`

Variáveis obrigatórias:

- `DATABASE_URL`: connection string PostgreSQL do Supabase (preferencialmente Shared/Session Pooler para o Render)
- `JWT_SECRET`: segredo criado pelo proprietário
- `ADMIN_TOKEN`: segredo administrativo diferente do JWT secret
- `CORS_ORIGIN`: `*` durante desenvolvimento ou domínio específico em produção

Nunca coloque `DATABASE_URL`, `JWT_SECRET` ou `ADMIN_TOKEN` no APK ou no GitHub.

## Supabase

Execute `sql/schema.sql` no SQL Editor. O script é idempotente e inclui alterações compatíveis com a versão 1 do Akiro.

## Endpoints principais

- `GET /health`
- `POST /v1/medias/auth/anonymous`
- `POST /v1/auth/register`
- `POST /v1/auth/login`
- `GET /v1/me`
- `GET /v1/home`
- `GET /v1/search/suggestion?q=`
- `GET /v1/animes/search?q=`
- `GET /v1/animes/trending`
- `GET /v1/animes/popular`
- `GET /v1/animes/recent`
- `GET /v1/animes/:id`
- `GET /v1/animes/:animeId/seasons/:season/episodes`
- `GET /v1/episodes/:episodeId`
- `GET /v1/episodes/:episodeId/sources`
- `GET /v1/episodes/:episodeId/prefetchPlayback`
- favoritos, histórico, downloads, notificações, gamificação e referral em `/v1/me/*`
- ingestão administrativa em `/v1/admin/*`

## Fontes de vídeo

O backend **não copia nem depende da API privada de outro aplicativo**. Fontes devem ser cadastradas por um administrador ou integradas a provedores que o proprietário esteja autorizado a usar. O Android pode continuar usando o fallback Torrentio já existente quando a API não retornar uma fonte.
