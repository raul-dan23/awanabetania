import { useState, useEffect, useRef } from 'react';

/**
 * Hook care se conecteaza la NFC Bridge-ul local (ws://localhost:7000).
 * Reconectare automata la 3s daca se pierde conexiunea.
 *
 * @param {function} onUid - callback apelat cu UID-ul (string) cand un card e detectat
 * @returns {boolean} connected - true daca bridge-ul e pornit si conectat
 */
export function useNfcBridge(onUid) {
    const [connected, setConnected] = useState(false);
    const wsRef = useRef(null);
    const reconnectRef = useRef(null);
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

                ws.onerror = () => setConnected(false);

                ws.onmessage = (e) => {
                    try {
                        const data = JSON.parse(e.data);
                        if (data.uid) onUidRef.current(data.uid);
                    } catch {}
                };
            } catch {}
        };

        connect();

        return () => {
            clearTimeout(reconnectRef.current);
            wsRef.current?.close();
        };
    }, []);

    return connected;
}
