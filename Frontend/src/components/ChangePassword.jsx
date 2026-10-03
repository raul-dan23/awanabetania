import React, { useState } from 'react';
import { API_URL } from '../config';

/**
 * Shown right after logging in with a temporary password (set by a director's reset):
 * the user picks their own password before entering the app.
 *
 * @param {Object} props
 * @param {string} props.currentPassword - The temporary password just used to log in
 * @param {Function} props.onDone - Called once the new password is saved
 */
const ChangePassword = ({ currentPassword, onDone }) => {
    const [pass, setPass] = useState('');
    const [confirm, setConfirm] = useState('');
    const [err, setErr] = useState('');
    const [loading, setLoading] = useState(false);

    const save = async (e) => {
        e.preventDefault();
        if (pass.length < 6) { setErr('Parola trebuie sa aiba cel putin 6 caractere.'); return; }
        if (pass !== confirm) { setErr('Parolele nu coincid.'); return; }
        if (pass === currentPassword) { setErr('Alege o parola diferita de cea temporara.'); return; }
        setLoading(true);
        setErr('');
        try {
            const r = await fetch(`${API_URL}/account/password`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ currentPassword, newPassword: pass }),
            });
            if (r.ok) onDone();
            else setErr(await r.text() || 'Parola nu a putut fi salvata.');
        } catch {
            setErr('Eroare de conexiune.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <form onSubmit={save} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <h1 className="auth-title">Alege o parola noua</h1>
            <p className="auth-subtitle">
                Ai intrat cu o parola temporara primita de la director. Alege acum una a ta.
            </p>
            <div className="auth-input-wrap">
                <span className="auth-input-icon">🔒</span>
                <input type="password" className="auth-input" placeholder="Parola noua (min. 6 caractere)"
                       value={pass} onChange={e => setPass(e.target.value)} required autoComplete="new-password" />
            </div>
            <div className="auth-input-wrap">
                <span className="auth-input-icon">🔒</span>
                <input type="password" className="auth-input" placeholder="Repeta parola noua"
                       value={confirm} onChange={e => setConfirm(e.target.value)} required autoComplete="new-password" />
            </div>
            {err && <div className="auth-error">{err}</div>}
            <button type="submit" disabled={loading} className="auth-submit">
                {loading ? <span className="auth-spinner" /> : 'Salveaza si intra'}
            </button>
        </form>
    );
};

export default ChangePassword;
