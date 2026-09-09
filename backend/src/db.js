import pg from 'pg';
import dotenv from 'dotenv';
dotenv.config();

const { Pool } = pg;
if (!process.env.DATABASE_URL) console.warn('DATABASE_URL is not configured.');
export const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  max: Number(process.env.DB_POOL_MAX || 5),
  idleTimeoutMillis: 30_000,
  connectionTimeoutMillis: 10_000,
  ssl: process.env.DATABASE_URL?.includes('localhost') ? false : { rejectUnauthorized: false }
});

export async function query(text, params = []) {
  const result = await pool.query(text, params);
  return result.rows;
}
