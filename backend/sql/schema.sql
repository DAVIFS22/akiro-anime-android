CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  display_name TEXT,
  avatar_url TEXT,
  xp INTEGER NOT NULL DEFAULT 0,
  level INTEGER NOT NULL DEFAULT 1,
  coins INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS anonymous_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  token TEXT NOT NULL UNIQUE,
  device_id TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS animes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tmdb_id INTEGER UNIQUE,
  mal_id INTEGER UNIQUE,
  imdb_id TEXT UNIQUE,
  kitsu_id TEXT UNIQUE,
  title TEXT NOT NULL,
  romaji_title TEXT,
  native_title TEXT,
  synopsis TEXT NOT NULL DEFAULT '',
  poster TEXT NOT NULL DEFAULT '',
  banner TEXT NOT NULL DEFAULT '',
  backdrop TEXT,
  genres JSONB NOT NULL DEFAULT '[]',
  year INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'UNKNOWN',
  type TEXT NOT NULL DEFAULT 'TV',
  rating DOUBLE PRECISION NOT NULL DEFAULT 0,
  rating_score_count INTEGER NOT NULL DEFAULT 0,
  age_rating TEXT NOT NULL DEFAULT '14+',
  studios JSONB NOT NULL DEFAULT '[]',
  total_episodes INTEGER NOT NULL DEFAULT 0,
  featured BOOLEAN NOT NULL DEFAULT false,
  popular BOOLEAN NOT NULL DEFAULT false,
  recent BOOLEAN NOT NULL DEFAULT false,
  trending BOOLEAN NOT NULL DEFAULT false,
  simulcast BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS seasons (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  anime_id UUID NOT NULL REFERENCES animes(id) ON DELETE CASCADE,
  season_number INTEGER NOT NULL,
  name TEXT NOT NULL DEFAULT 'Season',
  episode_count INTEGER NOT NULL DEFAULT 0,
  year INTEGER,
  poster TEXT,
  UNIQUE(anime_id, season_number)
);

CREATE TABLE IF NOT EXISTS episodes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  anime_id UUID NOT NULL REFERENCES animes(id) ON DELETE CASCADE,
  season_id UUID REFERENCES seasons(id) ON DELETE SET NULL,
  number INTEGER NOT NULL,
  title TEXT NOT NULL,
  thumbnail TEXT NOT NULL DEFAULT '',
  duration INTEGER NOT NULL DEFAULT 0,
  description TEXT,
  air_date DATE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(anime_id, season_id, number)
);

CREATE TABLE IF NOT EXISTS episode_sources (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  episode_id UUID NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
  provider TEXT NOT NULL,
  name TEXT NOT NULL,
  url TEXT NOT NULL,
  type TEXT NOT NULL DEFAULT 'hls',
  quality TEXT NOT NULL DEFAULT 'Auto',
  language TEXT,
  subtitles JSONB NOT NULL DEFAULT '[]',
  addon_id TEXT,
  playable_in_browser BOOLEAN NOT NULL DEFAULT false,
  enabled BOOLEAN NOT NULL DEFAULT true,
  priority INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS favorites (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  anime_id UUID NOT NULL REFERENCES animes(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY(user_id, anime_id)
);

CREATE TABLE IF NOT EXISTS watch_history (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  episode_id UUID NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
  position_seconds INTEGER NOT NULL DEFAULT 0,
  duration_seconds INTEGER NOT NULL DEFAULT 0,
  completed BOOLEAN NOT NULL DEFAULT false,
  watched_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY(user_id, episode_id)
);

CREATE TABLE IF NOT EXISTS downloads (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  episode_id UUID NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
  source_id UUID REFERENCES episode_sources(id) ON DELETE SET NULL,
  status TEXT NOT NULL DEFAULT 'queued',
  progress DOUBLE PRECISION NOT NULL DEFAULT 0,
  local_uri TEXT,
  error_message TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(user_id, episode_id)
);

CREATE TABLE IF NOT EXISTS notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  body TEXT NOT NULL,
  type TEXT NOT NULL DEFAULT 'system',
  data JSONB NOT NULL DEFAULT '{}',
  read_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS notification_settings (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  new_episodes BOOLEAN NOT NULL DEFAULT true,
  recommendations BOOLEAN NOT NULL DEFAULT true,
  system BOOLEAN NOT NULL DEFAULT true,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS referrals (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  code TEXT NOT NULL UNIQUE,
  claimed_at TIMESTAMPTZ,
  redeemed_count INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS achievements (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  key TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL,
  description TEXT NOT NULL,
  xp_reward INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_achievements (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  achievement_id UUID NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
  unlocked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY(user_id, achievement_id)
);

-- Backward-compatible additions for databases created with Akiro API v1.
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS xp INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS level INTEGER NOT NULL DEFAULT 1;
ALTER TABLE users ADD COLUMN IF NOT EXISTS coins INTEGER NOT NULL DEFAULT 0;
ALTER TABLE animes ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE animes ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE seasons ADD COLUMN IF NOT EXISTS poster TEXT;
ALTER TABLE episode_sources ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE downloads ADD COLUMN IF NOT EXISTS error_message TEXT;

CREATE INDEX IF NOT EXISTS idx_animes_title ON animes USING gin (to_tsvector('simple', title));
CREATE INDEX IF NOT EXISTS idx_animes_popular ON animes(popular, rating DESC);
CREATE INDEX IF NOT EXISTS idx_animes_trending ON animes(trending, rating DESC);
CREATE INDEX IF NOT EXISTS idx_animes_recent ON animes(recent, year DESC, rating DESC);
CREATE INDEX IF NOT EXISTS idx_episodes_anime ON episodes(anime_id, number);
CREATE INDEX IF NOT EXISTS idx_sources_episode ON episode_sources(episode_id, enabled, priority DESC);
CREATE INDEX IF NOT EXISTS idx_history_user ON watch_history(user_id, watched_at DESC);
CREATE INDEX IF NOT EXISTS idx_downloads_user ON downloads(user_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, created_at DESC);

INSERT INTO achievements(key, name, description, xp_reward) VALUES
 ('first_watch', 'Primeiro episódio', 'Assista ao primeiro episódio no Akiro.', 50),
 ('five_episodes', 'Maratonista', 'Assista a cinco episódios.', 150),
 ('favorite_anime', 'Colecionador', 'Adicione um anime aos favoritos.', 25)
ON CONFLICT (key) DO NOTHING;
