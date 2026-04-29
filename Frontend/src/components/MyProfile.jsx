import React, { useState, useEffect } from 'react';
import { API_URL } from '../config';

const MyProfile = ({ user, onUpdateUser }) => {
    const isChild = user.hasOwnProperty('parentPhone');
    const [isEditing, setIsEditing] = useState(false);
    const [formData, setFormData] = useState({ ...user, password: '' });
    const [loading, setLoading] = useState(false);
    const [allDepartments, setAllDepartments] = useState([]);
    const [showDeleteInput, setShowDeleteInput] = useState(false);
    const [deleteCode, setDeleteCode] = useState('');
    const [myFeedbacks, setMyFeedbacks] = useState([]);
    const [showFeedback, setShowFeedback] = useState(false);

    useEffect(() => {
        if (!isChild) {
            fetch(`${API_URL}/leaders/${user.id}`).then(r => r.ok ? r.json() : null).then(data => {
                if (data) { onUpdateUser(data); setFormData({ ...data, password: '' }); }
            });
            fetch(`${API_URL}/departments`).then(r => r.ok ? r.json() : []).then(setAllDepartments);
            fetch(`${API_URL}/feedback/leader/${user.id}`).then(r => r.ok ? r.json() : []).then(setMyFeedbacks).catch(() => {});
        } else {
            fetch(`${API_URL}/children/${user.id}`).then(r => r.json()).then(data => {
                onUpdateUser(data); setFormData({...data});
            });
        }
    }, []);

    const handleSave = () => {
        setLoading(true);
        const endpoint = isChild ? `${API_URL}/children/${user.id}` : `${API_URL}/leaders/${user.id}`;
        const payload = { ...formData };
        if (!payload.password) delete payload.password;
        fetch(endpoint, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })
            .then(res => res.ok ? res.json() : res.text().then(t => {throw t}))
            .then(u => { setLoading(false); setIsEditing(false); onUpdateUser(u); alert("Actualizat!"); })
            .catch((err) => { setLoading(false); alert(err || "Eroare server."); });
    };

    const toggleDepartment = (dept) => {
        const hasDept = formData.departments.some(d => d.id === dept.id);
        if (hasDept) setFormData({ ...formData, departments: formData.departments.filter(d => d.id !== dept.id) });
        else setFormData({ ...formData, departments: [...formData.departments, dept] });
    };

    const requestDeletionCode = () => {
        if (!window.confirm("Esti sigur ca vrei sa initiezi stergerea contului?")) return;
        fetch(`${API_URL}/account/request-deletion`, {
            method: 'POST', headers: {'Content-Type':'application/json'},
            body: JSON.stringify({ id: user.id, role: isChild ? 'CHILD' : 'LEADER' })
        }).then(res => res.text()).then(msg => alert(msg)).catch(() => alert("Eroare la solicitare cod."));
    };

    const performDeletion = () => {
        if (!deleteCode) return alert("Te rog introdu codul primit!");
        if (!window.confirm("ATENTIE! Actiunea este IREVERSIBILA. Continui?")) return;
        const baseUrl = isChild ? `${API_URL}/children/${user.id}` : `${API_URL}/leaders/${user.id}`;
        fetch(`${baseUrl}?code=${encodeURIComponent(deleteCode)}`, { method: 'DELETE' })
            .then(async res => {
                if (res.ok) { alert("Cont sters."); localStorage.clear(); window.location.reload(); }
                else { alert("Eroare: " + await res.text()); }
            }).catch(() => alert("Eroare de conexiune."));
    };

    const isAdmin = !isChild && user.id === 1;

    return (
        <div className="animate-in">

            {/* HERO — acelasi gradient ca Dashboard */}
            <div className="db-hero" style={{marginBottom:'24px', alignItems:'center'}}>
                <div className="db-hero-left" style={{display:'flex', alignItems:'center', gap:'20px'}}>
                    <div style={{
                        width:'66px', height:'66px', borderRadius:'18px',
                        background:'rgba(255,255,255,0.18)', backdropFilter:'blur(10px)',
                        border:'2px solid rgba(255,255,255,0.28)',
                        display:'flex', alignItems:'center', justifyContent:'center',
                        fontSize:'1.5rem', fontWeight:'900', color:'white',
                        letterSpacing:'-1px', flexShrink:0
                    }}>
                        {user.name?.charAt(0)}{user.surname?.charAt(0) || ''}
                    </div>
                    <div>
                        {isEditing ? (
                            <div style={{display:'flex', gap:'8px', marginBottom:'8px'}}>
                                <input className="login-input" value={formData.name} onChange={e=>setFormData({...formData,name:e.target.value})} placeholder="Prenume" style={{marginBottom:0}}/>
                                <input className="login-input" value={formData.surname} onChange={e=>setFormData({...formData,surname:e.target.value})} placeholder="Nume" style={{marginBottom:0}}/>
                            </div>
                        ) : (
                            <h1 style={{color:'white', fontSize:'1.9rem', fontWeight:'900', letterSpacing:'-0.5px', marginBottom:'8px', lineHeight:1}}>
                                {user.name} {user.surname}
                            </h1>
                        )}
                        <div style={{display:'flex', alignItems:'center', gap:'10px', flexWrap:'wrap'}}>
                            {isEditing ? (
                                <input className="login-input" value={formData.username||''} onChange={e=>setFormData({...formData,username:e.target.value.toLowerCase().replace(/\s/g,'')})} placeholder="username" style={{marginBottom:0, fontSize:'0.85rem', padding:'6px 12px', width:'160px'}}/>
                            ) : (
                                <span style={{color:'rgba(255,255,255,0.6)', fontSize:'0.95rem'}}>@{user.username || 'fara_username'}</span>
                            )}
                            <span className="db-role-pill">{isChild ? 'Copil' : (user.role || 'Lider')}</span>
                        </div>
                    </div>
                </div>
                <button onClick={() => isEditing ? handleSave() : setIsEditing(true)} style={{
                    background: isEditing ? 'rgba(5,205,153,0.22)' : 'rgba(255,255,255,0.14)',
                    border: `1.5px solid ${isEditing ? 'rgba(5,205,153,0.55)' : 'rgba(255,255,255,0.28)'}`,
                    color: isEditing ? '#05cd99' : 'white',
                    padding:'10px 24px', borderRadius:'14px',
                    fontWeight:'700', fontSize:'0.92rem', flexShrink:0,
                    transition:'all 0.2s'
                }}>
                    {loading ? '...' : isEditing ? 'Salveaza' : 'Editeaza'}
                </button>
            </div>

            {/* STAT CARDS — doar la copii */}
            {isChild && (
                <div className="db-stats" style={{marginBottom:'24px'}}>
                    <div className="db-stat-card db-stat-blue">
                        <div className="db-stat-number">{user.seasonPoints || 0}</div>
                        <div className="db-stat-label">Puncte Sezon</div>
                    </div>
                    <div className="db-stat-card db-stat-green">
                        <div className="db-stat-number">{user.totalAttendance || 0}</div>
                        <div className="db-stat-label">Prezente</div>
                    </div>
                    <div className="db-stat-card db-stat-purple">
                        <div className="db-stat-number">{user.attendanceStreak || 0}</div>
                        <div className="db-stat-label">Streak</div>
                    </div>
                    <div className="db-stat-card db-stat-amber">
                        <div className="db-stat-number">{[user.hasManual,user.hasShirt,user.hasHat].filter(Boolean).length}/3</div>
                        <div className="db-stat-label">Echipament</div>
                    </div>
                </div>
            )}

            {/* GRID — 2 coloane, acelasi .profile-grid ca restul */}
            <div className="profile-grid">

                {/* Date Personale */}
                <div className="card">
                    <p className="db-section-title">Date Personale</p>
                    {!isChild ? (
                        <div style={{display:'flex', flexDirection:'column'}}>
                            <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', padding:'14px 0', borderBottom:'1px solid var(--border-color)'}}>
                                <span style={{fontSize:'0.8rem', fontWeight:'700', color:'var(--text-secondary)', textTransform:'uppercase', letterSpacing:'0.6px'}}>Telefon</span>
                                {isEditing
                                    ? <input className="login-input" style={{marginBottom:0, maxWidth:'195px'}} value={formData.phoneNumber||''} onChange={e=>setFormData({...formData,phoneNumber:e.target.value})} placeholder="07xx xxx xxx"/>
                                    : <span style={{fontWeight:'700', color:'var(--text-primary)'}}>{user.phoneNumber || 'Nespecificat'}</span>
                                }
                            </div>
                            {isEditing && (
                                <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', padding:'14px 0', borderBottom:'1px solid var(--border-color)'}}>
                                    <span style={{fontSize:'0.8rem', fontWeight:'700', color:'var(--text-secondary)', textTransform:'uppercase', letterSpacing:'0.6px'}}>Parola Noua</span>
                                    <input type="password" className="login-input" style={{marginBottom:0, maxWidth:'195px'}} value={formData.password} onChange={e=>setFormData({...formData,password:e.target.value})} placeholder="Lasa gol pt. a pastra"/>
                                </div>
                            )}
                            <div style={{paddingTop:'14px'}}>
                                <span style={{fontSize:'0.8rem', fontWeight:'700', color:'var(--text-secondary)', textTransform:'uppercase', letterSpacing:'0.6px', display:'block', marginBottom:'12px'}}>Departamente</span>
                                {isEditing ? (
                                    <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'8px'}}>
                                        {allDepartments.map(dept => {
                                            const on = formData.departments?.some(d=>d.id===dept.id);
                                            return (
                                                <button key={dept.id} onClick={()=>toggleDepartment(dept)} style={{
                                                    padding:'9px 10px', borderRadius:'10px', fontSize:'0.85rem', fontWeight:'700',
                                                    background: on ? 'var(--accent)' : 'var(--bg-primary)',
                                                    color: on ? 'white' : 'var(--text-secondary)',
                                                    border:`2px solid ${on ? 'var(--accent)' : 'var(--border-color)'}`,
                                                    transition:'all 0.18s'
                                                }}>{dept.name}</button>
                                            );
                                        })}
                                    </div>
                                ) : (
                                    <div style={{display:'flex', flexWrap:'wrap', gap:'8px'}}>
                                        {user.departments?.length > 0
                                            ? user.departments.map(d => <span key={d.id} className="badge" style={{background:'#ede9fe', color:'#7c3aed'}}>{d.name}</span>)
                                            : <span style={{color:'var(--text-secondary)', fontSize:'0.9rem'}}>Niciun departament</span>
                                        }
                                    </div>
                                )}
                            </div>
                        </div>
                    ) : (
                        <div style={{display:'flex', flexDirection:'column'}}>
                            {[
                                {label:'Data Nasterii', key:'birthDate', type:'date', val: user.birthDate || 'Nespecificat'},
                                {label:'Parinte', key:'parentName', type:'text', val: user.parentName || 'Nespecificat'},
                                {label:'Telefon Parinte', key:'parentPhone', type:'text', val: user.parentPhone || 'Nespecificat'},
                            ].map((f, i, arr) => (
                                <div key={f.key} style={{display:'flex', justifyContent:'space-between', alignItems:'center', padding:'14px 0', borderBottom: i<arr.length-1 ? '1px solid var(--border-color)' : 'none'}}>
                                    <span style={{fontSize:'0.8rem', fontWeight:'700', color:'var(--text-secondary)', textTransform:'uppercase', letterSpacing:'0.6px', flexShrink:0, marginRight:'16px'}}>{f.label}</span>
                                    {isEditing
                                        ? <input type={f.type} className="login-input" style={{marginBottom:0, flex:1}} value={formData[f.key]||''} onChange={e=>setFormData({...formData,[f.key]:e.target.value})} placeholder={f.label}/>
                                        : <span style={{fontWeight:'700', color:'var(--text-primary)', textAlign:'right'}}>{f.val}</span>
                                    }
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* Coloana dreapta */}
                <div style={{display:'flex', flexDirection:'column', gap:'20px'}}>
                    {isChild && (
                        <div className="card">
                            <p className="db-section-title">Echipament</p>
                            {[
                                {label:'Manual', value: user.hasManual},
                                {label:'Tricou', value: user.hasShirt},
                                {label:'Caciula', value: user.hasHat},
                            ].map((item, i) => (
                                <div key={item.label} style={{display:'flex', alignItems:'center', justifyContent:'space-between', padding:'14px 0', borderBottom: i<2 ? '1px solid var(--border-color)' : 'none'}}>
                                    <span style={{fontWeight:'700', color:'var(--text-primary)'}}>{item.label}</span>
                                    <span style={{
                                        padding:'4px 14px', borderRadius:'20px', fontWeight:'700', fontSize:'0.82rem',
                                        background: item.value ? '#dcfce7' : '#fef2f2',
                                        color: item.value ? '#16a34a' : '#dc2626',
                                        border:`1px solid ${item.value ? '#bbf7d0' : '#fecaca'}`
                                    }}>
                                        {item.value ? 'Primit' : 'Lipsa'}
                                    </span>
                                </div>
                            ))}
                        </div>
                    )}

                    {!isAdmin && (
                        <div className="card" style={{border:'1px solid #fecaca'}}>
                            <p className="db-section-title" style={{color:'#dc2626'}}>Zona Periculoasa</p>
                            <p style={{fontSize:'0.88rem', color:'var(--text-secondary)', marginBottom:'16px', lineHeight:'1.55'}}>
                                Stergerea contului este ireversibila. Toate datele vor fi pierdute permanent.
                            </p>
                            {!showDeleteInput ? (
                                <div style={{display:'flex', flexDirection:'column', gap:'10px'}}>
                                    <button onClick={requestDeletionCode} style={{padding:'11px', background:'#fff7ed', color:'#ea580c', border:'1.5px solid #fed7aa', borderRadius:'12px', fontWeight:'700', fontSize:'0.9rem'}}>Solicita Cod de Stergere</button>
                                    <button onClick={()=>setShowDeleteInput(true)} style={{padding:'11px', background:'white', color:'#dc2626', border:'1.5px solid #fecaca', borderRadius:'12px', fontWeight:'700', fontSize:'0.9rem'}}>Am primit codul</button>
                                </div>
                            ) : (
                                <div style={{display:'flex', flexDirection:'column', gap:'10px'}}>
                                    <input className="login-input" style={{marginBottom:0}} placeholder="Introdu codul primit" value={deleteCode} onChange={e=>setDeleteCode(e.target.value)}/>
                                    <button onClick={performDeletion} style={{padding:'12px', background:'#dc2626', color:'white', border:'none', borderRadius:'12px', fontWeight:'800', fontSize:'0.9rem'}}>Sterge Contul</button>
                                </div>
                            )}
                        </div>
                    )}
                </div>

            </div>

            {/* DOSAR FEEDBACK — doar lideri */}
            {!isChild && (
                <div className="card" style={{marginTop:'20px'}}>
                    <div
                        onClick={() => setShowFeedback(!showFeedback)}
                        style={{display:'flex', justifyContent:'space-between', alignItems:'center', cursor:'pointer', userSelect:'none'}}
                    >
                        <div style={{display:'flex', alignItems:'center', gap:'10px', flexWrap:'wrap'}}>
                            <p className="db-section-title" style={{margin:0}}>Dosar Feedback</p>
                            {myFeedbacks.length > 0 && (
                                <span style={{background:'#fef3c7', color:'#d97706', border:'1px solid #fde68a', padding:'2px 9px', borderRadius:'20px', fontSize:'0.75rem', fontWeight:'800'}}>
                                    {myFeedbacks.length} evaluari
                                </span>
                            )}
                            {myFeedbacks.length > 0 && (() => {
                                const avg = (myFeedbacks.reduce((s,f) => s + (f.rating||0), 0) / myFeedbacks.length).toFixed(1);
                                return <span style={{color:'#f59e0b', fontWeight:'800', fontSize:'0.88rem'}}>★ {avg}</span>;
                            })()}
                        </div>
                        <div style={{
                            width:'30px', height:'30px', borderRadius:'50%', background:'var(--bg-primary)',
                            display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0,
                            transition:'transform 0.2s', transform: showFeedback ? 'rotate(180deg)' : 'rotate(0deg)'
                        }}>
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--text-secondary)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                                <polyline points="6 9 12 15 18 9"/>
                            </svg>
                        </div>
                    </div>

                    {showFeedback && (
                        <div className="animate-in" style={{marginTop:'16px', borderTop:'1px solid var(--border-color)', paddingTop:'16px'}}>
                            {myFeedbacks.length === 0 ? (
                                <div style={{textAlign:'center', padding:'24px 20px'}}>
                                    <div style={{width:'40px', height:'40px', borderRadius:'50%', background:'#f1f5f9', display:'flex', alignItems:'center', justifyContent:'center', margin:'0 auto 10px'}}>
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M9 12l2 2 4-4M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z" stroke="#22c55e" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
                                    </div>
                                    <p style={{color:'var(--text-secondary)', fontSize:'0.88rem', margin:0, fontWeight:'600'}}>Nicio evaluare inca.</p>
                                </div>
                            ) : (
                                <div style={{display:'flex', flexDirection:'column', gap:'8px', maxHeight:'360px', overflowY:'auto'}}>
                                    {myFeedbacks.map(f => {
                                        const label = f.rating >= 4 ? 'Excelent' : f.rating >= 3 ? 'Bun' : f.rating >= 2 ? 'Acceptabil' : 'Necesita imbunatatiri';
                                        return (
                                            <div key={f.id} style={{padding:'12px 14px', borderRadius:'10px', background:'var(--bg-primary)', boxShadow:'inset 4px 0 0 #f59e0b'}}>
                                                <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom: f.comment ? '7px' : 0}}>
                                                    <div style={{display:'flex', gap:'2px'}}>
                                                        {[1,2,3,4,5].map(s => (
                                                            <svg key={s} width="14" height="14" viewBox="0 0 24 24"
                                                                fill={s <= (f.rating||0) ? '#f59e0b' : 'none'}
                                                                stroke={s <= (f.rating||0) ? '#f59e0b' : '#d1d5db'}
                                                                strokeWidth="1.5">
                                                                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                                                            </svg>
                                                        ))}
                                                    </div>
                                                    <div style={{display:'flex', alignItems:'center', gap:'8px'}}>
                                                        <span style={{fontSize:'0.7rem', fontWeight:'800', color:'#d97706', background:'#fef3c7', padding:'1px 7px', borderRadius:'4px'}}>{label}</span>
                                                        <span style={{fontSize:'0.75rem', color:'var(--text-secondary)', fontWeight:'600'}}>{f.date}</span>
                                                    </div>
                                                </div>
                                                {f.comment && (
                                                    <p style={{margin:0, fontSize:'0.87rem', color:'var(--text-primary)', fontStyle:'italic', borderLeft:'2px solid #f59e0b', paddingLeft:'9px'}}>"{f.comment}"</p>
                                                )}
                                            </div>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    )}
                </div>
            )}

        </div>
    );
};

export default MyProfile;
