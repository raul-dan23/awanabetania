import React, { useState, useEffect } from 'react';

const API_URL = 'http://awana.betania-tm.ro/api';

const AdminDashboard = ({ currentUser }) => {
    const [isUnlocked, setIsUnlocked] = useState(false);
    const [pin, setPin] = useState('');
    const [data, setData] = useState({ leaders: [], children: [] });
    const [viewMode, setViewMode] = useState('LEADERS');
    const [searchTerm, setSearchTerm] = useState('');
    const [sortBy, setSortBy] = useState('name');
    const [visiblePasswords, setVisiblePasswords] = useState({});

    const MASTER_PIN = "0000";

    useEffect(() => {
        if (isUnlocked) fetchData();
    }, [isUnlocked]);

    const fetchData = () => {
        fetch(`${API_URL}/admin/all-users`)
            .then(res => res.json())
            .then(d => setData(d))
            .catch(() => alert("Eroare la incarcarea datelor!"));
    };

    const handleUnlock = () => {
        if (pin === MASTER_PIN) {
            setIsUnlocked(true);
        } else {
            alert("PIN Incorect!");
            setPin('');
        }
    };

    const revealPassword = (id, encryptedPass) => {
        if (visiblePasswords[id]) {
            const nv = { ...visiblePasswords };
            delete nv[id];
            setVisiblePasswords(nv);
            return;
        }
        fetch(`${API_URL}/admin/decrypt-password`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ password: encryptedPass })
        })
            .then(res => res.json())
            .then(resp => setVisiblePasswords(prev => ({ ...prev, [id]: resp.realPassword })))
            .catch(() => alert("Eroare decriptare."));
    };

    const getList = () => {
        const list = viewMode === 'LEADERS' ? data.leaders : data.children;
        let filtered = searchTerm
            ? list.filter(u =>
                u.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                u.surname.toLowerCase().includes(searchTerm.toLowerCase())
              )
            : list;
        if (viewMode === 'LEADERS') {
            if (sortBy === 'rating') filtered = [...filtered].sort((a,b) => (b.rating||0) - (a.rating||0));
            else filtered = [...filtered].sort((a,b) => a.name.localeCompare(b.name));
        } else {
            if (sortBy === 'points')     filtered = [...filtered].sort((a,b) => (b.seasonPoints||0) - (a.seasonPoints||0));
            else if (sortBy === 'attendance') filtered = [...filtered].sort((a,b) => (b.totalAttendance||0) - (a.totalAttendance||0));
            else filtered = [...filtered].sort((a,b) => a.name.localeCompare(b.name));
        }
        return filtered;
    };

    /* ── PIN SCREEN ───────────────────────────────────────── */
    if (!isUnlocked) {
        return (
            <div style={{height:'80vh', display:'flex', alignItems:'center', justifyContent:'center', padding:'20px'}}>
                <div style={{background:'white', borderRadius:'24px', padding:'40px 36px', boxShadow:'0 20px 60px rgba(0,0,0,0.12)', width:'100%', maxWidth:'320px', textAlign:'center'}}>
                    <div style={{
                        width:'64px', height:'64px', borderRadius:'18px',
                        background:'linear-gradient(135deg, #4318ff, #868cff)',
                        display:'flex', alignItems:'center', justifyContent:'center',
                        margin:'0 auto 20px'
                    }}>
                        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <rect x="3" y="11" width="18" height="11" rx="3" fill="white" opacity="0.9"/>
                            <path d="M7 11V7a5 5 0 0 1 10 0v4" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"/>
                        </svg>
                    </div>
                    <h2 style={{margin:'0 0 6px', color:'#1e293b', fontWeight:'900', fontSize:'1.4rem'}}>Control Center</h2>
                    <p style={{color:'#64748b', margin:'0 0 24px', fontSize:'0.88rem'}}>Zona restrictionata. Introduce PIN-ul master.</p>
                    <input
                        type="password" inputMode="numeric" pattern="[0-9]*"
                        value={pin}
                        onChange={e => setPin(e.target.value)}
                        onKeyDown={e => e.key === 'Enter' && handleUnlock()}
                        placeholder="••••"
                        autoFocus
                        style={{
                            width:'100%', padding:'14px', fontSize:'1.6rem', textAlign:'center',
                            borderRadius:'12px', border:'2px solid #e2e8f0', marginBottom:'14px',
                            outline:'none', fontFamily:'monospace', boxSizing:'border-box',
                            letterSpacing:'0.3em', color:'#1e293b'
                        }}
                    />
                    <button onClick={handleUnlock} style={{
                        width:'100%', padding:'14px',
                        background:'linear-gradient(135deg, #4318ff, #868cff)',
                        color:'white', fontWeight:'800', border:'none', borderRadius:'12px',
                        fontSize:'1rem', cursor:'pointer'
                    }}>
                        Deschide
                    </button>
                </div>
            </div>
        );
    }

    /* ── MAIN VIEW ────────────────────────────────────────── */
    const displayList = getList();
    const sortOpts = viewMode === 'LEADERS'
        ? [{key:'name',label:'Nume A–Z'},{key:'rating',label:'Rating'}]
        : [{key:'name',label:'Nume A–Z'},{key:'points',label:'Puncte'},{key:'attendance',label:'Prezente'}];

    return (
        <div className="animate-in" style={{maxWidth:'800px', margin:'0 auto', padding:'10px', paddingBottom:'100px'}}>

            {/* Hero */}
            <div className="db-hero" style={{marginBottom:'24px', alignItems:'center'}}>
                <div className="db-hero-left">
                    <span className="db-greeting">Sistem</span>
                    <h1 className="db-name">Control Center</h1>
                    <span className="db-role-pill">{data.leaders.length} lideri · {data.children.length} copii</span>
                </div>
                <button onClick={() => setIsUnlocked(false)} style={{
                    background:'rgba(255,255,255,0.14)', border:'1.5px solid rgba(255,255,255,0.28)',
                    color:'white', padding:'9px 18px', borderRadius:'12px', fontWeight:'700',
                    fontSize:'0.88rem', flexShrink:0, cursor:'pointer'
                }}>Iesi</button>
            </div>

            {/* Tabs Lideri / Copii */}
            <div style={{display:'flex', gap:'8px', marginBottom:'14px', background:'#f4f7fe', padding:'4px', borderRadius:'14px'}}>
                <button onClick={() => { setViewMode('LEADERS'); setSortBy('name'); }} style={{
                    flex:1, padding:'10px', border:'none', cursor:'pointer', borderRadius:'10px',
                    fontWeight:'800', fontSize:'0.9rem', transition:'all 0.18s',
                    background: viewMode==='LEADERS' ? 'white' : 'transparent',
                    color: viewMode==='LEADERS' ? '#0284c7' : '#64748b',
                    boxShadow: viewMode==='LEADERS' ? '0 2px 8px rgba(0,0,0,0.08)' : 'none'
                }}>Lideri ({data.leaders.length})</button>
                <button onClick={() => { setViewMode('CHILDREN'); setSortBy('name'); }} style={{
                    flex:1, padding:'10px', border:'none', cursor:'pointer', borderRadius:'10px',
                    fontWeight:'800', fontSize:'0.9rem', transition:'all 0.18s',
                    background: viewMode==='CHILDREN' ? 'white' : 'transparent',
                    color: viewMode==='CHILDREN' ? '#16a34a' : '#64748b',
                    boxShadow: viewMode==='CHILDREN' ? '0 2px 8px rgba(0,0,0,0.08)' : 'none'
                }}>Copii ({data.children.length})</button>
            </div>

            {/* Search */}
            <input
                className="login-input"
                placeholder={`Cauta ${viewMode === 'LEADERS' ? 'lider' : 'copil'} dupa nume...`}
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
                style={{marginBottom:'10px'}}
            />

            {/* Sort pills */}
            <div style={{display:'flex', gap:'6px', marginBottom:'20px', background:'#f4f7fe', padding:'4px', borderRadius:'14px'}}>
                {sortOpts.map(opt => (
                    <button key={opt.key} onClick={() => setSortBy(opt.key)} style={{
                        flex:1, padding:'7px 6px', border:'none', cursor:'pointer', borderRadius:'10px',
                        fontWeight:'700', fontSize:'0.78rem', transition:'all 0.18s',
                        background: sortBy === opt.key ? 'white' : 'transparent',
                        color: sortBy === opt.key ? '#4318ff' : '#64748b',
                        boxShadow: sortBy === opt.key ? '0 2px 8px rgba(0,0,0,0.08)' : 'none'
                    }}>{opt.label}</button>
                ))}
            </div>

            {/* Cards */}
            <div style={{display:'flex', flexDirection:'column', gap:'14px'}}>
                {displayList.map(user => {
                    const initials = `${user.name?.charAt(0)||''}${user.surname?.charAt(0)||''}`;
                    const isLeaderView = viewMode === 'LEADERS';
                    const accentLeft = isLeaderView
                        ? (user.role==='DIRECTOR' ? '#dc2626' : user.role==='COORDONATOR' ? '#3b82f6' : '#4318ff')
                        : '#16a34a';

                    return (
                        <div key={user.id} style={{
                            background:'white', borderRadius:'16px', overflow:'hidden',
                            boxShadow:'0 2px 8px rgba(0,0,0,0.06)',
                            border:'1px solid #e2e8f0',
                            borderLeft:`4px solid ${accentLeft}`
                        }}>
                            {/* Header card */}
                            <div style={{padding:'16px 20px', borderBottom:'1px solid #f1f5f9', display:'flex', alignItems:'center', gap:'12px'}}>
                                <div style={{
                                    width:'44px', height:'44px', borderRadius:'12px', flexShrink:0,
                                    background: isLeaderView
                                        ? `linear-gradient(135deg, ${accentLeft}, #93c5fd)`
                                        : 'linear-gradient(135deg, #16a34a, #4ade80)',
                                    display:'flex', alignItems:'center', justifyContent:'center',
                                    color:'white', fontWeight:'900', fontSize:'1rem'
                                }}>{initials}</div>
                                <div style={{flex:1, minWidth:0}}>
                                    <div style={{fontWeight:'900', color:'#1e293b', fontSize:'1.05rem'}}>{user.name} {user.surname}</div>
                                    <div style={{display:'flex', gap:'5px', marginTop:'3px', flexWrap:'wrap', alignItems:'center'}}>
                                        <span style={{fontSize:'0.7rem', color:'#64748b', fontWeight:'700', background:'#f1f5f9', padding:'1px 7px', borderRadius:'20px'}}>ID #{user.id}</span>
                                        {isLeaderView && user.role && (
                                            <span style={{
                                                fontSize:'0.7rem', fontWeight:'800', padding:'1px 8px', borderRadius:'20px',
                                                background: user.role==='DIRECTOR'?'#fee2e2':user.role==='COORDONATOR'?'#dbeafe':'#ede9fe',
                                                color: user.role==='DIRECTOR'?'#991b1b':user.role==='COORDONATOR'?'#1e40af':'#6d28d9'
                                            }}>{user.role}</span>
                                        )}
                                        {!isLeaderView && user.age && (
                                            <span style={{fontSize:'0.7rem', color:'#64748b', fontWeight:'700', background:'#f1f5f9', padding:'1px 7px', borderRadius:'20px'}}>{user.age} ani</span>
                                        )}
                                    </div>
                                </div>
                            </div>

                            {/* Body card */}
                            <div style={{padding:'14px 20px', display:'flex', flexDirection:'column', gap:'7px'}}>

                                {/* Username */}
                                {user.username && (
                                    <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', padding:'7px 11px', background:'#f8fafc', borderRadius:'8px'}}>
                                        <span style={{fontSize:'0.78rem', color:'#64748b', fontWeight:'600'}}>Username</span>
                                        <span style={{fontFamily:'monospace', fontWeight:'800', color:'#4318ff', fontSize:'0.88rem'}}>{user.username}</span>
                                    </div>
                                )}

                                {/* LIDER: telefon + rating */}
                                {isLeaderView && (
                                    <>
                                        {user.phoneNumber && (
                                            <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', padding:'7px 11px', background:'#f8fafc', borderRadius:'8px'}}>
                                                <span style={{fontSize:'0.78rem', color:'#64748b', fontWeight:'600'}}>Telefon</span>
                                                <a href={`tel:${user.phoneNumber}`} style={{fontWeight:'800', color:'#0284c7', textDecoration:'none', fontSize:'0.88rem'}}>{user.phoneNumber}</a>
                                            </div>
                                        )}
                                        {user.rating !== undefined && user.rating !== null && (
                                            <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', padding:'7px 11px', background:'#f8fafc', borderRadius:'8px'}}>
                                                <span style={{fontSize:'0.78rem', color:'#64748b', fontWeight:'600'}}>Rating</span>
                                                <span style={{fontWeight:'800', color:'#f59e0b', fontSize:'0.88rem'}}>★ {user.rating.toFixed(1)}</span>
                                            </div>
                                        )}
                                        {user.departments && user.departments.length > 0 && (
                                            <div style={{padding:'7px 11px', background:'#f8fafc', borderRadius:'8px'}}>
                                                <span style={{fontSize:'0.78rem', color:'#64748b', fontWeight:'600', display:'block', marginBottom:'6px'}}>Departamente</span>
                                                <div style={{display:'flex', flexWrap:'wrap', gap:'5px'}}>
                                                    {user.departments.map(d => (
                                                        <span key={d.id} style={{background:'#dcfce7', color:'#15803d', border:'1px solid #bbf7d0', padding:'2px 9px', borderRadius:'20px', fontSize:'0.75rem', fontWeight:'700'}}>{d.name}</span>
                                                    ))}
                                                </div>
                                            </div>
                                        )}
                                    </>
                                )}

                                {/* COPIL: parinte + stats + premii */}
                                {!isLeaderView && (
                                    <>
                                        <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', padding:'7px 11px', background:'#f8fafc', borderRadius:'8px'}}>
                                            <span style={{fontSize:'0.78rem', color:'#64748b', fontWeight:'600'}}>Parinte</span>
                                            <span style={{fontWeight:'800', color:'#1e293b', fontSize:'0.88rem'}}>{user.parentName || '—'}</span>
                                        </div>
                                        {user.parentPhone && (
                                            <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', padding:'7px 11px', background:'#f8fafc', borderRadius:'8px'}}>
                                                <span style={{fontSize:'0.78rem', color:'#64748b', fontWeight:'600'}}>Telefon</span>
                                                <a href={`tel:${user.parentPhone}`} style={{fontWeight:'800', color:'#16a34a', textDecoration:'none', fontSize:'0.88rem'}}>{user.parentPhone}</a>
                                            </div>
                                        )}
                                        {(user.totalAttendance !== undefined || user.seasonPoints !== undefined) && (
                                            <div style={{display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:'6px', marginTop:'2px'}}>
                                                {[
                                                    {label:'Prezente', value: user.totalAttendance||0, color:'#10b981'},
                                                    {label:'Streak',   value: user.attendanceStreak||0, color:'#4318ff'},
                                                    {label:'Puncte',   value: user.seasonPoints||0,    color:'#f59e0b'},
                                                ].map(s => (
                                                    <div key={s.label} style={{padding:'9px 6px', background:'#f8fafc', borderRadius:'8px', textAlign:'center'}}>
                                                        <div style={{fontWeight:'900', color:s.color, fontSize:'1.1rem', lineHeight:1}}>{s.value}</div>
                                                        <div style={{fontSize:'0.62rem', color:'#94a3b8', fontWeight:'700', textTransform:'uppercase', marginTop:'3px'}}>{s.label}</div>
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                        {(user.hasShirt !== undefined || user.hasHat !== undefined) && (
                                            <div style={{display:'flex', gap:'6px', flexWrap:'wrap', marginTop:'2px'}}>
                                                <span style={{padding:'3px 10px', borderRadius:'20px', fontSize:'0.73rem', fontWeight:'800', background: user.hasShirt?'#dcfce7':'#f1f5f9', color: user.hasShirt?'#15803d':'#94a3b8', border:`1px solid ${user.hasShirt?'#bbf7d0':'#e2e8f0'}`}}>Tricou {user.hasShirt ? 'DAT' : '—'}</span>
                                                <span style={{padding:'3px 10px', borderRadius:'20px', fontSize:'0.73rem', fontWeight:'800', background: user.hasHat?'#dbeafe':'#f1f5f9', color: user.hasHat?'#1d4ed8':'#94a3b8', border:`1px solid ${user.hasHat?'#93c5fd':'#e2e8f0'}`}}>Caciula {user.hasHat ? 'DATA' : '—'}</span>
                                            </div>
                                        )}
                                    </>
                                )}

                                {/* Parola */}
                                <div style={{marginTop:'4px', background:'#f8fafc', padding:'10px 14px', borderRadius:'10px', display:'flex', justifyContent:'space-between', alignItems:'center', gap:'10px'}}>
                                    <span style={{fontFamily:'monospace', fontSize:'1rem', fontWeight:'800', color: visiblePasswords[user.id] ? '#dc2626' : '#cbd5e1', letterSpacing: visiblePasswords[user.id] ? 'normal' : '0.15em'}}>
                                        {visiblePasswords[user.id] ? visiblePasswords[user.id] : '••••••••'}
                                    </span>
                                    <button
                                        onClick={() => revealPassword(user.id, user.password)}
                                        style={{
                                            background: visiblePasswords[user.id] ? '#fee2e2' : 'white',
                                            color: visiblePasswords[user.id] ? '#dc2626' : '#334155',
                                            border: '1px solid #e2e8f0',
                                            padding:'7px 13px', borderRadius:'8px', cursor:'pointer',
                                            fontWeight:'800', fontSize:'0.8rem', flexShrink:0
                                        }}
                                    >
                                        {visiblePasswords[user.id] ? 'Ascunde' : 'Vezi Parola'}
                                    </button>
                                </div>
                            </div>
                        </div>
                    );
                })}
                {displayList.length === 0 && (
                    <div style={{textAlign:'center', color:'#94a3b8', padding:'40px', fontStyle:'italic'}}>Nu am gasit niciun rezultat.</div>
                )}
            </div>
        </div>
    );
};

export default AdminDashboard;
