async function tmdb(path, params = {}) {
  const key = process.env.TMDB_API_KEY?.trim();
  const rawBearer = process.env.TMDB_API_READ_ACCESS_TOKEN?.trim();
  const bearer = rawBearer
    ? rawBearer.replace(/^Bearer\s+/i, '')
    : '';

  if (!key && !bearer) {
    throw new Error('TMDB_API_KEY_NOT_CONFIGURED');
  }

  const url = new URL(`${TMDB_BASE}${path}`);

  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null) {
      url.searchParams.set(k, String(v));
    }
  }

  // Tenta primeiro o TMDB Read Access Token
  if (bearer) {
    const response = await fetch(url, {
      headers: {
        accept: 'application/json',
        Authorization: `Bearer ${bearer}`
      },
      signal: AbortSignal.timeout(20_000)
    });

    if (response.ok) {
      return response.json();
    }

    // Se o Bearer estiver inválido, tenta a API Key
    if (response.status !== 401 || !key) {
      throw new Error(`TMDB_${response.status}`);
    }
  }

  // Fallback para TMDB API Key
  if (key) {
    const keyUrl = new URL(url);
    keyUrl.searchParams.set('api_key', key);

    const response = await fetch(keyUrl, {
      headers: {
        accept: 'application/json'
      },
      signal: AbortSignal.timeout(20_000)
    });

    if (!response.ok) {
      throw new Error(`TMDB_${response.status}`);
    }

    return response.json();
  }

  throw new Error('TMDB_401');
}
