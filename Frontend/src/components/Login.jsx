import React, { useState } from 'react';
import { API_URL } from '../config';
import AwanaLogo from '../AwanaLogo';

const Login = ({ onLogin, onSwitchToRegister }) => {
    const [form, setForm] = useState({ username:'', pass:'', role: 'LEADER' });
    const [err, setErr] = useState('');
    const [loading, setLoading] = useState(false);

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

    const roles = [
        { id: 'CHILD',    label: 'Copil'    },
        { id: 'LEADER',   label: 'Lider'    },
        { id: 'DIRECTOR', label: 'Director' },
    ];

    return (
        <div className="auth-wrap">
            {/* STANGA — branding */}
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

            {/* DREAPTA — formular */}
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
                </div>
            </div>
        </div>
    );
};

export default Login;
