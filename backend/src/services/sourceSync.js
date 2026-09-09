import { query } from '../db.js';

function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function fetchJson(url, options = {}) {
  const response = await fetch(url, { ...options, signal: AbortSignal.timeout(20_000), headers: { accept: 'application/json', ...(options.headers || {}) } });
  if (!response.ok) throw new Error(`SOURCE_PROVIDER_${response.status}`);
  return response.json();
}

// Provider contract: GET AKIRO_SOURCE_FEED_URL?episodeId=<uuid>&tmdbId=<id>&season=<n>&episode=<n>
// Response: { "sources": [{ "provider","name","url","type","quality","language","subtitles","playableInBrowser","priority" }] }
export async function syncConfiguredSources({ limit = 200, delayMs = 100 } = {}) {
  const base = process.env.AKIRO_SOURCE_FEED_URL;
  if (!base) return { enabled: false, imported: 0, skipped: 0, errors: [], message: 'AKIRO_SOURCE_FEED_URL not configured' };

  const episodes = await query(`
    SELECT e.id AS episode_id,a.tmdb_id,s.season_number,e.number AS episode_number
    FROM episodes e
    JOIN animes a ON a.id=e.anime_id
    LEFT JOIN seasons s ON s.id=e.season_id
    ORDER BY a.popular DESC,a.rating DESC,e.id
    LIMIT $1
  `, [Math.min(Math.max(Number(limit), 1), 1000)]);

  let imported = 0; let skipped = 0; const errors = [];
  for (const episode of episodes) {
    try {
      const url = new URL(base);
      url.searchParams.set('episodeId', episode.episode_id);
      if (episode.tmdb_id) url.searchParams.set('tmdbId', episode.tmdb_id);
      if (episode.season_number != null) url.searchParams.set('season', episode.season_number);
      url.searchParams.set('episode', episode.episode_number);
      const payload = await fetchJson(url, process.env.AKIRO_SOURCE_FEED_TOKEN ? { headers: { authorization: `Bearer ${process.env.AKIRO_SOURCE_FEED_TOKEN}` } } : {});
      for (const source of Array.isArray(payload.sources) ? payload.sources : []) {
        if (!source.url || !source.provider || !source.name) continue;
        await query(`
          INSERT INTO episode_sources(episode_id,provider,name,url,type,quality,language,subtitles,addon_id,playable_in_browser,priority)
          VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11)
          ON CONFLICT DO NOTHING
        `, [episode.episode_id, source.provider, source.name, source.url, source.type || 'hls', source.quality || 'Auto', source.language || null, JSON.stringify(source.subtitles || []), source.addonId || null, Boolean(source.playableInBrowser), Number(source.priority || 0)]);
        imported++;
      }
    } catch (error) {
      errors.push({ episodeId: episode.episode_id, error: error.message });
    }
    await sleep(delayMs);
  }
  return { enabled: true, processed: episodes.length, imported, skipped, errors };
}
