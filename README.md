# Akiro Anime v1.5.0

Akiro Anime em Kotlin/Jetpack Compose com uma camada opcional de API própria.

## API própria
O projeto agora inclui `backend/` com:
- PostgreSQL schema para animes, temporadas, episódios e fontes;
- autenticação JWT + bcrypt;
- favoritos;
- histórico/continue watching;
- downloads;
- endpoint de fontes;
- endpoint administrativo para cadastrar fontes;
- health check.

A API não usa credenciais privadas do Shinokai. A arquitetura foi inspirada apenas nos padrões técnicos observados na análise do XAPK.

## Integração Android
Se `AKIRO_API_BASE_URL` estiver configurada no build, o Akiro consulta a API para detalhes, episódios e fontes. Se a variável estiver vazia ou a API não fornecer uma fonte, o app mantém o fallback atual para Torrentio/TMDB.

No GitHub Actions, crie o secret `AKIRO_API_BASE_URL` com a URL pública da API, por exemplo `https://seu-servico.onrender.com/`.

## Akiro API v2 / Render

A pasta `backend/` agora está incluída neste projeto. Para o Render, use o mesmo repositório e configure **Root Directory = `backend`**, **Build Command = `npm install`** e **Start Command = `npm start`**.
