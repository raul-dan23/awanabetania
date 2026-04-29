import React, { useState, useEffect } from 'react';
import { API_URL } from '../config';
import AwanaLogo from '../AwanaLogo';

const Register = ({ onSwitchToLogin }) => {
    const [roleType, setRoleType] = useState('CHILD');
    const [form, setForm] = useState({ name:'', surname:'', pass:'', birthDate:'', parentName:'', phone:'', regCode: '' });
    const [msg, setMsg] = useState('');
    const [loading, setLoading] = useState(false);
    const [departments, setDepartments] = useState([]);
    const [selectedDepts, setSelectedDepts] = useState(new Set());

    useEffect(() => {
        fetch(`${API_URL}/departments`).then(r=>r.ok?r.json():[]).then(setDepartments).catch(()=>{});
    }, []);

    const toggleDept = (id) => {
        const next = new Set(selectedDepts);
        if(next.has(id)) next.delete(id); else next.add(id);
        setSelectedDepts(next);
    };

    const doRegister = (e) => {
        e.preventDefault();
        setLoading(true);
        setMsg('');
        const payload = {
            name: form.name, surname: form.surname, password: form.pass, role: roleType,
            birthDate: roleType === 'CHILD' ? form.birthDate : null,
            parentName: roleType === 'CHILD' ? form.parentName : null,
            parentPhone: roleType === 'CHILD' ? form.phone : null,
            phoneNumber: roleType !== 'CHILD' ? form.phone : null,
            registrationCode: roleType !== 'CHILD' ? form.regCode : null,
            departmentIds: roleType !== 'CHILD' ? Array.from(selectedDepts) : []
        };
        fetch(`${API_URL}/auth/register`, {
            method: 'POST', headers: {'Content-Type':'application/json'},
            body: JSON.stringify(payload)
        })
            .then(async r => {
                const text = await r.text();
                if(r.ok) {
                    setMsg('ok:' + text + '. Noteaza-ti username-ul pentru login!');
                    setTimeout(onSwitchToLogin, 4500);
                } else {
                    setMsg('err:' + text);
                }
            })
            .catch(() => setMsg('err:Eroare conexiune server.'))
            .finally(() => setLoading(false));
    };

    const roles = [
        { id: 'CHILD',    label: 'Copil'    },
        { id: 'LEADER',   label: 'Lider'    },
        { id: 'DIRECTOR', label: 'Director' },
    ];

    const isOk  = msg.startsWith('ok:');
    const isErr = msg.startsWith('err:');
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
                    <p className="auth-subtitle">Alatura-te comunitatii Awana Betania.</p>

                    <div className="auth-roles" style={{marginBottom:'20px'}}>
                        {roles.map(r => (
                            <button key={r.id} type="button"
                                onClick={() => { setRoleType(r.id); setSelectedDepts(new Set()); }}
                                className={`auth-role-btn ${roleType === r.id ? 'auth-role-active' : ''}`}>
                                {r.label}
                            </button>
                        ))}
                    </div>

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
                            <input type="password" placeholder="Parola" className="auth-input" onChange={e=>setForm({...form, pass:e.target.value})} required />
                        </div>

                        {roleType !== 'CHILD' && (
                            <div className="auth-input-wrap">
                                <span className="auth-input-icon">🛡️</span>
                                <input placeholder="Cod de Acces" className="auth-input" style={{borderColor:'#f87171'}} onChange={e=>setForm({...form, regCode:e.target.value})} required />
                            </div>
                        )}

                        {roleType === 'CHILD' ? (
                            <>
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
                            </>
                        ) : (
                            <>
                                <div className="auth-input-wrap">
                                    <span className="auth-input-icon">📞</span>
                                    <input placeholder="Telefonul tau" className="auth-input" onChange={e=>setForm({...form, phone:e.target.value})} required />
                                </div>
                                {departments.length > 0 && (
                                    <div>
                                        <p style={{fontSize:'0.8rem', fontWeight:'700', color:'var(--text-secondary)', textTransform:'uppercase', letterSpacing:'0.8px', marginBottom:'10px'}}>Departamente</p>
                                        <div className="auth-dept-grid">
                                            {departments.map(d => (
                                                <div key={d.id} onClick={() => toggleDept(d.id)}
                                                    className={`auth-dept-chip ${selectedDepts.has(d.id) ? 'auth-dept-selected' : ''}`}>
                                                    <span>{d.name}</span>
                                                    {selectedDepts.has(d.id) && <span>✓</span>}
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </>
                        )}

                        {msg && (
                            <div className={isOk ? 'auth-success' : 'auth-error'}>{msgText}</div>
                        )}

                        <button type="submit" disabled={loading} className="auth-submit" style={{marginTop:'8px'}}>
                            {loading ? <span className="auth-spinner" /> : 'Creeaza Cont'}
                        </button>
                    </form>

                    <p className="auth-switch" onClick={onSwitchToLogin}>
                        Ai deja cont? <strong>Logheaza-te</strong>
                    </p>
                </div>
            </div>
        </div>
    );
};

export default Register;
