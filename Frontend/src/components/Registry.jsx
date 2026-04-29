import React, { useState, useEffect } from 'react';
import { API_URL } from '../config';

const Registry = ({ user }) => {
    const [children, setChildren] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedChild, setSelectedChild] = useState(null);
    const [childWarnings, setChildWarnings] = useState([]);
    const [newWarning, setNewWarning] = useState({ description: '', suspension: false, remainingMeetings: 1 });
    const [availableManuals, setAvailableManuals] = useState([]);
    const [manualMode, setManualMode] = useState('SELECT');
    const [selectedManual, setSelectedManual] = useState('');
    const [customManualName, setCustomManualName] = useState('');
    const [search, setSearch] = useState('');
    const [sortBy, setSortBy] = useState('name');
    const [showAddWarning, setShowAddWarning] = useState(false);

    const isDirector = user && (user.role === 'DIRECTOR' || user.role === 'COORDONATOR');

    useEffect(() => { loadChildren(); }, []);

    const loadChildren = () => {
        fetch(`${API_URL}/children`).then(r => r.ok ? r.json() : []).then(async (kids) => {
            const existingManualsSet = new Set();
            kids.forEach(child => {
                if (child.manuals && Array.isArray(child.manuals)) child.manuals.forEach(m => existingManualsSet.add(m.name));
            });
            const uniqueManuals = Array.from(existingManualsSet).sort();
            setAvailableManuals(uniqueManuals);
            if (uniqueManuals.length > 0) { setSelectedManual(uniqueManuals[0]); setManualMode('SELECT'); } else { setManualMode('NEW'); }

            const kidsWithStatus = await Promise.all(kids.map(async (k) => {
                try {
                    const wRes = await fetch(`${API_URL}/warnings/child/${k.id}`);
                    const warnings = await wRes.json();
                    const isSuspended = Array.isArray(warnings) && warnings.length > 0 && warnings[0].suspension && warnings[0].remainingMeetings > 0;
                    return { ...k, isSuspended };
                } catch { return { ...k, isSuspended: false }; }
            }));
            setChildren(kidsWithStatus); setLoading(false);
        }).catch(() => setLoading(false));
    };

    const openChildFile = (child) => {
        fetch(`${API_URL}/children/${child.id}`).then(r => r.json()).then(freshData => {
            setSelectedChild(freshData);
            setCustomManualName('');
            fetch(`${API_URL}/warnings/child/${child.id}`).then(r => r.ok ? r.json() : []).then(setChildWarnings);
        });
    };

    const handleAssignManual = () => {
        const finalName = manualMode === 'NEW' ? customManualName : selectedManual;
        if (!finalName) return alert("Scrie nume manual!");
        fetch(`${API_URL}/children/${selectedChild.id}/assign-manual?manualName=${encodeURIComponent(finalName)}`, { method: 'POST' })
            .then(res => { if (res.ok) { alert("Atribuit!"); loadChildren(); openChildFile(selectedChild); }});
    };

    const handleAddWarning = () => {
        if (!newWarning.description) return alert("Scrie motiv!");
        const payload = { childId: selectedChild.id, ...newWarning, remainingMeetings: newWarning.suspension ? newWarning.remainingMeetings : 0 };
        fetch(`${API_URL}/warnings/add`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(payload) })
            .then(res => { if(res.ok) { alert("Salvat!"); openChildFile(selectedChild); setNewWarning({description:'', suspension:false, remainingMeetings:1}); }});
    };

    if (loading) return (
        <div className="animate-in db-loading">
            <div className="db-loading-pulse" style={{height:'140px'}} />
            <div style={{display:'grid', gridTemplateColumns:'1fr 1fr 1fr 1fr', gap:'15px'}}>
                {[1,2,3,4].map(i => <div key={i} className="db-loading-pulse" style={{height:'100px'}} />)}
            </div>
        </div>
    );

    /* ── DOSAR COPIL ───────────────────────────────────── */
    if (selectedChild) {
        const isCurrentlySuspended = Array.isArray(childWarnings) && childWarnings.length > 0 && childWarnings[0].suspension && childWarnings[0].remainingMeetings > 0;
        const initials = `${selectedChild.name?.charAt(0) || ''}${selectedChild.surname?.charAt(0) || ''}`;

        return (
            <div className="animate-in">

                {/* Hero */}
                <div className="db-hero" style={{marginBottom:'24px', alignItems:'center', ...(isCurrentlySuspended ? {background:'linear-gradient(135deg, #dc2626 0%, #9f1239 100%)'} : {})}}>
                    <div className="db-hero-left">
                        <span className="db-greeting">Dosar Copil</span>
                        <h1 className="db-name">{selectedChild.name} {selectedChild.surname}</h1>
                        <span className="db-role-pill" style={isCurrentlySuspended ? {background:'rgba(255,255,255,0.2)', color:'white', border:'1px solid rgba(255,255,255,0.3)'} : {}}>
                            {isCurrentlySuspended ? 'SUSPENDAT' : 'Activ'}
                        </span>
                    </div>
                    <button onClick={() => { setSelectedChild(null); loadChildren(); }} style={{
                        background:'rgba(255,255,255,0.14)', border:'1.5px solid rgba(255,255,255,0.28)',
                        color:'white', padding:'9px 18px', borderRadius:'12px', fontWeight:'700',
                        fontSize:'0.88rem', flexShrink:0, cursor:'pointer'
                    }}>
                        Inapoi
                    </button>
                </div>

                {/* Stat cards */}
                <div className="db-stats" style={{marginBottom:'24px'}}>
                    <div className="db-stat-card db-stat-blue">
                        <div className="db-stat-number">{selectedChild.age || '-'}</div>
                        <div className="db-stat-label">Ani</div>
                    </div>
                    <div className="db-stat-card db-stat-green">
                        <div className="db-stat-number">{selectedChild.totalAttendance || 0}</div>
                        <div className="db-stat-label">Prezente</div>
                    </div>
                    <div className="db-stat-card db-stat-purple">
                        <div className="db-stat-number">{selectedChild.attendanceStreak || 0}</div>
                        <div className="db-stat-label">Streak</div>
                    </div>
                    <div className="db-stat-card db-stat-amber">
                        <div className="db-stat-number">{selectedChild.seasonPoints || 0}</div>
                        <div className="db-stat-label">Puncte</div>
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
                                    background:'linear-gradient(135deg, var(--accent), #868cff)',
                                    display:'flex', alignItems:'center', justifyContent:'center',
                                    color:'white', fontWeight:'900', fontSize:'1rem'
                                }}>{initials}</div>
                                <div>
                                    <div style={{fontWeight:'800', color:'var(--text-primary)'}}>{selectedChild.name} {selectedChild.surname}</div>
                                    <div style={{fontSize:'0.78rem', color:'var(--text-secondary)'}}>{selectedChild.age} ani</div>
                                </div>
                            </div>
                            <div style={{display:'flex', flexDirection:'column', gap:'8px'}}>
                                <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', padding:'9px 12px', background:'var(--bg-primary)', borderRadius:'8px'}}>
                                    <span style={{color:'var(--text-secondary)', fontWeight:'600', fontSize:'0.88rem'}}>Parinte</span>
                                    <span style={{fontWeight:'800', color:'var(--text-primary)', fontSize:'0.9rem'}}>{selectedChild.parentName || '-'}</span>
                                </div>
                                <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', padding:'9px 12px', background:'var(--bg-primary)', borderRadius:'8px'}}>
                                    <span style={{color:'var(--text-secondary)', fontWeight:'600', fontSize:'0.88rem'}}>Telefon</span>
                                    <a href={`tel:${selectedChild.parentPhone}`} style={{fontWeight:'800', color:'var(--accent)', textDecoration:'none', fontSize:'0.9rem'}}>{selectedChild.parentPhone || '-'}</a>
                                </div>
                            </div>
                        </div>

                        {/* Inventar */}
                        <div className="card">
                            <p className="db-section-title" style={{marginBottom:'14px'}}>Inventar Premii</p>
                            <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'10px', marginBottom:'18px'}}>
                                <div style={{padding:'14px 10px', borderRadius:'12px', textAlign:'center', background: selectedChild.hasShirt ? '#dcfce7' : 'var(--bg-primary)', border:`2px solid ${selectedChild.hasShirt ? '#22c55e' : 'var(--border-color)'}`}}>
                                    <div style={{fontSize:'0.68rem', color:'var(--text-secondary)', fontWeight:'700', textTransform:'uppercase', letterSpacing:'0.5px', marginBottom:'4px'}}>Tricou</div>
                                    <div style={{fontWeight:'900', fontSize:'1rem', color: selectedChild.hasShirt ? '#16a34a' : 'var(--text-secondary)'}}>{selectedChild.hasShirt ? 'DAT' : 'Neacordat'}</div>
                                    {isDirector && !selectedChild.hasShirt && (
                                        <button onClick={()=>{if(window.confirm("Confirmi acordarea tricoului?")) fetch(`${API_URL}/children/${selectedChild.id}/give-reward?type=SHIRT`,{method:'POST'}).then(r=>{if(r.ok)openChildFile(selectedChild)})}} style={{marginTop:'8px', background:'#16a34a', color:'white', border:'none', borderRadius:'8px', padding:'5px 10px', fontWeight:'800', fontSize:'0.72rem', cursor:'pointer', width:'100%'}}>Acorda</button>
                                    )}
                                </div>
                                <div style={{padding:'14px 10px', borderRadius:'12px', textAlign:'center', background: selectedChild.hasHat ? '#dbeafe' : 'var(--bg-primary)', border:`2px solid ${selectedChild.hasHat ? '#3b82f6' : 'var(--border-color)'}`}}>
                                    <div style={{fontSize:'0.68rem', color:'var(--text-secondary)', fontWeight:'700', textTransform:'uppercase', letterSpacing:'0.5px', marginBottom:'4px'}}>Caciula</div>
                                    <div style={{fontWeight:'900', fontSize:'1rem', color: selectedChild.hasHat ? '#1d4ed8' : 'var(--text-secondary)'}}>{selectedChild.hasHat ? 'DATA' : 'Neacordata'}</div>
                                    {isDirector && !selectedChild.hasHat && (
                                        <button onClick={()=>{if(window.confirm("Confirmi acordarea caciulii?")) fetch(`${API_URL}/children/${selectedChild.id}/give-reward?type=HAT`,{method:'POST'}).then(r=>{if(r.ok)openChildFile(selectedChild)})}} style={{marginTop:'8px', background:'#1d4ed8', color:'white', border:'none', borderRadius:'8px', padding:'5px 10px', fontWeight:'800', fontSize:'0.72rem', cursor:'pointer', width:'100%'}}>Acorda</button>
                                    )}
                                </div>
                            </div>

                            <p className="db-section-title" style={{marginBottom:'10px'}}>Manuale</p>
                            {(selectedChild.manuals||[]).length > 0 ? (
                                <div style={{display:'flex', flexWrap:'wrap', gap:'7px', marginBottom:'14px'}}>
                                    {(selectedChild.manuals||[]).map((m,i) => (
                                        <span key={i} style={{
                                            display:'inline-flex', alignItems:'center', gap:'5px',
                                            background: m.status === 'COMPLETED' ? '#dcfce7' : '#ede9fe',
                                            color: m.status === 'COMPLETED' ? '#15803d' : '#6d28d9',
                                            border:`1px solid ${m.status === 'COMPLETED' ? '#bbf7d0' : '#ddd6fe'}`,
                                            padding:'4px 10px', borderRadius:'20px', fontSize:'0.8rem', fontWeight:'700'
                                        }}>
                                            {m.name}
                                            <span style={{fontSize:'0.68rem', opacity:0.7}}>· {m.status}</span>
                                        </span>
                                    ))}
                                </div>
                            ) : (
                                <p style={{color:'var(--text-secondary)', fontSize:'0.85rem', fontStyle:'italic', margin:'0 0 12px'}}>Niciun manual atribuit.</p>
                            )}
                            {isDirector && (
                                <div style={{display:'flex', flexDirection:'column', gap:'8px'}}>
                                    {availableManuals.length > 0 && (
                                        <select className="login-input" onChange={e => { setSelectedManual(e.target.value); setManualMode('SELECT'); }} style={{margin:0}}>
                                            {availableManuals.map(m => <option key={m} value={m}>{m}</option>)}
                                        </select>
                                    )}
                                    <input className="login-input" placeholder="Sau adauga manual nou..." value={customManualName} onChange={e => { setCustomManualName(e.target.value); setManualMode(e.target.value ? 'NEW' : 'SELECT'); }} style={{margin:0}} />
                                    <button onClick={handleAssignManual} style={{background:'var(--accent)', color:'white', border:'none', padding:'10px', borderRadius:'10px', fontWeight:'800', cursor:'pointer', fontSize:'0.9rem'}}>Atribuie Manual</button>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Coloana dreapta — Disciplina */}
                    <div className="card">
                        {/* Header disciplina */}
                        <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'16px'}}>
                            <div style={{display:'flex', alignItems:'center', gap:'8px'}}>
                                <p className="db-section-title" style={{margin:0}}>Disciplina</p>
                                {childWarnings.length > 0 && (
                                    <span style={{
                                        background: childWarnings.some(w => w.suspension && w.remainingMeetings > 0) ? '#fee2e2' : '#fef3c7',
                                        color: childWarnings.some(w => w.suspension && w.remainingMeetings > 0) ? '#dc2626' : '#d97706',
                                        padding:'2px 9px', borderRadius:'20px', fontSize:'0.75rem', fontWeight:'800',
                                        border: `1px solid ${childWarnings.some(w => w.suspension && w.remainingMeetings > 0) ? '#fca5a5' : '#fde68a'}`
                                    }}>{childWarnings.length}</span>
                                )}
                            </div>
                            {isDirector && (
                                <button onClick={() => { setShowAddWarning(!showAddWarning); setNewWarning({description:'', suspension:false, remainingMeetings:1}); }} style={{
                                    width:'32px', height:'32px', borderRadius:'50%',
                                    background: showAddWarning ? '#fee2e2' : '#be123c',
                                    color: showAddWarning ? '#dc2626' : 'white',
                                    border:'none', cursor:'pointer', fontWeight:'900', fontSize:'1.2rem',
                                    display:'flex', alignItems:'center', justifyContent:'center',
                                    transition:'all 0.18s', flexShrink:0
                                }}>{showAddWarning ? '×' : '+'}</button>
                            )}
                        </div>

                        {/* Form adauga sanctiune — collapsible */}
                        {isDirector && showAddWarning && (
                            <div className="animate-in" style={{display:'flex', flexDirection:'column', gap:'10px', marginBottom:'18px', padding:'16px', background:'#fff1f2', borderRadius:'14px', border:'1px solid #fecaca'}}>
                                <input
                                    className="login-input"
                                    placeholder="Descrie motivul sanctiunii..."
                                    value={newWarning.description}
                                    onChange={e => setNewWarning({...newWarning, description: e.target.value})}
                                    style={{margin:0}}
                                    autoFocus
                                />
                                {/* Toggle tip: Avertisment / Suspendare */}
                                <div style={{display:'flex', gap:'8px'}}>
                                    <button onClick={() => setNewWarning({...newWarning, suspension:false})} style={{
                                        flex:1, padding:'9px', borderRadius:'10px', fontWeight:'800', fontSize:'0.82rem', cursor:'pointer', border:'none',
                                        background: !newWarning.suspension ? '#fef3c7' : 'var(--bg-primary)',
                                        color: !newWarning.suspension ? '#d97706' : 'var(--text-secondary)',
                                        boxShadow: !newWarning.suspension ? '0 0 0 2px #fde68a' : 'none'
                                    }}>Avertisment</button>
                                    <button onClick={() => setNewWarning({...newWarning, suspension:true})} style={{
                                        flex:1, padding:'9px', borderRadius:'10px', fontWeight:'800', fontSize:'0.82rem', cursor:'pointer', border:'none',
                                        background: newWarning.suspension ? '#fee2e2' : 'var(--bg-primary)',
                                        color: newWarning.suspension ? '#dc2626' : 'var(--text-secondary)',
                                        boxShadow: newWarning.suspension ? '0 0 0 2px #fca5a5' : 'none'
                                    }}>Suspendare</button>
                                </div>
                                {newWarning.suspension && (
                                    <div style={{display:'flex', alignItems:'center', gap:'12px', padding:'10px 14px', background:'#fee2e2', borderRadius:'10px'}}>
                                        <span style={{fontSize:'0.88rem', fontWeight:'700', color:'#be123c', flex:1}}>Numar ture suspendat:</span>
                                        <input type="number" min="1" value={newWarning.remainingMeetings} onChange={e => setNewWarning({...newWarning, remainingMeetings: parseInt(e.target.value)})} style={{width:'70px', padding:'8px', textAlign:'center', border:'1.5px solid #be123c', borderRadius:'8px', fontWeight:'900', color:'#be123c', outline:'none', background:'white'}} />
                                    </div>
                                )}
                                <button onClick={() => { handleAddWarning(); setShowAddWarning(false); }} style={{background:'#be123c', color:'white', width:'100%', padding:'11px', borderRadius:'10px', fontWeight:'800', border:'none', cursor:'pointer', fontSize:'0.9rem'}}>Salveaza Sanctiunea</button>
                            </div>
                        )}

                        {/* Lista sanctiuni */}
                        <div style={{display:'flex', flexDirection:'column', gap:'8px', maxHeight:'380px', overflowY:'auto'}}>
                            {childWarnings.length === 0 ? (
                                <div style={{textAlign:'center', padding:'30px 20px'}}>
                                    <div style={{width:'44px', height:'44px', borderRadius:'50%', background:'#f1f5f9', display:'flex', alignItems:'center', justifyContent:'center', margin:'0 auto 10px'}}>
                                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M9 12l2 2 4-4M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z" stroke="#22c55e" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
                                    </div>
                                    <p style={{color:'var(--text-secondary)', fontSize:'0.88rem', margin:0, fontWeight:'600'}}>Fara sanctiuni</p>
                                </div>
                            ) : (
                                childWarnings.map(w => (
                                    <div key={w.id} style={{
                                        padding:'12px 14px', borderRadius:'10px',
                                        background:'var(--bg-primary)',
                                        boxShadow:`inset 4px 0 0 ${w.suspension ? '#dc2626' : '#f59e0b'}`
                                    }}>
                                        <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'5px'}}>
                                            <span style={{
                                                fontSize:'0.68rem', fontWeight:'800', textTransform:'uppercase', letterSpacing:'0.5px',
                                                color: w.suspension ? '#dc2626' : '#d97706',
                                                background: w.suspension ? '#fee2e2' : '#fef3c7',
                                                padding:'2px 8px', borderRadius:'4px'
                                            }}>
                                                {w.suspension ? `Suspendare · ${w.remainingMeetings} ture` : 'Avertisment'}
                                            </span>
                                            <span style={{fontSize:'0.75rem', color:'var(--text-secondary)', fontWeight:'600'}}>{w.date}</span>
                                        </div>
                                        <p style={{margin:0, fontSize:'0.88rem', color:'var(--text-primary)', fontWeight:'600'}}>{w.description}</p>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    /* ── LISTA COPII ────────────────────────────────────── */
    const suspendedCount = children.filter(c => c.isSuspended).length;
    let filtered = children.filter(c =>
        search === '' || `${c.name} ${c.surname}`.toLowerCase().includes(search.toLowerCase())
    );
    if (sortBy === 'suspended') filtered = filtered.filter(c => c.isSuspended);
    else if (sortBy === 'points') filtered = [...filtered].sort((a,b) => (b.seasonPoints||0) - (a.seasonPoints||0));
    else if (sortBy === 'attendance') filtered = [...filtered].sort((a,b) => (b.totalAttendance||0) - (a.totalAttendance||0));
    else filtered = [...filtered].sort((a,b) => a.name.localeCompare(b.name));

    return (
        <div className="animate-in">

            <div className="db-hero" style={{marginBottom:'24px', alignItems:'center'}}>
                <div className="db-hero-left">
                    <span className="db-greeting">Club Awana</span>
                    <h1 className="db-name">Registru Copii</h1>
                    <span className="db-role-pill">{children.length} copii inscrisi</span>
                </div>
                {suspendedCount > 0 && (
                    <div style={{background:'rgba(220,38,38,0.18)', borderRadius:'14px', padding:'10px 18px', textAlign:'center', border:'1.5px solid rgba(220,38,38,0.35)', flexShrink:0}}>
                        <div style={{color:'rgba(255,255,255,0.75)', fontSize:'0.7rem', fontWeight:'700', textTransform:'uppercase', letterSpacing:'1px'}}>Suspendati</div>
                        <div style={{color:'white', fontSize:'1.7rem', fontWeight:'900', lineHeight:1.1}}>{suspendedCount}</div>
                    </div>
                )}
            </div>

            <input
                className="login-input"
                placeholder="Cauta copil dupa nume..."
                value={search}
                onChange={e => setSearch(e.target.value)}
                style={{marginBottom:'12px'}}
            />

            {/* Sort / Filter pills */}
            <div style={{display:'flex', gap:'6px', marginBottom:'20px', background:'var(--bg-primary)', padding:'4px', borderRadius:'14px'}}>
                {[
                    {key:'name', label:'Nume A–Z'},
                    {key:'points', label:'Puncte'},
                    {key:'attendance', label:'Prezente'},
                    {key:'suspended', label:`Suspendati${suspendedCount > 0 ? ` (${suspendedCount})` : ''}`}
                ].map(opt => (
                    <button key={opt.key} onClick={() => setSortBy(opt.key)} style={{
                        flex:1, padding:'8px 6px', border:'none', cursor:'pointer', borderRadius:'10px',
                        fontWeight:'700', fontSize:'0.78rem', transition:'all 0.18s',
                        background: sortBy === opt.key ? 'white' : 'transparent',
                        color: sortBy === opt.key ? (opt.key === 'suspended' ? '#dc2626' : 'var(--accent)') : 'var(--text-secondary)',
                        boxShadow: sortBy === opt.key ? '0 2px 8px rgba(0,0,0,0.08)' : 'none'
                    }}>{opt.label}</button>
                ))}
            </div>

            <div style={{display:'flex', flexDirection:'column', gap:'10px'}}>
                {filtered.map(c => {
                    const initials = `${c.name?.charAt(0) || ''}${c.surname?.charAt(0) || ''}`;
                    return (
                        <div key={c.id} style={{
                            background:'white', padding:'14px 18px', borderRadius:'14px',
                            border:'1px solid var(--border-color)',
                            borderLeft:`4px solid ${c.isSuspended ? '#dc2626' : '#22c55e'}`,
                            boxShadow:'0 2px 6px rgba(0,0,0,0.04)',
                            display:'flex', alignItems:'center', gap:'14px'
                        }}>
                            <div style={{
                                width:'42px', height:'42px', borderRadius:'12px', flexShrink:0,
                                background: c.isSuspended ? 'linear-gradient(135deg,#dc2626,#9f1239)' : 'linear-gradient(135deg,var(--accent),#868cff)',
                                display:'flex', alignItems:'center', justifyContent:'center',
                                color:'white', fontWeight:'900', fontSize:'0.95rem'
                            }}>{initials}</div>

                            <div style={{flex:1, minWidth:0}}>
                                <div style={{fontWeight:'800', color:'var(--text-primary)', whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis'}}>{c.name} {c.surname}</div>
                                <div style={{display:'flex', gap:'14px', marginTop:'3px', flexWrap:'wrap'}}>
                                    <span style={{fontSize:'0.76rem', color:'var(--text-secondary)', fontWeight:'600'}}>{c.totalAttendance || 0} prezente</span>
                                    <span style={{fontSize:'0.76rem', color:'var(--text-secondary)', fontWeight:'600'}}>Streak {c.attendanceStreak || 0}</span>
                                    <span style={{fontSize:'0.76rem', color:'var(--text-secondary)', fontWeight:'600'}}>{c.seasonPoints || 0} pct.</span>
                                </div>
                            </div>

                            {c.isSuspended && (
                                <span style={{background:'#fee2e2', color:'#dc2626', padding:'3px 9px', borderRadius:'20px', fontSize:'0.72rem', fontWeight:'800', border:'1px solid #fca5a5', flexShrink:0}}>SUSPENDAT</span>
                            )}

                            <button onClick={() => openChildFile(c)} style={{
                                background:'var(--accent)', color:'white', border:'none',
                                padding:'8px 16px', borderRadius:'10px', fontWeight:'800',
                                cursor:'pointer', fontSize:'0.83rem', flexShrink:0
                            }}>
                                Dosar
                            </button>
                        </div>
                    );
                })}
                {filtered.length === 0 && (
                    <div style={{textAlign:'center', color:'var(--text-secondary)', padding:'40px', fontStyle:'italic'}}>Nu am gasit niciun copil.</div>
                )}
            </div>
        </div>
    );
};

export default Registry;
