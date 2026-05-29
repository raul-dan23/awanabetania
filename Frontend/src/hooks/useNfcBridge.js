import { useState, useEffect, useRef } from 'react';

/**
 * Manages the WebSocket connection to the local NFC bridge JAR.
 * Reconnects automatically every 3 seconds on disconnect.
 *
 * The bridge sends `{"uid":"A1B2C3D4"}` whenever a card is tapped on the
 * PC/SC reader. The hook fires `onUid` with the UID string and keeps the
 * callback in a ref so callers can pass a new function on every render
 * without triggering a reconnect.
 *
 * @param {Function} onUid - Called with the UID string each time a card is read
 * @returns {boolean} connected - true when the bridge WebSocket is open
 */
export function useNfcBridge(onUid) {
    const [connected, setConnected] = useState(false);
    const wsRef = useRef(null);
    const reconnectRef = useRef(null);
    // Keep the callback in a ref to avoid restarting the socket on every render
    const onUidRef = useRef(onUid);

    useEffect(() => { onUidRef.current = onUid; }, [onUid]);

    useEffect(() => {
        const connect = () => {
            try {
                const ws = new WebSocket('ws://localhost:7000');
                wsRef.current = ws;

                ws.onopen = () => setConnected(true);

                ws.onclose = () => {
                    setConnected(false);
                    reconnectRef.current = setTimeout(connect, 3000);
                };

                // onerror always fires before onclose, so we just track state here
                ws.onerror = () => setConnected(false);

                ws.onmessage = (e) => {
                    try {
                        const data = JSON.parse(e.data);
                        if (data.uid) onUidRef.current(data.uid);
                    } catch {
                        // Ignore non-JSON or unexpected messages from the bridge
                    }
                };
            } catch {
                // WebSocket constructor can throw if the URL is invalid; safe to ignore
            }
        };

        connect();

        return () => {
            clearTimeout(reconnectRef.current);
            wsRef.current?.close();
        };
    }, []);

    return connected;
}
