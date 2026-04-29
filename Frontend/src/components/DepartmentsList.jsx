import React, { useState, useEffect } from 'react';
import { API_URL } from '../config';

const DepartmentsList = ({ user }) => {
    const [departments, setDepartments] = useState([]);
    const [leaders, setLeaders] = useState([]);
    const [meetings, setMeetings] = useState([]);
    const [selectedDept, setSelectedDept] = useState(null);
    const [deptMembers, setDeptMembers] = useState([]);
    const [scheduleData, setScheduleData] = useState({});
    const [eligibleMap, setEligibleMap] = useState({});

    // Search state
    const [headSearch, setHeadSearch] = useState('');
    const [headDropOpen, setHeadDropOpen] = useState(false);
    const [meetingSearch, setMeetingSearch] = useState({});
    const [openMeetingDrop, setOpenMeetingDrop] = useState(null);

    const isDirector = user.role === 'DIRECTOR' || user.role === 'COORDONATOR';

    const loadData = () => {
        fetch(`${API_URL}/departments`).then(r=>r.json()).then(setDepartments).catch(()=>{});
        fetch(`${API_URL}/leaders`).then(r=>r.json()).then(setLeaders).catch(()=>{});
        fetch(`${API_URL}/meetings`).then(r=>r.ok?r.json():[]).then(setMeetings).catch(()=>{});
    };
    useEffect(() => { loadData(); }, []);

    const handleSelectDept = (dept) => {
        setSelectedDept(dept); setDeptMembers([]); setScheduleData({}); setEligibleMap({});
        setHeadSearch(''); setHeadDropOpen(false); setMeetingSearch({}); setOpenMeetingDrop(null);
        fetch(`${API_URL}/departments/${dept.id}/members`).then(r => r.ok ? r.json() : []).then(setDeptMembers);
        meetings.forEach(m => {
            fetch(`${API_URL}/departments/plan/${m.id}`).then(r => r.ok ? r.json() : null).then(data => {
                if(data) {
                    if (data.assignments && data.assignments[dept.id]) setScheduleData(prev => ({ ...prev, [m.id]: data.assignments[dept.id] }));
                    if (data.eligibleLeaders) setEligibleMap(prev => ({ ...prev, ...data.eligibleLeaders }));
                }
            });
        });
    };

    const handleAddLeader = (meetingId, leaderId) => {
        if(!leaderId) return;
        fetch(`${API_URL}/departments/nominate`, {
            method: 'POST', headers: {'Content-Type':'application/json'},
            body: JSON.stringify({ meetingId, deptId: selectedDept.id, leaderId: parseInt(leaderId) })
        }).then(res => {
            if(res.ok) { handleSelectDept(selectedDept); alert("Propunere trimisa!"); }
            else alert("Eroare! Poate liderul e deja asignat.");
        });
    };

    const handleRemove = (meetingId, leaderId) => {
        if(!window.confirm("Stergi liderul din tura?")) return;
        fetch(`${API_URL}/departments/remove?meetingId=${meetingId}&deptId=${selectedDept.id}&leaderId=${leaderId}`, { method: 'DELETE' })
            .then(res => { if(res.ok) handleSelectDept(selectedDept); else alert("Eroare la stergere."); });
    };

    const handleResponse = (assignmentId, response) => {
        fetch(`${API_URL}/departments/respond`, {
            method: 'POST', headers: {'Content-Type':'application/json'},
            body: JSON.stringify({ assignmentId, response })
        }).then(res => { if(res.ok) { alert(response === 'ACCEPTED' ? "Ai confirmat!" : "Ai refuzat."); handleSelectDept(selectedDept); }});
    };

    const setHeadLeader = (deptId, leaderId) => {
        if(!leaderId) return;
        fetch(`${API_URL}/departments/${deptId}/set-head`, {
            method: 'POST', headers: {'Content-Type':'application/json'}, body: leaderId
        }).then(res => { if(res.ok) { alert("Sef salvat!"); loadData(); setSelectedDept(null); } else alert("Eroare la salvare."); });
    };

    if(!departments.length) return <div className="animate-in" style={{padding:'20px', color:'var(--text-secondary)'}}>Se incarca departamentele...</div>;

    /* ── DETALIU DEPARTAMENT ──────────────────────────────── */
    if (selectedDept) {
        const canEdit = isDirector || (selectedDept.headLeader && selectedDept.headLeader.id === user.id);
        const eligibleForThisDept = eligibleMap[selectedDept.id] || [];
        const displayLeaders = eligibleForThisDept.length > 0 ? eligibleForThisDept : deptMembers;

        return (
            <div className="animate-in">

                {/* Hero departament */}
                <div className="db-hero" style={{marginBottom:'24px', alignItems:'center'}}>
                    <div className="db-hero-left">
                        <span className="db-greeting">Departament</span>
                        <h1 className="db-name" style={{marginBottom:'10px'}}>{selectedDept.name}</h1>
                        <span className="db-role-pill">
                            {selectedDept.headLeader ? `Responsabil: ${selectedDept.headLeader.name} ${selectedDept.headLeader.surname}` : 'Fara responsabil'}
                        </span>
                    </div>
                    <button onClick={() => setSelectedDept(null)} style={{
                        background:'rgba(255,255,255,0.14)', border:'1.5px solid rgba(255,255,255,0.28)',
                        color:'white', padding:'9px 18px', borderRadius:'12px', fontWeight:'700',
                        fontSize:'0.88rem', flexShrink:0
                    }}>
                        Inapoi
                    </button>
                </div>

                <div className="profile-grid">

                    {/* Coloana stanga */}
                    <div style={{display:'flex', flexDirection:'column', gap:'16px'}}>

                        {/* Responsabil */}
                        <div className="card">
                            <p className="db-section-title" style={{marginBottom:'12px'}}>Responsabil Departament</p>
                            {selectedDept.headLeader ? (
                                <div style={{display:'flex', alignItems:'center', gap:'12px'}}>
                                    <div style={{
                                        width:'44px', height:'44px', borderRadius:'12px', flexShrink:0,
                                        background:'linear-gradient(135deg, var(--accent), #868cff)',
                                        display:'flex', alignItems:'center', justifyContent:'center',
                                        color:'white', fontWeight:'900', fontSize:'1rem'
                                    }}>
                                        {selectedDept.headLeader.name?.charAt(0)}{selectedDept.headLeader.surname?.charAt(0)}
                                    </div>
                                    <div style={{flex:1}}>
                                        <div style={{fontWeight:'800', color:'var(--text-primary)'}}>{selectedDept.headLeader.name} {selectedDept.headLeader.surname}</div>
                                        <div style={{fontSize:'0.78rem', color:'var(--text-secondary)', textTransform:'uppercase', letterSpacing:'0.5px', marginTop:'2px'}}>Responsabil</div>
                                    </div>
                                    {isDirector && (
                                        <button onClick={() => { setHeadDropOpen(true); setHeadSearch(''); }} style={{background:'var(--bg-primary)', border:'1px solid var(--border-color)', color:'var(--text-secondary)', borderRadius:'8px', padding:'5px 10px', fontWeight:'700', fontSize:'0.8rem', flexShrink:0}}>
                                            Schimba
                                        </button>
                                    )}
                                </div>
                            ) : (
                                <p style={{color:'var(--text-secondary)', fontSize:'0.9rem', fontStyle:'italic', margin:'0 0 10px'}}>Niciun responsabil desemnat.</p>
                            )}
                            {isDirector && (headDropOpen || !selectedDept.headLeader) && (
                                <div style={{position:'relative', marginTop:'10px'}}>
                                    <input
                                        className="login-input"
                                        placeholder="Cauta lider..."
                                        value={headSearch}
                                        autoFocus
                                        onChange={e => setHeadSearch(e.target.value)}
                                        onBlur={() => setTimeout(() => setHeadDropOpen(false), 150)}
                                        style={{marginBottom:0}}
                                    />
                                    <div className="animate-in" style={{position:'absolute', top:'calc(100% + 4px)', left:0, right:0, background:'white', border:'1px solid var(--border-color)', borderRadius:'12px', zIndex:20, maxHeight:'200px', overflowY:'auto', boxShadow:'0 8px 24px rgba(0,0,0,0.1)'}}>
                                        {leaders
                                            .filter(l => headSearch === '' || `${l.name} ${l.surname}`.toLowerCase().includes(headSearch.toLowerCase()))
                                            .map(l => (
                                                <div key={l.id} onMouseDown={() => { setHeadLeader(selectedDept.id, l.id); setHeadDropOpen(false); }} style={{padding:'11px 14px', cursor:'pointer', fontWeight:'700', color:'var(--text-primary)', borderBottom:'1px solid var(--border-color)', display:'flex', justifyContent:'space-between', alignItems:'center'}}>
                                                    <span>{l.name} {l.surname}</span>
                                                    <span style={{fontSize:'0.75rem', color:'var(--accent)', fontWeight:'800'}}>Alege</span>
                                                </div>
                                            ))
                                        }
                                        {leaders.filter(l => `${l.name} ${l.surname}`.toLowerCase().includes(headSearch.toLowerCase())).length === 0 && (
                                            <div style={{padding:'12px 14px', color:'var(--text-secondary)', fontSize:'0.88rem'}}>Niciun rezultat.</div>
                                        )}
                                    </div>
                                </div>
                            )}
                        </div>

                        {/* Echipa permanenta */}
                        <div className="card">
                            <p className="db-section-title" style={{marginBottom:'12px'}}>Echipa Permanenta</p>
                            {deptMembers.length === 0 ? (
                                <p style={{color:'var(--text-secondary)', fontSize:'0.9rem', fontStyle:'italic', margin:0}}>Nu sunt membri inscriși permanent.</p>
                            ) : (
                                <div style={{display:'flex', flexWrap:'wrap', gap:'8px'}}>
                                    {deptMembers.map(m => (
                                        <span key={m.id} style={{
                                            display:'inline-flex', alignItems:'center', gap:'6px',
                                            background:'#ede9fe', color:'#6d28d9',
                                            padding:'5px 12px', borderRadius:'20px',
                                            fontSize:'0.83rem', fontWeight:'700',
                                            border:'1px solid #ddd6fe'
                                        }}>
                                            {m.name} {m.surname}
                                        </span>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Coloana dreapta — Planificare seri */}
                    <div className="card" style={{padding:'20px'}}>
                        <p className="db-section-title" style={{marginBottom:'16px'}}>Planificare Seri</p>
                        <div style={{display:'flex', flexDirection:'column', gap:'12px'}}>
                            {meetings.slice(0, 5).map(m => {
                                const assigned = scheduleData[m.id] || [];
                                const mSearch = meetingSearch[m.id] || '';
                                return (
                                    <div key={m.id} style={{border:'1px solid var(--border-color)', borderRadius:'14px', overflow:'hidden'}}>
                                        {/* Header sedinta */}
                                        <div style={{padding:'12px 16px', background:'var(--bg-primary)', borderBottom:'1px solid var(--border-color)', display:'flex', justifyContent:'space-between', alignItems:'center'}}>
                                            <div>
                                                <span style={{fontWeight:'800', color:'var(--accent)', fontSize:'0.92rem'}}>{m.date}</span>
                                                <span style={{color:'var(--text-secondary)', fontSize:'0.85rem', marginLeft:'8px'}}>{m.description || 'Seara de Club'}</span>
                                            </div>
                                            <span style={{
                                                background: assigned.length > 0 ? '#dcfce7' : 'white',
                                                color: assigned.length > 0 ? '#15803d' : 'var(--text-secondary)',
                                                border: `1px solid ${assigned.length > 0 ? '#bbf7d0' : 'var(--border-color)'}`,
                                                borderRadius:'20px', padding:'2px 10px',
                                                fontSize:'0.75rem', fontWeight:'700'
                                            }}>
                                                {assigned.length} lideri
                                            </span>
                                        </div>

                                        <div style={{padding:'12px 16px'}}>
                                            {/* Lideri asignati */}
                                            {assigned.length === 0 ? (
                                                <p style={{margin:'0 0 10px', color:'var(--text-secondary)', fontSize:'0.85rem', fontStyle:'italic'}}>Nimeni planificat inca.</p>
                                            ) : (
                                                <div style={{display:'flex', flexWrap:'wrap', gap:'7px', marginBottom:'10px'}}>
                                                    {assigned.map(a => {
                                                        const accepted = a.status === 'ACCEPTED';
                                                        const pending = a.status === 'PENDING';
                                                        return (
                                                            <span key={a.id} style={{
                                                                display:'inline-flex', alignItems:'center', gap:'6px',
                                                                background: accepted ? '#dcfce7' : '#fff7ed',
                                                                color: accepted ? '#15803d' : '#9a3412',
                                                                border: `1px solid ${accepted ? '#bbf7d0' : '#fdba74'}`,
                                                                padding:'4px 10px', borderRadius:'20px',
                                                                fontSize:'0.83rem', fontWeight:'700'
                                                            }}>
                                                                {a.leader ? `${a.leader.name} ${a.leader.surname}` : '?'}
                                                                {pending && <span style={{fontSize:'0.7rem', opacity:0.7}}>· astept</span>}
                                                                {canEdit && (
                                                                    <button onClick={() => handleRemove(m.id, a.leader.id)} style={{background:'none', border:'none', cursor:'pointer', color:'inherit', opacity:0.6, fontWeight:'900', fontSize:'0.9rem', lineHeight:1, padding:'0 0 0 2px'}}>×</button>
                                                                )}
                                                                {pending && a.leader && a.leader.id === user.id && (
                                                                    <span style={{display:'inline-flex', gap:'4px', marginLeft:'2px'}}>
                                                                        <button onClick={() => handleResponse(a.id, 'ACCEPTED')} style={{width:'18px', height:'18px', borderRadius:'50%', background:'#15803d', color:'white', border:'none', cursor:'pointer', fontSize:'0.7rem', fontWeight:'900', display:'flex', alignItems:'center', justifyContent:'center'}}>✓</button>
                                                                        <button onClick={() => handleResponse(a.id, 'DECLINED')} style={{width:'18px', height:'18px', borderRadius:'50%', background:'#dc2626', color:'white', border:'none', cursor:'pointer', fontSize:'0.7rem', fontWeight:'900', display:'flex', alignItems:'center', justifyContent:'center'}}>✕</button>
                                                                    </span>
                                                                )}
                                                            </span>
                                                        );
                                                    })}
                                                </div>
                                            )}

                                            {/* Search propunere */}
                                            {canEdit && (
                                                <div style={{position:'relative'}}>
                                                    <input
                                                        className="login-input"
                                                        placeholder="Cauta si propune lider..."
                                                        value={mSearch}
                                                        onFocus={() => setOpenMeetingDrop(m.id)}
                                                        onChange={e => setMeetingSearch(prev => ({...prev, [m.id]: e.target.value}))}
                                                        onBlur={() => setTimeout(() => setOpenMeetingDrop(null), 150)}
                                                        style={{marginBottom:0, fontSize:'0.85rem', padding:'8px 12px'}}
                                                    />
                                                    {openMeetingDrop === m.id && (
                                                        <div className="animate-in" style={{position:'absolute', top:'calc(100% + 4px)', left:0, right:0, background:'white', border:'1px solid var(--border-color)', borderRadius:'12px', zIndex:20, maxHeight:'170px', overflowY:'auto', boxShadow:'0 8px 24px rgba(0,0,0,0.1)'}}>
                                                            {displayLeaders
                                                                .filter(l => !assigned.some(a => a.leader?.id === l.id))
                                                                .filter(l => mSearch === '' || `${l.name} ${l.surname}`.toLowerCase().includes(mSearch.toLowerCase()))
                                                                .sort((a, b) => a.name.localeCompare(b.name))
                                                                .map(l => (
                                                                    <div key={l.id} onMouseDown={() => { handleAddLeader(m.id, l.id); setMeetingSearch(prev => ({...prev, [m.id]: ''})); setOpenMeetingDrop(null); }} style={{padding:'10px 14px', cursor:'pointer', fontWeight:'700', color:'var(--text-primary)', borderBottom:'1px solid var(--border-color)', display:'flex', justifyContent:'space-between', alignItems:'center'}}>
                                                                        <span>{l.name} {l.surname}</span>
                                                                        <span style={{fontSize:'0.75rem', color:'var(--accent)', fontWeight:'800'}}>Propune</span>
                                                                    </div>
                                                                ))
                                                            }
                                                            {displayLeaders.filter(l => !assigned.some(a => a.leader?.id === l.id) && `${l.name} ${l.surname}`.toLowerCase().includes(mSearch.toLowerCase())).length === 0 && (
                                                                <div style={{padding:'11px 14px', color:'var(--text-secondary)', fontSize:'0.85rem'}}>Niciun lider disponibil.</div>
                                                            )}
                                                        </div>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>

                </div>
            </div>
        );
    }

    /* ── LISTA DEPARTAMENTE ──────────────────────────────── */
    return (
        <div className="animate-in">

            {/* Hero */}
            <div className="db-hero" style={{marginBottom:'24px', alignItems:'center'}}>
                <div className="db-hero-left">
                    <span className="db-greeting">Structura organizationala</span>
                    <h1 className="db-name" style={{marginBottom:'10px'}}>Departamente</h1>
                    <span className="db-role-pill">{departments.length} departamente active</span>
                </div>
                <div style={{textAlign:'right', flexShrink:0, zIndex:1}}>
                    <div className="db-date-day">{departments.length}</div>
                    <div className="db-date-month">total</div>
                </div>
            </div>

            {/* Grid carduri */}
            <div style={{display:'grid', gridTemplateColumns:'repeat(auto-fill, minmax(260px, 1fr))', gap:'14px'}}>
                {departments.map((d, i) => {
                    const accentColors = ['var(--accent)', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];
                    const color = accentColors[i % accentColors.length];
                    return (
                        <div key={d.id} onClick={() => handleSelectDept(d)} style={{
                            background:'white', borderRadius:'20px', padding:'22px',
                            cursor:'pointer', border:'1px solid var(--border-color)',
                            boxShadow:'var(--card-shadow)', transition:'transform 0.18s, box-shadow 0.18s',
                            borderLeft:`5px solid ${color}`,
                            position:'relative', overflow:'hidden'
                        }}>
                            {/* Background accent circle */}
                            <div style={{position:'absolute', top:'-20px', right:'-20px', width:'80px', height:'80px', borderRadius:'50%', background:color, opacity:0.06, pointerEvents:'none'}}/>

                            <div style={{fontWeight:'900', fontSize:'1.1rem', color:'var(--text-primary)', marginBottom:'10px'}}>{d.name}</div>

                            {d.headLeader ? (
                                <div style={{display:'flex', alignItems:'center', gap:'9px'}}>
                                    <div style={{width:'28px', height:'28px', borderRadius:'8px', background:color, display:'flex', alignItems:'center', justifyContent:'center', color:'white', fontWeight:'800', fontSize:'0.72rem', flexShrink:0}}>
                                        {d.headLeader.name?.charAt(0)}{d.headLeader.surname?.charAt(0)}
                                    </div>
                                    <span style={{fontSize:'0.85rem', color:'var(--text-secondary)', fontWeight:'600'}}>
                                        {d.headLeader.name} {d.headLeader.surname}
                                    </span>
                                </div>
                            ) : (
                                <span style={{fontSize:'0.82rem', color:'#f59e0b', fontWeight:'700', background:'#fef3c7', padding:'3px 10px', borderRadius:'20px', border:'1px solid #fde68a'}}>
                                    Fara responsabil
                                </span>
                            )}

                            <div style={{position:'absolute', bottom:'18px', right:'18px'}}>
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{opacity:0.5}}>
                                    <polyline points="9,18 15,12 9,6"/>
                                </svg>
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default DepartmentsList;
