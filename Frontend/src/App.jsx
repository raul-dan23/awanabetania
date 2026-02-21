import React, { useState, useEffect } from 'react';
import './App.css';
import AwanaLogo from './AwanaLogo';
import AdminDashboard from './AdminDashboard';

// const API_URL = 'http://awana.betania-tm.ro/api';
//const API_URL = 'http://86.106.170.96:8080/api';
const API_URL = 'http://awana.betania-tm.ro/api'; // asta e pentru server
//const API_URL = 'http://localhost:8080/api'; //pentru localhost
// const API_URL = 'http://192.168.1.156:8080/api'; // pentru localhost dar pe tel, dar nu uita sa verifici sa fie pe port ok

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
const SplashScreen = () => (
    <div className="splash-screen">
        <div className="logo-anim"><AwanaLogo width="280px" /></div>
        <p style={{marginTop: '20px', color: 'white', opacity: 0.8}}>Se initializeaza sistemul...</p>
    </div>
);

// ==========================================
// 2. MEETING FEEDBACK
// ==========================================
const MeetingFeedback = ({ meeting, user, onComplete, onCancel }) => {
    const [leaders, setLeaders] = useState([]);
    const [generalData, setGeneralData] = useState({ rating: 0, feedback: '' });
    const [leaderEvals, setLeaderEvals] = useState({});

    useEffect(() => {
        fetch(`${API_URL}/leaders`).then(r=>r.json()).then(setLeaders).catch(()=>{});
    }, []);

    const handleLeaderChange = (id, field, value) => {
        setLeaderEvals(prev => ({ ...prev, [id]: { ...prev[id], [field]: value } }));
    };

    const finishNight = () => {
        if(generalData.rating === 0) {
            if(!window.confirm("Nu ai dat o nota generala serii. Continui fara nota?")) return;
        }

        const evaluationsList = Object.keys(leaderEvals).map(lid => ({
            leaderId: parseInt(lid),
            rating: leaderEvals[lid].rating || 0,
            comment: leaderEvals[lid].comment || ''
        }));

        const payload = {
            meetingId: meeting.id,
            directorId: user.id,
            generalRating: generalData.rating,
            generalFeedback: generalData.feedback,
            evaluations: evaluationsList
        };

        fetch(`${API_URL}/feedback/save`, {
            method: 'POST', headers: {'Content-Type':'application/json'},
            body: JSON.stringify(payload)
        }).then(res => {
            if(res.ok) { onComplete(); } else alert("Eroare la salvare feedback.");
        }).catch(() => alert("Eroare server."));
    };

    return (
        <div className="animate-in">
            {/* MODIFICAT: Buton cu clasa CSS */}
            <button onClick={onCancel} className="btn-secondary" style={{marginBottom:'20px'}}>
                ⬅ Anuleaza Inchiderea
            </button>

            <h2 style={{borderBottom:'2px solid var(--accent)', paddingBottom:'10px'}}>🏁 Finalizare Seara: {meeting.date}</h2>

            <div className="card" style={{marginBottom:'20px', borderLeft:'5px solid var(--accent)'}}>
                <h3>General - Cum a fost seara?</h3>
                <div style={{marginBottom:'10px'}}>
                    <label>Rating: </label>
                    {[1,2,3,4,5].map(star => (
                        <span key={star} onClick={()=>setGeneralData({...generalData, rating: star})}
                              style={{cursor:'pointer', fontSize:'2rem', color: star <= generalData.rating ? 'orange' : '#ddd', margin:'0 5px'}}>★</span>
                    ))}
                </div>
                <textarea placeholder="Observatii generale despre intalnire..." value={generalData.feedback} onChange={e=>setGeneralData({...generalData, feedback: e.target.value})} />
            </div>

            <div className="card">
                <h3>Evaluare Lideri</h3>
                <div style={{display:'grid', gap:'15px', maxHeight:'400px', overflowY:'auto'}}>
                    {leaders.map(l => (
                        <div key={l.id} style={{padding:'10px', background:'#f9f9f9', borderRadius:'10px', border:'1px solid #eee'}}>
                            <div style={{fontWeight:'bold'}}>{l.name} {l.surname}</div>
                            <div style={{display:'flex', gap:'10px', alignItems:'center', marginTop:'5px'}}>
                                <div>{[1,2,3,4,5].map(s => <span key={s} onClick={()=>handleLeaderChange(l.id, 'rating', s)} style={{cursor:'pointer', fontSize:'1.2rem', color:s<=(leaderEvals[l.id]?.rating||0)?'orange':'#ddd'}}>★</span>)}</div>
                                <input placeholder="Comentariu personal..." style={{flex:1, padding:'5px', borderRadius:'5px', border:'1px solid #ddd'}} value={leaderEvals[l.id]?.comment||''} onChange={e=>handleLeaderChange(l.id, 'comment', e.target.value)} />
                            </div>
                        </div>
                    ))}
                </div>
                <button onClick={finishNight} style={{marginTop:'20px', width:'100%', padding:'15px', background:'#ef4444', color:'white', borderRadius:'10px', fontWeight:'bold', fontSize:'1.2rem', border:'2px solid #b91c1c', cursor:'pointer'}}>🚨 INCHEIE DEFINITIV SEARA</button>
            </div>
        </div>
    );
};


// ==========================================
// COMPONENTA: STICKER MAP (Reparată pentru ID-uri mari)
// ==========================================
const StickerMap = ({ child, user, onUnlock }) => {
    const [stickers, setStickers] = useState([]);
    const isLeader = user && !user.hasOwnProperty('parentPhone');

    useEffect(() => {
        fetch(`${API_URL}/stickers`)
            .then(r => r.ok ? r.json() : [])
            .then(data => {
                // Le sortăm după ID ca să fim siguri că Rank 1 e primul, Rank 2 al doilea etc.
                const sorted = data.sort((a, b) => a.id - b.id);
                setStickers(sorted);
            })
            .catch(() => setStickers([]));
    }, []);

    const currentLevel = child.progress ? child.progress.lastStickerId : 0;

    return (
        <div className="card" style={{ background: '#f8fafc', border: '2px solid #e2e8f0', marginBottom:'20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <h3 style={{ margin: 0, color: '#334155' }}>📚 Album Stickere</h3>
                <span className="badge" style={{ background: '#3b82f6', color: 'white', fontSize: '0.9rem' }}>
                    {currentLevel} / {stickers.length} Colectate
                </span>
            </div>

            <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(90px, 1fr))',
                gap: '10px',
                justifyItems: 'center'
            }}>
                {stickers.map((sticker, index) => {
                    // --- MODIFICAREA CRITICĂ ESTE AICI ---
                    // Calculăm nivelul real bazat pe poziția în listă (1, 2, 3...), nu pe ID-ul din baza de date
                    const level = index + 1;

                    const isUnlocked = level <= currentLevel;
                    const isNext = level === currentLevel + 1;

                    // Stiluri pentru card
                    let style = {
                        width: '100%', height: '100px', borderRadius: '8px',
                        display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center',
                        fontSize: '0.8rem', fontWeight: 'bold', position: 'relative',
                        transition: 'all 0.2s',
                        background: '#94a3b8', color: 'white', border: '1px solid #64748b', opacity: 0.6,
                        overflow: 'hidden', cursor: 'default'
                    };

                    // Stiluri dinamice
                    if (isUnlocked) {
                        style = { ...style, background: 'white', border: '2px solid #22c55e', color: '#15803d', opacity: 1 };
                    } else if (isNext) {
                        style = {
                            ...style, background: '#fef3c7', border: '2px dashed #d97706', color: '#b45309',
                            opacity: 1, transform: 'scale(1.05)', boxShadow: '0 0 10px rgba(245, 158, 11, 0.4)',
                            cursor: isLeader ? 'pointer' : 'default', zIndex: 10
                        };
                    }

                    return (
                        <div key={sticker.id} style={style} onClick={() => { if(isLeader && isNext) onUnlock(child.id); }}>
                            {sticker.imagePath ? (
                                <>
                                    <img
                                        src={sticker.imagePath}
                                        alt={sticker.name}
                                        style={{
                                            width: '100%', height: '100%', objectFit: 'contain',
                                            filter: isUnlocked ? 'none' : 'grayscale(100%) blur(1px)',
                                            opacity: isUnlocked ? 1 : 0.5
                                        }}
                                    />
                                    {!isUnlocked && (
                                        <div style={{position:'absolute', top:0, left:0, width:'100%', height:'100%', display:'flex', alignItems:'center', justifyContent:'center', fontSize:'2rem', zIndex:2}}>
                                            {isNext ? '🔓' : '🔒'}
                                        </div>
                                    )}
                                </>
                            ) : (
                                isUnlocked ? (
                                    <> <span style={{fontSize:'1.5rem'}}>✅</span> <span>{sticker.name}</span> </>
                                ) : (
                                    <>
                                        <span style={{fontSize:'1.5rem', marginBottom:'5px'}}>{isNext ? '🔓' : '🔒'}</span>
                                        <span>{isNext ? 'APASĂ!' : 'Blocat'}</span>
                                    </>
                                )
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

// ==========================================
// COMPONENTA NOUA: STICKERS HUB (Albumul cu Stickere)
// ==========================================
const StickersHub = ({ user }) => {
    const [children, setChildren] = useState([]);
    const [selectedChild, setSelectedChild] = useState(null);
    const [loading, setLoading] = useState(true);

    const isLeader = user && !user.hasOwnProperty('parentPhone');

    useEffect(() => {
        // Incarcam copiii
        fetch(`${API_URL}/children`)
            .then(r => r.ok ? r.json() : [])
            .then(data => {
                setChildren(data.sort((a,b) => a.name.localeCompare(b.name)));
                setLoading(false);
            })
            .catch(() => setLoading(false));
    }, []);

    // Functia de refresh doar pentru un copil (dupa ce deblochezi)
    const refreshChild = (childId) => {
        fetch(`${API_URL}/children/${childId}`).then(r=>r.json()).then(setSelectedChild);
    };

    const handleUnlockSticker = (childId) => {
        if(!window.confirm("Confirm: Copilul a spus lecția și deschidem lacătul?")) return;
        fetch(`${API_URL}/children/${childId}/unlock-next`, { method: 'POST' }).then(async res => {
            if(res.ok) {
                alert("🎉 " + await res.text());
                refreshChild(childId); // Reincarcam doar copilul curent
            }
            else alert("Eroare");
        });
    };

    if (loading) return <div className="animate-in"><p>Se încarcă albumele...</p></div>;

    // VEDEREA 2: ALBUMUL UNUI COPIL
    if (selectedChild) {
        return (
            <div className="animate-in">
                <button onClick={() => setSelectedChild(null)} className="btn-secondary" style={{ marginBottom: '20px' }}>
                    ⬅ Înapoi la Toți Copiii
                </button>

                <div className="card" style={{borderTop:'5px solid var(--accent)'}}>
                    <h2 style={{marginTop:0}}>Albumul lui {selectedChild.name} {selectedChild.surname}</h2>

                    {/* Folosim componenta StickerMap (design-ul cu lacăt) */}
                    <StickerMap
                        child={selectedChild}
                        user={user}
                        onUnlock={handleUnlockSticker}
                    />
                </div>
            </div>
        );
    }

    // VEDEREA 1: LISTA CU TOȚI COPIII
    return (
        <div className="animate-in">
            <h2>🏆 Albume Stickere</h2>
            <p style={{color:'#666', marginBottom:'20px'}}>Selectează un copil pentru a-i vedea sau debloca stickerele.</p>

            <div style={{display:'grid', gridTemplateColumns:'repeat(auto-fill, minmax(200px, 1fr))', gap:'15px'}}>
                {children.map(c => (
                    <div
                        key={c.id}
                        onClick={() => setSelectedChild(c)}
                        style={{
                            background:'white', padding:'20px', borderRadius:'15px',
                            border:'1px solid #eee', cursor:'pointer',
                            boxShadow:'0 2px 5px rgba(0,0,0,0.05)',
                            transition:'transform 0.2s', textAlign:'center'
                        }}
                        onMouseOver={e => e.currentTarget.style.transform = 'translateY(-3px)'}
                        onMouseOut={e => e.currentTarget.style.transform = 'translateY(0)'}
                    >
                        <div style={{fontSize:'2rem', marginBottom:'5px'}}>📒</div>
                        <h3 style={{margin:0, fontSize:'1.1rem', color:'var(--text-primary)'}}>{c.name} {c.surname}</h3>
                        <div style={{marginTop:'5px', fontSize:'0.9rem', color:'gray'}}>
                            Nivel: <strong>{c.progress ? c.progress.lastStickerId : 0}</strong>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

// ==========================================
// 3. DEPARTMENTS LIST (Actualizat pentru filtrare membri)
// ==========================================
const DepartmentsList = ({ user }) => {
    const [departments, setDepartments] = useState([]);
    const [leaders, setLeaders] = useState([]);
    const [meetings, setMeetings] = useState([]);

    const [selectedDept, setSelectedDept] = useState(null);
    const [deptMembers, setDeptMembers] = useState([]);
    const [scheduleData, setScheduleData] = useState({});

    // --- STATE NOU PENTRU HARTA LIDERILOR ---
    const [eligibleMap, setEligibleMap] = useState({});

    const isDirector = user.role === 'DIRECTOR' || user.role === 'COORDONATOR';

    const loadData = () => {
        fetch(`${API_URL}/departments`).then(r=>r.json()).then(setDepartments).catch(()=>{});
        fetch(`${API_URL}/leaders`).then(r=>r.json()).then(setLeaders).catch(()=>{});
        fetch(`${API_URL}/meetings`).then(r=>r.ok?r.json():[]).then(setMeetings).catch(()=>{});
    };

    useEffect(() => { loadData(); }, []);

    const handleSelectDept = (dept) => {
        setSelectedDept(dept);
        setDeptMembers([]);
        setScheduleData({});
        setEligibleMap({}); // Resetam harta temporar

        fetch(`${API_URL}/departments/${dept.id}/members`).then(r => r.ok ? r.json() : []).then(setDeptMembers);

        meetings.forEach(m => {
            fetch(`${API_URL}/departments/plan/${m.id}`).then(r => r.ok ? r.json() : null).then(data => {
                if(data) {
                    // 1. Salvam asignarile
                    if (data.assignments && data.assignments[dept.id]) {
                        setScheduleData(prev => ({ ...prev, [m.id]: data.assignments[dept.id] }));
                    }
                    // 2. Salvam harta cu lideri eligibili (Membrilor)
                    if (data.eligibleLeaders) {
                        setEligibleMap(prev => ({ ...prev, ...data.eligibleLeaders }));
                    }
                }
            });
        });
    };

    const handleAddLeader = (meetingId, leaderIdStr) => {
        if(!leaderIdStr) return;
        const leaderId = parseInt(leaderIdStr);

        fetch(`${API_URL}/departments/nominate`, {
            method: 'POST', headers: {'Content-Type':'application/json'},
            body: JSON.stringify({ meetingId, deptId: selectedDept.id, leaderId })
        }).then(res => {
            if(res.ok) {
                handleSelectDept(selectedDept);
                alert("✅ Propunere trimisă!");
            } else {
                alert("Eroare! Poate liderul e deja asignat.");
            }
        });
    };

    // ... handleRemove, handleResponse, setHeadLeader raman la fel ...
    const handleRemove = (meetingId, leaderId) => {
        if(!window.confirm("Ștergi liderul din tură?")) return;
        fetch(`${API_URL}/departments/remove?meetingId=${meetingId}&deptId=${selectedDept.id}&leaderId=${leaderId}`, { method: 'DELETE' })
            .then(res => { if(res.ok) handleSelectDept(selectedDept); else alert("Eroare la ștergere."); });
    };

    const handleResponse = (assignmentId, response) => {
        fetch(`${API_URL}/departments/respond`, {
            method: 'POST', headers: {'Content-Type':'application/json'},
            body: JSON.stringify({ assignmentId, response })
        }).then(res => { if(res.ok) { alert(response === 'ACCEPTED' ? "✅ Ai confirmat!" : "❌ Ai refuzat."); handleSelectDept(selectedDept); }});
    };

    const setHeadLeader = (deptId, leaderId) => {
        if(!leaderId) return;
        fetch(`${API_URL}/departments/${deptId}/set-head`, {
            method: 'POST', headers: {'Content-Type':'application/json'}, body: leaderId
        }).then(res => { if(res.ok) { alert("✅ Șef salvat!"); loadData(); setSelectedDept(null); } else alert("Eroare la salvare."); });
    };

    if(!departments.length) return <div className="animate-in"><p>Se încarcă departamentele...</p></div>;

    if (selectedDept) {
        const canEdit = isDirector || (selectedDept.headLeader && selectedDept.headLeader.id === user.id);

        // Luam lista de membri direct din Map-ul venit de la backend
        // Daca nu exista in map, folosim lista goala []
        const eligibleForThisDept = eligibleMap[selectedDept.id] || [];

        // Fallback: Daca map-ul e gol (inca se incarca), folosim deptMembers incarcat separat
        const displayLeaders = eligibleForThisDept.length > 0 ? eligibleForThisDept : deptMembers;

        return (
            <div className="animate-in">
                <button onClick={() => setSelectedDept(null)} className="btn-secondary" style={{marginBottom:'20px'}}>
                    ⬅ Înapoi la Listă
                </button>

                <div className="card" style={{borderLeft: '5px solid var(--accent)'}}>
                    <h1 style={{marginTop:0}}>{selectedDept.name}</h1>

                    <div style={{background:'#f0f9ff', padding:'15px', borderRadius:'10px', border:'1px solid #bae6fd', marginBottom:'20px'}}>
                        <h3 style={{marginTop:0, fontSize:'1rem', color:'#0369a1'}}>👑 Responsabil Departament</h3>
                        {selectedDept.headLeader ? (
                            <div style={{fontWeight:'bold', fontSize:'1.1rem'}}>👤 {selectedDept.headLeader.name} {selectedDept.headLeader.surname}</div>
                        ) : (
                            <p style={{fontStyle:'italic', color:'#666'}}>Niciun șef desemnat.</p>
                        )}
                        {isDirector && (
                            <div style={{marginTop:'10px', display:'flex', gap:'10px', alignItems:'center'}}>
                                <span style={{fontSize:'0.85rem'}}>Schimbă:</span>
                                <select className="login-input" style={{margin:0, padding:'5px', width:'auto', flex:1}} onChange={(e) => setHeadLeader(selectedDept.id, e.target.value)}>
                                    <option value="">-- Alege Lider --</option>
                                    {leaders.map(l => <option key={l.id} value={l.id}>{l.name} {l.surname}</option>)}
                                </select>
                            </div>
                        )}
                    </div>

                    <div style={{marginBottom:'25px'}}>
                        <h3 style={{borderBottom:'2px solid #eee', paddingBottom:'5px'}}>👥 Echipa Permanentă</h3>
                        {deptMembers.length === 0 ? <p style={{color:'#888', fontStyle:'italic'}}>Nu sunt membri înscriși permanent.</p> : (
                            <div style={{display:'flex', flexWrap:'wrap', gap:'10px'}}>
                                {deptMembers.map(m => (
                                    <span key={m.id} className="badge" style={{background:'#f3f4f6', color:'#374151', border:'1px solid #e5e7eb'}}>🔹 {m.name} {m.surname}</span>
                                ))}
                            </div>
                        )}
                    </div>

                    <h3>📅 Planificare Seri</h3>
                    <div style={{display:'flex', flexDirection:'column', gap:'15px'}}>
                        {meetings.slice(0, 5).map(m => {
                            const assigned = scheduleData[m.id] || [];
                            return (
                                <div key={m.id} style={{border:'1px solid #eee', padding:'15px', borderRadius:'10px', background:'white'}}>
                                    <div style={{fontWeight:'bold', borderBottom:'1px solid #eee', marginBottom:'10px', color:'var(--accent)'}}>
                                        {m.date} <span style={{fontWeight:'normal', color:'black'}}> - {m.description || 'Seară de Club'}</span>
                                    </div>

                                    <div style={{display:'flex', flexWrap:'wrap', gap:'10px', marginBottom:'10px'}}>
                                        {assigned.length === 0 && <span style={{fontSize:'0.85rem', color:'#aaa'}}>Nimeni planificat.</span>}
                                        {assigned.map(a => (
                                            <div key={a.id} style={{
                                                background: a.status==='ACCEPTED'?'#dcfce7':'#fff7ed',
                                                color: a.status==='ACCEPTED'?'#166534':'#9a3412',
                                                padding:'5px 10px', borderRadius:'15px',
                                                border: a.status==='ACCEPTED'?'1px solid #86efac':'1px solid #fdba74',
                                                display:'flex', alignItems:'center', gap:'5px', fontSize:'0.9rem'
                                            }}>
                                                <span>{a.leader ? a.leader.name + ' ' + a.leader.surname : '?'}</span>
                                                {a.status === 'PENDING' && <span title="În așteptare">⏳</span>}
                                                {canEdit && <button onClick={()=>handleRemove(m.id, a.leader.id)} style={{color:'red', background:'none', fontWeight:'bold', border:'none', cursor:'pointer', marginLeft:'5px'}}>×</button>}
                                                {a.status === 'PENDING' && a.leader && a.leader.id === user.id && (
                                                    <div style={{display:'flex', gap:'5px', marginLeft:'5px'}}>
                                                        <button onClick={()=>handleResponse(a.id, 'ACCEPTED')} style={{background:'green', color:'white', border:'none', borderRadius:'50%', width:'20px', height:'20px', cursor:'pointer'}}>✓</button>
                                                        <button onClick={()=>handleResponse(a.id, 'DECLINED')} style={{background:'red', color:'white', border:'none', borderRadius:'50%', width:'20px', height:'20px', cursor:'pointer'}}>✕</button>
                                                    </div>
                                                )}
                                            </div>
                                        ))}
                                    </div>

                                    {canEdit && (
                                        <div style={{marginTop:'10px', paddingTop:'10px', borderTop:'1px dashed #eee'}}>
                                            <label style={{fontSize:'0.85rem', fontWeight:'bold', display:'block', marginBottom:'3px'}}>Propune un membru:</label>

                                            {/* AICI FOLOSIM LISTA FILTRATĂ (displayLeaders) */}
                                            <select className="login-input" style={{padding:'8px', margin:0}} onChange={(e)=>{handleAddLeader(m.id, e.target.value); e.target.value='';}}>
                                                <option value="">+ Selectează membru</option>
                                                {displayLeaders
                                                    .filter(l => !assigned.some(a => a.leader?.id === l.id))
                                                    .sort((a,b) => a.name.localeCompare(b.name))
                                                    .map(l => (
                                                        <option key={l.id} value={l.id}>
                                                            {l.name} {l.surname}
                                                        </option>
                                                    ))
                                                }
                                            </select>
                                        </div>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                </div>
            </div>
        );
    }

    // ... renderul principal ramane neschimbat
    return (
        <div className="animate-in">
            <h2>🏢 Departamente</h2>
            <div style={{display:'grid', gridTemplateColumns:'repeat(auto-fill, minmax(250px, 1fr))', gap:'15px'}}>
                {departments.map(d => (
                    <div key={d.id} onClick={()=>handleSelectDept(d)} style={{padding:'20px', background:'white', borderRadius:'15px', cursor:'pointer', border:'1px solid #eee', boxShadow:'0 2px 5px rgba(0,0,0,0.05)', transition:'transform 0.2s'}}>
                        <h3 style={{margin:'0 0 5px 0', color:'var(--accent)'}}>{d.name}</h3>
                        <p style={{margin:0, fontSize:'0.9rem', color:'#666'}}>
                            {d.headLeader ? `👤 Șef: ${d.headLeader.name} ${d.headLeader.surname}` : '⚠️ Fără Șef'}
                        </p>
                    </div>
                ))}
            </div>
        </div>
    );
};

// ==========================================
// 4. LEADERS REGISTRY (LISTA CARDURI + DOSAR CLASIC)
// ==========================================
const LeadersRegistry = () => {
    const [leaders, setLeaders] = useState([]);
    const [selectedLeader, setSelectedLeader] = useState(null);
    const [feedbacks, setFeedbacks] = useState([]);
    const [loading, setLoading] = useState(true);

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

    if (loading) return <div className="animate-in"><p>Se incarca...</p></div>;

    // --- DOSAR DESCHIS ---
    if (selectedLeader) {
        return (
            <div className="animate-in">
                <button onClick={() => setSelectedLeader(null)} className="btn-secondary" style={{marginBottom:'20px'}}>
                    ⬅ Înapoi la Listă
                </button>

                <div className="card" style={{borderLeft: '5px solid var(--accent)'}}>
                    {/* Header Dosar: Nume + Rating */}
                    <div style={{display:'flex', justifyContent:'space-between', alignItems:'flex-start', flexWrap:'wrap', gap:'10px'}}>
                        <div>
                            <h1 style={{margin:0}}>{selectedLeader.name} {selectedLeader.surname}</h1>
                            <span className="badge" style={{background:'#e0e7ff', color:'var(--accent)', marginTop:'5px'}}>{selectedLeader.role || 'Lider'}</span>
                        </div>
                        <div style={{textAlign:'right'}}>
                            <div style={{color:'orange', fontSize:'1.5rem'}}>
                                {[1,2,3,4,5].map(s => (<span key={s} style={{opacity: s <= Math.round(selectedLeader.rating || 0) ? 1 : 0.3}}>★</span>))}
                            </div>
                            <div style={{fontSize:'1.2rem', fontWeight:'bold', color:'#555'}}>
                                {selectedLeader.rating ? selectedLeader.rating.toFixed(1) : '0.0'}
                            </div>
                        </div>
                    </div>

                    {/* Continut: Grid (2 col desktop, 1 col mobil) */}
                    <div className="registry-grid" style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'30px', marginTop:'30px'}}>
                        {/* Stanga: Contact */}
                        <div>
                            <h3>📋 Date de Contact</h3>
                            <p style={{marginTop:'10px'}}><strong>Telefon:</strong> <a href={`tel:${selectedLeader.phoneNumber}`} style={{textDecoration:'none', color:'var(--primary)'}}>{selectedLeader.phoneNumber || 'Nespecificat'}</a></p>

                            <h3 style={{marginTop:'20px'}}>🏷️ Departamente</h3>
                            <div style={{display:'flex', flexWrap:'wrap', gap:'8px', marginTop:'10px'}}>
                                {selectedLeader.departments && selectedLeader.departments.length > 0 ?
                                    (selectedLeader.departments.map(d => (<span key={d.id} className="badge" style={{background:'#dcfce7', color:'#166534'}}>{d.name}</span>)))
                                    : (<p style={{color:'#888', fontStyle:'italic'}}>Nu este inscris in niciun departament.</p>)}
                            </div>
                        </div>

                        {/* Dreapta: Feedback */}
                        <div>
                            <h3>⭐ Istoric Feedback</h3>
                            <div style={{background:'#fff', padding:'10px', borderRadius:'10px', border:'1px solid #eee', maxHeight:'400px', overflowY:'auto'}}>
                                {feedbacks.length === 0 ? <p style={{color:'#888', fontStyle:'italic'}}>Nu exista evaluari.</p> : (feedbacks.map(f => (
                                    <div key={f.id} style={{background:'#f9f9f9', padding:'10px', borderRadius:'8px', border:'1px solid #ddd', marginBottom:'8px', position:'relative'}}>
                                        <div style={{fontSize:'0.8rem', color:'#888'}}>📅 {f.date}</div>
                                        <div style={{color:'orange', fontSize:'1.2rem', margin:'5px 0'}}>{"★".repeat(f.rating)} <span style={{color:'black', fontSize:'0.9rem', opacity:0.5}}>({f.rating})</span></div>
                                        {f.comment && <div style={{fontStyle:'italic'}}>"{f.comment}"</div>}
                                        {f.comment && <button onClick={() => handleDeleteFeedback(f.id)} style={{position:'absolute', top:'5px', right:'5px', border:'none', background:'transparent', color:'red', cursor:'pointer'}}>🗑️</button>}
                                    </div>
                                )))}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    // --- LISTA LIDERI (Clasa leaders-table face magia) ---
    return (
        <div className="animate-in">
            <h2>👔 Registru Lideri</h2>
            <div className="table-container">
                <table className="leaders-table" style={{width:'100%', borderCollapse:'collapse'}}>
                    <thead><tr><th>Nume</th><th>Rol</th><th>Telefon</th><th>Media</th><th>Actiuni</th></tr></thead>
                    <tbody>
                    {leaders.map(l => (
                        <tr key={l.id}>
                            <td style={{fontWeight:'bold'}}>{l.name} {l.surname}</td>
                            <td><span className="badge" style={{background: l.role==='DIRECTOR'?'#fee2e2':'#f3f4f6', color: l.role==='DIRECTOR'?'#991b1b':'#374151'}}>{l.role}</span></td>
                            <td style={{fontFamily:'monospace'}}>{l.phoneNumber || '-'}</td>
                            <td><span style={{color:'orange', fontWeight:'bold'}}>★ {l.rating ? l.rating.toFixed(1) : '0.0'}</span></td>
                            <td><button onClick={() => handleSelectLeader(l)} style={{background:'var(--accent)', color:'white', padding:'5px 15px', borderRadius:'5px', border:'none', cursor:'pointer'}}>Dosar</button></td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

// ==========================================
// 5. SCORING WIDGET (Actualizat cu Puncte Manuale / Extra)
// ==========================================
const ScoringWidget = () => {
    const [children, setChildren] = useState([]);
    const [selectedChild, setSelectedChild] = useState(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [isSearching, setIsSearching] = useState(false);

    // Stari pentru butoanele standard
    const [pointsData, setPointsData] = useState({
        attended: false, hasBible: false, hasHandbook: false,
        lesson: false, friend: false, hasUniform: false
    });

    // NOU: Stare pentru punctele manuale (extra)
    const [extraPoints, setExtraPoints] = useState('');

    useEffect(() => {
        fetch(`${API_URL}/children`)
            .then(r => r.ok ? r.json() : [])
            .then(data => { if(Array.isArray(data)) setChildren(data.sort((a,b) => a.name.localeCompare(b.name))); })
            .catch(()=>{});
    }, []);

    const getFilteredChildren = () => {
        if (!searchTerm) return [];
        const term = searchTerm.toLowerCase().trim();
        return children.filter(c => {
            const nameNormal = `${c.name} ${c.surname}`.toLowerCase();
            const nameReverse = `${c.surname} ${c.name}`.toLowerCase();
            return nameNormal.includes(term) || nameReverse.includes(term);
        });
    };

    // ACTUALIZAT: Adunam si punctele manuale la total
    const calculateTotal = () => {
        let total = 0;
        if (pointsData.attended) total += 1000;
        if (pointsData.hasBible) total += 500;
        if (pointsData.hasHandbook) total += 500;
        if (pointsData.lesson) total += 1000;
        if (pointsData.friend) total += 1000;
        if (pointsData.hasUniform) total += 10000;

        // Adaugam punctele extra (daca a scris ceva valid)
        const extra = parseInt(extraPoints);
        if (!isNaN(extra)) {
            total += extra;
        }

        return total;
    };

    const togglePoint = (key) => {
        setPointsData(prev => ({ ...prev, [key]: !prev[key] }));
    };

    // ACTUALIZAT: Trimitem punctele extra catre server
    const handleSaveScore = () => {
        if (!selectedChild) return;

        const payload = {
            childId: selectedChild.id,
            ...pointsData,
            extraPoints: parseInt(extraPoints) || 0 // Trimitem cat a scris sau 0
        };

        fetch(`${API_URL}/scores/add`, {
            method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)
        })
            .then(async res => {
                if(res.ok) {
                    alert(`✅ Puncte salvate pentru ${selectedChild.name}!`);
                    // Resetam tot dupa salvare
                    setPointsData({ attended: false, hasBible: false, hasHandbook: false, lesson: false, friend: false, hasUniform: false });
                    setExtraPoints('');
                    setSelectedChild(null);
                    setSearchTerm('');
                    setIsSearching(false);
                } else alert("❌ EROARE: " + await res.text());
            }).catch(err => alert("❌ Eroare server!"));
    };

    const getButtonStyle = (isActive) => ({
        display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '15px', borderRadius: '10px',
        cursor: 'pointer', transition: 'all 0.2s', fontWeight: 'bold', fontSize: '1rem',
        border: '2px solid var(--accent)',
        background: isActive ? 'var(--accent)' : 'white',
        color: isActive ? 'white' : 'var(--accent)',
        boxShadow: isActive ? '0 4px 10px rgba(0,0,0,0.2)' : 'none'
    });

    return (
        <div className="animate-in" style={{padding:'20px', background:'white', borderRadius:'15px', boxShadow:'0 4px 15px rgba(0,0,0,0.05)'}}>
            <h3 style={{borderBottom:'2px solid var(--accent)', paddingBottom:'10px', marginBottom:'20px', marginTop:0}}>⭐ Acordare Puncte</h3>

            {/* ECRAN 1: STANDBY */}
            {!selectedChild && !isSearching && (
                <div style={{textAlign:'center', padding:'20px 0'}}>
                    <p style={{color:'#64748b', marginBottom:'20px'}}>Apasă mai jos pentru a căuta un copil.</p>
                    <button onClick={() => setIsSearching(true)} style={{background:'var(--accent)', color:'white', border:'none', padding:'15px 30px', borderRadius:'30px', fontSize:'1.1rem', fontWeight:'bold', boxShadow:'0 4px 10px rgba(67, 24, 255, 0.3)', width:'100%', maxWidth:'300px', cursor:'pointer'}}>
                        🔍 CAUTĂ UN COPIL
                    </button>
                </div>
            )}

            {/* ECRAN 2: CĂUTARE */}
            {!selectedChild && isSearching && (
                <div className="animate-in">
                    <div style={{display:'flex', gap:'10px', marginBottom:'15px'}}>
                        <input className="login-input" placeholder="Scrie nume..." value={searchTerm} autoFocus onChange={e => setSearchTerm(e.target.value)} style={{margin:0, flex:1}} />
                        <button onClick={() => { setIsSearching(false); setSearchTerm(''); }} style={{background:'var(--accent)', color:'white', border:'none', borderRadius:'10px', fontWeight:'bold', padding:'0 20px', cursor:'pointer'}}>Anulează</button>
                    </div>
                    <div style={{maxHeight:'250px', overflowY:'auto', border:'1px solid #f1f5f9', borderRadius:'10px'}}>
                        {searchTerm.length > 0 && getFilteredChildren().length === 0 ? <div style={{padding:'15px', color:'#94a3b8', textAlign:'center'}}>Niciun rezultat.</div> : getFilteredChildren().map(c => (
                            <div key={c.id} onClick={() => setSelectedChild(c)} style={{padding:'15px', borderBottom:'1px solid #f1f5f9', cursor:'pointer', display:'flex', justifyContent:'space-between', alignItems:'center', fontWeight:'bold', color:'var(--text-primary)'}}>
                                <span>{c.name} {c.surname}</span><span style={{color:'var(--accent)', fontSize:'0.9rem'}}>ALEGE ➜</span>
                            </div>
                        ))}
                        {searchTerm.length === 0 && <div style={{padding:'15px', color:'#94a3b8', textAlign:'center', fontStyle:'italic'}}>Începe să scrii numele...</div>}
                    </div>
                </div>
            )}

            {/* ECRAN 3: PUNCTAJ */}
            {selectedChild && (
                <div className="animate-in">
                    <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'15px'}}>
                        <h2 style={{margin:0, color:'var(--text-primary)', fontSize:'1.3rem'}}>{selectedChild.name} {selectedChild.surname}</h2>
                        <button onClick={() => setSelectedChild(null)} style={{background:'var(--accent)', color:'white', padding:'8px 15px', borderRadius:'8px', cursor:'pointer', fontWeight:'bold', fontSize:'0.9rem', border:'none'}}>✕ Închide</button>
                    </div>

                    <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'10px'}}>
                        <div style={{gridColumn:'span 2', ...getButtonStyle(pointsData.attended)}} onClick={() => togglePoint('attended')}>
                            ✅ PREZENT
                        </div>

                        {[
                            {k:'hasBible', l:'📖 Biblie'}, {k:'friend', l:'👋 Prieten'},
                            {k:'hasHandbook', l:'📘 Manual'}, {k:'lesson', l:'📝 Lecție gata'}
                        ].map(item => (
                            <div key={item.k} style={getButtonStyle(pointsData[item.k])} onClick={() => togglePoint(item.k)}>
                                {item.l}
                            </div>
                        ))}

                        <div style={{gridColumn:'span 2', ...getButtonStyle(pointsData.hasUniform)}} onClick={() => togglePoint('hasUniform')}>
                            👕 Costum Awana special (+10.000)
                        </div>
                    </div>

                    {/* NOU: INPUT PENTRU PUNCTE MANUALE / EXTRA */}
                    <div style={{marginTop: '15px', background: '#f8fafc', padding: '15px', borderRadius: '10px', border: '2px dashed #cbd5e1', display: 'flex', alignItems: 'center', gap: '15px'}}>
                        <div style={{fontSize: '2rem'}}>✨</div>
                        <div style={{flex: 1}}>
                            <label style={{display: 'block', fontWeight: 'bold', color: '#334155', marginBottom: '5px'}}>Puncte Extra (Manuale)</label>
                            <input
                                type="number"
                                value={extraPoints}
                                onChange={(e) => setExtraPoints(e.target.value)}
                                placeholder="Ex: 1000 (pt a 2-a lecție)"
                                style={{width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #94a3b8', fontSize: '1rem', margin: 0}}
                            />
                        </div>
                    </div>

                    {/* TOTAL SI SALVARE */}
                    <div style={{marginTop:'25px', textAlign:'center', paddingTop:'15px', borderTop:'1px solid #eee'}}>
                        <h4 style={{margin:'0 0 15px 0', fontSize:'1.2rem', color:'#64748b'}}>Total de adăugat: <span style={{color:'var(--accent)', fontSize:'1.8rem', fontWeight:'bold'}}>{calculateTotal()}</span></h4>
                        <button onClick={handleSaveScore} style={{background:'#15803d', color:'white', padding:'15px', width:'100%', borderRadius:'12px', fontWeight:'bold', fontSize:'1.1rem', border:'none', cursor:'pointer', boxShadow:'0 4px 10px rgba(21, 128, 61, 0.3)'}}>
                            💾 SALVEAZĂ PUNCTELE
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

// ==========================================
// 6. TEAMS MANAGER (MODIFICARE VERIFICARE SERVER)
// ==========================================
const TeamsManager = () => {
    // ... (restul state-urilor raman la fel: mode, myColor etc.) ...
    const [mode, setMode] = useState('selection');
    const [myColor, setMyColor] = useState('red');
    const [availableKids, setAvailableKids] = useState([]);
    const [myTeamMembers, setMyTeamMembers] = useState([]);
    const [scores, setScores] = useState({ individual: 0, game: 0, total: 0 });
    const [ranking, setRanking] = useState([]);
    const [isDouble, setIsDouble] = useState(false);
    const [manualPoints, setManualPoints] = useState('');
    const [manualTeam, setManualTeam] = useState('red');
    const [searchAvailable, setSearchAvailable] = useState('');

    // --- STATE PENTRU SECURITATE ---
    const [showPinModal, setShowPinModal] = useState(false);
    const [pinInput, setPinInput] = useState('');
    const [loadingPin, setLoadingPin] = useState(false); // State nou pentru incarcare

    // ... (restul constantelor teams, teamStyles, refreshData, useEffect raman la fel) ...
    const teams = ['red', 'blue', 'green', 'yellow'];
    const teamStyles = { red: { bg: '#fee2e2', border: '#ef4444', text: '#b91c1c' }, blue: { bg: '#dbeafe', border: '#3b82f6', text: '#1d4ed8' }, green: { bg: '#dcfce7', border: '#22c55e', text: '#15803d' }, yellow: { bg: '#fef9c3', border: '#eab308', text: '#a16207' } };
    const currentTheme = teamStyles[myColor];

    const refreshData = () => { fetch(`${API_URL}/teams/available`).then(r => r.ok?r.json():[]).then(setAvailableKids).catch(()=>{}); fetch(`${API_URL}/teams/status/${myColor}`).then(r => r.ok?r.json():null).then(data => { if(data) { setMyTeamMembers(data.members || []); setScores({ individual: data.individualScore || 0, game: data.gameScore || 0, total: data.totalScore || 0 }); } }).catch(()=>{}); };
    useEffect(() => { refreshData(); const interval = setInterval(refreshData, 2000); return () => clearInterval(interval); }, [myColor]);

    const handleAccessGames = () => {
        if (mode === 'games') return;
        setShowPinModal(true);
        setPinInput('');
    };

    // --- MODIFICARE AICI: VERIFICARE CU SERVERUL ---
    const verifyPin = () => {
        if (!pinInput) return alert("Scrie codul!");

        setLoadingPin(true);

        fetch(`${API_URL}/meetings/check-pin`, {
            method: 'POST',
            headers: {'Content-Type':'application/json'},
            body: JSON.stringify({ pin: pinInput })
        })
            .then(async res => {
                setLoadingPin(false);
                if (res.ok) {
                    // COD CORECT
                    setShowPinModal(false);
                    setMode('games');
                } else {
                    // COD GRESIT
                    alert("❌ Cod Incorect! Cere codul de la Director/Mesaje.");
                    setPinInput('');
                }
            })
            .catch(() => {
                setLoadingPin(false);
                alert("Eroare conexiune server.");
            });
    };

    // ... (restul funcțiilor pickChild, removeChild, sendPoints raman IDENTICE) ...
    const pickChild = (childId) => { fetch(`${API_URL}/teams/pick`, { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({ childId: childId, teamColor: myColor }) }).then(async res => { if(res.ok) refreshData(); else alert(await res.text()); }); };
    const removeChildFromTeam = (child) => { if(!window.confirm(`Îl scoți pe ${child.name} din echipa ${myColor.toUpperCase()}?`)) return; fetch(`${API_URL}/teams/remove`, { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({ childId: child.id }) }).then(async res => { if(res.ok) { refreshData(); } else { alert("Eroare: " + await res.text()); }}); };
    const handleTeamPress = (c) => { if(!ranking.includes(c)) setRanking([...ranking, c]); };
    const sendGamePoints = () => { if(ranking.length===0) return alert("Selecteaza ordinea!"); fetch(`${API_URL}/teams/game-round`, { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({ ranking, isDouble }) }).then(res => { if(res.ok) { alert("✅ Clasament salvat!"); setRanking([]); setIsDouble(false); refreshData(); } }); };
    const sendManualPoints = () => { if(!manualPoints || isNaN(manualPoints)) return alert("Introdu un număr valid!"); fetch(`${API_URL}/teams/add-manual-points`, { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({ teamColor: manualTeam, points: parseInt(manualPoints) }) }).then(async res => { if(res.ok) { alert(`✅ ${await res.text()}`); setManualPoints(''); refreshData(); } else { alert("Eroare server: " + await res.text()); } }); };
    const getPoints = (i) => { const pts=[1000,500,300,100]; return i<4 ? (isDouble?pts[i]*2:pts[i]) : 0; };
    const getFilteredAvailable = () => { if(!searchAvailable) return availableKids; const term = searchAvailable.toLowerCase(); return availableKids.filter(c => c.name.toLowerCase().includes(term) || c.surname.toLowerCase().includes(term)); };

    return (
        <div className="animate-in" style={{padding:'20px', background:'white', borderRadius:'15px', position:'relative'}}>

            {/* --- MODAL PIN --- */}
            {showPinModal && (
                <div style={{position:'fixed', top:0, left:0, width:'100%', height:'100%', background:'rgba(0,0,0,0.8)', zIndex:1000, display:'flex', justifyContent:'center', alignItems:'center'}}>
                    <div className="animate-in" style={{background:'white', padding:'30px', borderRadius:'15px', width:'90%', maxWidth:'320px', textAlign:'center', boxShadow:'0 10px 25px rgba(0,0,0,0.5)'}}>
                        <div style={{fontSize:'3rem', marginBottom:'10px'}}>🔒</div>
                        <h3 style={{margin:0, color:'#333'}}>Acces Restricționat</h3>
                        <p style={{color:'#666', marginBottom:'20px'}}>Verifică-ți mesajele pentru codul serii.</p>

                        <input
                            type="password" inputMode="numeric" pattern="[0-9]*" maxLength="4" autoFocus placeholder="Cod PIN"
                            value={pinInput} onChange={(e) => setPinInput(e.target.value)}
                            style={{width:'100%', padding:'15px', fontSize:'1.5rem', textAlign:'center', letterSpacing:'5px', borderRadius:'10px', border:'2px solid #ddd', marginBottom:'20px'}}
                        />

                        <div style={{display:'flex', gap:'10px'}}>
                            <button onClick={() => setShowPinModal(false)} style={{flex:1, padding:'12px', background:'#f1f5f9', color:'#64748b', border:'none', borderRadius:'8px', fontWeight:'bold', cursor:'pointer'}}>Anulează</button>
                            <button onClick={verifyPin} disabled={loadingPin} style={{flex:1, padding:'12px', background:'var(--accent)', color:'white', border:'none', borderRadius:'8px', fontWeight:'bold', cursor:'pointer'}}>
                                {loadingPin ? '...' : 'Confirmă'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* ... (RESTUL HTML-ULUI PENTRU SELECTIE SI JOCURI RAMANE EXACT LA FEL CA INAINTE) ... */}
            <div style={{display:'flex', gap:'10px', marginBottom:'20px'}}>
                <button onClick={()=>setMode('selection')} style={{flex:1, padding:'15px', borderRadius:'10px', border:'none', background:mode==='selection'?'var(--accent)':'#eee', color:mode==='selection'?'white':'#666', fontWeight:'bold'}}>1. Selectie Echipa</button>
                <button onClick={handleAccessGames} style={{flex:1, padding:'15px', borderRadius:'10px', border:'none', background:mode==='games'?'var(--accent)':'#eee', color:mode==='games'?'white':'#666', fontWeight:'bold', display:'flex', alignItems:'center', justifyContent:'center', gap:'5px'}}>{mode !== 'games' && <span>🔒</span>} 2. Punctaje Jocuri</button>
            </div>

            {mode === 'selection' && (
                <div className="teams-container" style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'20px'}}>
                    <div style={{gridColumn:'span 2', display:'flex', gap:'5px', overflowX:'auto', paddingBottom:'10px'}}>
                        {teams.map(c => <button key={c} onClick={()=>setMyColor(c)} style={{flex:1, padding:'10px', borderRadius:'8px', fontWeight:'bold', textTransform:'uppercase', border:'none', background:myColor===c?teamStyles[c].border:'#f3f4f6', color:myColor===c?'white':'#666'}}>{c}</button>)}
                    </div>
                    <div className="card available-kids-section" style={{height:'500px', display:'flex', flexDirection:'column', border:'2px solid #cbd5e1', boxShadow:'0 4px 6px -1px rgba(0, 0, 0, 0.1)', background:'#f8fafc'}}>
                        <div style={{borderBottom:'1px solid #e2e8f0', paddingBottom:'10px', marginBottom:'5px'}}>
                            <h3 style={{marginTop:0, color:'#334155', marginBottom:'5px'}}>👶 Disponibili ({availableKids.length})</h3>
                            <input className="login-input" placeholder="🔍 Caută nume..." value={searchAvailable} onChange={e => setSearchAvailable(e.target.value)} style={{margin:0, padding:'8px', fontSize:'0.9rem'}} />
                        </div>
                        <div className="available-kids-scroll" style={{overflowY:'auto', flex:1, paddingRight:'5px'}}>
                            {getFilteredAvailable().map(c => (
                                <div key={c.id} onClick={()=>pickChild(c.id)} className="kid-card-horizontal" style={{padding:'12px', marginBottom:'8px', cursor:'pointer', display:'flex', justifyContent:'space-between', alignItems:'center', background:'white', borderRadius:'8px', border:'1px solid #e2e8f0', transition:'transform 0.1s', boxShadow:'0 1px 2px rgba(0,0,0,0.05)'}}>
                                    <strong style={{fontSize:'1rem'}}>{c.name} {c.surname}</strong><span style={{background:'#dcfce7', color:'#166534', borderRadius:'50%', width:'24px', height:'24px', display:'flex', alignItems:'center', justifyContent:'center', fontWeight:'bold'}}>+</span>
                                </div>
                            ))}
                            {searchAvailable && getFilteredAvailable().length === 0 && <div style={{textAlign:'center', color:'#94a3b8', padding:'10px'}}>Nu am găsit.</div>}
                        </div>
                    </div>
                    <div className="card my-team-section" style={{height:'300px', display:'flex', flexDirection:'column', background:currentTheme.bg, border:`2px solid ${currentTheme.border}`}}>
                        <h3 style={{marginTop:0, color:currentTheme.text, textTransform:'uppercase', fontSize:'1rem'}}>Echipa Mea</h3>
                        <div style={{fontSize:'2rem', fontWeight:'bold', color:currentTheme.text, lineHeight:1}}>{scores.total} <span style={{fontSize:'0.9rem'}}>pct</span></div>
                        <div style={{fontSize:'0.75rem', marginBottom:'10px', color:currentTheme.text, opacity:0.8}}>(Indiv: {scores.individual} + Joc: {scores.game})</div>
                        <div style={{overflowY:'auto', flex:1, background:'rgba(255,255,255,0.5)', borderRadius:'8px', padding:'5px'}}>
                            {myTeamMembers.length === 0 && <div style={{padding:'10px', fontSize:'0.8rem', fontStyle:'italic', color:currentTheme.text}}>Niciun membru încă.</div>}
                            {myTeamMembers.map(c => (
                                <div key={c.id} onClick={() => removeChildFromTeam(c)} style={{padding:'6px 8px', borderBottom:`1px solid ${currentTheme.text}20`, color:currentTheme.text, fontSize:'0.9rem', cursor:'pointer', display:'flex', justifyContent:'space-between'}} title="Apasă pentru a scoate din echipă">
                                    <span>✅ {c.name} {c.surname}</span><span style={{fontWeight:'bold', opacity:0.6}}>✕</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            )}

            {mode === 'games' && (
                <div style={{display:'flex', flexDirection:'column', gap:'20px'}}>
                    <div className="card">
                        <div style={{display:'flex', justifyContent:'space-between', marginBottom:'20px'}}>
                            <h2 style={{margin:0}}>🎮 Punctaje Jocuri</h2>
                            <button onClick={()=>setIsDouble(!isDouble)} style={{padding:'10px 20px', background: isDouble ? '#f59e0b' : '#000000', color: 'white', border:'none', borderRadius:'10px', fontWeight:'bold', boxShadow:'0 2px 5px rgba(0,0,0,0.2)'}}>{isDouble ? 'DUBLU' : 'NORMAL'}</button>
                        </div>
                        <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'15px', marginBottom:'30px'}}>
                            {teams.map(c => {
                                const idx = ranking.indexOf(c);
                                return <button key={c} onClick={()=>handleTeamPress(c)} disabled={idx!==-1} style={{padding:'30px', borderRadius:'15px', border:'none', fontSize:'1.5rem', fontWeight:'bold', textTransform:'uppercase', background: idx!==-1 ? '#333' : teamStyles[c].border, color:'white', opacity: idx!==-1 ? 0.6 : 1, boxShadow:'0 4px 0 rgba(0,0,0,0.2)'}}>
                                    {c} {idx!==-1 && <div style={{fontSize:'1rem', color:'#fbbf24', marginTop:'5px'}}>LOCUL {idx+1} (+{getPoints(idx)})</div>}
                                </button>
                            })}
                        </div>
                        <div style={{display:'flex', gap:'10px'}}>
                            <button onClick={()=>setRanking([])} style={{flex:1, padding:'15px', background:'#ef4444', color:'white', border:'none', borderRadius:'10px', fontWeight:'bold'}}>🗑️ RESET</button>
                            <button onClick={sendGamePoints} style={{flex:2, padding:'15px', background:'#22c55e', color:'white', border:'none', borderRadius:'10px', fontWeight:'bold'}}>✅ TRIMITE</button>
                        </div>
                        <div style={{marginTop:'30px', paddingTop:'20px', borderTop:'2px dashed #eee'}}>
                            <h3 style={{margin:'0 0 15px 0', fontSize:'1.1rem', color:'#64748b'}}>✍️ Puncte Bonus</h3>
                            <div style={{display:'flex', gap:'10px', flexWrap:'wrap'}}>
                                <select className="login-input" style={{margin:0, flex:1, minWidth:'100px'}} value={manualTeam} onChange={e => setManualTeam(e.target.value)}>{teams.map(t => <option key={t} value={t}>{t.toUpperCase()}</option>)}</select>
                                <input type="number" className="login-input" placeholder="Pct (ex: 3200)" style={{margin:0, flex:1, minWidth:'120px'}} value={manualPoints} onChange={e => setManualPoints(e.target.value)} />
                                <button onClick={sendManualPoints} style={{background:'#22c55e', color:'white', border:'none', borderRadius:'10px', padding:'12px 20px', fontWeight:'bold', cursor:'pointer', boxShadow:'0 2px 5px rgba(34, 197, 94, 0.4)'}}>✅ TRIMITE</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};


// ==========================================
// 7. DEPARTMENTS PLAN (FINAL)
// ==========================================
const DepartmentsPlan = ({ meetingId, user, onConfirm, isConfirmed }) => {
    const [meetings, setMeetings] = useState([]);
    const [selectedMeetingId, setSelectedMeetingId] = useState(meetingId);
    const [departments, setDepartments] = useState([]);
    const [leaders, setLeaders] = useState([]);
    const [plan, setPlan] = useState({ assignments: {}, directorDay: null });

    const isDirector = user && (user.role === 'DIRECTOR' || user.role === 'COORDONATOR');
    const directorsList = leaders.filter(l => l.role === 'DIRECTOR' || l.role === 'COORDONATOR');

    useEffect(() => { fetch(`${API_URL}/meetings`).then(r=>r.ok?r.json():[]).then(setMeetings); fetch(`${API_URL}/departments`).then(r=>r.ok?r.json():[]).then(setDepartments); fetch(`${API_URL}/leaders`).then(r=>r.ok?r.json():[]).then(setLeaders); }, []);

    useEffect(() => { const idToUse = meetingId || selectedMeetingId; if(!idToUse) return; fetch(`${API_URL}/departments/plan/${idToUse}`).then(r=>r.ok?r.json():null).then(d=>{ if(d) setPlan(d); }); }, [meetingId, selectedMeetingId]);

    const assignLeader = (deptId, leaderIdStr) => { if(!leaderIdStr) return; const idToUse = meetingId || selectedMeetingId; const leaderId = parseInt(leaderIdStr); fetch(`${API_URL}/departments/assign`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({ meetingId: idToUse, deptId, leaderId }) }).then(res => { if (res.ok) { const newPlan = { ...plan }; if(!newPlan.assignments[deptId]) newPlan.assignments[deptId] = []; const foundLeader = leaders.find(l=>l.id===leaderId); newPlan.assignments[deptId].push({ leader: foundLeader, id: 'temp' + Date.now() }); setPlan(newPlan); } else alert("Eroare la asignare."); }); };
    const removeAssignment = (deptId, leaderId) => { const idToUse = meetingId || selectedMeetingId; fetch(`${API_URL}/departments/remove?meetingId=${idToUse}&deptId=${deptId}&leaderId=${leaderId}`, { method: 'DELETE' }).then(res => { if (res.ok) { const newPlan = { ...plan }; if (newPlan.assignments[deptId]) { newPlan.assignments[deptId] = newPlan.assignments[deptId].filter(item => item.leader && item.leader.id !== leaderId); setPlan(newPlan); } } }); };
    const setDirector = (leaderId) => { if(!leaderId) return; const idToUse = meetingId || selectedMeetingId; fetch(`${API_URL}/departments/director/${idToUse}`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: parseInt(leaderId) }).then(res => { if(res.ok) setPlan(prev => ({...prev, directorDay: leaders.find(l=>l.id===parseInt(leaderId))})); }); };

    return (
        <div className="animate-in">
            <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'20px'}}>
                <h2 style={{margin:0}}>📋 Planificare Departamente</h2>
                {meetingId && isDirector && (<button onClick={onConfirm} style={{background: isConfirmed ? '#f3f4f6' : 'var(--success)', color: isConfirmed ? '#333' : 'white', padding:'10px 20px', borderRadius:'10px', fontWeight:'bold', border:'none', cursor:'pointer'}}>{isConfirmed ? '✏️ Editeaza Organizarea' : '✅ Salveaza & Continua'}</button>)}
            </div>
            {!meetingId && (<div className="card" style={{marginBottom:'20px'}}><label style={{fontWeight:'bold'}}>Alege Intalnirea:</label><select className="login-input" onChange={e => setSelectedMeetingId(e.target.value)}><option value="">-- Alege data --</option>{meetings.map(m => <option key={m.id} value={m.id}>{m.date} - {m.description}</option>)}</select></div>)}
            {(meetingId || selectedMeetingId) && (
                <div style={{opacity: (isConfirmed && meetingId) ? 0.7 : 1, pointerEvents: (isConfirmed && meetingId) ? 'none' : 'auto'}}>
                    <div className="card" style={{marginBottom:'20px', background:'#fff7ed', border:'1px solid #ffedd5'}}><h3>👑 Director de Zi</h3><select className="login-input" onChange={e => setDirector(e.target.value)} value={plan.directorDay?.id || ''} disabled={!isDirector && !meetingId}><option value="">-- {isDirector || meetingId ? 'Alege Director' : 'Neselectat'} --</option>{directorsList.map(l => (<option key={l.id} value={l.id}>{l.name} {l.surname}</option>))}</select>{!isDirector && plan.directorDay && <p style={{fontWeight:'bold', marginTop:'5px'}}>Azi conduce: {plan.directorDay.name} {plan.directorDay.surname}</p>}</div>

                    {/* CLASA departments-grid (GRID 1 col pe Mobil / 2 col Desktop) */}
                    <div className="departments-grid" style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'20px'}}>
                        {departments.map(dept => (
                            <div key={dept.id} className="card dept-card" style={{position:'relative'}}>
                                <div style={{display:'flex', justifyContent:'space-between', alignItems:'center'}}><h3 style={{margin:0, fontSize:'1.2rem'}}>{dept.name}</h3><span style={{fontSize:'0.8rem', background:'#eee', padding:'3px 8px', borderRadius:'10px'}}>Necesar: {dept.minLeaders}-{dept.maxLeaders}</span></div>
                                <div className="dept-leaders-list" style={{margin:'15px 0', minHeight:'40px'}}>
                                    {plan.assignments[dept.id]?.map(assignment => {
                                        const l = assignment.leader;
                                        if(!l) return null;
                                        return (
                                            <div key={assignment.id || l.id} style={{padding:'8px', borderBottom:'1px solid #eee', display:'flex', justifyContent:'space-between', alignItems:'center'}}>
                                                <span style={{fontWeight:'bold'}}>👤 {l.name} {l.surname}</span>
                                                {(isDirector || (meetingId && isDirector)) && (<button onClick={() => removeAssignment(dept.id, l.id)} style={{color:'red', fontWeight:'bold', border:'none', background:'transparent', cursor:'pointer'}}>×</button>)}
                                            </div>
                                        );
                                    })}
                                </div>
                                {(isDirector || (meetingId && isDirector)) && (
                                    <select className="login-input" style={{padding:'8px', fontSize:'0.9rem'}} onChange={(e) => { assignLeader(dept.id, e.target.value); e.target.value=''; }}>
                                        <option value="">+ Adauga Lider</option>
                                        {leaders.map(l => { const isAssignedHere = plan.assignments[dept.id]?.some(existing => existing.leader && existing.leader.id === l.id); if(isAssignedHere) return null; return <option key={l.id} value={l.id}>{l.name} {l.surname}</option>; })}
                                    </select>
                                )}
                                {dept.headLeader && (<div style={{marginTop:'10px', fontSize:'0.75rem', color:'var(--accent)', fontWeight:'bold', textAlign:'right'}}>Responsabil: {dept.headLeader.name} {dept.headLeader.surname}</div>)}
                            </div>
                        ))}</div>
                </div>
            )}
        </div>
    );
};

// ==========================================
// 8. CALENDAR MANAGER (FINAL)
// ==========================================
const CalendarManager = ({ user }) => {
    const [meetings, setMeetings] = useState([]);
    const [activeSession, setActiveSession] = useState(null);
    const [showFeedbackModal, setShowFeedbackModal] = useState(false);
    const [activeTab, setActiveTab] = useState('plan');
    const [isOrganized, setIsOrganized] = useState(false);
    const [newDate, setNewDate] = useState('');
    const [newDesc, setNewDesc] = useState('');

    const isDirector = user.role === 'DIRECTOR' || user.role === 'COORDONATOR';

    const loadMeetings = () => { fetch(`${API_URL}/meetings`).then(r => r.ok?r.json():[]).then(setMeetings).catch(()=>{}); };
    useEffect(() => { loadMeetings(); }, []);

    const addMeeting = () => { if(!newDate) return alert("Alege o data!"); fetch(`${API_URL}/meetings/add`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({ date: newDate, description: newDesc }) }).then(() => { loadMeetings(); setNewDate(''); setNewDesc(''); }); };

    const finalizeClose = () => {
        fetch(`${API_URL}/meetings/close/${activeSession.id}`, { method: 'POST' }).then(async res => {
            if(res.ok) { alert(await res.text()); setShowFeedbackModal(false); setActiveSession(null); loadMeetings(); }
            else alert("Eroare la inchidere.");
        });
    };

    if (showFeedbackModal) return <MeetingFeedback meeting={activeSession} user={user} onComplete={finalizeClose} onCancel={() => setShowFeedbackModal(false)} />;

    if (activeSession) {
        return (
            <div className="animate-in">
                {/* CLASE NOI PENTRU HEADER SESIUNE */}
                <div className="session-header" style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'20px'}}>
                    <div className="session-title-wrapper">
                        <h2 className="session-title" style={{margin:0}}>📅 {activeSession.date}</h2>
                    </div>

                    <div className="session-buttons" style={{display:'flex', gap:'10px'}}>
                        <button onClick={() => setActiveSession(null)} className="btn-secondary session-btn-small" style={{marginBottom:0}}>
                            ⬅ Inapoi
                        </button>
                        {isDirector && (
                            <button
                                onClick={()=>{ if(!window.confirm("🚨 ATENTIE!\n\nInchizi seara?\n\nContinui?")) return; setShowFeedbackModal(true) }}
                                style={{background:'#ef4444', color:'white', padding:'10px 20px', borderRadius:'10px', fontWeight:'bold', border:'2px solid #b91c1c', cursor:'pointer', boxShadow: '0 4px 6px rgba(0,0,0,0.2)'}}
                                className="session-btn-small"
                            >
                                🏁 INCHEIE SEARA
                            </button>
                        )}
                    </div>
                </div>

                <div style={{display:'flex', gap:'10px', marginBottom:'20px'}}>
                    <button onClick={() => setActiveTab('plan')} style={{flex:1, padding:'10px', background:activeTab==='plan'?'var(--accent)':'white', color:activeTab==='plan'?'white':'black', borderRadius:'10px', border:'none', fontWeight:'bold'}}>📋 Organizare</button>
                    {(isOrganized || !isDirector) && (<><button onClick={() => setActiveTab('scoring')} style={{flex:1, padding:'10px', background:activeTab==='scoring'?'var(--accent)':'white', color:activeTab==='scoring'?'white':'black', borderRadius:'10px', border:'none', fontWeight:'bold'}}>⭐ Scoring</button><button onClick={() => setActiveTab('teams')} style={{flex:1, padding:'10px', background:activeTab==='teams'?'var(--accent)':'white', color:activeTab==='teams'?'white':'black', borderRadius:'10px', border:'none', fontWeight:'bold'}}>🚩 Echipe</button></>)}
                </div>
                {activeTab === 'plan' && <DepartmentsPlan meetingId={activeSession.id} user={user} isConfirmed={isOrganized} onConfirm={() => { setIsOrganized(!isOrganized); if(!isOrganized) setActiveTab('scoring'); }} />}
                {activeTab === 'scoring' && <ScoringWidget />}
                {activeTab === 'teams' && <TeamsManager />}
            </div>
        );
    }

    return (
        <div className="animate-in">
            <h2>📅 Calendar & Sesiuni Active</h2>
            {isDirector && (<div className="card" style={{marginBottom:'20px', background:'#f0f9ff', border:'1px solid #bae6fd'}}><h3>➕ Programeaza</h3><div style={{display:'flex', gap:'10px', marginTop:'10px'}}><input type="date" className="login-input" value={newDate} onChange={e=>setNewDate(e.target.value)} /><input type="text" placeholder="Descriere" className="login-input" value={newDesc} onChange={e=>setNewDesc(e.target.value)} /><button onClick={addMeeting} style={{background:'var(--accent)', color:'white', padding:'0 20px', borderRadius:'10px', fontWeight:'bold'}}>Adauga</button></div></div>)}
            <div className="card"><h3>Urmeaza...</h3>{meetings.length === 0 ? <p style={{color:'#888'}}>Nu exista intalniri viitoare.</p> : (
                <div style={{marginTop:'15px'}}>
                    {meetings.map(m => (
                        /* CLASA meeting-item */
                        <div key={m.id} className="meeting-item" style={{display:'flex', justifyContent:'space-between', alignItems:'center', padding:'15px', borderBottom:'1px solid #eee'}}>
                            <div><div style={{fontSize:'1.1rem', fontWeight:'bold'}}>📅 {m.date}</div><div style={{color:'gray', fontSize:'0.9rem'}}>{m.description || 'Standard'}</div></div>
                            <div className="meeting-actions" style={{display:'flex', gap:'10px'}}>
                                {isDirector && (<button onClick={() => { setActiveSession(m); setActiveTab('plan'); }} style={{background:'#fff', color:'var(--accent)', border:'1px solid var(--accent)', padding:'8px 15px', borderRadius:'8px', fontWeight:'bold', cursor:'pointer'}}>✏️ Planifica</button>)}
                                <button onClick={() => setActiveSession(m)} style={{background:'var(--success)', color:'white', padding:'10px 20px', borderRadius:'8px', fontWeight:'bold', border:'none', cursor:'pointer'}}>▶️ INTRA</button>
                            </div>
                        </div>
                    ))}
                </div>
            )}</div>
        </div>
    );
};


// ==========================================
// 9. REGISTRY (FINAL - LAYOUT MODERN LISTĂ PE MOBIL)
// ==========================================
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
            .then(res => { if (res.ok) { alert("✅ Atribuit!"); loadChildren(); openChildFile(selectedChild); }});
    };

    const handleAddWarning = () => {
        if (!newWarning.description) return alert("Scrie motiv!");
        const payload = { childId: selectedChild.id, ...newWarning, remainingMeetings: newWarning.suspension ? newWarning.remainingMeetings : 0 };
        fetch(`${API_URL}/warnings/add`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(payload) })
            .then(res => { if(res.ok) { alert("✅ Salvat!"); openChildFile(selectedChild); setNewWarning({description:'', suspension:false, remainingMeetings:1}); }});
    };

    if (loading) return <p>Se incarca...</p>;

    if (selectedChild) {
        const isCurrentlySuspended = Array.isArray(childWarnings) && childWarnings.length > 0 && childWarnings[0].suspension && childWarnings[0].remainingMeetings > 0;

        return (
            <div className="animate-in">
                <button onClick={() => { setSelectedChild(null); loadChildren(); }} className="btn-secondary" style={{ marginBottom: '20px' }}>⬅ Inapoi la Lista</button>

                <div className="card" style={{ borderLeft: isCurrentlySuspended ? '8px solid red' : '8px solid green' }}>
                    <div className="registry-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div className="registry-title"><h1 style={{margin:0}}>📂 Dosar: {selectedChild.name} {selectedChild.surname}</h1></div>
                        <div className="registry-status">{isCurrentlySuspended && <span style={{ background: 'red', color: 'white', padding: '5px 15px', borderRadius: '15px', fontWeight:'bold', fontSize:'0.9rem' }}>⛔ SUSPENDAT</span>}</div>
                    </div>

                    <div className="registry-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '30px', marginTop: '20px' }}>
                        <div>
                            <div style={{ padding:'15px', background:'#f9f9f9', borderRadius:'10px' }}>
                                <h3>📋 Statistici</h3>
                                <p>Varsta: <strong>{selectedChild.age} ani</strong></p>
                                <p>Parinte: {selectedChild.parentName} ({selectedChild.parentPhone})</p>
                                <p>Prezente Totale: {selectedChild.totalAttendance || 0}</p>
                                <p>Streak Curent: {selectedChild.attendanceStreak || 0}</p>
                            </div>
                        </div>
                        <div style={{display:'flex', flexDirection:'column', gap:'20px'}}>
                            <div style={{padding:'15px', background:'#f0f9ff', borderRadius:'10px', border:'1px solid #bae6fd'}}>
                                <h3 style={{marginTop:0, color:'#0369a1'}}>🎒 Inventar</h3>
                                <div style={{display:'flex', gap:'10px', marginBottom:'15px'}}>
                                    <button onClick={()=>{if(window.confirm("Are?")) fetch(`${API_URL}/children/${selectedChild.id}/give-reward?type=SHIRT`,{method:'POST'}).then(r=>{if(r.ok)openChildFile(selectedChild)})}} disabled={!isDirector || selectedChild.hasShirt} style={{flex:1, padding:'8px', background: selectedChild.hasShirt?'#16a34a':'#e2e8f0', color:selectedChild.hasShirt?'white':'black', border:'none', borderRadius:'5px'}}>👕 {selectedChild.hasShirt?'DAT':'TRICOU'}</button>
                                    <button onClick={()=>{if(window.confirm("Are?")) fetch(`${API_URL}/children/${selectedChild.id}/give-reward?type=HAT`,{method:'POST'}).then(r=>{if(r.ok)openChildFile(selectedChild)})}} disabled={!isDirector || selectedChild.hasHat} style={{flex:1, padding:'8px', background: selectedChild.hasHat?'#0284c7':'#e2e8f0', color:selectedChild.hasHat?'white':'black', border:'none', borderRadius:'5px'}}>🧢 {selectedChild.hasHat?'DATA':'CACIULA'}</button>
                                </div>
                                {isDirector && (
                                    <div style={{background:'white', padding:'10px', borderRadius:'5px'}}>
                                        <div style={{display:'flex', gap:'5px', marginBottom:'5px'}}>
                                            {availableManuals.length>0 && <select className="login-input" onChange={e=>setSelectedManual(e.target.value)} style={{flex:1, margin:0}}>{availableManuals.map(m=><option key={m} value={m}>{m}</option>)}</select>}
                                            <input className="login-input" placeholder="Manual Nou" value={customManualName} onChange={e=>setCustomManualName(e.target.value)} style={{flex:1, margin:0}} />
                                        </div>
                                        <button onClick={handleAssignManual} style={{width:'100%', background:'#0ea5e9', color:'white', border:'none', padding:'8px', borderRadius:'5px', fontWeight:'bold'}}>💾 ATRIBUIE MANUAL</button>
                                    </div>
                                )}
                                <div style={{marginTop:'10px'}}>{(selectedChild.manuals||[]).map((m,i)=><div key={i} style={{fontSize:'0.9rem', padding:'5px 0', borderBottom:'1px dashed #ccc'}}>📖 {m.name} ({m.status})</div>)}</div>
                            </div>
                            <div style={{padding:'15px', background:'#fff1f2', borderRadius:'10px', border:'1px solid #fda4af'}}>
                                <h3 style={{marginTop:0, color:'#be123c'}}>⚠️ Disciplină</h3>
                                {isDirector && (
                                    <div style={{display:'flex', flexDirection:'column', gap:'10px'}}>
                                        <input className="login-input" placeholder="Motiv sancțiune..." value={newWarning.description} onChange={e=>setNewWarning({...newWarning, description:e.target.value})} style={{margin:0}} />
                                        <div style={{display:'flex', alignItems:'center', gap:'15px'}}>
                                            <label style={{color:'#be123c', fontWeight:'bold', display:'flex', alignItems:'center', gap:'5px', cursor:'pointer', margin:0}}>
                                                <input type="checkbox" style={{width:'20px', height:'20px', margin:0}} checked={newWarning.suspension} onChange={e=>setNewWarning({...newWarning, suspension:e.target.checked})} />
                                                SUSPENDARE
                                            </label>
                                            {newWarning.suspension && (
                                                <div style={{display:'flex', alignItems:'center', gap:'5px'}}>
                                                    <span style={{fontSize:'0.9rem', fontWeight:'bold', color:'#be123c'}}>Nr. Ture:</span>
                                                    <input type="number" min="1" value={newWarning.remainingMeetings} onChange={e=>setNewWarning({...newWarning, remainingMeetings:parseInt(e.target.value)})} style={{width:'60px', padding:'5px', margin:0, textAlign:'center', border:'1px solid #be123c', borderRadius:'5px', fontWeight:'bold'}} />
                                                </div>
                                            )}
                                        </div>
                                        <button onClick={handleAddWarning} style={{background:'#be123c', color:'white', width:'100%', padding:'10px', borderRadius:'8px', fontWeight:'bold', marginTop:'5px', border:'none'}}>SALVEAZĂ SANCȚIUNEA</button>
                                    </div>
                                )}
                                <div style={{marginTop:'15px', maxHeight:'150px', overflowY:'auto', borderTop:'1px solid #fecaca', paddingTop:'10px'}}>
                                    {childWarnings.map(w => (
                                        <div key={w.id} style={{fontSize:'0.85rem', padding:'8px 0', borderBottom:'1px dashed #fecaca', color:'#7f1d1d'}}>
                                            <strong>📅 {w.date}</strong>: {w.description}
                                            {w.suspension && <span style={{background:'#be123c', color:'white', padding:'2px 6px', borderRadius:'4px', marginLeft:'5px', fontSize:'0.75rem'}}>Suspendat ({w.remainingMeetings})</span>}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="animate-in">
            <h2>📋 Registru</h2>
            {/* AICI ESTE SCHIMBAREA MAJORA PENTRU TABEL */}
            {/* Am scos minWidth: 800px si am pus clasa registry-table */}
            <div className="table-container">
                <table className="registry-table" style={{width:'100%', borderCollapse:'collapse'}}>
                    <thead>
                    <tr><th>Nume</th><th>Status</th><th>Prezente</th><th>Puncte</th><th>Actiuni</th></tr>
                    </thead>
                    <tbody>
                    {children.map(c=>(
                        <tr key={c.id}>
                            <td style={{fontWeight:'bold'}}>{c.name} {c.surname}</td>
                            <td>{c.isSuspended ? <span style={{color:'red', fontWeight:'bold'}}>SUSPENDAT</span> : <span style={{color:'green'}}>OK</span>}</td>
                            <td>{c.totalAttendance}</td>
                            <td>{c.seasonPoints}</td>
                            <td>
                                <button onClick={()=>openChildFile(c)} style={{background:'var(--accent)', color:'white', padding:'5px 15px', borderRadius:'5px'}}>
                                    Dosar
                                </button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

// ==========================================
// 10. DASHBOARD (RESPONSIVE: FIX PE DESKTOP, SCROLL PE MOBIL)
// ==========================================
const Dashboard = ({ user }) => {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const isChild = user && user.hasOwnProperty('parentPhone');

    useEffect(() => {
        const url = user ? `${API_URL}/dashboard/stats?leaderId=${user.id}` : `${API_URL}/dashboard/stats`;
        fetch(url).then(r => r.ok ? r.json() : null).then(data => { if(data) { setStats(data); setLoading(false); }}).catch(() => setLoading(false));
    }, [user]);

    const handleDeleteNotification = (id) => {
        if(!window.confirm("Ștergi notificarea?")) return;
        fetch(`${API_URL}/notifications/${id}`, { method: 'DELETE' })
            .then(res => {
                if(res.ok) {
                    setStats(prevStats => ({
                        ...prevStats,
                        notifications: prevStats.notifications.filter(n => n.id !== id)
                    }));
                } else alert("Eroare la ștergere.");
            })
            .catch(() => alert("Eroare server."));
    };

    const getProgressMessages = () => {
        if (!isChild) return [];
        const streak = user.attendanceStreak || 0;
        const msgs = [];
        if (!user.hasShirt) msgs.push((5-streak) <= 0 ? "🎁 Eligibil TRICOU!" : `👕 Mai are ${5-streak} prezente pana la Tricou.`);
        else if (!user.hasHat) msgs.push((user.attendanceStreak > 5 ? 10 : 5) - streak <= 0 ? "🎁 Eligibil CACIULA!" : `🧢 Mai are ${(user.attendanceStreak > 5 ? 10 : 5) - streak} prezente pana la Caciula.`);
        return msgs;
    };

    if (loading) return <div className="animate-in"><p>Se incarca...</p></div>;
    if (!stats) return <div className="animate-in"><p>Eroare incarcare date.</p></div>;

    const progressMsgs = getProgressMessages();
    const notificationsToDisplay = isChild ? [] : (stats.notifications || []);

    return (
        // Folosim clasa 'dashboard-wrapper' definita in CSS pentru comportamentul dual (Fix/Scroll)
        <div className="animate-in dashboard-wrapper">

            {/* --- 1. HEADER --- */}
            <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'20px', flexShrink: 0}}>
                <div>
                    <h2 style={{color:'var(--accent)', fontWeight:'900', fontSize:'2rem', margin:0, letterSpacing:'-1px'}}>{stats.clubName}</h2>
                    <p className="header-subtitle" style={{color:'var(--text-secondary)', fontSize:'1rem', marginTop:'2px'}}>
                        Salut, <strong>{user ? user.name : 'Utilizator'}</strong>!
                    </p>
                </div>
                <span className="badge" style={{background:'white', border:'1px solid var(--accent)', color:'var(--accent)', fontSize:'0.9rem'}}>
                    📅 {new Date().toLocaleDateString('ro-RO', { weekday: 'short', day: 'numeric', month: 'short' })}
                </span>
            </div>

            {/* --- 2. WIDGETS STATISTICI --- */}
            <div className="stats-widgets" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginBottom: '20px', flexShrink: 0 }}>
                {/* Card Copii */}
                <div style={{
                    background: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)',
                    borderRadius: '15px', padding: '20px', color: 'white',
                    boxShadow: '0 5px 15px -3px rgba(59, 130, 246, 0.4)',
                    display: 'flex', alignItems: 'center', justifyContent: 'space-between'
                }}>
                    <div>
                        <div style={{fontSize:'2.5rem', fontWeight:'800', lineHeight:1}}>{stats.kidsCount}</div>
                        <div style={{fontSize:'0.9rem', opacity:0.9, fontWeight:'600'}}>Copii</div>
                    </div>
                    <div style={{fontSize:'3rem', opacity:0.3}}>👶</div>
                </div>

                {/* Card Lideri */}
                <div style={{
                    background: 'linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%)',
                    borderRadius: '15px', padding: '20px', color: 'white',
                    boxShadow: '0 5px 15px -3px rgba(139, 92, 246, 0.4)',
                    display: 'flex', alignItems: 'center', justifyContent: 'space-between'
                }}>
                    <div>
                        <div style={{fontSize:'2.5rem', fontWeight:'800', lineHeight:1}}>{stats.leadersCount}</div>
                        <div style={{fontSize:'0.9rem', opacity:0.9, fontWeight:'600'}}>Lideri</div>
                    </div>
                    <div style={{fontSize:'3rem', opacity:0.3}}>👔</div>
                </div>
            </div>

            {/* --- 3. DIRECTORI (SCROLL ORIZONTAL - ARATA BINE SI PE MOBIL) --- */}
            <div style={{marginBottom:'20px', flexShrink: 0}}>
                <h3 style={{fontSize:'1rem', marginBottom:'10px', color:'var(--text-secondary)'}}>📞 Contacte Directori</h3>

                {/* Lista Orizontala */}
                <div className="directors-scroll">
                    {stats.directors && stats.directors.map(dir => (
                        <div key={dir.id} className="card" style={{
                            minWidth: '220px', margin: 0, padding: '15px',
                            display: 'flex', flexDirection: 'column', justifyContent: 'center',
                            background: '#f8fafc', border: '1px solid #e2e8f0',
                            flexShrink: 0 /* Important sa nu se striveasca */
                        }}>
                            <div style={{fontWeight:'bold', fontSize:'1rem', color:'var(--text-primary)'}}>
                                {dir.name} {dir.surname}
                            </div>
                            <a href={`tel:${dir.phoneNumber}`} style={{
                                color:'var(--primary)', fontWeight:'bold', textDecoration:'none',
                                marginTop:'5px', fontSize:'0.9rem', display:'flex', alignItems:'center', gap:'5px'
                            }}>
                                📞 {dir.phoneNumber || '-'}
                            </a>
                        </div>
                    ))}
                </div>
            </div>

            {/* --- 4. NOTIFICARI (SCROLL VERTICAL PE DESKTOP / CURGE PE MOBIL) --- */}
            <div className="card dashboard-scroll-area">
                <h3 style={{marginBottom:'15px'}}>
                    🔔 {isChild ? 'Noutăți & Progres' : 'Avizier Digital'}
                </h3>

                <div>
                    {/* Progres Copii */}
                    {isChild && progressMsgs.map((msg, i) => (
                        <div key={i} style={{background:'#eff6ff', borderLeft:'5px solid #3b82f6', color:'#1e40af', padding:'15px', borderRadius:'8px', marginBottom:'10px', fontWeight:'bold', fontSize:'0.95rem'}}>
                            {msg}
                        </div>
                    ))}

                    {/* Lista Notificari */}
                    {notificationsToDisplay.length > 0 ? (
                        notificationsToDisplay.map((n) => {
                            const isReward = n.type && n.type.includes('ELIGIBLE');
                            const bgColor = isReward ? '#f0fdf4' : '#fffbeb';
                            const textColor = isReward ? '#15803d' : '#b45309';
                            const borderColor = isReward ? '#22c55e' : '#f59e0b';

                            return (
                                <div key={n.id} style={{
                                    background: bgColor, color: textColor, padding:'15px 35px 15px 15px',
                                    borderRadius:'10px', borderLeft: `4px solid ${borderColor}`,
                                    marginBottom:'10px', position: 'relative', fontSize:'0.9rem',
                                    boxShadow:'0 1px 2px rgba(0,0,0,0.02)'
                                }}>
                                    {isReward && <span style={{fontSize:'1.1rem', marginRight:'5px'}}>🎁</span>}
                                    <span style={{whiteSpace:'pre-wrap'}}>{n.message}</span>
                                    <button onClick={() => handleDeleteNotification(n.id)}
                                            style={{
                                                position: 'absolute', top: '5px', right: '5px',
                                                background:'transparent', color: textColor,
                                                fontSize: '1.2rem', opacity: 0.6, padding:'5px'
                                            }}>×</button>
                                </div>
                            );
                        })
                    ) : ((!isChild || progressMsgs.length === 0) &&
                        <div style={{textAlign:'center', padding:'30px', color:'#94a3b8'}}>Nu sunt notificări.</div>
                    )}
                </div>
            </div>

            {/* --- 5. FOOTER (VERSET) --- */}
            <div style={{textAlign: 'center', flexShrink: 0, paddingTop:'10px', paddingBottom:'20px'}}>
                <p style={{fontSize: '0.9rem', fontStyle: 'italic', color: '#64748b', margin:0}}>
                    „Isus Hristos este acelaşi ieri şi azi şi în veci!” <span style={{fontWeight:'bold', color:'var(--accent)'}}>(Evrei 13:8)</span>
                </p>
            </div>
        </div>
    );
};

// ==========================================
// 11. MY PROFILE (REPARAT - ȘTERGERE ȘI EDITARE USERNAME)
// ==========================================
const MyProfile = ({ user, onUpdateUser }) => {
    const isChild = user.hasOwnProperty('parentPhone');
    const [isEditing, setIsEditing] = useState(false);
    const [formData, setFormData] = useState({ ...user, password: '' });
    const [loading, setLoading] = useState(false);
    const [allDepartments, setAllDepartments] = useState([]);
    const [showDeleteInput, setShowDeleteInput] = useState(false);
    const [deleteCode, setDeleteCode] = useState('');

    useEffect(() => {
        if (!isChild) {
            fetch(`${API_URL}/leaders/${user.id}`).then(r => r.ok ? r.json() : null).then(data => {
                if (data) { onUpdateUser(data); setFormData({ ...data, password: '' }); }
            });
            fetch(`${API_URL}/departments`).then(r => r.ok ? r.json() : []).then(setAllDepartments);
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
            .then(u => {
                setLoading(false); setIsEditing(false); onUpdateUser(u); alert("✅ Actualizat!");
            }).catch((err) => { setLoading(false); alert(err || "Eroare server."); });
    };

    const toggleDepartment = (dept) => {
        const hasDept = formData.departments.some(d => d.id === dept.id);
        if (hasDept) setFormData({ ...formData, departments: formData.departments.filter(d => d.id !== dept.id) });
        else setFormData({ ...formData, departments: [...formData.departments, dept] });
    };

    const requestDeletionCode = () => {
        if(!window.confirm("⚠️ Esti sigur ca vrei sa initiezi stergerea contului?")) return;
        fetch(`${API_URL}/account/request-deletion`, {
            method: 'POST',
            headers: {'Content-Type':'application/json'},
            body: JSON.stringify({ id: user.id, role: isChild ? 'CHILD' : 'LEADER' })
        })
            .then(res => res.text()).then(msg => alert(msg))
            .catch(err => alert("Eroare la solicitare cod."));
    };

    const performDeletion = () => {
        if(!deleteCode) return alert("Te rog introdu codul primit!");
        if(!window.confirm("🚨 ATENTIE! Actiunea este IREVERSIBILA. Continui?")) return;

        const baseUrl = isChild ? `${API_URL}/children/${user.id}` : `${API_URL}/leaders/${user.id}`;
        // REPARAȚIE: Trimitem codul corect prin Query Parameter
        const fullUrl = `${baseUrl}?code=${encodeURIComponent(deleteCode)}`;

        fetch(fullUrl, { method: 'DELETE' })
            .then(async res => {
                if(res.ok) { alert("✅ Cont sters."); localStorage.clear(); window.location.reload(); }
                else { const errorMsg = await res.text(); alert("❌ Eroare: " + errorMsg); }
            }).catch(err => alert("Eroare de conexiune."));
    };

    const isAdmin = !isChild && user.id === 1;

    const StatusItem = ({ icon, label, value }) => (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px', background: value ? '#dcfce7' : '#fee2e2', borderRadius: '8px', border: value ? '1px solid #16a34a' : '1px solid #dc2626', color: value ? '#166534' : '#991b1b', fontWeight: 'bold' }}>
            <span style={{display:'flex', alignItems:'center', gap:'8px'}}>{icon} {label}</span>
            <span>{value ? 'DA ✅' : 'NU ❌'}</span>
        </div>
    );

    return (
        <div className="animate-in">
            <div style={{display:'flex', justifyContent:'space-between', alignItems:'center'}}>
                <h2>👤 Contul Meu</h2>
                <button onClick={() => isEditing ? handleSave() : setIsEditing(true)} style={{background: isEditing ? 'var(--success)' : 'var(--accent)', color: 'white', padding:'10px 20px', borderRadius:'10px', fontWeight:'bold', border:'none', cursor:'pointer'}}>
                    {loading ? '...' : (isEditing ? '💾 Salveaza' : '✏️ Editeaza')}
                </button>
            </div>

            <div className="card" style={{marginTop:'20px', display:'flex', gap:'20px', alignItems:'center'}}>
                <div style={{width:'80px', height:'80px', borderRadius:'50%', background:'var(--accent)', color:'white', display:'flex', justifyContent:'center', alignItems:'center', fontSize:'2.5rem', fontWeight:'bold', flexShrink:0}}>
                    {user.name ? user.name.charAt(0) : 'U'}
                </div>
                <div style={{flex:1}}>
                    {isEditing ? (
                        <div style={{display:'flex', flexDirection:'column', gap:'10px'}}>
                            <div style={{display:'flex', gap:'10px'}}>
                                <input className="login-input" value={formData.name} onChange={e=>setFormData({...formData, name:e.target.value})} placeholder="Prenume"/>
                                <input className="login-input" value={formData.surname} onChange={e=>setFormData({...formData, surname:e.target.value})} placeholder="Nume"/>
                            </div>
                            <input className="login-input" value={formData.username || ''} onChange={e=>setFormData({...formData, username: e.target.value.toLowerCase().replace(/\s/g, '')})} placeholder="username"/>
                        </div>
                    ) : (
                        <>
                            <h1 className="profile-name" style={{margin:0}}>{user.name} {user.surname}</h1>
                            <p style={{margin:0, color:'#666'}}>@{user.username || 'fara_username'}</p>
                        </>
                    )}
                    <span className="badge" style={{background:'#e0f2fe', color:'#0284c7', marginTop:'5px'}}>
                        {isChild ? '👶 Copil' : `👔 ${user.role || 'Lider'}`}
                    </span>
                </div>
            </div>

            <div className="profile-grid" style={{ marginTop:'20px' }}>
                <div className="card">
                    <h3>📋 Date Personale</h3>
                    <div style={{marginTop:'15px', display:'flex', flexDirection:'column', gap:'15px'}}>
                        {!isChild ? (
                            <>
                                <div><label>Telefon:</label>{isEditing ? <input className="login-input" value={formData.phoneNumber||''} onChange={e=>setFormData({...formData, phoneNumber:e.target.value})}/> : <p>📞 {user.phoneNumber||'Nespecificat'}</p>}</div>
                                {isEditing && (<div><label>🔒 Parola:</label><input type="password" className="login-input" value={formData.password} onChange={e=>setFormData({...formData, password: e.target.value})} /></div>)}
                                <div style={{marginTop:'10px'}}>
                                    <label>Departamente:</label>
                                    {isEditing ? (
                                        <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'10px', marginTop:'5px'}}>
                                            {allDepartments.map(dept => (
                                                <label key={dept.id} style={{display:'flex', alignItems:'center', gap:'8px', cursor:'pointer'}}>
                                                    <input type="checkbox" checked={formData.departments?.some(d => d.id === dept.id)} onChange={() => toggleDepartment(dept)} />
                                                    {dept.name}
                                                </label>
                                            ))}
                                        </div>
                                    ) : (
                                        <div style={{display:'flex', flexWrap:'wrap', gap:'8px'}}>{user.departments?.map(d => (<span key={d.id} className="badge">🏷️ {d.name}</span>))}</div>
                                    )}
                                </div>
                            </>
                        ) : (
                            <>
                                <div><label>Data Nasterii:</label>{isEditing ? <input type="date" className="login-input" value={formData.birthDate||''} onChange={e=>setFormData({...formData, birthDate:e.target.value})}/> : <p>🎂 {user.birthDate||'-'}</p>}</div>
                                <div><label>Parinte:</label>{isEditing ? <input className="login-input" value={formData.parentName||''} onChange={e=>setFormData({...formData, parentName:e.target.value})}/> : <p>👤 {user.parentName||'-'}</p>}</div>
                                <div><label>Telefon Parinte:</label>{isEditing ? <input className="login-input" value={formData.parentPhone||''} onChange={e=>setFormData({...formData, parentPhone:e.target.value})}/> : <p>📞 {user.parentPhone||'-'}</p>}</div>
                                <div style={{display: 'flex', gap: '10px', marginTop: '10px'}}>
                                    <div style={{flex: 1, background: '#f0f9ff', padding: '10px', borderRadius: '8px', textAlign:'center'}}>⭐ {user.seasonPoints || 0} pct</div>
                                    <div style={{flex: 1, background: '#f0fdf4', padding: '10px', borderRadius: '8px', textAlign:'center'}}>✅ {user.totalAttendance || 0} prez</div>
                                </div>
                            </>
                        )}
                    </div>
                </div>

                <div style={{display:'flex', flexDirection:'column', gap:'20px'}}>
                    {isChild && (
                        <div className="card">
                            <h3>🎒 Inventar</h3>
                            <div style={{display:'flex', flexDirection:'column', gap:'10px', marginTop:'15px'}}>
                                <StatusItem icon="📚" label="Manual" value={user.hasManual} />
                                <StatusItem icon="👕" label="Tricou" value={user.hasShirt} />
                                <StatusItem icon="🧢" label="Căciulă" value={user.hasHat} />
                            </div>
                        </div>
                    )}

                    {!isAdmin && (
                        <div className="card" style={{border:'2px solid #fee2e2'}}>
                            <h3 style={{color:'#dc2626'}}>🚨 Zona de Pericol</h3>
                            {!showDeleteInput ? (
                                <div style={{display:'flex', flexDirection:'column', gap:'10px'}}>
                                    <button onClick={requestDeletionCode} style={{padding:'10px', background:'orange', color:'white', border:'none', borderRadius:'8px', cursor:'pointer'}}>Cere Cod</button>
                                    <button onClick={() => setShowDeleteInput(true)} style={{padding:'10px', background:'#fee2e2', color:'#dc2626', border:'1px solid #dc2626', borderRadius:'8px', cursor:'pointer'}}>Am codul!</button>
                                </div>
                            ) : (
                                <div style={{display:'flex', flexDirection:'column', gap:'10px'}}>
                                    <input className="login-input" placeholder="Cod" value={deleteCode} onChange={e => setDeleteCode(e.target.value)} />
                                    <button onClick={performDeletion} style={{padding:'10px', background:'#dc2626', color:'white', border:'none', borderRadius:'8px', cursor:'pointer'}}>STERGE CONT</button>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};


// ==========================================
// 12. REGISTER (ACTUALIZAT CU AFISARE USERNAME)
// ==========================================
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
                    // SERVERUL trimite acum: "Cont creat! Username: davidpopescu"
                    setMsg('✅ ' + text + '. Notează-ți username-ul pentru login!');
                    // Lăsăm 4 secunde să apuce să citească username-ul generat
                    setTimeout(onSwitchToLogin, 4500);
                } else {
                    setMsg('❌ ' + text);
                }
            })
            .catch(() => setMsg('❌ Eroare conexiune server.'))
            .finally(() => setLoading(false));
    };

    return (
        <div className="login-container" style={{display:'flex', justifyContent:'center', alignItems:'center', height:'100vh', width:'100vw', background:'var(--bg-primary)'}}>
            <div className="login-box" style={{
                width:'100%', maxWidth:'420px',
                background:'white', padding:'40px', borderRadius:'20px',
                boxShadow:'0 10px 30px rgba(0,0,0,0.1)', textAlign:'center',
                maxHeight:'90vh', overflowY:'auto'
            }}>
                <div style={{marginBottom:'20px', display:'flex', justifySelf:'center'}}><AwanaLogo width="200px"/></div>
                <h2 style={{marginBottom:'25px', fontSize:'1.8rem', color:'#111'}}>Înregistrare Cont</h2>

                <div style={{display:'flex', gap:'10px', marginBottom:'20px'}}>
                    {['CHILD','LEADER','DIRECTOR'].map(t => (
                        <button key={t} type="button" onClick={()=>{setRoleType(t); setSelectedDepts(new Set());}}
                                style={{
                                    flex:1, padding:'12px',
                                    background: roleType===t ? '#111' : '#f3f4f6',
                                    color: roleType===t ? 'white' : '#666',
                                    fontWeight:'bold', border:'none', borderRadius:'8px', fontSize:'0.95rem', cursor:'pointer', transition:'0.2s'
                                }}>
                            {t==='CHILD'?'Copil':t==='LEADER'?'Lider':'Director'}
                        </button>
                    ))}
                </div>

                <form onSubmit={doRegister} style={{display:'flex', flexDirection:'column', gap:'12px'}}>
                    <input placeholder="Prenume" className="login-input" style={{fontSize:'1rem', color:'#000'}} onChange={e=>setForm({...form, name:e.target.value})} required/>
                    <input placeholder="Nume" className="login-input" style={{fontSize:'1rem', color:'#000'}} onChange={e=>setForm({...form, surname:e.target.value})} required/>
                    <input type="password" placeholder="Parolă" className="login-input" style={{fontSize:'1rem', color:'#000'}} onChange={e=>setForm({...form, pass:e.target.value})} required/>

                    {roleType !== 'CHILD' && (
                        <input placeholder="🔒 Cod de Acces" className="login-input" style={{borderColor:'red', fontSize:'1rem'}} onChange={e=>setForm({...form, regCode:e.target.value})} required/>
                    )}

                    {roleType === 'CHILD' ? (
                        <>
                            <label style={{fontSize:'0.9rem', display:'block', textAlign:'left', fontWeight:'bold', color:'#444'}}>Data Nașterii:</label>
                            <input type="date" className="login-input" style={{fontSize:'1rem'}} onChange={e=>setForm({...form, birthDate:e.target.value})} required/>
                            <input placeholder="Nume Părinte" className="login-input" style={{fontSize:'1rem'}} onChange={e=>setForm({...form, parentName:e.target.value})} required/>
                            <input placeholder="Telefon Părinte" className="login-input" style={{fontSize:'1rem'}} onChange={e=>setForm({...form, phone:e.target.value})} required/>
                        </>
                    ) : (
                        <>
                            <input placeholder="Telefonul Tău" className="login-input" style={{fontSize:'1rem'}} onChange={e=>setForm({...form, phone:e.target.value})} required/>
                            <div style={{textAlign:'left', marginTop:'10px'}}>
                                <label style={{fontWeight:'bold', fontSize:'1rem', color:'#333'}}>Departamente:</label>
                                <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'10px', marginTop:'10px', maxHeight:'150px', overflowY:'auto', background:'#f8f9fa', padding:'15px', borderRadius:'10px', border:'1px solid #e9ecef'}}>
                                    {departments.map(d => (
                                        <div key={d.id} onClick={() => toggleDept(d.id)} style={{cursor:'pointer', display:'flex', alignItems:'center', gap:'8px', fontSize:'0.95rem', fontWeight:'500', color:'#333'}}>
                                            <input type="checkbox" checked={selectedDepts.has(d.id)} readOnly style={{width:'18px', height:'18px', cursor:'pointer'}}/>
                                            {d.name}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </>
                    )}

                    {msg && (
                        <div style={{
                            color: msg.startsWith('✅')?'#15803d':'#dc2626',
                            background: msg.startsWith('✅')?'#f0fdf4':'#fee2e2',
                            padding:'12px', borderRadius:'8px', fontWeight:'bold', fontSize:'0.95rem', border:'1px solid'
                        }}>
                            {msg}
                        </div>
                    )}

                    <button type="submit" disabled={loading} style={{
                        width:'100%', padding:'15px',
                        background:'#000', color:'white',
                        borderRadius:'10px', fontWeight:'bold', fontSize:'1.1rem',
                        marginTop:'20px', border:'none', cursor: loading ? 'wait' : 'pointer'
                    }}>
                        {loading ? 'Se procesează...' : 'Creează Cont'}
                    </button>
                </form>

                <p onClick={onSwitchToLogin} style={{marginTop:'25px', cursor:'pointer', color:'var(--accent)', fontWeight:'bold', fontSize:'1rem', textDecoration:'underline'}}>
                    Ai deja cont? Loghează-te
                </p>
            </div>
        </div>
    );
};


// ==========================================
// 13. LOGIN (CENTRAT, SCRIS MARE, BUTOANE NEGRE)
// ==========================================
const Login = ({ onLogin, onSwitchToRegister }) => {
    // Am schimbat 'user' in 'username' pentru a fi cat mai clar
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
            body: JSON.stringify({
                username: form.username, // Acum trimite corect noul field
                password: form.pass,
                role: form.role
            }),
            signal: controller.signal
        })
            .then(async r => {
                clearTimeout(timeoutId);
                const contentType = r.headers.get("content-type");
                if (contentType && contentType.indexOf("application/json") !== -1) {
                    const data = await r.json();
                    if(r.ok) onLogin(data);
                    else setErr(data.message || 'Date greșite!');
                } else {
                    const text = await r.text();
                    setErr(text || 'Date greșite!');
                }
            })
            .catch((error) => {
                clearTimeout(timeoutId);
                if (error.name === 'AbortError') setErr('❌ Serverul nu răspunde (Timeout).');
                else setErr('❌ Server offline sau eroare conexiune.');
            })
            .finally(() => setLoading(false));
    };

    return (
        <div className="login-container" style={{display:'flex', justifyContent:'center', alignItems:'center', height:'100vh', width:'100vw', background:'var(--bg-primary)'}}>
            <div className="login-box" style={{
                width:'100%', maxWidth:'420px',
                background:'white', padding:'40px', borderRadius:'20px',
                boxShadow:'0 10px 30px rgba(0,0,0,0.1)', textAlign:'center'
            }}>
                <div style={{marginBottom:'20px', display:'flex', justifySelf:'center'}}><AwanaLogo width="200px"/></div>
                <h2 style={{marginBottom:'30px', fontSize:'1.8rem', color:'#111'}}>Autentificare</h2>

                {/* Butoane Rol - Negre */}
                <div style={{display:'flex', gap:'10px', marginBottom:'25px'}}>
                    {['CHILD','LEADER','DIRECTOR'].map(t => (
                        <button key={t} type="button"
                                onClick={()=>setForm({...form, role:t})}
                                style={{
                                    flex:1, padding:'12px',
                                    background: form.role === t ? '#111' : '#f3f4f6', // Negru activ
                                    color: form.role === t ? 'white' : '#666',
                                    fontWeight:'bold', border:'none', borderRadius:'8px', fontSize:'0.95rem',
                                    cursor: 'pointer', transition: 'all 0.2s'
                                }}>
                            {t==='CHILD'?'Copil':t==='LEADER'?'Lider':'Director'}
                        </button>
                    ))}
                </div>

                <form onSubmit={doLogin} style={{display:'flex', flexDirection:'column', gap:'15px'}}>
                    <input
                        type="text"
                        placeholder="Nume utilizator (ex: davidpopescu)"
                        className="login-input"
                        style={{fontSize:'1.1rem', padding:'15px', color:'#000'}}
                        value={form.username}
                        onChange={e => {
                            // Fortam scrierea cu litere mici si fara spatii vizual
                            const cleanValue = e.target.value.toLowerCase().replace(/\s/g, '');
                            setForm({...form, username: cleanValue});
                        }}
                        required
                    />
                    <input
                        type="password"
                        placeholder="Parolă"
                        className="login-input"
                        style={{fontSize:'1.1rem', padding:'15px', color:'#000'}}
                        value={form.pass}
                        onChange={e=>setForm({...form, pass:e.target.value})}
                        required
                    />

                    {err && <div style={{color:'#dc2626', background:'#fee2e2', padding:'12px', borderRadius:'8px', marginBottom:'5px', fontSize:'1rem', fontWeight:'bold', border:'1px solid #fca5a5'}}>{err}</div>}

                    <button type="submit" disabled={loading} style={{
                        width:'100%', padding:'15px',
                        background:'#000', // Buton Login Negru
                        color:'white',
                        borderRadius:'10px', fontWeight:'bold', fontSize:'1.2rem',
                        marginTop:'15px', border:'none', cursor: loading ? 'wait' : 'pointer',
                        boxShadow:'0 4px 10px rgba(0,0,0,0.2)'
                    }}>
                        {loading ? 'Se verifică...' : 'Intră în Cont'}
                    </button>
                </form>

                <p onClick={onSwitchToRegister} style={{marginTop:'30px', cursor:'pointer', color:'var(--accent)', borderTop:'1px solid #eee', paddingTop:'20px', fontWeight:'bold', fontSize:'1rem'}}>
                    Nu ai cont? Înregistrează-te
                </p>
            </div>
        </div>
    );
};

// ==========================================
// 15. APP MAIN (FINAL SI CORECTAT)
// ==========================================
function App() {
    // 1. Logoul (tinut minte pe sesiune ca sa nu fie enervant)
    const [loading, setLoading] = useState(() => {
        return !sessionStorage.getItem('hasSeenLogo');
    });

    // 2. USER-UL: Il incarcam din memoria permanenta ca sa nu mai ceara login la refresh
    const [user, setUser] = useState(() => {
        const savedUser = localStorage.getItem('awanaLoggedUser');
        return savedUser ? JSON.parse(savedUser) : null;
    });

    const [register, setRegister] = useState(false);

    // 3. PAGINA: Tine minte unde erai inainte de refresh (Registru, Dashboard, etc.)
    const [page, setPage] = useState(() => {
        return localStorage.getItem('awanaCurrentPage') || 'dashboard';
    });

    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

    // ==========================================
    // EFECTE MAGICE: Salvare automata in fundal
    // ==========================================

    // Cand userul se schimba (ex: se logheaza sau delogheaza) -> actualizam memoria
    useEffect(() => {
        if (user) {
            localStorage.setItem('awanaLoggedUser', JSON.stringify(user));
        } else {
            // Daca a dat logout (user e null), stergem datele
            localStorage.removeItem('awanaLoggedUser');
            localStorage.removeItem('awanaCurrentPage');
        }
    }, [user]);

    // Cand schimba pagina -> o tinem minte imediat
    useEffect(() => {
        if (user) {
            localStorage.setItem('awanaCurrentPage', page);
        }
    }, [page, user]);

    // Animatia cu Logo Awana
    useEffect(() => {
        if (loading) {
            const timer = setTimeout(() => {
                setLoading(false);
                sessionStorage.setItem('hasSeenLogo', 'true');
            }, 2000);
            return () => clearTimeout(timer);
        }
    }, [loading]);

    // O functie mica pentru schimbarea paginii (probabil o ai deja mai jos, dar asigura-te ca arata cam asa)
    const navigateTo = (pageName) => {
        setPage(pageName);
        setMobileMenuOpen(false);
    };

    if (loading) return <SplashScreen />;
    if (!user) return register ? <Register onSwitchToLogin={() => setRegister(false)} /> : <Login onLogin={setUser} onSwitchToRegister={() => setRegister(true)} />;

    const isChild = user.hasOwnProperty('parentPhone');
    const isDirector = user.role === 'DIRECTOR' || user.role === 'COORDONATOR';

    return (
        <div className="app-container">

            {/* HEADER MOBIL (Invizibil pe Laptop) */}
            <div className="mobile-header">
                <div style={{display:'flex', alignItems:'center', gap:'15px'}}>
                    <button className="hamburger-btn" onClick={() => setMobileMenuOpen(true)}>☰</button>
                    <span style={{fontWeight:'800', fontSize:'1.2rem', color:'var(--accent)'}}>Awana Betania</span>
                </div>
                <div style={{width:'35px', height:'35px', background:'var(--accent)', color:'white', borderRadius:'50%', display:'flex', alignItems:'center', justifyContent:'center', fontWeight:'bold'}}>
                    {user.name.charAt(0)}
                </div>
            </div>

            {/* OVERLAY (Invizibil pe Laptop) */}
            <div className={`mobile-overlay ${mobileMenuOpen ? 'visible' : ''}`} onClick={() => setMobileMenuOpen(false)}></div>

            {/* SIDEBAR (Fix pe Laptop / Sertar pe Mobil) */}
            <div className={`sidebar ${mobileMenuOpen ? 'open' : ''}`}>

                {/* Buton X (Doar pe mobil) */}
                <button className="close-menu-btn" onClick={() => setMobileMenuOpen(false)}>✕</button>

                {/* 1. LOGO */}
                <div className="sidebar-logo-container">
                    <AwanaLogo width="160px" />
                </div>

                {/* 2. TITLU SECȚIUNE */}
                <div style={{margin:'10px 0 10px 5px', fontSize:'0.85rem', color:'var(--text-secondary)', fontWeight:'800', letterSpacing:'1px', textTransform:'uppercase'}}>
                    {isChild ? 'Meniu' : 'Management'}
                </div>

                {/* 3. DASHBOARD */}
                <button className={`nav-btn ${page==='dashboard'?'active':''}`} onClick={()=>navigateTo('dashboard')}>
                    📊 Dashboard
                </button>

                {/* 4. CONTUL MEU */}
                <button className={`nav-btn ${page==='profile'?'active':''}`} onClick={()=>navigateTo('profile')}>
                    👤 Contul Meu
                </button>

                {/* 5. RESTUL PAGINILOR (Calendar, etc.) */}
                {!isChild && (
                    <>
                        <button className={`nav-btn ${page==='calendar'?'active':''}`} onClick={()=>navigateTo('calendar')}>📅 Calendar & Sesiuni</button>
                        <button className={`nav-btn ${page==='departments'?'active':''}`} onClick={()=>navigateTo('departments')}>🏢 Departamente</button>
                        <button className={`nav-btn ${page==='stickers'?'active':''}`} onClick={()=>navigateTo('stickers')}>🏆 Albume Stickere</button>
                        <button className={`nav-btn ${page==='registry'?'active':''}`} onClick={()=>navigateTo('registry')}>🗂️ Registru Copii</button>

                        {isDirector && (
                            <button className={`nav-btn ${page==='leaders'?'active':''}`} onClick={()=>navigateTo('leaders')}>👔 Registru Lideri</button>
                        )}
                        {/* BUTON GOD MODE - Doar pentru Admin (ID 1) */}
                        {user.id === 1 && !isChild && (
                            <button
                                className={`nav-btn ${page==='admin'?'active':''}`} // Am pus stilul corect de meniu
                                style={{ color: '#e11d48', fontWeight: '900', borderLeft: '4px solid #e11d48' }}
                                onClick={() => setPage('admin')} // <--- CORECT: setPage('admin')
                            >
                                🛡️ Control Center
                            </button>
                        )}
                    </>
                )}

                <div style={{marginTop: 'auto', paddingTop:'20px'}}>
                    <button className="nav-btn" onClick={() => { setUser(null); window.location.reload(); }} style={{color:'var(--danger)'}}>
                        🚪 Ieșire
                    </button>
                </div>
            </div>

            {/* CONTINUT PRINCIPAL */}
            <div className="main-content">
                {page === 'dashboard' && <Dashboard user={user} />}
                {page === 'profile' && <MyProfile user={user} onUpdateUser={setUser} />}
                {!isChild && page === 'stickers' && <StickersHub user={user} />}
                {!isChild && page === 'calendar' && <CalendarManager user={user} />}
                {!isChild && page === 'departments' && <DepartmentsList user={user} />}
                {!isChild && page === 'registry' && <Registry user={user} />}
                {!isChild && page === 'leaders' && isDirector && <LeadersRegistry />}

                {/* 👇 LINIA NOUA PENTRU ADMIN 👇 */}
                {!isChild && page === 'admin' && <AdminDashboard currentUser={user} />}
            </div>
        </div>
    );
}

export default App;
