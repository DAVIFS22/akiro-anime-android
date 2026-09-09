import dotenv from 'dotenv';
dotenv.config();
import jwt from 'jsonwebtoken';
import bcrypt from 'bcryptjs';
import { query } from './db.js';

const secret = process.env.JWT_SECRET;
if (!secret) console.warn('JWT_SECRET is not configured.');

export async function hashPassword(password) { return bcrypt.hash(password, 12); }
export async function verifyPassword(password, hash) { return bcrypt.compare(password, hash); }
export function signUser(user) {
  if (!secret) throw new Error('JWT_SECRET_NOT_CONFIGURED');
  return jwt.sign({ sub: user.id, email: user.email }, secret, { expiresIn: '30d', issuer: 'akiro-api' });
}

export async function requireAuth(req, res, next) {
  try {
    if (!secret) return res.status(503).json({ error: 'AUTH_NOT_CONFIGURED' });
    const header = req.headers.authorization || '';
    const token = header.startsWith('Bearer ') ? header.slice(7) : null;
    if (!token) return res.status(401).json({ error: 'AUTH_REQUIRED' });
    const payload = jwt.verify(token, secret, { issuer: 'akiro-api' });
    const users = await query('SELECT id,email,display_name,avatar_url,xp,level,coins FROM users WHERE id=$1', [payload.sub]);
    if (!users[0]) return res.status(401).json({ error: 'INVALID_SESSION' });
    req.user = users[0];
    next();
  } catch { res.status(401).json({ error: 'INVALID_SESSION' }); }
}

export function requireAdmin(req, res, next) {
  const token = process.env.ADMIN_TOKEN;
  if (!token || req.headers.authorization !== `Bearer ${token}`) return res.status(403).json({ error: 'ADMIN_REQUIRED' });
  next();
}
