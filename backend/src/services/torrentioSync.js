import { query } from '../db.js';

const KITSU_BASE = 'https://kitsu.io/api/edge';
const TORRENTIO_BASE = 'https://torrentio.strem.fun';

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function getJson(url) {
  const response = await fetch(url, {
    headers: {
      accept: 'application/json'
    },
    signal: AbortSignal.timeout(20_000)
  });

  if (!response.ok) {
    throw new Error(`UPSTREAM_${response.status}`);
  }

  return response.json();
}

function clean(value) {
  return String(value || '')
    .toLowerCase()
    .replace(/[^\p{L}\p{N}]+/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function similarity(a, b) {
  a = clean(a);
  b = clean(b);

  if (!a || !b) return 0;
  if (a === b) return 100;
  if (a.includes(b) || b.includes(a)) return 88;

  const aa = new Set(
    a.split(' ').filter((x) => x.length > 2)
  );

  const bb = new Set(
    b.split(' ').filter((x) => x.length > 2)
  );

  return aa.size
    ? Math.round(
        ([...aa].filter((x) => bb.has(x)).length / aa.size) * 80
      )
    : 0;
}

async function resolveKitsuId(anime) {
  if (anime.kitsu_id) {
    return anime.kitsu_id;
  }

  const names = [
    anime.title,
    anime.romaji_title,
    anime.native_title
  ].filter(Boolean);

  let best = null;

  for (const name of names) {
    const url = new URL(`${KITSU_BASE}/anime`);

    url.searchParams.set('filter[text]', name);
    url.searchParams.set('page[limit]', '20');

    const data = await getJson(url);

    for (const item of data.data || []) {
      const attrs = item.attributes || {};

      const candidates = [
        attrs.canonicalTitle,
        attrs.slug,
        ...Object.values(attrs.titles || {})
      ].filter(Boolean);

      const score = Math.max(
        ...candidates.map((value) => similarity(name, value)),
        0
      );

      if (!best || score > best.score) {
        best = {
          id: item.id,
          score
        };
      }
    }
  }

  if (best && best.score >= 55) {
    await query(
      `
        UPDATE animes
        SET kitsu_id = $2,
            updated_at = now()
        WHERE id = $1
          AND (kitsu_id IS NULL OR kitsu_id <> $2)
      `,
      [anime.id, best.id]
    );

    return best.id;
  }

  return null;
}

function parseQuality(title) {
  const text = title.toLowerCase();

  if (text.includes('2160p') || text.includes('4k')) {
    return '4K';
  }

  if (text.includes('1080p')) {
    return '1080p';
  }

  if (text.includes('720p')) {
    return '720p';
  }

  if (text.includes('480p')) {
    return '480p';
  }

  return 'Auto';
}

function score(title) {
  const text = title.toLowerCase();

  let value = 0;

  if (text.includes('2160p') || text.includes('4k')) {
    value += 65;
  } else if (text.includes('1080p')) {
    value += 50;
  } else if (text.includes('720p')) {
    value += 35;
  } else if (text.includes('480p')) {
    value += 15;
  }

  if (text.includes('hevc') || text.includes('x265')) {
    value += 8;
  }

  if (text.includes('dual') || text.includes('multi')) {
    value += 5;
  }

  if (
    text.includes('cam') ||
    text.includes('hdcam') ||
    /\bts\b/.test(text)
  ) {
    value -= 100;
  }

  if (
    text.includes('sample') ||
    text.includes('trailer')
  ) {
    value -= 80;
  }

  return value;
}

async function fetchStreams(kitsuId, season, episode) {
  const ids = [
    `kitsu:${kitsuId}:${season}:${episode}`,
    `kitsu:${kitsuId}:${episode}`
  ];

  const all = [];

  for (const id of ids) {
    const url =
      `${TORRENTIO_BASE}/stream/anime/` +
      `${encodeURIComponent(id)}.json`;

    try {
      const data = await getJson(url);

      for (const stream of data.streams || []) {
        let playable = stream.url || '';

        if (!playable && stream.infoHash) {
          playable =
            `magnet:?xt=urn:btih:${stream.infoHash}`;
        }

        if (!playable) {
          continue;
        }

        const title = String(
          stream.title ||
          stream.name ||
          'Fonte Torrentio'
        )
          .replace(/\n/g, ' ')
          .trim();

        all.push({
          title,
          url: playable,
          quality: parseQuality(title),
          score: score(title)
        });
      }
    } catch {
      // Uma convenção que falhar não interrompe o episódio.
    }

    if (all.length >= 12) {
      break;
    }
  }

  const seen = new Set();

  return all
    .sort((a, b) => b.score - a.score)
    .filter((stream) => {
      if (seen.has(stream.url)) {
        return false;
      }

      seen.add(stream.url);
      return true;
    })
    .slice(0, 12);
}

export async function syncTorrentioSources({
  limit = 500,
  delayMs = 150
} = {}) {
  if (
    String(process.env.AKIRO_ENABLE_TORRENTIO_SYNC || '')
      .toLowerCase() !== 'true'
  ) {
    return {
      enabled: false,
      processed: 0,
      imported: 0,
      errors: [],
      message:
        'Set AKIRO_ENABLE_TORRENTIO_SYNC=true to enable the public Stremio/Torrentio adapter.'
    };
  }

  /*
   * IMPORTANTE:
   * O banco do Akiro usa:
   * episodes.episode_number
   *
   * Não usar episodes.number.
   */
  const episodes = await query(
    `
      SELECT
        e.id AS episode_id,
        a.id AS anime_id,
        a.title,
        a.romaji_title,
        a.native_title,
        a.kitsu_id,
        s.season_number,
        e.episode_number AS episode_number
      FROM episodes e
      JOIN animes a
        ON a.id = e.anime_id
      LEFT JOIN seasons s
        ON s.id = e.season_id
      WHERE e.air_date IS NULL
         OR e.air_date <= CURRENT_DATE
      ORDER BY
        (
          e.air_date >= CURRENT_DATE - INTERVAL '30 days'
        ) DESC,
        a.popular DESC,
        a.rating DESC,
        e.air_date DESC NULLS LAST
      LIMIT $1
    `,
    [
      Math.min(
        Math.max(Number(limit), 1),
        2000
      )
    ]
  );

  let processed = 0;
  let imported = 0;
  let skipped = 0;

  const errors = [];
  const kitsuCache = new Map();

  for (const ep of episodes) {
    processed++;

    try {
      if (
        ep.season_number == null ||
        ep.episode_number == null
      ) {
        skipped++;
        continue;
      }

      let kitsuId =
        ep.kitsu_id ||
        kitsuCache.get(ep.anime_id);

      if (!kitsuId) {
        kitsuId = await resolveKitsuId(ep);

        if (kitsuId) {
          kitsuCache.set(
            ep.anime_id,
            kitsuId
          );
        }
      }

      if (!kitsuId) {
        skipped++;
        continue;
      }

      const streams = await fetchStreams(
        kitsuId,
        ep.season_number,
        ep.episode_number
      );

      for (const stream of streams) {
        const exists = await query(
          `
            SELECT id
            FROM episode_sources
            WHERE episode_id = $1
              AND provider = $2
              AND url = $3
            LIMIT 1
          `,
          [
            ep.episode_id,
            'Torrentio',
            stream.url
          ]
        );

        if (exists[0]) {
          continue;
        }

        await query(
          `
            INSERT INTO episode_sources (
              episode_id,
              provider,
              name,
              url,
              type,
              quality,
              language,
              subtitles,
              addon_id,
              playable_in_browser,
              priority
            )
            VALUES (
              $1,
              'Torrentio',
              $2,
              $3,
              $4,
              $5,
              NULL,
              '[]',
              'torrentio',
              false,
              $6
            )
          `,
          [
            ep.episode_id,
            stream.title,
            stream.url,
            stream.url.startsWith('magnet:')
              ? 'magnet'
              : 'hls',
            stream.quality,
            stream.score
          ]
        );

        imported++;
      }
    } catch (error) {
      errors.push({
        episodeId: ep.episode_id,
        error: error.message
      });
    }

    await sleep(delayMs);
  }

  return {
    enabled: true,
    processed,
    imported,
    skipped,
    errors
  };
}
