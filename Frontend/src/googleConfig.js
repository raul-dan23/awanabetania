import { useEffect, useState } from 'react';
import { API_URL } from './config';

// Asked once per page load and shared by every caller
let configPromise = null;

/** The public Google client id configured on the server, or null while Google sign-in is off. */
export function loadGoogleClientId() {
    if (!configPromise) {
        configPromise = fetch(`${API_URL}/auth/config`)
            .then(r => (r.ok ? r.json() : {}))
            .then(cfg => cfg.googleClientId || null)
            .catch(() => null);
    }
    return configPromise;
}

/**
 * Whether "Continua cu Google" is set up on the server: null while checking, then true or
 * false. Screens use it to leave out Google buttons and texts until it is configured.
 */
export function useGoogleSignIn() {
    const [available, setAvailable] = useState(null);
    useEffect(() => {
        let active = true;
        loadGoogleClientId().then(id => { if (active) setAvailable(!!id); });
        return () => { active = false; };
    }, []);
    return available;
}
