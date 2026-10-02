/**
 * Base URL for all API requests, set per environment through VITE_API_URL:
 * Frontend/.env.development for `npm run dev`, Frontend/.env.production for `npm run build`.
 * To point a local build elsewhere, put VITE_API_URL in .env.development.local (git-ignored).
 */
export const API_URL = import.meta.env.VITE_API_URL || '/api';
