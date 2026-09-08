# Akiro Anime — Android Nativo (Kotlin + Jetpack Compose)

Reescrita 100% nativa do projeto web Akiro Anime. Sem WebView — telas em
Jetpack Compose, rede via Retrofit, players nativos via Media3.

## Status atual

O que já existe e deve compilar:
- Estrutura Gradle completa (`build.gradle.kts`, módulo `app`)
- Modelos de dados (`data/model/Anime.kt`) migrados de `types/anime.ts`
- Camada de rede TMDB (`network/TmdbApi.kt`, `RetrofitClient.kt`) — chama
  a API do TMDB **diretamente** (sem proxy CORS, diferente da versão web)
- `AnimeRepository` mapeando respostas do TMDB pro modelo do app
- **Home** — "Em alta" e "Populares" com pôsteres reais do TMDB
- **Busca** — campo de texto com debounce, resultados em grade
- **Detalhes do anime** — sinopse, gêneros, seletor de temporada, lista de episódios, botão de favoritar
- **Favoritos / Continuar assistindo** — duas abas, salvos localmente via DataStore
- **Player** — Media3/ExoPlayer, toca URLs diretas (mp4/HLS); mostra aviso claro quando só existe fonte `magnet:`
- Navegação por abas inferiores (Início / Buscar / Favoritos) + telas de detalhe e player empilhadas por cima
- Armazenamento local (`data/storage/StorageService.kt`) via DataStore, substituindo `localStorage`
- Workflow do GitHub Actions pra compilar o APK automaticamente

## O que falta (o maior ponto em aberto)

**Resolver links `magnet:` dos addons (Torrentio/Brazuca) para algo reproduzível.**
Não existe solução trivial nativa — provavelmente vai precisar de um pequeno
serviço hospedado que converte torrent → stream HTTP. Sem isso, o player
mostra a mensagem de "não foi possível reproduzir" pra qualquer episódio
cuja única fonte seja magnet.

Outros pontos menores:
- A tela de detalhes ainda não busca as fontes de stream dos addons (Torrentio/Brazuca) — está preparada para receber uma URL, mas essa busca ainda não foi implementada
- Autenticação e sincronização Firebase (ver seção abaixo)
- Notificações de novos episódios

## Antes de rodar o build

### 1. Chave da API do TMDB
Crie um secret no GitHub chamado `TMDB_API_KEY` (Settings → Secrets and variables → Actions) com sua chave do TMDB.
Sem isso as chamadas ao catálogo retornam vazio.

### 2. Firebase — atenção, precisa reconfigurar
O arquivo `firebase-applet-config.json` do projeto original é uma
configuração **web** do Firebase. Ela **não funciona** no Android nativo.
Para reativar login/reviews/sincronização:
1. Abra o [Firebase Console](https://console.firebase.google.com/), projeto `gen-lang-client-0742851387`
2. Adicione um app Android novo com o pacote `com.akiro.anime`
3. Baixe o `google-services.json` gerado e coloque em `app/google-services.json`
4. Descomente as duas linhas `com.google.gms.google-services` em `build.gradle.kts` e `app/build.gradle.kts`

Até isso ser feito, o app compila e funciona (catálogo, navegação) mas sem login/sincronização.

### 3. Ícone do app
Coloquei um ícone provisório (triângulo de play). Troque
`app/src/main/res/drawable/ic_launcher_foreground.xml` pela arte final quando tiver.

## Como gerar o APK

O repositório já tem um workflow (`.github/workflows/build-apk.yml`) configurado.
Depois de subir este código pro GitHub (com o secret `TMDB_API_KEY` configurado):

1. Vá na aba **Actions** do repositório
2. Rode o workflow **Build Akiro Anime APK** manualmente (ou dê um push na branch `main`)
3. Quando terminar, baixe o artefato `akiro-anime-debug-apk` — é o `.apk` pronto pra instalar

**Importante:** este projeto foi escrito sem conseguir compilar localmente (o
ambiente que gerou este código não tem acesso ao repositório Maven do
Google). É bem possível que o primeiro build no GitHub Actions aponte algum
erro de compilação — se acontecer, me manda o log de erro que eu corrijo.


## Reprodução Torrent / Torrentio

O player agora aceita `magnet:` além de URLs HTTP(S). Quando uma fonte compatível com Torrentio devolver um magnet, o fluxo é:

1. `TorrentManager` resolve os metadados com libtorrent4j.
2. O app escolhe o maior arquivo de vídeo do torrent e ignora os demais arquivos.
3. O download é colocado em modo sequencial para priorizar o começo do vídeo.
4. Um servidor HTTP é aberto somente em `127.0.0.1` e fornece `Range` para o Media3/ExoPlayer.
5. Ao sair do player, o torrent e o servidor local são encerrados.

A integração não depende de um servidor remoto para transformar o magnet em vídeo: o processamento BitTorrent ocorre no próprio Android.

### Dependências

O projeto usa os artefatos Android do `org.libtorrent4j` para `arm64-v8a`, `armeabi-v7a`, `x86` e `x86_64`. O projeto libtorrent4j documenta suporte a magnet links e download sequencial e publica artefatos separados por arquitetura. 

### Observação

Torrentio é um addon de descoberta de streams; ele não é o motor BitTorrent. Portanto, para uma fonte do addon que seja um `magnet:`, o app precisa resolver o magnet localmente como implementado aqui. Fontes HTTP/HLS continuam sendo reproduzidas diretamente pelo Media3.


## Build no GitHub Actions
O workflow `.github/workflows/build-apk.yml` compila diretamente o projeto na raiz usando Gradle 8.7 e JDK 17.
O APK de debug é publicado como artifact `akiro-anime-debug-apk`.

## Requisitos do streaming
A versão atual usa libtorrent4j 2.1.0-39 para magnets e Media3 para reprodução. Essa versão do libtorrent4j exige Android API 28 ou superior, por isso o `minSdk` é 28.

O app aceita fontes HTTP(S) diretas e magnets quando uma fonte de streaming os fornecer. A integração do catálogo TMDB e a camada de torrent são separadas da UI.
