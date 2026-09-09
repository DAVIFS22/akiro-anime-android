import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import dotenv from 'dotenv';
import crypto from 'node:crypto';
import { z } from 'zod';
import { query, pool } from './db.js';
import { hashPassword, verifyPassword, signUser, requireAuth, requireAdmin } from './auth.js';
import { syncCatalog } from './services/catalogSync.js';
import { syncConfiguredSources } from './services/sourceSync.js';
import { syncTorrentioSources } from './services/torrentioSync.js';

dotenv.config();
const app = express();
const port = Number(process.env.PORT || 10000);

app.disable('x-powered-by');
app.use(helmet({ crossOriginResourcePolicy: false }));
app.use(cors({ origin: process.env.CORS_ORIGIN || '*' }));
app.use(express.json({ limit: '1mb' }));
app.use(rateLimit({ windowMs: 60_000, limit: 120, standardHeaders: 'draft-7', legacyHeaders: false }));

const asyncRoute = (handler) => (req, res, next) => Promise.resolve(handler(req, res, next)).catch(next);
const parseJson = (value, fallback = []) => {
  if (Array.isArray(value)) return value;
  try { return value ? JSON.parse(value) : fallback; } catch { return fallback; }
};
const animeLookup = async (id) => /^\d+$/.test(String(id))
  ? query('SELECT * FROM animes WHERE tmdb_id=$1 LIMIT 1', [Number(id)])
  : query('SELECT * FROM animes WHERE id=$1 LIMIT 1', [id]);

app.get('/health', asyncRoute(async (_req, res) => {
  await query('SELECT 1');
  res.json({ status: 'ok', service: 'akiro-api', version: '2.2.1', database: 'connected', timestamp: new Date().toISOString() });
}));

// Anonymous playback/session bootstrap, inspired by the media-session pattern found in the analyzed app.
app.post('/v1/medias/auth/anonymous', asyncRoute(async (req, res) => {
  const body = z.object({ deviceId: z.string().max(200).optional() }).parse(req.body || {});
  const token = crypto.randomBytes(32).toString('hex');
  const rows = await query('INSERT INTO anonymous_sessions(token,device_id) VALUES($1,$2) RETURNING id,token,created_at', [token, body.deviceId || null]);
  res.status(201).json({ session: rows[0] });
}));

app.post('/v1/auth/register', asyncRoute(async (req, res) => {
  const body = z.object({ email: z.string().email(), password: z.string().min(8).max(128), displayName: z.string().trim().min(1).max(80).optional() }).parse(req.body);
  const email = body.email.toLowerCase();
  const exists = await query('SELECT id FROM users WHERE lower(email)=lower($1)', [email]);
  if (exists[0]) return res.status(409).json({ error: 'EMAIL_ALREADY_EXISTS' });
  const hash = await hashPassword(body.password);
  const rows = await query('INSERT INTO users(email,password_hash,display_name) VALUES($1,$2,$3) RETURNING id,email,display_name,avatar_url,xp,level,coins', [email, hash, body.displayName || null]);
  const user = rows[0];
  await query('INSERT INTO notification_settings(user_id) VALUES($1) ON CONFLICT DO NOTHING', [user.id]);
  res.status(201).json({ user, token: signUser(user) });
}));

app.post('/v1/auth/login', asyncRoute(async (req, res) => {
  const body = z.object({ email: z.string().email(), password: z.string() }).parse(req.body);
  const rows = await query('SELECT id,email,display_name,avatar_url,xp,level,coins,password_hash FROM users WHERE lower(email)=lower($1)', [body.email]);
  if (!rows[0] || !(await verifyPassword(body.password, rows[0].password_hash))) return res.status(401).json({ error: 'INVALID_CREDENTIALS' });
  const { password_hash: _hash, ...user } = rows[0];
  res.json({ user, token: signUser(user) });
}));

app.get('/v1/me', requireAuth, asyncRoute(async (req, res) => {
  const settings = await query('SELECT new_episodes,recommendations,system FROM notification_settings WHERE user_id=$1', [req.user.id]);
  res.json({ user: req.user, notificationSettings: settings[0] || { new_episodes: true, recommendations: true, system: true } });
}));

const animeList = async (where = '', params = [], order = 'rating DESC', limit = 30) => {
  const rows = await query(`SELECT * FROM animes ${where ? `WHERE ${where}` : ''} ORDER BY ${order} LIMIT ${Number(limit)}`, params);
  return rows;
};

app.get('/v1/home', asyncRoute(async (_req, res) => {
  const [featured, trending, popular, recent, simulcast] = await Promise.all([
    animeList('featured=true', [], 'rating DESC', 12),
    animeList('trending=true', [], 'rating DESC', 20),
    animeList('popular=true', [], 'rating DESC', 20),
    animeList('recent=true', [], 'year DESC, rating DESC', 20),
    animeList('simulcast=true', [], 'year DESC, rating DESC', 20)
  ]);
  res.json({ featured, trending, popular, recent, simulcast });
}));

for (const [path, flag, order] of [
  ['/v1/animes/trending', 'trending', 'rating DESC'],
  ['/v1/animes/popular', 'popular', 'rating DESC'],
  ['/v1/animes/recent', 'recent', 'year DESC, rating DESC'],
  ['/v1/animes/simulcast', 'simulcast', 'year DESC, rating DESC']
]) {
  app.get(path, asyncRoute(async (req, res) => {
    const limit = Math.min(Math.max(Number(req.query.limit || 30), 1), 100);
    res.json({ items: await animeList(`${flag}=true`, [], order, limit) });
  }));
}

app.get('/v1/search/suggestion', asyncRoute(async (req, res) => {
  const q = String(req.query.q || '').trim();
  if (!q) return res.json({ items: [] });
  const rows = await query('SELECT id,tmdb_id,title,poster,year FROM animes WHERE title ILIKE $1 OR romaji_title ILIKE $1 ORDER BY popular DESC,rating DESC LIMIT 8', [`%${q}%`]);
  res.json({ items: rows });
}));

app.get('/v1/animes/search', asyncRoute(async (req, res) => {
  const q = String(req.query.q || '').trim();
  if (!q) return res.json({ items: [] });
  const limit = Math.min(Math.max(Number(req.query.limit || 30), 1), 100);
  const rows = await query(`SELECT * FROM animes WHERE title ILIKE $1 OR romaji_title ILIKE $1 OR native_title ILIKE $1 ORDER BY popular DESC,trending DESC,rating DESC LIMIT ${limit}`, [`%${q}%`]);
  res.json({ items: rows });
}));

app.get('/v1/animes/:id', asyncRoute(async (req, res) => {
  const rows = await animeLookup(req.params.id);
  if (!rows[0]) return res.status(404).json({ error: 'ANIME_NOT_FOUND' });
  const anime = rows[0];
  const seasons = await query('SELECT * FROM seasons WHERE anime_id=$1 ORDER BY season_number', [anime.id]);
  res.json({ ...anime, genres: parseJson(anime.genres), studios: parseJson(anime.studios), seasons });
}));

app.get('/v1/animes/:animeId/seasons/:season/episodes', asyncRoute(async (req, res) => {
  const animeRows = await animeLookup(req.params.animeId);
  if (!animeRows[0]) return res.status(404).json({ error: 'ANIME_NOT_FOUND' });
  const seasonNumber = Number(req.params.season);
  if (!Number.isInteger(seasonNumber)) return res.status(400).json({ error: 'INVALID_SEASON' });
  const seasonRows = await query('SELECT id FROM seasons WHERE anime_id=$1 AND season_number=$2', [animeRows[0].id, seasonNumber]);
  if (!seasonRows[0]) return res.json({ items: [] });
  const episodes = await query('SELECT id,anime_id,season_id,number,title,thumbnail,duration,description,air_date FROM episodes WHERE anime_id=$1 AND season_id=$2 ORDER BY number', [animeRows[0].id, seasonRows[0].id]);
  res.json({ items: episodes });
}));

app.get('/v1/episodes/:episodeId', asyncRoute(async (req, res) => {
  const rows = await query('SELECT e.*,a.title AS anime_title,a.poster AS anime_poster FROM episodes e JOIN animes a ON a.id=e.anime_id WHERE e.id=$1', [req.params.episodeId]);
  if (!rows[0]) return res.status(404).json({ error: 'EPISODE_NOT_FOUND' });
  res.json(rows[0]);
}));

app.get('/v1/episodes/:episodeId/sources', asyncRoute(async (req, res) => {
  const rows = await query(`SELECT id,provider,name,url,type,quality,language,subtitles,addon_id AS "addonId",playable_in_browser AS "playableInBrowser",priority FROM episode_sources WHERE episode_id=$1 AND enabled=true ORDER BY priority DESC, CASE quality WHEN '1080p' THEN 4 WHEN '720p' THEN 3 WHEN '480p' THEN 2 ELSE 1 END DESC`, [req.params.episodeId]);
  res.json({ items: rows.map(r => ({ ...r, subtitles: parseJson(r.subtitles) })) });
}));

app.get('/v1/episodes/:episodeId/prefetchPlayback', asyncRoute(async (req, res) => {
  const episode = await query('SELECT id,anime_id,season_id,number,title FROM episodes WHERE id=$1', [req.params.episodeId]);
  if (!episode[0]) return res.status(404).json({ error: 'EPISODE_NOT_FOUND' });
  const sources = await query(`SELECT id,provider,name,url,type,quality,language,subtitles,addon_id AS "addonId",playable_in_browser AS "playableInBrowser",priority FROM episode_sources WHERE episode_id=$1 AND enabled=true ORDER BY priority DESC`, [req.params.episodeId]);
  res.json({ episode: episode[0], sources: sources.map(s => ({ ...s, subtitles: parseJson(s.subtitles) })) });
}));

app.get('/v1/me/favorites', requireAuth, asyncRoute(async (req, res) => {
  const rows = await query(`SELECT a.* FROM favorites f JOIN animes a ON a.id=f.anime_id WHERE f.user_id=$1 ORDER BY f.created_at DESC`, [req.user.id]);
  res.json({ items: rows });
}));

const resolveAnimeId = async (id) => {
  const rows = await animeLookup(id);
  return rows[0]?.id || null;
};

app.post('/v1/me/favorites/:animeId', requireAuth, asyncRoute(async (req, res) => {
  const animeId = await resolveAnimeId(req.params.animeId);
  if (!animeId) return res.status(404).json({ error: 'ANIME_NOT_FOUND' });
  await query('INSERT INTO favorites(user_id,anime_id) VALUES($1,$2) ON CONFLICT DO NOTHING', [req.user.id, animeId]);
  await unlockAchievement(req.user.id, 'favorite_anime');
  res.json({ favorited: true });
}));

app.delete('/v1/me/favorites/:animeId', requireAuth, asyncRoute(async (req, res) => {
  const animeId = await resolveAnimeId(req.params.animeId);
  if (animeId) await query('DELETE FROM favorites WHERE user_id=$1 AND anime_id=$2', [req.user.id, animeId]);
  res.json({ favorited: false });
}));

app.get('/v1/me/history/continue-watching', requireAuth, asyncRoute(async (req, res) => {
  const rows = await query(`SELECT h.episode_id,h.position_seconds,h.duration_seconds,h.completed,h.watched_at,e.number,e.title,e.thumbnail,e.anime_id,a.title AS anime_title,a.poster AS anime_poster FROM watch_history h JOIN episodes e ON e.id=h.episode_id JOIN animes a ON a.id=e.anime_id WHERE h.user_id=$1 AND h.completed=false ORDER BY h.watched_at DESC LIMIT 50`, [req.user.id]);
  res.json({ items: rows });
}));

app.get('/v1/me/watch-again', requireAuth, asyncRoute(async (req, res) => {
  const rows = await query(`SELECT h.episode_id,h.position_seconds,h.duration_seconds,h.watched_at,e.number,e.title,e.thumbnail,e.anime_id,a.title AS anime_title FROM watch_history h JOIN episodes e ON e.id=h.episode_id JOIN animes a ON a.id=e.anime_id WHERE h.user_id=$1 ORDER BY h.watched_at DESC LIMIT 50`, [req.user.id]);
  res.json({ items: rows });
}));

async function unlockAchievement(userId, key) {
  const achievement = await query('SELECT id,xp_reward FROM achievements WHERE key=$1', [key]);
  if (!achievement[0]) return;
  const inserted = await query('INSERT INTO user_achievements(user_id,achievement_id) VALUES($1,$2) ON CONFLICT DO NOTHING RETURNING achievement_id', [userId, achievement[0].id]);
  if (inserted[0]) {
    await query('UPDATE users SET xp=xp+$2, level=GREATEST(1,FLOOR((xp+$2)/500)+1), updated_at=now() WHERE id=$1', [userId, achievement[0].xp_reward]);
  }
}

app.post('/v1/me/history', requireAuth, asyncRoute(async (req, res) => {
  const body = z.object({ episodeId: z.string().uuid(), positionSeconds: z.number().int().min(0), durationSeconds: z.number().int().min(0), completed: z.boolean().default(false) }).parse(req.body);
  await query(`INSERT INTO watch_history(user_id,episode_id,position_seconds,duration_seconds,completed,watched_at) VALUES($1,$2,$3,$4,$5,now()) ON CONFLICT(user_id,episode_id) DO UPDATE SET position_seconds=EXCLUDED.position_seconds,duration_seconds=EXCLUDED.duration_seconds,completed=EXCLUDED.completed,watched_at=now()`, [req.user.id, body.episodeId, body.positionSeconds, body.durationSeconds, body.completed]);
  const countRows = await query('SELECT COUNT(*)::int AS count FROM watch_history WHERE user_id=$1 AND completed=true', [req.user.id]);
  if (body.completed && countRows[0]?.count >= 1) await unlockAchievement(req.user.id, 'first_watch');
  if (body.completed && countRows[0]?.count >= 5) await unlockAchievement(req.user.id, 'five_episodes');
  res.status(204).end();
}));

app.get('/v1/me/downloads', requireAuth, asyncRoute(async (req, res) => {
  const rows = await query(`SELECT d.*,e.number,e.title,e.thumbnail FROM downloads d JOIN episodes e ON e.id=d.episode_id WHERE d.user_id=$1 ORDER BY d.created_at DESC`, [req.user.id]);
  res.json({ items: rows });
}));

app.post('/v1/me/downloads', requireAuth, asyncRoute(async (req, res) => {
  const body = z.object({ episodeId: z.string().uuid(), sourceId: z.string().uuid().optional() }).parse(req.body);
  const rows = await query(`INSERT INTO downloads(user_id,episode_id,source_id,status,progress) VALUES($1,$2,$3,'queued',0) ON CONFLICT(user_id,episode_id) DO UPDATE SET source_id=COALESCE(EXCLUDED.source_id,downloads.source_id),status='queued',error_message=NULL,updated_at=now() RETURNING *`, [req.user.id, body.episodeId, body.sourceId || null]);
  res.status(201).json(rows[0]);
}));

app.patch('/v1/me/downloads/:id', requireAuth, asyncRoute(async (req, res) => {
  const body = z.object({ status: z.string().min(1).max(30).optional(), progress: z.number().min(0).max(100).optional(), localUri: z.string().max(1000).optional(), errorMessage: z.string().max(500).optional() }).parse(req.body);
  const rows = await query(`UPDATE downloads SET status=COALESCE($3,status),progress=COALESCE($4,progress),local_uri=COALESCE($5,local_uri),error_message=$6,updated_at=now() WHERE id=$1 AND user_id=$2 RETURNING *`, [req.params.id, req.user.id, body.status || null, body.progress ?? null, body.localUri || null, body.errorMessage || null]);
  if (!rows[0]) return res.status(404).json({ error: 'DOWNLOAD_NOT_FOUND' });
  res.json(rows[0]);
}));

app.get('/v1/me/notifications/unread-counter', requireAuth, asyncRoute(async (req, res) => {
  const rows = await query('SELECT COUNT(*)::int AS count FROM notifications WHERE user_id=$1 AND read_at IS NULL', [req.user.id]);
  res.json({ count: rows[0]?.count || 0 });
}));

app.get('/v1/me/notifications', requireAuth, asyncRoute(async (req, res) => {
  const rows = await query('SELECT * FROM notifications WHERE user_id=$1 ORDER BY created_at DESC LIMIT 100', [req.user.id]);
  res.json({ items: rows });
}));

app.post('/v1/me/notifications/read-all', requireAuth, asyncRoute(async (req, res) => {
  await query('UPDATE notifications SET read_at=now() WHERE user_id=$1 AND read_at IS NULL', [req.user.id]);
  res.json({ success: true });
}));

app.get('/v1/me/notification-settings', requireAuth, asyncRoute(async (req, res) => {
  const rows = await query('SELECT new_episodes,recommendations,system FROM notification_settings WHERE user_id=$1', [req.user.id]);
  res.json(rows[0] || { new_episodes: true, recommendations: true, system: true });
}));

app.patch('/v1/me/notification-settings', requireAuth, asyncRoute(async (req, res) => {
  const body = z.object({ newEpisodes: z.boolean().optional(), recommendations: z.boolean().optional(), system: z.boolean().optional() }).parse(req.body);
  const rows = await query(`INSERT INTO notification_settings(user_id,new_episodes,recommendations,system) VALUES($1,COALESCE($2,true),COALESCE($3,true),COALESCE($4,true)) ON CONFLICT(user_id) DO UPDATE SET new_episodes=COALESCE($2,notification_settings.new_episodes),recommendations=COALESCE($3,notification_settings.recommendations),system=COALESCE($4,notification_settings.system),updated_at=now() RETURNING new_episodes,recommendations,system`, [req.user.id, body.newEpisodes ?? null, body.recommendations ?? null, body.system ?? null]);
  res.json(rows[0]);
}));

app.get('/v1/me/gamification', requireAuth, asyncRoute(async (req, res) => {
  const user = await query('SELECT id,xp,level,coins FROM users WHERE id=$1', [req.user.id]);
  const achievements = await query(`SELECT a.key,a.name,a.description,a.xp_reward,ua.unlocked_at FROM user_achievements ua JOIN achievements a ON a.id=ua.achievement_id WHERE ua.user_id=$1 ORDER BY ua.unlocked_at DESC`, [req.user.id]);
  res.json({ user: user[0], achievements });
}));

app.get('/v1/me/referral', requireAuth, asyncRoute(async (req, res) => {
  let rows = await query('SELECT * FROM referrals WHERE user_id=$1 LIMIT 1', [req.user.id]);
  if (!rows[0]) rows = await query('INSERT INTO referrals(user_id,code) VALUES($1,$2) RETURNING *', [req.user.id, `AK-${crypto.randomBytes(4).toString('hex').toUpperCase()}`]);
  res.json(rows[0]);
}));

app.post('/v1/me/referral/claim', requireAuth, asyncRoute(async (req, res) => {
  const body = z.object({ code: z.string().trim().min(4).max(50) }).parse(req.body);
  const ref = await query('SELECT id,user_id FROM referrals WHERE upper(code)=upper($1)', [body.code]);
  if (!ref[0]) return res.status(404).json({ error: 'REFERRAL_NOT_FOUND' });
  if (ref[0].user_id === req.user.id) return res.status(400).json({ error: 'SELF_REFERRAL' });
  await query('UPDATE referrals SET redeemed_count=redeemed_count+1 WHERE id=$1', [ref[0].id]);
  await query('UPDATE users SET coins=coins+10 WHERE id=$1 OR id=$2', [req.user.id, ref[0].user_id]);
  res.json({ success: true, reward: 10 });
}));

// Admin/catalog ingestion. This lets the owner populate the API without exposing DB credentials to the app.
app.post('/v1/admin/animes', requireAdmin, asyncRoute(async (req, res) => {
  const body = z.object({ tmdbId: z.number().int().optional(), malId: z.number().int().optional(), imdbId: z.string().optional(), kitsuId: z.string().optional(), title: z.string().min(1), romajiTitle: z.string().optional(), nativeTitle: z.string().optional(), synopsis: z.string().optional(), poster: z.string().optional(), banner: z.string().optional(), backdrop: z.string().optional(), genres: z.array(z.any()).default([]), year: z.number().int().optional(), status: z.string().optional(), type: z.string().optional(), rating: z.number().optional(), ratingScoreCount: z.number().int().optional(), ageRating: z.string().optional(), studios: z.array(z.any()).default([]), totalEpisodes: z.number().int().optional(), featured: z.boolean().default(false), popular: z.boolean().default(false), recent: z.boolean().default(false), trending: z.boolean().default(false), simulcast: z.boolean().default(false) }).parse(req.body);
  const rows = await query(`INSERT INTO animes(tmdb_id,mal_id,imdb_id,kitsu_id,title,romaji_title,native_title,synopsis,poster,banner,backdrop,genres,year,status,type,rating,rating_score_count,age_rating,studios,total_episodes,featured,popular,recent,trending,simulcast) VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19,$20,$21,$22,$23,$24,$25) ON CONFLICT (tmdb_id) DO UPDATE SET title=EXCLUDED.title,romaji_title=EXCLUDED.romaji_title,native_title=EXCLUDED.native_title,synopsis=EXCLUDED.synopsis,poster=EXCLUDED.poster,banner=EXCLUDED.banner,backdrop=EXCLUDED.backdrop,genres=EXCLUDED.genres,year=EXCLUDED.year,status=EXCLUDED.status,type=EXCLUDED.type,rating=EXCLUDED.rating,rating_score_count=EXCLUDED.rating_score_count,age_rating=EXCLUDED.age_rating,studios=EXCLUDED.studios,total_episodes=EXCLUDED.total_episodes,featured=EXCLUDED.featured,popular=EXCLUDED.popular,recent=EXCLUDED.recent,trending=EXCLUDED.trending,simulcast=EXCLUDED.simulcast,updated_at=now() RETURNING *`, [body.tmdbId ?? null,body.malId ?? null,body.imdbId ?? null,body.kitsuId ?? null,body.title,body.romajiTitle ?? null,body.nativeTitle ?? null,body.synopsis ?? '',body.poster ?? '',body.banner ?? '',body.backdrop ?? null,JSON.stringify(body.genres),body.year ?? 0,body.status ?? 'UNKNOWN',body.type ?? 'TV',body.rating ?? 0,body.ratingScoreCount ?? 0,body.ageRating ?? '14+',JSON.stringify(body.studios),body.totalEpisodes ?? 0,body.featured,body.popular,body.recent,body.trending,body.simulcast]);
  res.status(201).json(rows[0]);
}));

app.post('/v1/admin/seasons', requireAdmin, asyncRoute(async (req, res) => {
  const body = z.object({ animeId: z.string().uuid(), seasonNumber: z.number().int().min(0), name: z.string().min(1), episodeCount: z.number().int().min(0).default(0), year: z.number().int().optional(), poster: z.string().optional() }).parse(req.body);
  const rows = await query(`INSERT INTO seasons(anime_id,season_number,name,episode_count,year,poster) VALUES($1,$2,$3,$4,$5,$6) ON CONFLICT(anime_id,season_number) DO UPDATE SET name=EXCLUDED.name,episode_count=EXCLUDED.episode_count,year=EXCLUDED.year,poster=EXCLUDED.poster RETURNING *`, [body.animeId,body.seasonNumber,body.name,body.episodeCount,body.year ?? null,body.poster ?? null]);
  res.status(201).json(rows[0]);
}));

app.post('/v1/admin/episodes', requireAdmin, asyncRoute(async (req, res) => {
  const body = z.object({ animeId: z.string().uuid(), seasonId: z.string().uuid().optional(), number: z.number().int().min(0), title: z.string().min(1), thumbnail: z.string().optional(), duration: z.number().int().min(0).default(0), description: z.string().optional(), airDate: z.string().optional() }).parse(req.body);
  const rows = await query(`INSERT INTO episodes(anime_id,season_id,number,title,thumbnail,duration,description,air_date) VALUES($1,$2,$3,$4,$5,$6,$7,$8) ON CONFLICT(anime_id,season_id,number) DO UPDATE SET title=EXCLUDED.title,thumbnail=EXCLUDED.thumbnail,duration=EXCLUDED.duration,description=EXCLUDED.description,air_date=EXCLUDED.air_date RETURNING *`, [body.animeId,body.seasonId ?? null,body.number,body.title,body.thumbnail ?? '',body.duration,body.description ?? null,body.airDate ?? null]);
  res.status(201).json(rows[0]);
}));

app.post('/v1/admin/sources', requireAdmin, asyncRoute(async (req, res) => {
  const body = z.object({ episodeId: z.string().uuid(), provider: z.string().min(1), name: z.string().min(1), url: z.string().url(), type: z.string().default('hls'), quality: z.string().default('Auto'), language: z.string().optional(), subtitles: z.array(z.any()).default([]), addonId: z.string().optional(), playableInBrowser: z.boolean().default(false), priority: z.number().int().default(0) }).parse(req.body);
  const rows = await query(`INSERT INTO episode_sources(episode_id,provider,name,url,type,quality,language,subtitles,addon_id,playable_in_browser,priority) VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11) RETURNING id,episode_id,provider,name,url,type,quality,language,subtitles,addon_id AS "addonId",playable_in_browser AS "playableInBrowser",priority,enabled`, [body.episodeId,body.provider,body.name,body.url,body.type,body.quality,body.language || null,JSON.stringify(body.subtitles),body.addonId || null,body.playableInBrowser,body.priority]);
  res.status(201).json(rows[0]);
}));

app.patch('/v1/admin/sources/:id', requireAdmin, asyncRoute(async (req, res) => {
  const body = z.object({ enabled: z.boolean().optional(), priority: z.number().int().optional() }).parse(req.body);
  const rows = await query('UPDATE episode_sources SET enabled=COALESCE($2,enabled),priority=COALESCE($3,priority),updated_at=now() WHERE id=$1 RETURNING *', [req.params.id, body.enabled ?? null, body.priority ?? null]);
  if (!rows[0]) return res.status(404).json({ error: 'SOURCE_NOT_FOUND' });
  res.json(rows[0]);
}));

app.get('/v1/admin/status', requireAdmin, asyncRoute(async (_req, res) => {
  const [animes, seasons, episodes, sources, users, latestEpisode, latestSource] = await Promise.all([
    query('SELECT COUNT(*)::int AS count FROM animes'),
    query('SELECT COUNT(*)::int AS count FROM seasons'),
    query('SELECT COUNT(*)::int AS count FROM episodes'),
    query('SELECT COUNT(*)::int AS count FROM episode_sources WHERE enabled=true'),
    query('SELECT COUNT(*)::int AS count FROM users'),
    query(`SELECT e.id,e.title,e.number,e.air_date,a.title AS anime FROM episodes e JOIN animes a ON a.id=e.anime_id ORDER BY e.created_at DESC LIMIT 1`),
    query(`SELECT provider,COUNT(*)::int AS count,MAX(created_at) AS latest FROM episode_sources GROUP BY provider ORDER BY latest DESC`)
  ]);
  res.json({
    version: '2.2.1',
    database: 'connected',
    counts: { animes: animes[0].count, seasons: seasons[0].count, episodes: episodes[0].count, sources: sources[0].count, users: users[0].count },
    latestEpisode: latestEpisode[0] || null,
    sourcesByProvider: latestSource
  });
}));

app.post('/v1/admin/sync/catalog', requireAdmin, asyncRoute(async (req, res) => {
  const body = z.object({ pages: z.number().int().min(1).max(5).default(1), perPage: z.number().int().min(1).max(30).default(20), language: z.string().default('pt-BR') }).parse(req.body || {});
  const result = await syncCatalog(body);
  res.json(result);
}));

app.post('/v1/admin/sync/sources', requireAdmin, asyncRoute(async (req, res) => {
  const body = z.object({ limit: z.number().int().min(1).max(1000).default(200) }).parse(req.body || {});
  const result = await syncConfiguredSources(body);
  res.json(result);
}));

app.post('/v1/admin/sync/all', requireAdmin, asyncRoute(async (req, res) => {
  const body = z.object({ pages: z.number().int().min(1).max(5).default(1), perPage: z.number().int().min(1).max(30).default(20), language: z.string().default('pt-BR'), sourceLimit: z.number().int().min(1).max(1000).default(200) }).parse(req.body || {});
  const catalog = await syncCatalog(body);
  const configured = await syncConfiguredSources({ limit: body.sourceLimit });
  const torrentio = await syncTorrentioSources({ limit: body.sourceLimit });
  res.json({ catalog, sources: { configured, torrentio } });
}));

app.post('/v1/admin/notifications/broadcast', requireAdmin, asyncRoute(async (req, res) => {
  const body = z.object({ title: z.string().min(1).max(120), body: z.string().min(1).max(500), type: z.string().default('system'), data: z.record(z.any()).default({}) }).parse(req.body);
  const result = await query('SELECT id FROM users');
  for (const user of result) await query('INSERT INTO notifications(user_id,title,body,type,data) VALUES($1,$2,$3,$4,$5)', [user.id,body.title,body.body,body.type,JSON.stringify(body.data)]);
  res.json({ success: true, recipients: result.length });
}));

app.use((err, _req, res, _next) => {
  console.error(err);
  if (err?.name === 'ZodError') return res.status(400).json({ error: 'INVALID_REQUEST', details: err.issues });
  if (err?.code === '23505') return res.status(409).json({ error: 'CONFLICT' });
  if (err?.code === '23503') return res.status(400).json({ error: 'INVALID_REFERENCE' });
  res.status(500).json({ error: 'INTERNAL_SERVER_ERROR' });
});

const server = app.listen(port, '0.0.0.0', () => console.log(`Akiro API v2.2 listening on ${port}`));
process.on('SIGTERM', async () => { server.close(); await pool.end(); });
