import React, { useEffect, useRef, useState } from 'react';
import { API_URL } from '../config';

// Shared by every button on the page: the server settings and Google's script load once.
let configPromise = null;
let scriptPromise = null;

/** The public Google client id configured on the server, or null. */
function loadClientId() {
    if (!configPromise) {
        configPromise = fetch(`${API_URL}/auth/config`)
            .then(r => (r.ok ? r.json() : {}))
            .then(cfg => cfg.googleClientId || null)
            .catch(() => null);
    }
    return configPromise;
}

/** Google Identity Services, the script behind the official "Sign in with Google" button. */
function loadGoogleScript() {
    if (!scriptPromise) {
        scriptPromise = new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = 'https://accounts.google.com/gsi/client';
            script.async = true;
            script.onload = resolve;
            script.onerror = reject;
            document.head.appendChild(script);
        });
    }
    return scriptPromise;
}

/**
 * Google's official button. When the person picks their Google account, Google hands the
 * page a signed ID token ("credential"), which the server verifies. Renders nothing when
 * the server has no Google client id configured or Google cannot be reached.
 *
 * @param {Object} props
 * @param {Function} props.onCredential - Called with the ID token string
 * @param {string} [props.text] - Google's caption: 'continue_with' or 'signin_with'
 * @param {React.ReactNode} [props.caption] - Shown above the button once it is available
 */
const GoogleSignInButton = ({ onCredential, text = 'continue_with', caption = null }) => {
    const container = useRef(null);
    const callback = useRef(onCredential);
    const [ready, setReady] = useState(false);

    useEffect(() => {
        callback.current = onCredential;
    }, [onCredential]);

    useEffect(() => {
        let cancelled = false;
        loadClientId()
            .then(clientId => {
                if (!clientId) return null;
                return loadGoogleScript().then(() => {
                    if (cancelled || !container.current) return;
                    window.google.accounts.id.initialize({
                        client_id: clientId,
                        callback: response => callback.current(response.credential),
                    });
                    window.google.accounts.id.renderButton(container.current, {
                        theme: 'outline', size: 'large', shape: 'pill', text, locale: 'ro', width: 280,
                    });
                    setReady(true);
                });
            })
            .catch(() => { /* Google unreachable: no button, password login still works */ });
        return () => { cancelled = true; };
    }, [text]);

    // The container stays visible: Google sizes the button from it while drawing.
    return (
        <div>
            {ready && caption}
            <div ref={container} style={{ display: 'flex', justifyContent: 'center' }} />
        </div>
    );
};

export default GoogleSignInButton;
