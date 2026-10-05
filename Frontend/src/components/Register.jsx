import React, { useState } from 'react';
import { API_URL } from '../config';
import AwanaLogo from '../AwanaLogo';

/**
 * Registration form for children (birth date, parent name, parent phone).
 * Leaders do not register here: the director adds them in the Control Center.
 *
 * On success, shows the assigned username and redirects to login after 4.5s.
 *
 * @param {Object} props
 * @param {Function} props.onSwitchToLogin - Called when the user wants to go back to login
 */
const Register = ({ onSwitchToLogin }) => {
    const [form, setForm] = useState({ name: '', surname: '', pass: '', birthDate: '', parentName: '', phone: '' });
    // Prefixed with 'ok:' or 'err:' so the same state variable drives both success and error UI
    const [msg, setMsg] = useState('');
    const [loading, setLoading] = useState(false);

    /**
     * Submits the registration payload. The backend generates and returns the
     * username, which is shown to the user before redirecting to login.
     */
    const doRegister = (e) => {
        e.preventDefault();
        if (form.pass.length < 6) { setMsg('err:Parola trebuie sa aiba cel putin 6 caractere.'); return; }
        setLoading(true);
        setMsg('');
        const payload = {
            role: 'CHILD', name: form.name, surname: form.surname, password: form.pass,
            birthDate: form.birthDate, parentName: form.parentName, parentPhone: form.phone,
        };
        fetch(`${API_URL}/auth/register`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
            .then(async r => {
                const text = await r.text();
                if (r.ok) {
                    setMsg('ok:' + text + '. Noteaza-ti username-ul pentru login!');
                    setTimeout(onSwitchToLogin, 4500);
                } else {
                    setMsg('err:' + text);
                }
            })
            .catch(() => setMsg('err:Eroare conexiune server.'))
            .finally(() => setLoading(false));
    };

    // The 'ok:'/'err:' prefix drives which CSS class is applied
    const isOk = msg.startsWith('ok:');
    const msgText = msg.slice(3);

    return (
        <div className="auth-wrap">
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

            <div className="auth-right">
                <div className="auth-form-box">
                    <div className="auth-mobile-logo"><AwanaLogo width="130px" /></div>
                    <h1 className="auth-title">Cont Nou</h1>
                    <p className="auth-subtitle">Inscrie un copil in clubul Awana Betania.</p>

                    <form onSubmit={doRegister} style={{display:'flex', flexDirection:'column', gap:'12px'}}>
                        <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'12px'}}>
                            <div className="auth-input-wrap">
                                <span className="auth-input-icon" style={{fontSize:'0.8rem'}}>A</span>
                                <input placeholder="Prenume" className="auth-input" onChange={e=>setForm({...form, name:e.target.value})} required />
                            </div>
                            <div className="auth-input-wrap">
                                <span className="auth-input-icon" style={{fontSize:'0.8rem'}}>Z</span>
                                <input placeholder="Nume" className="auth-input" onChange={e=>setForm({...form, surname:e.target.value})} required />
                            </div>
                        </div>

                        <div className="auth-input-wrap">
                            <span className="auth-input-icon">🔒</span>
                            <input type="password" placeholder="Parola (min. 6 caractere)" className="auth-input" onChange={e=>setForm({...form, pass:e.target.value})} required autoComplete="new-password" />
                        </div>
                        <div className="auth-input-wrap">
                            <span className="auth-input-icon">📅</span>
                            <input type="date" className="auth-input" onChange={e=>setForm({...form, birthDate:e.target.value})} required />
                        </div>
                        <div className="auth-input-wrap">
                            <span className="auth-input-icon">👤</span>
                            <input placeholder="Nume Parinte" className="auth-input" onChange={e=>setForm({...form, parentName:e.target.value})} required />
                        </div>
                        <div className="auth-input-wrap">
                            <span className="auth-input-icon">📞</span>
                            <input placeholder="Telefon Parinte" className="auth-input" onChange={e=>setForm({...form, phone:e.target.value})} required />
                        </div>

                        {msg && (
                            <div className={isOk ? 'auth-success' : 'auth-error'}>{msgText}</div>
                        )}

                        <button type="submit" disabled={loading} className="auth-submit" style={{marginTop:'8px'}}>
                            {loading ? <span className="auth-spinner" /> : 'Creeaza Cont'}
                        </button>
                    </form>

                    <p style={{fontSize:'0.82rem', color:'var(--text-secondary)', textAlign:'center', margin:'14px 0 0'}}>
                        Esti lider? Nu iti faci cont aici: te adauga directorul, din Control Center.
                    </p>

                    <p className="auth-switch" onClick={onSwitchToLogin}>
                        Ai deja cont? <strong>Logheaza-te</strong>
                    </p>
                </div>
            </div>
        </div>
    );
};

export default Register;
