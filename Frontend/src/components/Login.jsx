import React, { useState } from 'react';
import { API_URL } from '../config';
import AwanaLogo from '../AwanaLogo';

/**
 * Authentication screen. Supports three flows:
 *  1. Normal login (child / leader / director role selector + credentials)
 *  2. Account registration link
 *  3. Guest arbiter entry for Olimpiada (no account required — just session code + name)
 *
 * @param {Object} props
 * @param {Function} props.onLogin - Called with the user object on successful login
 * @param {Function} props.onSwitchToRegister - Called when the user wants to register
 * @param {Function} props.onGuestArbiter - Called with `{ code, name, sessionName }` for guest arbiters
 */
const Login = ({ onLogin, onSwitchToRegister, onGuestArbiter }) => {
    const [form, setForm] = useState({ username:'', pass:'', role: 'LEADER' });
    const [err, setErr] = useState('');
    const [loading, setLoading] = useState(false);
    const [guestMode, setGuestMode] = useState(false);
    const [guestForm, setGuestForm] = useState({ code: '', name: '' });
    const [guestErr, setGuestErr] = useState('');
    const [guestLoading, setGuestLoading] = useState(false);

    /**
     * Submits login credentials with a 5-second timeout to avoid hanging on
     * an unresponsive server.
     */
    const doLogin = (e) => {
        e.preventDefault();
        setLoading(true);
        setErr('');
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 5000);
        fetch(`${API_URL}/auth/login`, {
            method: 'POST',
            headers: {'Content-Type':'application/json'},
            body: JSON.stringify({ username: form.username, password: form.pass, role: form.role }),
            signal: controller.signal
        })
            .then(async r => {
                clearTimeout(timeoutId);
                const contentType = r.headers.get("content-type");
                if (contentType && contentType.includes("application/json")) {
                    const data = await r.json();
                    if(r.ok) onLogin(data);
                    else setErr(data.message || 'Date gresite!');
                } else {
                    // Some error responses come back as plain text
                    setErr(await r.text() || 'Date gresite!');
                }
            })
            .catch((error) => {
                clearTimeout(timeoutId);
                if (error.name === 'AbortError') setErr('Serverul nu raspunde. Incearca din nou.');
                else setErr('Server offline sau eroare de conexiune.');
            })
            .finally(() => setLoading(false));
    };

    /**
     * Validates the Olimpiada session code before granting guest arbiter access.
     * Rejects closed sessions with a user-visible error.
     */
    const enterAsGuest = async (e) => {
        e.preventDefault();
        if (!guestForm.code.trim() || !guestForm.name.trim()) return;
        setGuestLoading(true);
        setGuestErr('');
        try {
            const r = await fetch(`${API_URL}/olimpiada/session/${guestForm.code.trim().toUpperCase()}`);
            if (r.ok) {
                const s = await r.json();
                if (s.status === 'CLOSED') { setGuestErr('Aceasta sesiune este inchisa.'); return; }
                onGuestArbiter({ code: s.code, name: guestForm.name.trim(), sessionName: s.name });
            } else {
                setGuestErr('Codul sesiunii nu a fost gasit.');
            }
        } catch {
            setGuestErr('Eroare de conexiune.');
        } finally {
            setGuestLoading(false);
        }
    };

    const roles = [
        { id: 'CHILD',    label: 'Copil'    },
        { id: 'LEADER',   label: 'Lider'    },
        { id: 'DIRECTOR', label: 'Director' },
    ];

    return (
        <div className="auth-wrap">
            {/* Left branding panel — decorative, hidden on mobile */}
            <div className="auth-left">
                <div className="auth-left-circle auth-left-circle-1" />
                <div className="auth-left-circle auth-left-circle-2" />
                <div className="auth-left-circle auth-left-circle-3" />
                <div className="auth-left-content">
                    <AwanaLogo width="210px" />
                    <h2 className="auth-left-title">Awana Betania</h2>
                    <p className="auth-left-sub">Timisoara</p>
                    <div className="auth-left-divider" />
                    <p className="auth-left-verse">
                        "Isus Hristos este acelasi ieri si azi si in veci."
                        <strong>Evrei 13:8</strong>
                    </p>
                </div>
            </div>

            {/* Right panel — login form */}
            <div className="auth-right">
                <div className="auth-form-box">
                    <div className="auth-mobile-logo"><AwanaLogo width="130px" /></div>

                    <h1 className="auth-title">Bun venit!</h1>
                    <p className="auth-subtitle">Intra in contul tau pentru a continua.</p>

                    <div className="auth-roles">
                        {roles.map(r => (
                            <button key={r.id} type="button"
                                onClick={() => setForm({...form, role: r.id})}
                                className={`auth-role-btn ${form.role === r.id ? 'auth-role-active' : ''}`}>
                                {r.label}
                            </button>
                        ))}
                    </div>

                    <form onSubmit={doLogin} style={{display:'flex', flexDirection:'column', gap:'14px'}}>
                        <div className="auth-input-wrap">
                            <span className="auth-input-icon">@</span>
                            <input
                                type="text"
                                placeholder="Nume utilizator"
                                className="auth-input"
                                value={form.username}
                                // Strip spaces and force lowercase to match what the backend stores
                                onChange={e => setForm({...form, username: e.target.value.toLowerCase().replace(/\s/g,'')})}
                                required
                                autoComplete="username"
                            />
                        </div>
                        <div className="auth-input-wrap">
                            <span className="auth-input-icon">🔒</span>
                            <input
                                type="password"
                                placeholder="Parola"
                                className="auth-input"
                                value={form.pass}
                                onChange={e => setForm({...form, pass: e.target.value})}
                                required
                                autoComplete="current-password"
                            />
                        </div>

                        {err && <div className="auth-error">{err}</div>}

                        <button type="submit" disabled={loading} className="auth-submit">
                            {loading ? <span className="auth-spinner" /> : 'Intra in Cont'}
                        </button>
                    </form>

                    <p className="auth-switch" onClick={onSwitchToRegister}>
                        Nu ai cont? <strong>Inregistreaza-te</strong>
                    </p>

                    {/* Olimpiada guest entry — collapses into a form when activated */}
                    <div style={{ marginTop: 16, borderTop: '1px solid #e5e7eb', paddingTop: 14 }}>
                        {!guestMode ? (
                            <button type="button" onClick={() => setGuestMode(true)} style={{
                                width: '100%', padding: '10px', borderRadius: 8,
                                border: '1.5px solid #e5e7eb', background: '#f9fafb',
                                color: '#374151', fontWeight: 500, cursor: 'pointer', fontSize: '0.9rem'
                            }}>
                                Intru ca arbitru (Olimpiada)
                            </button>
                        ) : (
                            <form onSubmit={enterAsGuest} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                                <div style={{ fontWeight: 600, fontSize: '0.88rem', color: '#374151' }}>Arbitru invitat</div>
                                <input
                                    placeholder="Cod sesiune (ex: OLM26)"
                                    value={guestForm.code}
                                    onChange={e => setGuestForm(f => ({ ...f, code: e.target.value.toUpperCase() }))}
                                    required
                                    style={{ padding: '9px 12px', borderRadius: 8, border: '1px solid #d1d5db', fontSize: '0.9rem' }}
                                />
                                <input
                                    placeholder="Numele tau (ex: Raul)"
                                    value={guestForm.name}
                                    onChange={e => setGuestForm(f => ({ ...f, name: e.target.value }))}
                                    required
                                    style={{ padding: '9px 12px', borderRadius: 8, border: '1px solid #d1d5db', fontSize: '0.9rem' }}
                                />
                                {guestErr && <div style={{ color: '#dc2626', fontSize: '0.82rem' }}>{guestErr}</div>}
                                <div style={{ display: 'flex', gap: 8 }}>
                                    <button type="button" onClick={() => setGuestMode(false)} style={{
                                        flex: 1, padding: '9px', borderRadius: 8,
                                        border: '1px solid #e5e7eb', background: '#f3f4f6',
                                        cursor: 'pointer', fontSize: '0.88rem'
                                    }}>
                                        Inapoi
                                    </button>
                                    <button type="submit" disabled={guestLoading} style={{
                                        flex: 2, padding: '9px', borderRadius: 8,
                                        border: 'none', background: '#4f46e5', color: '#fff',
                                        fontWeight: 600, cursor: 'pointer', fontSize: '0.88rem'
                                    }}>
                                        {guestLoading ? 'Se verifica...' : 'Intra'}
                                    </button>
                                </div>
                            </form>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Login;
