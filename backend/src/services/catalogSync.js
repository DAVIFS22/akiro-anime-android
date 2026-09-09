import { query } from '../db.js';

const TMDB_BASE = 'https://api.themoviedb.org/3';
const TMDB_IMAGE = 'https://image.tmdb.org/t/p/w500';
const TMDB_BACKDROP = 'https://image.tmdb.org/t/p/w1280';

async function tmdb(path, params = {}) {
  const key = process.env.TMDB_API_KEY;
  const bearer = process.env.TMDB_API_READ_ACCESS_TOKEN;
  if (!key && !bearer) throw new Error('TMDB_API_KEY_NOT_CONFIGURED');
  const url = new URL(`${TMDB_BASE}${path}`);
  for (const [k, v] of Object.entries(params)) if (v !== undefined && v !== null) url.searchParams.set(k, String(v));
  if (key && !bearer) url.searchParams.set('api_key', key);
  const response = await fetch(url, {
    headers: { accept: 'application/json', ...(bearer ? { Authorization: `Bearer ${bearer}` } : {}) },
    signal: AbortSignal.timeout(20_000)
  });
  if (!response.ok) throw new Error(`TMDB_${response.status}`);
  return response.json();
}

function image(path, base = TMDB_IMAGE) { return path ? `${base}${path}` : ''; }
function yearOf(date) { return date ? Number(String(date).slice(0, 4)) || 0 : 0; }
function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function upsertAnime(show, details) {
  const genres = (details.genres || show.genre_ids || []).map(g => typeof g === 'object' ? { id: g.id, name: g.name } : { id: g, name: '' });
  const studios = (details.production_companies || []).map(s => ({ id: s.id, name: s.name }));
  const flags = {
    featured: Number(show.popularity || 0) >= 150,
    popular: Number(show.popularity || 0) >= 50,
    recent: yearOf(show.first_air_date) >= new Date().getFullYear() - 1,
    trending: Number(show.popularity || 0) >= 80,
    simulcast: show.in_production === true || details.status === 'Returning Series'
  };

  const rows = await query(`
    INSERT INTO animes(
      tmdb_id,title,romaji_title,native_title,synopsis,poster,banner,backdrop,genres,year,status,type,rating,rating_score_count,age_rating,studios,total_episodes,featured,popular,recent,trending,simulcast
    ) VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19,$20,$21,$22,$23,$24)
    ON CONFLICT(tmdb_id) DO UPDATE SET
      title=EXCLUDED.title, synopsis=EXCLUDED.synopsis, poster=EXCLUDED.poster, banner=EXCLUDED.banner,
      backdrop=EXCLUDED.backdrop, genres=EXCLUDED.genres, year=EXCLUDED.year, status=EXCLUDED.status,
      type=EXCLUDED.type, rating=EXCLUDED.rating, rating_score_count=EXCLUDED.rating_score_count,
      studios=EXCLUDED.studios,total_episodes=EXCLUDED.total_episodes,featured=EXCLUDED.featured,
      popular=EXCLUDED.popular,recent=EXCLUDED.recent,trending=EXCLUDED.trending,simulcast=EXCLUDED.simulcast,updated_at=now()
    RETURNING id,tmdb_id,title
  `, [
    details.id,
    details.name || show.name || `TMDB ${details.id}`,
    details.original_name || null,
    details.original_name || null,
    details.overview || '',
    image(details.poster_path),
    image(details.backdrop_path, TMDB_BACKDROP),
    image(details.backdrop_path, TMDB_BACKDROP),
    JSON.stringify(genres),
    yearOf(details.first_air_date),
    details.status || 'UNKNOWN',
    'TV',
    Number(details.vote_average || 0),
    Number(details.vote_count || 0),
    '14+',
    JSON.stringify(studios),
    Number(details.number_of_episodes || 0),
    flags.featured, flags.popular, flags.recent, flags.trending, flags.simulcast
  ]);
  return rows[0];
}

async function syncSeasonsAndEpisodes(anime, details, options) {
  const seasons = (details.seasons || []).filter(s => Number(s.season_number) >= 0);
  let episodes = 0;
  for (const season of seasons) {
    const seasonRows = await query(`
      INSERT INTO seasons(anime_id,season_number,name,episode_count,year,poster)
      VALUES($1,$2,$3,$4,$5,$6)
      ON CONFLICT(anime_id,season_number) DO UPDATE SET name=EXCLUDED.name,episode_count=EXCLUDED.episode_count,year=EXCLUDED.year,poster=EXCLUDED.poster
      RETURNING id
    `, [anime.id, season.season_number, season.name || `Season ${season.season_number}`, season.episode_count || 0, yearOf(season.air_date), image(season.poster_path)]);
    const seasonId = seasonRows[0].id;
    const data = await tmdb(`/tv/${details.id}/season/${season.season_number}`, { language: options.language });
    for (const ep of data.episodes || []) {
      await query(`
        INSERT INTO episodes(anime_id,season_id,number,title,thumbnail,duration,description,air_date)
        VALUES($1,$2,$3,$4,$5,$6,$7,$8)
        ON CONFLICT(anime_id,season_id,number) DO UPDATE SET title=EXCLUDED.title,thumbnail=EXCLUDED.thumbnail,duration=EXCLUDED.duration,description=EXCLUDED.description,air_date=EXCLUDED.air_date
      `, [anime.id, seasonId, ep.episode_number, ep.name || `Episode ${ep.episode_number}`, image(ep.still_path), Number(ep.runtime || 0) * 60, ep.overview || null, ep.air_date || null]);
      episodes++;
    }
    await sleep(options.delayMs);
  }
  await query('UPDATE animes SET total_episodes=$2,updated_at=now() WHERE id=$1', [anime.id, episodes]);
  return { seasons: seasons.length, episodes };
}

export async function syncCatalog({ pages = 1, perPage = 20, language = 'pt-BR', delayMs = 150 } = {}) {
  const maxPages = Math.min(Math.max(Number(pages), 1), 5);
  const limit = Math.min(Math.max(Number(perPage), 1), 30);
  const result = { startedAt: new Date().toISOString(), anime: 0, seasons: 0, episodes: 0, errors: [] };
  const seen = new Set();
  const today = new Date().toISOString().slice(0, 10);
  const weekAgo = new Date(Date.now() - 7 * 86400000).toISOString().slice(0, 10);

  // Two discovery passes: popularity keeps the catalog rich, while the recent-airing
  // pass prevents newly airing anime from being missed just because their popularity is low.
  const passes = [
    { sort_by: 'popularity.desc' },
    { sort_by: 'first_air_date.desc', 'air_date.gte': weekAgo, 'air_date.lte': today }
  ];

  for (const pass of passes) {
    for (let page = 1; page <= maxPages; page++) {
      const discovered = await tmdb('/discover/tv', {
        language,
        page,
        sort_by: pass.sort_by,
        with_genres: '16',
        include_adult: false,
        with_original_language: 'ja',
        ...(pass['air_date.gte'] ? { 'air_date.gte': pass['air_date.gte'], 'air_date.lte': pass['air_date.lte'] } : {})
      });
      for (const show of (discovered.results || []).slice(0, limit)) {
        if (seen.has(show.id)) continue;
        seen.add(show.id);
        try {
          const details = await tmdb(`/tv/${show.id}`, { language });
          const anime = await upsertAnime(show, details);
          const synced = await syncSeasonsAndEpisodes(anime, details, { language, delayMs });
          result.anime++;
          result.seasons += synced.seasons;
          result.episodes += synced.episodes;
        } catch (error) {
          result.errors.push({ tmdbId: show.id, title: show.name, error: error.message });
        }
        await sleep(delayMs);
      }
    }
  }
  result.finishedAt = new Date().toISOString();
  return result;
}
