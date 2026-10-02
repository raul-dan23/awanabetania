import { API_URL } from './config';

const TOKEN_KEY = 'awanaToken';

/** Returns the stored JWT, or null when nobody is logged in. */
export function getToken() {
    try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
}

/** Stores the JWT issued by /api/auth/login. */
export function setToken(token) {
    try { token ? localStorage.setItem(TOKEN_KEY, token) : localStorage.removeItem(TOKEN_KEY); } catch { /* private mode */ }
}

/** Drops the stored JWT — called on logout and whenever the server rejects it. */
export function clearToken() {
    try { localStorage.removeItem(TOKEN_KEY); } catch { /* private mode */ }
}

/**
 * Wraps the global fetch so every call to our own API carries the bearer token.
 *
 * Doing it here rather than at each of the ~80 call sites keeps the change small and
 * means a request added later is authenticated automatically instead of silently
 * failing. Requests to other hosts are passed through untouched, so the token is never
 * leaked to a third party.
 *
 * A 401 means the token is missing, expired or invalid: the stored session is cleared
 * and the page reloaded, which lands the user back on the login screen.
 */
export function installAuthFetch({ onUnauthorized } = {}) {
    if (window.__awanaAuthFetchInstalled) return;
    window.__awanaAuthFetchInstalled = true;

    const nativeFetch = window.fetch.bind(window);

    window.fetch = async (input, init) => {
        const url = typeof input === 'string' ? input : (input && input.url) || '';
        const isOwnApi = url.startsWith(API_URL) || url.startsWith('/api/');

        let request = input;
        let options = init;

        if (isOwnApi) {
            const token = getToken();
            if (token) {
                const headers = new Headers(
                    (init && init.headers) || (typeof input !== 'string' && input.headers) || {}
                );
                if (!headers.has('Authorization')) {
                    headers.set('Authorization', `Bearer ${token}`);
                }
                options = { ...(init || {}), headers };
                // A Request object carries its own headers, so rebuild it with the new ones.
                if (typeof input !== 'string') request = new Request(input, options);
            }
        }

        const response = await nativeFetch(request, typeof input === 'string' ? options : undefined);

        // The login call legitimately answers 401 on wrong credentials — leave it alone.
        const isLogin = url.includes('/auth/login');
        if (isOwnApi && !isLogin && response.status === 401) {
            clearToken();
            if (onUnauthorized) onUnauthorized();
        }

        return isOwnApi && !response.ok ? readableError(response) : response;
    };
}

/**
 * The API reports errors as RFC 7807 problem JSON ({"status":409,"detail":"..."}).
 * The screens read error bodies with res.text() and show them to the user as they are,
 * so hand them just the human-readable `detail`. Anything else passes through unchanged.
 */
async function readableError(response) {
    const type = response.headers.get('content-type') || '';
    if (!type.includes('application/problem+json')) return response;
    try {
        const problem = await response.clone().json();
        if (!problem || typeof problem.detail !== 'string') return response;
        return new Response(problem.detail, {
            status: response.status,
            statusText: response.statusText,
            headers: { 'Content-Type': 'text/plain;charset=UTF-8' },
        });
    } catch {
        return response;
    }
}
