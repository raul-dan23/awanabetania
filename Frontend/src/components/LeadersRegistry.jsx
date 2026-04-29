import React, { useState, useEffect } from 'react';
import { API_URL } from '../config';

const LeadersRegistry = () => {
    const [leaders, setLeaders] = useState([]);
    const [selectedLeader, setSelectedLeader] = useState(null);
    const [feedbacks, setFeedbacks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState('');
    const [sortBy, setSortBy] = useState('name');

    useEffect(() => {
        fetch(`${API_URL}/leaders`).then(r => r.ok ? r.json() : []).then(data => { setLeaders(data); setLoading(false); }).catch(() => setLoading(false));
    }, []);

    const handleSelectLeader = (l) => {
        setSelectedLeader(l);
        fetch(`${API_URL}/feedback/leader/${l.id}`).then(r => r.ok ? r.json() : []).then(setFeedbacks).catch(() => setFeedbacks([]));
    };

    const handleDeleteFeedback = (id) => {
        if(!window.confirm("Stergi comentariul?")) return;
        fetch(`${API_URL}/feedback/delete/${id}`, { method: 'DELETE' }).then(res => {
            if(res.ok) { setFeedbacks(feedbacks.map(f => f.id === id ? { ...f, comment: null } : f)); alert("Sters!"); }
        });
    };

    const StarRating = ({ rating, size = 16 }) => (
        <div style={{display:'flex', gap:'2px'}}>
            {[1,2,3,4,5].map(s => (
                <svg key={s} width={size} height={size} viewBox="0 0 24 24"
                    fill={s <= Math.round(rating || 0) ? '#f59e0b' : 'none'}
                    stroke={s <= Math.round(rating || 0) ? '#f59e0b' : '#d1d5db'}
                    strokeWidth="1.5" xmlns="http://www.w3.org/2000/svg">
                    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                </svg>
            ))}
        </div>
    );

    const roleStyle = (role) => {
        if (role === 'DIRECTOR')   return { bg:'#fee2e2', color:'#991b1b', border:'#fca5a5', left:'#dc2626' };
        if (role === 'COORDONATOR') return { bg:'#dbeafe', color:'#1e40af', border:'#93c5fd', left:'#3b82f6' };
        return { bg:'#ede9fe', color:'#6d28d9', border:'#ddd6fe', left:'var(--accent)' };
    };

    if (loading) return (
        <div className="animate-in db-loading">
            <div className="db-loading-pulse" style={{height:'140px'}} />
            <div style={{display:'grid', gridTemplateColumns:'1fr 1fr 1fr', gap:'15px'}}>
                {[1,2,3].map(i => <div key={i} className="db-loading-pulse" style={{height:'100px'}} />)}
            </div>
        </div>
    );

    /* ── DOSAR LIDER ─────────────────────────────────────── */
    if (selectedLeader) {
        const rs = roleStyle(selectedLeader.role);
        const initials = `${selectedLeader.name?.charAt(0) || ''}${selectedLeader.surname?.charAt(0) || ''}`;
        const avgRating = selectedLeader.rating ? selectedLeader.rating.toFixed(1) : '0.0';

        return (
            <div className="animate-in">

                <div className="db-hero" style={{marginBottom:'24px', alignItems:'center'}}>
                    <div className="db-hero-left">
                        <span className="db-greeting">Dosar Lider</span>
                        <h1 className="db-name">{selectedLeader.name} {selectedLeader.surname}</h1>
                        <span className="db-role-pill">{selectedLeader.role || 'LIDER'}</span>
                    </div>
                    <button onClick={() => setSelectedLeader(null)} style={{
                        background:'rgba(255,255,255,0.14)', border:'1.5px solid rgba(255,255,255,0.28)',
                        color:'white', padding:'9px 18px', borderRadius:'12px', fontWeight:'700',
                        fontSize:'0.88rem', flexShrink:0, cursor:'pointer'
                    }}>Inapoi</button>
                </div>

                {/* 3 Stat cards */}
                <div style={{display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:'15px', marginBottom:'24px'}}>
                    <div className="db-stat-card db-stat-amber">
                        <div className="db-stat-number">{avgRating}</div>
                        <div className="db-stat-label">Rating Mediu</div>
                    </div>
                    <div className="db-stat-card db-stat-blue">
                        <div className="db-stat-number">{feedbacks.length}</div>
                        <div className="db-stat-label">Evaluari</div>
                    </div>
                    <div className="db-stat-card db-stat-purple">
                        <div className="db-stat-number">{(selectedLeader.departments||[]).length}</div>
                        <div className="db-stat-label">Departamente</div>
                    </div>
                </div>

                <div className="profile-grid">

                    {/* Coloana stanga */}
                    <div style={{display:'flex', flexDirection:'column', gap:'16px'}}>

                        {/* Contact */}
                        <div className="card">
                            <p className="db-section-title" style={{marginBottom:'14px'}}>Date Contact</p>
                            <div style={{display:'flex', alignItems:'center', gap:'12px', marginBottom:'14px'}}>
                                <div style={{
                                    width:'44px', height:'44px', borderRadius:'12px', flexShrink:0,
                                    background:'linear-gradient(135deg,var(--accent),#868cff)',
                                    display:'flex', alignItems:'center', justifyContent:'center',
                                    color:'white', fontWeight:'900', fontSize:'1rem'
                                }}>{initials}</div>
                                <div>
                                    <div style={{fontWeight:'800', color:'var(--text-primary)'}}>{selectedLeader.name} {selectedLeader.surname}</div>
                                    <span style={{display:'inline-block', marginTop:'3px', background:rs.bg, color:rs.color, border:`1px solid ${rs.border}`, padding:'2px 10px', borderRadius:'20px', fontSize:'0.72rem', fontWeight:'800'}}>{selectedLeader.role || 'LIDER'}</span>
                                </div>
                            </div>
                            <div style={{padding:'9px 12px', background:'var(--bg-primary)', borderRadius:'8px', display:'flex', justifyContent:'space-between', alignItems:'center'}}>
                                <span style={{color:'var(--text-secondary)', fontWeight:'600', fontSize:'0.88rem'}}>Telefon</span>
                                <a href={`tel:${selectedLeader.phoneNumber}`} style={{fontWeight:'800', color:'var(--accent)', textDecoration:'none', fontSize:'0.9rem'}}>{selectedLeader.phoneNumber || 'Nespecificat'}</a>
                            </div>
                        </div>

                        {/* Departamente */}
                        <div className="card">
                            <p className="db-section-title" style={{marginBottom:'12px'}}>Departamente</p>
                            {(selectedLeader.departments||[]).length > 0 ? (
                                <div style={{display:'flex', flexWrap:'wrap', gap:'8px'}}>
                                    {(selectedLeader.departments||[]).map(d => (
                                        <span key={d.id} style={{background:'#dcfce7', color:'#15803d', border:'1px solid #bbf7d0', padding:'5px 12px', borderRadius:'20px', fontSize:'0.83rem', fontWeight:'700'}}>{d.name}</span>
                                    ))}
                                </div>
                            ) : (
                                <p style={{color:'var(--text-secondary)', fontSize:'0.88rem', fontStyle:'italic', margin:0}}>Nu este inscris in niciun departament.</p>
                            )}
                        </div>

                        {/* Rating vizual */}
                        <div className="card">
                            <p className="db-section-title" style={{marginBottom:'14px'}}>Rating General</p>
                            <div style={{display:'flex', alignItems:'center', gap:'16px'}}>
                                <div style={{fontSize:'2.8rem', fontWeight:'900', color:'#f59e0b', lineHeight:1}}>{avgRating}</div>
                                <div>
                                    <StarRating rating={selectedLeader.rating} size={20} />
                                    <div style={{fontSize:'0.78rem', color:'var(--text-secondary)', marginTop:'5px', fontWeight:'600'}}>{feedbacks.length} evaluari totale</div>
                                </div>
                            </div>
                            {feedbacks.length > 0 && (
                                <div style={{marginTop:'14px', background:'var(--bg-primary)', borderRadius:'8px', height:'8px', overflow:'hidden'}}>
                                    <div style={{width:`${((selectedLeader.rating||0)/5)*100}%`, height:'100%', background:'linear-gradient(90deg,#f59e0b,#d97706)', borderRadius:'999px', transition:'width 0.6s ease'}}/>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Coloana dreapta — Feedback */}
                    <div className="card">
                        <p className="db-section-title" style={{marginBottom:'16px'}}>Istoric Feedback</p>
                        <div style={{display:'flex', flexDirection:'column', gap:'10px', maxHeight:'520px', overflowY:'auto'}}>
                            {feedbacks.length === 0 ? (
                                <div style={{textAlign:'center', padding:'30px 20px'}}>
                                    <div style={{width:'44px', height:'44px', borderRadius:'50%', background:'#f1f5f9', display:'flex', alignItems:'center', justifyContent:'center', margin:'0 auto 10px'}}>
                                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M9 12l2 2 4-4M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z" stroke="#22c55e" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
                                    </div>
                                    <p style={{color:'var(--text-secondary)', fontSize:'0.88rem', margin:0, fontWeight:'600'}}>Nicio evaluare inregistrata.</p>
                                </div>
                            ) : feedbacks.map(f => (
                                <div key={f.id} style={{padding:'14px', borderRadius:'12px', background:'var(--bg-primary)', boxShadow:'inset 4px 0 0 #f59e0b'}}>
                                    <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'6px'}}>
                                        <StarRating rating={f.rating} size={15} />
                                        <div style={{display:'flex', alignItems:'center', gap:'8px'}}>
                                            <span style={{fontSize:'0.75rem', color:'var(--text-secondary)', fontWeight:'600'}}>{f.date}</span>
                                            {f.comment && (
                                                <button onClick={() => handleDeleteFeedback(f.id)} title="Sterge comentariu" style={{background:'none', border:'none', cursor:'pointer', padding:'2px', color:'#dc2626', opacity:0.55, display:'flex', alignItems:'center'}}>
                                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M10 11v6M14 11v6"/><path d="M9 6V4h6v2"/></svg>
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                    <div style={{fontSize:'0.75rem', fontWeight:'700', color:'#d97706', marginBottom: f.comment ? '7px' : 0}}>
                                        {f.rating}/5 — {f.rating >= 4 ? 'Excelent' : f.rating >= 3 ? 'Bun' : f.rating >= 2 ? 'Acceptabil' : 'Necesita imbunatatiri'}
                                    </div>
                                    {f.comment && <p style={{margin:0, fontSize:'0.87rem', color:'var(--text-primary)', fontStyle:'italic', borderLeft:'2px solid #f59e0b', paddingLeft:'9px'}}>"{f.comment}"</p>}
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    /* ── LISTA LIDERI ─────────────────────────────────────── */
    let filtered = leaders.filter(l =>
        search === '' || `${l.name} ${l.surname}`.toLowerCase().includes(search.toLowerCase())
    );
    if (sortBy === 'rating')   filtered = [...filtered].sort((a,b) => (b.rating||0) - (a.rating||0));
    else if (sortBy === 'role') filtered = [...filtered].sort((a,b) => (a.role||'').localeCompare(b.role||''));
    else                        filtered = [...filtered].sort((a,b) => a.name.localeCompare(b.name));

    return (
        <div className="animate-in">

            <div className="db-hero" style={{marginBottom:'24px', alignItems:'center'}}>
                <div className="db-hero-left">
                    <span className="db-greeting">Echipa Clubului</span>
                    <h1 className="db-name">Registru Lideri</h1>
                    <span className="db-role-pill">{leaders.length} lideri activi</span>
                </div>
            </div>

            <input
                className="login-input"
                placeholder="Cauta lider dupa nume..."
                value={search}
                onChange={e => setSearch(e.target.value)}
                style={{marginBottom:'12px'}}
            />

            <div style={{display:'flex', gap:'6px', marginBottom:'20px', background:'var(--bg-primary)', padding:'4px', borderRadius:'14px'}}>
                {[{key:'name',label:'Nume A–Z'},{key:'rating',label:'Rating'},{key:'role',label:'Rol'}].map(opt => (
                    <button key={opt.key} onClick={() => setSortBy(opt.key)} style={{
                        flex:1, padding:'8px 6px', border:'none', cursor:'pointer', borderRadius:'10px',
                        fontWeight:'700', fontSize:'0.82rem', transition:'all 0.18s',
                        background: sortBy === opt.key ? 'white' : 'transparent',
                        color: sortBy === opt.key ? 'var(--accent)' : 'var(--text-secondary)',
                        boxShadow: sortBy === opt.key ? '0 2px 8px rgba(0,0,0,0.08)' : 'none'
                    }}>{opt.label}</button>
                ))}
            </div>

            <div style={{display:'flex', flexDirection:'column', gap:'10px'}}>
                {filtered.map(l => {
                    const initials = `${l.name?.charAt(0)||''}${l.surname?.charAt(0)||''}`;
                    const rs = roleStyle(l.role);
                    return (
                        <div key={l.id} style={{
                            background:'white', padding:'14px 18px', borderRadius:'14px',
                            border:'1px solid var(--border-color)', borderLeft:`4px solid ${rs.left}`,
                            boxShadow:'0 2px 6px rgba(0,0,0,0.04)',
                            display:'flex', alignItems:'center', gap:'14px'
                        }}>
                            <div style={{
                                width:'42px', height:'42px', borderRadius:'12px', flexShrink:0,
                                background:'linear-gradient(135deg,var(--accent),#868cff)',
                                display:'flex', alignItems:'center', justifyContent:'center',
                                color:'white', fontWeight:'900', fontSize:'0.95rem'
                            }}>{initials}</div>

                            <div style={{flex:1, minWidth:0}}>
                                <div style={{display:'flex', alignItems:'center', gap:'7px', flexWrap:'wrap'}}>
                                    <span style={{fontWeight:'800', color:'var(--text-primary)'}}>{l.name} {l.surname}</span>
                                    <span style={{background:rs.bg, color:rs.color, border:`1px solid ${rs.border}`, padding:'1px 8px', borderRadius:'20px', fontSize:'0.7rem', fontWeight:'800'}}>{l.role||'LIDER'}</span>
                                </div>
                                <div style={{display:'flex', gap:'12px', marginTop:'3px'}}>
                                    <span style={{fontSize:'0.76rem', color:'var(--text-secondary)', fontWeight:'600'}}>{l.phoneNumber||'—'}</span>
                                    <span style={{fontSize:'0.76rem', color:'#f59e0b', fontWeight:'800'}}>★ {l.rating ? l.rating.toFixed(1) : '0.0'}</span>
                                </div>
                            </div>

                            <button onClick={() => handleSelectLeader(l)} style={{
                                background:'var(--accent)', color:'white', border:'none',
                                padding:'8px 16px', borderRadius:'10px', fontWeight:'800',
                                cursor:'pointer', fontSize:'0.83rem', flexShrink:0
                            }}>Dosar</button>
                        </div>
                    );
                })}
                {filtered.length === 0 && (
                    <div style={{textAlign:'center', color:'var(--text-secondary)', padding:'40px', fontStyle:'italic'}}>Nu am gasit niciun lider.</div>
                )}
            </div>
        </div>
    );
};

export default LeadersRegistry;
