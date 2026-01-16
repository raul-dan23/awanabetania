import React, { useState, useEffect } from 'react';
import './App.css';
import AwanaLogo from './AwanaLogo';

// ==========================================
// CONFIGURARE GLOBALA
// ==========================================
//const API_URL = 'http://localhost:8080/api';
const API_URL = 'http://awana.annadanut.ro:8080/api';

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
// 3. DEPARTMENTS LIST
// ==========================================
const DepartmentsList = ({ user }) => {
    const [departments, setDepartments] = useState([]);
    const [leaders, setLeaders] = useState([]);
    const [meetings, setMeetings] = useState([]);

    const [selectedDept, setSelectedDept] = useState(null);
    const [deptMembers, setDeptMembers] = useState([]);
    const [scheduleData, setScheduleData] = useState({});

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

        fetch(`${API_URL}/departments/${dept.id}/members`).then(r => r.ok ? r.json() : []).then(setDeptMembers);

        meetings.forEach(m => {
            fetch(`${API_URL}/departments/plan/${m.id}`).then(r => r.ok ? r.json() : null).then(data => {
                if(data && data.assignments && data.assignments[dept.id]) {
                    setScheduleData(prev => ({ ...prev, [m.id]: data.assignments[dept.id] }));
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

        return (
            <div className="animate-in">
                {/* MODIFICAT: Buton cu clasa CSS */}
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
                                            <label style={{fontSize:'0.85rem', fontWeight:'bold', display:'block', marginBottom:'3px'}}>Propune un lider:</label>
                                            <select className="login-input" style={{padding:'8px', margin:0}} onChange={(e)=>{handleAddLeader(m.id, e.target.value); e.target.value='';}}>
                                                <option value="">+ Selectează din toți liderii</option>
                                                {leaders
                                                    .filter(l => !assigned.some(a => a.leader?.id === l.id))
                                                    .sort((a,b) => a.name.localeCompare(b.name))
                                                    .map(l => (
                                                        <option key={l.id} value={l.id} style={{fontWeight: deptMembers.some(dm => dm.id === l.id) ? 'bold' : 'normal'}}>
                                                            {l.name} {l.surname} {deptMembers.some(dm => dm.id === l.id) ? '(Membru)' : ''}
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
// 4. LEADERS REGISTRY
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

    if (selectedLeader) {
        return (
            <div className="animate-in">
                {/* MODIFICAT: Buton cu clasa CSS */}
                <button onClick={() => setSelectedLeader(null)} className="btn-secondary" style={{marginBottom:'20px'}}>
                    ⬅ Înapoi la Listă
                </button>

                <div className="card" style={{borderLeft: '5px solid var(--accent)'}}>
                    <div style={{display:'flex', justifyContent:'space-between', alignItems:'flex-start'}}>
                        <div><h1 style={{margin:0}}>{selectedLeader.name} {selectedLeader.surname}</h1><span className="badge" style={{background:'#e0e7ff', color:'var(--accent)', marginTop:'5px'}}>{selectedLeader.role || 'Lider'}</span></div>
                        <div style={{textAlign:'right'}}><div style={{color:'orange', fontSize:'1.5rem'}}>{[1,2,3,4,5].map(s => (<span key={s} style={{opacity: s <= Math.round(selectedLeader.rating || 0) ? 1 : 0.3}}>★</span>))}</div><div style={{fontSize:'1.2rem', fontWeight:'bold', color:'#555'}}>{selectedLeader.rating ? selectedLeader.rating.toFixed(1) : '0.0'}</div></div>
                    </div>
                    <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'30px', marginTop:'30px'}}>
                        <div>
                            <h3>📋 Date de Contact</h3>
                            <p style={{marginTop:'10px'}}><strong>Telefon:</strong> {selectedLeader.phoneNumber || 'Nespecificat'}</p>
                            <h3 style={{marginTop:'20px'}}>🏷️ Departamente</h3>
                            <div style={{display:'flex', flexWrap:'wrap', gap:'8px', marginTop:'10px'}}>{selectedLeader.departments && selectedLeader.departments.length > 0 ? (selectedLeader.departments.map(d => (<span key={d.id} className="badge" style={{background:'#dcfce7', color:'#166534'}}>{d.name}</span>))) : (<p style={{color:'#888', fontStyle:'italic'}}>Nu este inscris in niciun departament.</p>)}</div>
                        </div>
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

    return (
        <div className="animate-in">
            <h2>👔 Registru Lideri</h2>
            <div className="table-container" style={{overflowX:'auto'}}>
                <table style={{minWidth:'800px'}}>
                    <thead><tr><th>Nume</th><th>Rol</th><th>Telefon</th><th>Media</th><th>Actiuni</th></tr></thead>
                    <tbody>
                    {leaders.map(l => (
                        <tr key={l.id}>
                            <td style={{fontWeight:'bold'}}>{l.name} {l.surname}</td>
                            <td><span className="badge" style={{background: l.role==='DIRECTOR'?'#fee2e2':'#f3f4f6', color: l.role==='DIRECTOR'?'#991b1b':'#374151'}}>{l.role}</span></td>
                            <td>{l.phoneNumber || '-'}</td>
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
// 5. SCORING WIDGET (ACTUALIZAT)
// ==========================================
/**
 * Componenta pentru acordarea punctelor individuale.
 * Bifam ce a facut copilul (biblie, uniforma) si salvam.
 */
const ScoringWidget = () => {
    const [children, setChildren] = useState([]);
    const [selectedChildId, setSelectedChildId] = useState('');
    // Starea checkbox-urilor
    const [pointsData, setPointsData] = useState({
        attended: false,
        hasBible: false,
        hasHandbook: false,
        lesson: false,
        friend: false,
        hasUniform: false
    });

    // Incarca copiii ordonati alfabetic
    useEffect(() => {
        fetch(`${API_URL}/children`)
            .then(r => r.ok ? r.json() : [])
            .then(data => {
                if(Array.isArray(data)) {
                    setChildren(data.sort((a,b) => a.name.localeCompare(b.name)));
                }
            })
            .catch(()=>{});
    }, []);

    // Calcul live pentru a arata totalul inainte de salvare
    const calculateTotal = () => {
        let total = 0;
        if (pointsData.attended) total += 1000;
        if (pointsData.hasBible) total += 500;
        if (pointsData.hasHandbook) total += 500;
        if (pointsData.lesson) total += 1000;
        if (pointsData.friend) total += 1000;
        if (pointsData.hasUniform) total += 10000;
        return total;
    };

    const handleCheckbox = (e) => setPointsData({ ...pointsData, [e.target.name]: e.target.checked });

    const handleSaveScore = () => {
        if (!selectedChildId) { alert("Selecteaza un copil!"); return; }

        const payload = {
            childId: parseInt(selectedChildId),
            ...pointsData,
            extraPoints: 0
        };

        fetch(`${API_URL}/scores/add`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        })
            .then(async res => {
                // AICI ESTE SECRETUL: Citim textul erorii de la server
                const text = await res.text();

                if(res.ok) {
                    alert("✅ " + text); // Afiseaza "Puncte salvate! Total: X"
                    // Resetam formularul ca sa fim gata pentru urmatorul copil
                    setPointsData({ attended: false, hasBible: false, hasHandbook: false, lesson: false, friend: false, hasUniform: false });
                    setSelectedChildId('');
                } else {
                    // Afisam eroarea clara (ex: Nu e sesiune activa, Deja punctat etc.)
                    alert("❌ EROARE: " + text);
                }
            })
            .catch(err => {
                console.error(err);
                alert("❌ Eroare de conexiune cu serverul!");
            });
    };

    return (
        <div className="animate-in" style={{padding:'20px', background:'white', borderRadius:'15px', boxShadow:'0 4px 15px rgba(0,0,0,0.05)'}}>
            <h3 style={{borderBottom:'2px solid var(--accent)', paddingBottom:'10px', marginBottom:'20px'}}>⭐ Acordare Puncte Individuale</h3>

            <label style={{fontWeight:'bold', display:'block', marginBottom:'5px'}}>Selectează Copilul:</label>
            <select className="login-input" value={selectedChildId} onChange={e => setSelectedChildId(e.target.value)}>
                <option value="">-- Cauta copil --</option>
                {children.map(c => <option key={c.id} value={c.id}>{c.name} {c.surname}</option>)}
            </select>

            <div style={{marginTop:'20px', display:'grid', gridTemplateColumns:'1fr 1fr', gap:'15px'}}>
                {/* Prezența e cea mai importantă */}
                <div style={{gridColumn:'span 2', display:'flex', alignItems:'center', gap:'10px', padding:'15px', border:'2px solid #22c55e', background:'#f0fdf4', borderRadius:'8px'}}>
                    <input type="checkbox" style={{width:'25px', height:'25px', cursor:'pointer'}} name="attended" checked={pointsData.attended} onChange={handleCheckbox} />
                    <span style={{fontWeight:'bold', fontSize:'1.1rem', color:'#15803d'}}>✅ PREZENT (+1000)</span>
                </div>

                <label style={{display:'flex', alignItems:'center', gap:'10px', padding:'10px', background:'#f9f9f9', borderRadius:'8px', cursor:'pointer'}}>
                    <input type="checkbox" style={{width:'20px', height:'20px'}} name="hasBible" checked={pointsData.hasBible} onChange={handleCheckbox}/>
                    <span>📖 Biblie</span>
                </label>

                <label style={{display:'flex', alignItems:'center', gap:'10px', padding:'10px', background:'#f9f9f9', borderRadius:'8px', cursor:'pointer'}}>
                    <input type="checkbox" style={{width:'20px', height:'20px'}} name="hasHandbook" checked={pointsData.hasHandbook} onChange={handleCheckbox}/>
                    <span>📘 Manual</span>
                </label>

                <label style={{display:'flex', alignItems:'center', gap:'10px', padding:'10px', background:'#f9f9f9', borderRadius:'8px', cursor:'pointer'}}>
                    <input type="checkbox" style={{width:'20px', height:'20px'}} name="lesson" checked={pointsData.lesson} onChange={handleCheckbox}/>
                    <span>📝 Tema</span>
                </label>

                <label style={{display:'flex', alignItems:'center', gap:'10px', padding:'10px', background:'#f9f9f9', borderRadius:'8px', cursor:'pointer'}}>
                    <input type="checkbox" style={{width:'20px', height:'20px'}} name="friend" checked={pointsData.friend} onChange={handleCheckbox}/>
                    <span>👋 Prieten</span>
                </label>

                <label style={{gridColumn:'span 2', display:'flex', alignItems:'center', gap:'10px', padding:'15px', background:'#fff7ed', border:'2px solid #fdba74', borderRadius:'8px', color:'#c2410c', fontWeight:'bold', cursor:'pointer'}}>
                    <input type="checkbox" style={{width:'20px', height:'20px'}} name="hasUniform" checked={pointsData.hasUniform} onChange={handleCheckbox} />
                    👕 ARE UNIFORMĂ (+10.000)
                </label>
            </div>

            <div style={{marginTop:'25px', textAlign:'center', borderTop:'1px solid #eee', paddingTop:'15px'}}>
                <h4 style={{margin:'10px 0', fontSize:'1.2rem'}}>Total puncte: <span style={{color:'var(--accent)', fontSize:'1.8rem', fontWeight:'bold'}}>{calculateTotal()}</span></h4>
                <button onClick={handleSaveScore} style={{background:'var(--success)', color:'white', padding:'15px', width:'100%', borderRadius:'10px', fontWeight:'bold', fontSize:'1.1rem', border:'none', cursor:'pointer', boxShadow:'0 4px 10px rgba(5, 205, 153, 0.3)'}}>
                    💾 SALVEAZĂ PUNCTELE
                </button>
            </div>
        </div>
    );
};

// ==========================================
// 6. TEAMS MANAGER
// ==========================================
const TeamsManager = () => {
    const [mode, setMode] = useState('selection');
    const [myColor, setMyColor] = useState('red');
    const [availableKids, setAvailableKids] = useState([]);
    const [myTeamMembers, setMyTeamMembers] = useState([]);
    const [scores, setScores] = useState({ individual: 0, game: 0, total: 0 });
    const [ranking, setRanking] = useState([]);
    const [isDouble, setIsDouble] = useState(false);

    const teams = ['red', 'blue', 'green', 'yellow'];
    const teamStyles = {
        red: { bg: '#fee2e2', border: '#ef4444', text: '#b91c1c' },
        blue: { bg: '#dbeafe', border: '#3b82f6', text: '#1d4ed8' },
        green: { bg: '#dcfce7', border: '#22c55e', text: '#15803d' },
        yellow: { bg: '#fef9c3', border: '#eab308', text: '#a16207' }
    };
    const currentTheme = teamStyles[myColor];

    const refreshData = () => {
        fetch(`${API_URL}/teams/available`).then(r => r.ok?r.json():[]).then(setAvailableKids).catch(()=>{});
        fetch(`${API_URL}/teams/status/${myColor}`).then(r => r.ok?r.json():null).then(data => {
            if(data) {
                setMyTeamMembers(data.members || []);
                setScores({ individual: data.individualScore || 0, game: data.gameScore || 0, total: data.totalScore || 0 });
            }
        }).catch(()=>{});
    };

    useEffect(() => { refreshData(); const interval = setInterval(refreshData, 2000); return () => clearInterval(interval); }, [myColor]);

    const pickChild = (childId) => {
        fetch(`${API_URL}/teams/pick`, { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({ childId: childId, teamColor: myColor }) })
            .then(async res => { if(res.ok) refreshData(); else alert(await res.text()); });
    };

    const handleTeamPress = (c) => { if(!ranking.includes(c)) setRanking([...ranking, c]); };

    const sendGamePoints = () => {
        if(ranking.length===0) return alert("Selecteaza!");
        fetch(`${API_URL}/teams/game-round`, { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({ ranking, isDouble }) })
            .then(res => { if(res.ok) { alert("✅ Trimis!"); setRanking([]); setIsDouble(false); refreshData(); } });
    };

    const getPoints = (i) => { const pts=[1000,500,300,100]; return i<4 ? (isDouble?pts[i]*2:pts[i]) : 0; };

    return (
        <div className="animate-in" style={{padding:'20px', background:'white', borderRadius:'15px'}}>
            <div style={{display:'flex', gap:'10px', marginBottom:'20px'}}>
                <button onClick={()=>setMode('selection')} style={{flex:1, padding:'15px', borderRadius:'10px', border:'none', background:mode==='selection'?'var(--accent)':'#eee', color:mode==='selection'?'white':'#666', fontWeight:'bold'}}>1. Selectie Echipa</button>
                <button onClick={()=>setMode('games')} style={{flex:1, padding:'15px', borderRadius:'10px', border:'none', background:mode==='games'?'var(--accent)':'#eee', color:mode==='games'?'white':'#666', fontWeight:'bold'}}>2. Punctaje Jocuri</button>
            </div>

            {mode === 'selection' && (
                <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'20px'}}>
                    <div style={{gridColumn:'span 2', display:'flex', gap:'5px', overflowX:'auto', paddingBottom:'10px'}}>{teams.map(c => <button key={c} onClick={()=>setMyColor(c)} style={{flex:1, padding:'10px', borderRadius:'8px', fontWeight:'bold', textTransform:'uppercase', border:'none', background:myColor===c?teamStyles[c].border:'#f3f4f6', color:myColor===c?'white':'#666'}}>Sunt {c}</button>)}</div>
                    <div className="card" style={{height:'500px', display:'flex', flexDirection:'column', border:'1px dashed #ccc'}}><h3 style={{marginTop:0}}>👶 Disponibili ({availableKids.length})</h3><div style={{overflowY:'auto', flex:1}}>{availableKids.map(c => (<div key={c.id} onClick={()=>pickChild(c.id)} style={{padding:'10px', borderBottom:'1px solid #eee', cursor:'pointer', display:'flex', justifyContent:'space-between', background:'white'}}><strong>{c.name} {c.surname}</strong><span>+</span></div>))}</div></div>
                    <div className="card" style={{height:'500px', display:'flex', flexDirection:'column', background:currentTheme.bg, border:`2px solid ${currentTheme.border}`}}><h3 style={{marginTop:0, color:currentTheme.text, textTransform:'uppercase'}}>Echipa Mea</h3><div style={{fontSize:'2.5rem', fontWeight:'bold', color:currentTheme.text}}>{scores.total} <span style={{fontSize:'1rem'}}>pct</span></div><div style={{fontSize:'0.8rem', marginBottom:'10px', color:currentTheme.text}}>(Indiv: {scores.individual} + Joc: {scores.game})</div><div style={{overflowY:'auto', flex:1}}>{myTeamMembers.map(c => <div key={c.id} style={{padding:'5px 0', borderBottom:`1px solid ${currentTheme.text}40`, color:currentTheme.text}}>✅ {c.name} {c.surname}</div>)}</div></div>
                </div>
            )}

            {mode === 'games' && (
                <div className="card">
                    <div style={{display:'flex', justifyContent:'space-between', marginBottom:'20px'}}><h2 style={{margin:0}}>🎮 Panou Jocuri</h2><button onClick={()=>setIsDouble(!isDouble)} style={{padding:'10px', background:isDouble?'#f59e0b':'#eee', border:'none', borderRadius:'10px', fontWeight:'bold'}}>{isDouble?'🔥 DUBLU':'Normal'}</button></div>
                    <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'15px', marginBottom:'30px'}}>{teams.map(c => { const idx = ranking.indexOf(c); return <button key={c} onClick={()=>handleTeamPress(c)} disabled={idx!==-1} style={{padding:'30px', borderRadius:'15px', border:'none', fontSize:'1.5rem', fontWeight:'bold', textTransform:'uppercase', background:idx!==-1?'#333':teamStyles[c].border, color:'white', opacity:idx!==-1?0.6:1}}>{c} {idx!==-1 && <div style={{fontSize:'1rem', color:'#fbbf24'}}>LOCUL {idx+1} (+{getPoints(idx)})</div>}</button> })}</div>
                    <div style={{display:'flex', gap:'10px'}}><button onClick={()=>setRanking([])} style={{flex:1, padding:'15px', background:'#ef4444', color:'white', border:'none', borderRadius:'10px', fontWeight:'bold'}}>🗑️ RESET</button><button onClick={sendGamePoints} style={{flex:2, padding:'15px', background:'#22c55e', color:'white', border:'none', borderRadius:'10px', fontWeight:'bold'}}>✅ TRIMITE</button></div>
                </div>
            )}
        </div>
    );
};

// ==========================================
// 7. DEPARTMENTS PLAN (FINAL SI REPARAT)
// ==========================================
const DepartmentsPlan = ({ meetingId, user, onConfirm, isConfirmed }) => {
    const [meetings, setMeetings] = useState([]);
    const [selectedMeetingId, setSelectedMeetingId] = useState(meetingId);
    const [departments, setDepartments] = useState([]);
    const [leaders, setLeaders] = useState([]);
    const [plan, setPlan] = useState({ assignments: {}, directorDay: null });

    const isDirector = user && (user.role === 'DIRECTOR' || user.role === 'COORDONATOR');
    const directorsList = leaders.filter(l => l.role === 'DIRECTOR' || l.role === 'COORDONATOR');

    useEffect(() => {
        fetch(`${API_URL}/meetings`).then(r=>r.ok?r.json():[]).then(setMeetings);
        fetch(`${API_URL}/departments`).then(r=>r.ok?r.json():[]).then(setDepartments);
        fetch(`${API_URL}/leaders`).then(r=>r.ok?r.json():[]).then(setLeaders);
    }, []);

    useEffect(() => {
        const idToUse = meetingId || selectedMeetingId;
        if(!idToUse) return;
        fetch(`${API_URL}/departments/plan/${idToUse}`).then(r=>r.ok?r.json():null).then(d=>{ if(d) setPlan(d); });
    }, [meetingId, selectedMeetingId]);

    const assignLeader = (deptId, leaderIdStr) => {
        if(!leaderIdStr) return;
        const idToUse = meetingId || selectedMeetingId;
        const leaderId = parseInt(leaderIdStr);
        fetch(`${API_URL}/departments/assign`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({ meetingId: idToUse, deptId, leaderId }) })
            .then(res => {
                if (res.ok) {
                    const newPlan = { ...plan };
                    if(!newPlan.assignments[deptId]) newPlan.assignments[deptId] = [];
                    // FIX 1: Adaugam structura corecta { leader: ... } ca sa se vada imediat
                    const foundLeader = leaders.find(l=>l.id===leaderId);
                    newPlan.assignments[deptId].push({ leader: foundLeader, id: 'temp' + Date.now() });
                    setPlan(newPlan);
                } else alert("Eroare la asignare.");
            });
    };

    const removeAssignment = (deptId, leaderId) => {
        const idToUse = meetingId || selectedMeetingId;
        fetch(`${API_URL}/departments/remove?meetingId=${idToUse}&deptId=${deptId}&leaderId=${leaderId}`, { method: 'DELETE' })
            .then(res => {
                if (res.ok) {
                    const newPlan = { ...plan };
                    if (newPlan.assignments[deptId]) {
                        // FIX 2: Filtram corect verificand leader.id
                        newPlan.assignments[deptId] = newPlan.assignments[deptId].filter(item => item.leader && item.leader.id !== leaderId);
                        setPlan(newPlan);
                    }
                }
            });
    };

    const setDirector = (leaderId) => {
        if(!leaderId) return;
        const idToUse = meetingId || selectedMeetingId;
        fetch(`${API_URL}/departments/director/${idToUse}`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: parseInt(leaderId) })
            .then(res => { if(res.ok) setPlan(prev => ({...prev, directorDay: leaders.find(l=>l.id===parseInt(leaderId))})); });
    };

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
                    <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'20px'}}>{departments.map(dept => (
                        <div key={dept.id} className="card" style={{position:'relative'}}>
                            <div style={{display:'flex', justifyContent:'space-between', alignItems:'center'}}><h3 style={{margin:0, fontSize:'1.2rem'}}>{dept.name}</h3><span style={{fontSize:'0.8rem', background:'#eee', padding:'3px 8px', borderRadius:'10px'}}>Necesar: {dept.minLeaders}-{dept.maxLeaders}</span></div>

                            {/* ZONA LISTA LIDERI - AICI ERA PROBLEMA */}
                            <div style={{margin:'15px 0', minHeight:'40px'}}>
                                {plan.assignments[dept.id]?.map(assignment => {
                                    // Protectie: Daca cumva vine obiect gol sau fara lider
                                    const l = assignment.leader;
                                    if(!l) return null;

                                    return (
                                        <div key={assignment.id || l.id} style={{padding:'8px', borderBottom:'1px solid #eee', display:'flex', justifyContent:'space-between', alignItems:'center'}}>
                                            {/* Aici afisam corect l.name (numele liderului din interiorul asignarii) */}
                                            <span style={{fontWeight:'bold'}}>👤 {l.name} {l.surname}</span>
                                            {(isDirector || (meetingId && isDirector)) && (
                                                <button onClick={() => removeAssignment(dept.id, l.id)} style={{color:'red', fontWeight:'bold', border:'none', background:'transparent', cursor:'pointer'}}>×</button>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>

                            {(isDirector || (meetingId && isDirector)) && (
                                <select className="login-input" style={{padding:'8px', fontSize:'0.9rem'}} onChange={(e) => { assignLeader(dept.id, e.target.value); e.target.value=''; }}>
                                    <option value="">+ Adauga Lider</option>
                                    {leaders.map(l => {
                                        // Verificam daca liderul e deja pus, uitandu-ne in interiorul obiectelor assignment
                                        const isAssignedHere = plan.assignments[dept.id]?.some(existing => existing.leader && existing.leader.id === l.id);
                                        if(isAssignedHere) return null;
                                        return <option key={l.id} value={l.id}>{l.name} {l.surname}</option>;
                                    })}
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
// 8. CALENDAR MANAGER (REPARAT)
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

    const addMeeting = () => {
        if(!newDate) return alert("Alege o data!");
        fetch(`${API_URL}/meetings/add`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({ date: newDate, description: newDesc }) }).then(() => { loadMeetings(); setNewDate(''); setNewDesc(''); });
    };

    const finalizeClose = () => {
        fetch(`${API_URL}/meetings/close/${activeSession.id}`, { method: 'POST' }).then(async res => {
            if(res.ok) { alert("✅ Seara incheiata cu succes!"); setShowFeedbackModal(false); setActiveSession(null); loadMeetings(); } else alert("Eroare la inchidere.");
        });
    };

    if (showFeedbackModal) return <MeetingFeedback meeting={activeSession} user={user} onComplete={finalizeClose} onCancel={() => setShowFeedbackModal(false)} />;

    if (activeSession) {
        return (
            <div className="animate-in">
                <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'20px'}}>
                    <div>
                        {/* Buton Înapoi stilizat corect */}
                        <button onClick={() => setActiveSession(null)} className="btn-secondary" style={{marginBottom:'10px'}}>
                            ⬅ Inapoi la Calendar
                        </button>
                        <h2 style={{margin:0}}>📅 {activeSession.date}</h2>
                    </div>
                    {isDirector && <button onClick={()=>{if(!window.confirm("⚠️ Esti sigur ca vrei sa inchei seara?")) return; setShowFeedbackModal(true)}} style={{background:'#ef4444', color:'white', padding:'10px 20px', borderRadius:'10px', fontWeight:'bold', border:'2px solid #b91c1c', cursor:'pointer'}}>🏁 INCHEIE SEARA</button>}
                </div>
                <div style={{display:'flex', gap:'10px', marginBottom:'20px'}}>
                    <button onClick={() => setActiveTab('plan')} style={{flex:1, padding:'10px', background:activeTab==='plan'?'var(--accent)':'white', color:activeTab==='plan'?'white':'black', borderRadius:'10px', border:'none', fontWeight:'bold'}}>📋 Organizare</button>
                    {(isOrganized || !isDirector) && (<><button onClick={() => setActiveTab('scoring')} style={{flex:1, padding:'10px', background:activeTab==='scoring'?'var(--accent)':'white', color:activeTab==='scoring'?'white':'black', borderRadius:'10px', border:'none', fontWeight:'bold'}}>⭐ Scoring Individual</button><button onClick={() => setActiveTab('teams')} style={{flex:1, padding:'10px', background:activeTab==='teams'?'var(--accent)':'white', color:activeTab==='teams'?'white':'black', borderRadius:'10px', border:'none', fontWeight:'bold'}}>🚩 Echipe & Jocuri</button></>)}
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
            <div className="card"><h3>Urmeaza...</h3>{meetings.length === 0 ? <p style={{color:'#888'}}>Nu exista intalniri viitoare.</p> : (<div style={{marginTop:'15px'}}>{meetings.map(m => (<div key={m.id} style={{display:'flex', justifyContent:'space-between', alignItems:'center', padding:'15px', borderBottom:'1px solid #eee'}}><div><div style={{fontSize:'1.1rem', fontWeight:'bold'}}>📅 {m.date}</div><div style={{color:'gray', fontSize:'0.9rem'}}>{m.description || 'Standard'}</div></div><div style={{display:'flex', gap:'10px'}}>{isDirector && (<button onClick={() => { setActiveSession(m); setActiveTab('plan'); }} style={{background:'#fff', color:'var(--accent)', border:'1px solid var(--accent)', padding:'8px 15px', borderRadius:'8px', fontWeight:'bold', cursor:'pointer'}}>✏️ Planifica</button>)}<button onClick={() => setActiveSession(m)} style={{background:'var(--success)', color:'white', padding:'10px 20px', borderRadius:'8px', fontWeight:'bold', border:'none', cursor:'pointer'}}>▶️ INTRA</button></div></div>))}</div>)}</div>
        </div>
    );
};

// ==========================================
// 9. REGISTRY & DOSARE
// ==========================================
const Registry = ({ user }) => {
    const [children, setChildren] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedChild, setSelectedChild] = useState(null);
    const [childWarnings, setChildWarnings] = useState([]);
    const [newWarning, setNewWarning] = useState({ description: '', suspension: false, remainingMeetings: 1 });

    const isDirector = user && (user.role === 'DIRECTOR' || user.role === 'COORDONATOR');

    useEffect(() => {
        fetch(`${API_URL}/children`).then(r => r.ok?r.json():[]).then(async (kids) => {
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
    }, []);

    const openChildFile = (child) => {
        setSelectedChild(child);
        setChildWarnings([]);
        fetch(`${API_URL}/warnings/child/${child.id}`).then(r => r.ok ? r.json() : []).then(data => setChildWarnings(Array.isArray(data) ? data : [])).catch(() => setChildWarnings([]));
    };

    const handleAddWarning = () => {
        if(!newWarning.description) { alert("Scrie motivul!"); return; }
        const payload = { childId: selectedChild.id, description: newWarning.description, suspension: newWarning.suspension, remainingMeetings: newWarning.suspension ? newWarning.remainingMeetings : 0 };
        fetch(`${API_URL}/warnings/add`, { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload) })
            .then(res => { if(res.ok) { alert("✅ Salvat!"); openChildFile(selectedChild); setNewWarning({ description: '', suspension: false, remainingMeetings: 1 }); } else alert("Eroare server."); });
    };

    const getProgressMessages = (child) => {
        const streak = child.attendanceStreak || 0;
        const lessons = child.lessonsCompleted || 0;
        const msgs = [];
        if (!child.hasShirt) msgs.push(5-streak <= 0 ? "🎁 Eligibil TRICOU!" : `👕 Mai are ${5-streak} prezente pana la Tricou.`);
        else if (!child.hasHat) msgs.push(10-streak <= 0 ? "🎁 Eligibil CACIULA!" : `🧢 Mai are ${10-streak} prezente pana la Caciula.`);
        msgs.push(`🏅 Mai are ${3-(lessons%3)} lectii pana la Insigna.`);
        return msgs;
    };

    if (loading) return <p>Se incarca datele...</p>;

    if (selectedChild) {
        const isCurrentlySuspended = Array.isArray(childWarnings) && childWarnings.length > 0 && childWarnings[0].suspension && childWarnings[0].remainingMeetings > 0;
        return (
            <div className="animate-in">
                {/* MODIFICAT: Buton cu clasa CSS */}
                <button onClick={() => setSelectedChild(null)} className="btn-secondary" style={{marginBottom:'20px'}}>
                    ⬅ Inapoi
                </button>

                <div className="card" style={{borderLeft: isCurrentlySuspended ? '8px solid red' : '8px solid green'}}>
                    <div style={{display:'flex', justifyContent:'space-between', alignItems:'center'}}><h1>📂 Dosar: {selectedChild.name} {selectedChild.surname}</h1>{isCurrentlySuspended && <span style={{background:'red', color:'white', padding:'10px 20px', borderRadius:'20px', fontWeight:'bold', fontSize:'1.2rem', boxShadow:'0 4px 10px rgba(255,0,0,0.3)'}}>⛔ SUSPENDAT</span>}</div>
                    <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'30px', marginTop:'20px'}}>
                        <div>
                            <h3>📋 Date & Contact</h3>
                            <p><strong>Parinte:</strong> {selectedChild.parentName} ({selectedChild.parentPhone})</p>
                            <p><strong>Varsta:</strong> {selectedChild.age} ani</p>
                            <p><strong>Prezente Totale:</strong> {selectedChild.totalAttendance || 0}</p>
                            <h3 style={{marginTop:'20px'}}>🎒 Inventar</h3>
                            <div style={{display:'flex', gap:'10px'}}><span className="badge" style={{background: selectedChild.hasManual ? '#dcfce7':'#eee'}}>📘 Manual</span><span className="badge" style={{background: selectedChild.hasShirt ? '#dcfce7':'#eee'}}>👕 Tricou</span><span className="badge" style={{background: selectedChild.hasHat ? '#dcfce7':'#eee'}}>🧢 Caciula</span></div>
                            <div style={{marginTop:'15px', background:'#f0f9ff', padding:'15px', borderRadius:'10px'}}><strong>🚀 Progres:</strong>{getProgressMessages(selectedChild).map((m, i) => <div key={i} style={{fontSize:'0.9rem', marginTop:'5px'}}>• {m}</div>)}</div>
                        </div>
                        <div>
                            <h3 style={{color:'red'}}>⚠️ Disciplina & Istoric</h3>

                            {/* MODIFICAT: Stil curat pentru textarea */}
                            {isDirector && (
                                <div style={{background:'#fff5f5', padding:'15px', borderRadius:'10px', border:'1px solid #feb2b2'}}>
                                    <label style={{fontWeight:'bold', color:'#991b1b', marginBottom:'5px', display:'block'}}>Motiv sancțiune:</label>
                                    <textarea
                                        placeholder="Scrie aici motivul (ex: A vorbit urât)..."
                                        value={newWarning.description}
                                        onChange={e => setNewWarning({...newWarning, description: e.target.value})}
                                    />
                                    <div style={{display:'flex', alignItems:'center', gap:'15px', marginTop:'10px'}}>
                                        <label style={{color:'red', fontWeight:'bold', display:'flex', alignItems:'center', gap:'5px', cursor:'pointer'}}>
                                            <input type="checkbox" style={{width:'20px', height:'20px'}} checked={newWarning.suspension} onChange={e => setNewWarning({...newWarning, suspension: e.target.checked})} />
                                            ⛔ SUSPENDARE?
                                        </label>

                                        {newWarning.suspension && (
                                            <label style={{display:'flex', alignItems:'center', gap:'5px', fontWeight:'bold', color:'#991b1b'}}>
                                                Nr. Ture:
                                                <input type="number" min="1" value={newWarning.remainingMeetings} onChange={e => setNewWarning({...newWarning, remainingMeetings: parseInt(e.target.value)})} style={{width:'60px'}} />
                                            </label>
                                        )}
                                    </div>
                                    <button onClick={handleAddWarning} style={{background:'#ef4444', color:'white', padding:'12px', width:'100%', borderRadius:'8px', fontWeight:'bold', marginTop:'15px', border:'none', cursor:'pointer', boxShadow:'0 4px 10px rgba(239, 68, 68, 0.2)'}}>
                                        SALVEAZĂ SANCȚIUNEA
                                    </button>
                                </div>
                            )}

                            <h4 style={{marginTop:'20px'}}>📜 Istoric</h4><div style={{maxHeight:'150px', overflowY:'auto'}}>{Array.isArray(childWarnings) && childWarnings.map(w => (<div key={w.id} style={{borderBottom:'1px solid #eee', padding:'8px 0', fontSize:'0.9rem'}}><div style={{fontWeight:'bold'}}>{w.date} {w.suspension ? <span style={{color:'red'}}>(Suspendat {w.remainingMeetings} ture)</span> : '(Avertisment)'}</div><div style={{color:'#555'}}>{w.description}</div></div>))}</div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="animate-in">
            <h2>📋 Registru</h2>
            <div className="table-container" style={{overflowX:'auto'}}>
                <table style={{minWidth:'900px'}}><thead><tr><th>Nume</th><th>Status</th><th>Prezente</th><th>Puncte</th><th>Actiuni</th></tr></thead><tbody>{children.map(c => (<tr key={c.id} style={{background: c.isSuspended ? '#fee2e2' : 'transparent'}}><td style={{fontWeight:'bold'}}>{c.name} {c.surname}</td><td>{c.isSuspended ? <span style={{background:'red', color:'white', padding:'2px 8px', borderRadius:'4px', fontSize:'0.8rem', fontWeight:'bold'}}>⛔ SUSPENDAT</span> : <span style={{color:'green', fontSize:'0.8rem'}}>OK</span>}</td><td>{c.totalAttendance || 0}</td><td style={{fontWeight:'bold', color:'var(--accent)'}}>{c.seasonPoints || 0}</td><td><button onClick={() => openChildFile(c)} style={{background:'var(--accent)', color:'white', padding:'5px 15px', borderRadius:'5px'}}>Dosar</button></td></tr>))}</tbody></table>
            </div>
        </div>
    );
};

// ==========================================
// 10. DASHBOARD (FINAL - STERGE CHENARUL GALBEN)
// ==========================================
const Dashboard = ({ user }) => {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const isChild = user && user.hasOwnProperty('parentPhone');

    useEffect(() => {
        const url = user ? `${API_URL}/dashboard/stats?leaderId=${user.id}` : `${API_URL}/dashboard/stats`;
        fetch(url).then(r => r.ok ? r.json() : null).then(data => { if(data) { setStats(data); setLoading(false); }}).catch(() => setLoading(false));
    }, [user]);

    // --- FUNCTIA MAGICĂ DE REACTUALIZARE ---
    const handleDeleteNotification = (id) => {
        if(!window.confirm("Ștergi notificarea?")) return;

        // 1. Trimitem cererea la server
        fetch(`${API_URL}/notifications/${id}`, { method: 'DELETE' })
            .then(res => {
                if(res.ok) {
                    // 2. AICI STERGE CHENARUL: Actualizam starea locala (filtram lista)
                    setStats(prevStats => {
                        // Cream o lista noua care contine TOT, minus elementul cu ID-ul sters
                        const newNotifications = prevStats.notifications.filter(n => n.id !== id);

                        // Returnam obiectul stats actualizat
                        return {
                            ...prevStats,
                            notifications: newNotifications
                        };
                    });
                } else {
                    alert("Eroare la ștergere.");
                }
            })
            .catch(() => alert("Eroare server."));
    };

    const getProgressMessages = () => {
        if (!isChild) return [];
        const msgs = [];
        const streak = user.attendanceStreak || 0;
        if (!user.hasShirt) msgs.push((5-streak) <= 0 ? "🎁 Eligibil TRICOU!" : `👕 Mai are ${5-streak} prezente pana la Tricou.`);
        else if (!user.hasHat) msgs.push((user.attendanceStreak > 5 ? 10 : 5) - streak <= 0 ? "🎁 Eligibil CACIULA!" : `🧢 Mai are ${(user.attendanceStreak > 5 ? 10 : 5) - streak} prezente pana la Caciula.`);
        return msgs;
    };

    if (loading) return <div className="animate-in"><p>Se incarca...</p></div>;
    if (!stats) return <div className="animate-in"><p>Eroare incarcare date.</p></div>;

    const progressMsgs = getProgressMessages();
    // Pentru copii nu aratam notificarile din baza de date, doar progresul
    const notificationsToDisplay = isChild ? [] : (stats.notifications || []);

    return (
        <div className="animate-in">
            <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'30px'}}>
                <div><h2 style={{color:'var(--accent)', fontWeight:'800', fontSize:'2rem'}}>{stats.clubName}</h2><p style={{color:'var(--text-secondary)'}}>Salut, {user ? user.name : 'Utilizator'}!</p></div>
                <div style={{textAlign:'right'}}><span className="badge" style={{background:'#e0e7ff', color:'var(--accent)', fontSize:'1rem'}}>📅 {new Date().toLocaleDateString('ro-RO')}</span></div>
            </div>
            <div className="stats-grid">
                <div className="card"><h3>👶 Copii</h3><p className="big-number">{stats.kidsCount}</p></div>
                <div className="card"><h3>👔 Lideri</h3><p className="big-number">{stats.leadersCount}</p></div>
                <div className="card" style={{overflowY:'auto', maxHeight:'160px'}}><h3>📞 Directori</h3>{stats.directors && stats.directors.map(dir => (<div key={dir.id} style={{borderBottom:'1px solid #eee', padding:'5px 0', fontSize:'0.9rem'}}><strong>{dir.name} {dir.surname}</strong><br/><span style={{color:'var(--accent)'}}>{dir.phoneNumber || '-'}</span></div>))}</div>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '30px' }}>
                <div className="card">
                    <h3 style={{marginBottom:'15px'}}>🔔 {isChild ? 'Notificari' : 'Notificari & Feedback'}</h3>

                    {/* Mesaje Copii (Raman la fel) */}
                    {isChild && progressMsgs.map((msg, i) => (<div key={i} style={{background:'#eff6ff', borderLeft:'4px solid #3b82f6', color:'#1e40af', padding:'15px', borderRadius:'8px', marginBottom:'10px', fontWeight:'bold', fontSize:'0.95rem'}}>{msg}</div>))}

                    {/* NOTIFICARI LIDERI */}
                    {notificationsToDisplay.length > 0 ? (
                        notificationsToDisplay.map((n) => (
                            <div key={n.id} style={{
                                background:'#fff3cd',
                                color:'#856404',
                                padding:'15px',
                                paddingRight:'35px', // Loc pentru X
                                borderRadius:'8px',
                                marginBottom:'10px',
                                whiteSpace:'pre-wrap',
                                borderLeft:'4px solid #ffc107',
                                fontSize:'0.9rem',
                                position: 'relative' // Necesar pt X
                            }}>
                                {/* Mesajul */}
                                {n.message}

                                {/* Butonul X */}
                                <div
                                    onClick={() => handleDeleteNotification(n.id)}
                                    style={{
                                        position: 'absolute',
                                        top: '5px',
                                        right: '5px',
                                        width: '24px',
                                        height: '24px',
                                        cursor: 'pointer',
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        color: '#856404',
                                        fontWeight: 'bold',
                                        fontSize: '1.2rem',
                                        opacity: 0.5
                                    }}
                                    title="Șterge"
                                    onMouseOver={(e) => e.target.style.opacity = 1}
                                    onMouseOut={(e) => e.target.style.opacity = 0.5}
                                >
                                    ×
                                </div>
                            </div>
                        ))
                    ) : ((!isChild || progressMsgs.length === 0) && <p style={{color:'#888'}}>Nu ai notificari noi.</p>)}
                </div>
                <div className="card"><h3 style={{marginBottom:'15px'}}>📌 Planificare</h3>{stats.reminders && stats.reminders.map((r, i) => <div key={i} style={{padding:'10px', borderBottom:'1px solid #eee'}}>✅ {r}</div>)}</div>
            </div>
        </div>
    );
};


// ==========================================
// 11. MY PROFILE
// ==========================================
const MyProfile = ({ user, onUpdateUser }) => {
    const isChild = user.hasOwnProperty('parentPhone');
    const [isEditing, setIsEditing] = useState(false);
    const [formData, setFormData] = useState({ ...user, password: '' });
    const [loading, setLoading] = useState(false);
    const [allDepartments, setAllDepartments] = useState([]);

    // State pentru stergere
    const [showDeleteInput, setShowDeleteInput] = useState(false);
    const [deleteCode, setDeleteCode] = useState('');

    useEffect(() => {
        if (!isChild) {
            fetch(`${API_URL}/leaders/${user.id}`).then(r => r.ok ? r.json() : null).then(data => { if (data) { onUpdateUser(data); setFormData({ ...data, password: '' }); }});
            fetch(`${API_URL}/departments`).then(r => r.ok ? r.json() : []).then(setAllDepartments);
        }
    }, []);

    const handleSave = () => {
        setLoading(true);
        const endpoint = isChild ? `${API_URL}/children/${user.id}` : `${API_URL}/leaders/${user.id}`;
        fetch(endpoint, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(formData) })
            .then(res => res.json()).then(u => { setLoading(false); setIsEditing(false); onUpdateUser(u); alert("✅ Actualizat!"); }).catch(() => { setLoading(false); alert("Eroare server."); });
    };

    const toggleDepartment = (dept) => {
        const hasDept = formData.departments.some(d => d.id === dept.id);
        if (hasDept) setFormData({ ...formData, departments: formData.departments.filter(d => d.id !== dept.id) });
        else setFormData({ ...formData, departments: [...formData.departments, dept] });
    };

    const requestDeletionCode = () => {
        if(!window.confirm("⚠️ Esti sigur ca vrei sa initiezi stergerea contului? Vei primi un cod de la Director.")) return;

        fetch(`${API_URL}/account/request-deletion`, {
            method: 'POST', headers: {'Content-Type':'application/json'},
            body: JSON.stringify({ id: user.id, role: isChild ? 'CHILD' : 'LEADER' })
        }).then(res => res.text()).then(msg => alert(msg));
    };

    const performDeletion = () => {
        if(!deleteCode) return alert("Introdu codul!");
        if(!window.confirm("🚨 ATENTIE! Aceasta actiune este IREVERSIBILA. Toate datele tale vor fi sterse.")) return;

        fetch(`${API_URL}/account/delete`, {
            method: 'POST', headers: {'Content-Type':'application/json'},
            body: JSON.stringify({ id: user.id, role: isChild ? 'CHILD' : 'LEADER', code: deleteCode })
        }).then(async res => {
            const text = await res.text();
            if(res.ok) {
                alert("✅ Cont sters cu succes. La revedere!");
                window.location.reload();
            } else {
                alert("❌ Eroare: " + text);
            }
        });
    };

    const isAdmin = !isChild && user.id === 1;

    return (
        <div className="animate-in">
            <div style={{display:'flex', justifyContent:'space-between', alignItems:'center'}}>
                <h2>👤 Contul Meu</h2>
                <button onClick={() => isEditing ? handleSave() : setIsEditing(true)} style={{background: isEditing ? 'var(--success)' : 'var(--accent)', color: 'white', padding:'10px 20px', borderRadius:'10px', fontWeight:'bold'}}>
                    {loading ? '...' : (isEditing ? '💾 Salveaza' : '✏️ Editeaza')}
                </button>
            </div>

            <div className="card" style={{marginTop:'20px', display:'flex', gap:'20px', alignItems:'center'}}>
                <div style={{width:'60px', height:'60px', borderRadius:'50%', background:'var(--accent)', color:'white', display:'flex', justifyContent:'center', alignItems:'center', fontSize:'2rem', fontWeight:'bold'}}>{user.name ? user.name.charAt(0) : 'U'}</div>
                <div>{isEditing ? <div style={{display:'flex', gap:'5px'}}><input className="login-input" value={formData.name} onChange={e=>setFormData({...formData, name:e.target.value})}/><input className="login-input" value={formData.surname} onChange={e=>setFormData({...formData, surname:e.target.value})}/></div> : <h1>{user.name} {user.surname}</h1>}<span className="badge" style={{background:'#e0f2fe', color:'#0284c7'}}>{isChild ? '👶 Copil' : `👔 ${user.role || 'Lider'}`}</span></div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '30px', marginTop:'20px' }}>
                <div className="card">
                    <h3>📋 Date Personale</h3>
                    <div style={{marginTop:'15px', display:'flex', flexDirection:'column', gap:'10px'}}>
                        {!isChild ? (
                            <>
                                <div><label>Telefon:</label>{isEditing ? <input className="login-input" value={formData.phoneNumber||''} onChange={e=>setFormData({...formData, phoneNumber:e.target.value})}/> : <p>📞 {user.phoneNumber||'Nespecificat'}</p>}</div>
                                {isEditing && (<div style={{background:'#fff1f2', padding:'10px', borderRadius:'8px', border:'1px solid #ffccd5', marginTop:'10px'}}><label style={{fontWeight:'bold', color:'#e11d48'}}>🔒 Schimba Parola:</label><input type="password" className="login-input" style={{background:'white', margin:0}} value={formData.password} onChange={e=>setFormData({...formData, password: e.target.value})} /></div>)}
                                <div style={{marginTop:'20px', paddingTop:'15px', borderTop:'1px solid #eee'}}><label style={{fontWeight:'bold'}}>Departamentele Mele:</label>{isEditing ? (<div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'10px'}}>{allDepartments.map(dept => (<label key={dept.id} style={{display:'flex', alignItems:'center', gap:'8px'}}><input type="checkbox" checked={formData.departments.some(d => d.id === dept.id)} onChange={() => toggleDepartment(dept)} />{dept.name}</label>))}</div>) : (<div style={{display:'flex', flexWrap:'wrap', gap:'8px'}}>{user.departments && user.departments.map(d => (<span key={d.id} className="badge" style={{background:'#dcfce7', color:'#166534'}}>🏷️ {d.name}</span>))}</div>)}</div>
                            </>
                        ) : (<><div><label>Data Nasterii:</label>{isEditing ? <input type="date" className="login-input" value={formData.birthDate||''} onChange={e=>setFormData({...formData, birthDate:e.target.value})}/> : <p>🎂 {user.birthDate||'-'}</p>}</div><div><label>Parinte:</label>{isEditing ? <input className="login-input" value={formData.parentName||''} onChange={e=>setFormData({...formData, parentName:e.target.value})}/> : <p>👤 {user.parentName||'-'}</p>}</div><div><label>Telefon:</label>{isEditing ? <input className="login-input" value={formData.parentPhone||''} onChange={e=>setFormData({...formData, parentPhone:e.target.value})}/> : <p>📞 {user.parentPhone||'-'}</p>}</div></>)}
                    </div>
                </div>

                {!isAdmin && (
                    <div className="card" style={{border:'2px solid #fee2e2'}}>
                        <h3 style={{color:'#dc2626'}}>🚨 Zona de Pericol</h3>
                        <p style={{fontSize:'0.9rem', marginBottom:'15px'}}>Dacă vrei să ștergi contul, trebuie să soliciți un cod de la Administrator.</p>

                        {!showDeleteInput ? (
                            <div style={{display:'flex', flexDirection:'column', gap:'10px'}}>
                                <button onClick={requestDeletionCode} style={{padding:'10px', background:'orange', color:'white', border:'none', borderRadius:'8px', fontWeight:'bold', cursor:'pointer'}}>
                                    📨 Pasul 1: Solicita Cod Ștergere
                                </button>
                                <button onClick={() => setShowDeleteInput(true)} style={{padding:'10px', background:'#fee2e2', color:'#dc2626', border:'1px solid #dc2626', borderRadius:'8px', fontWeight:'bold', cursor:'pointer'}}>
                                    🗑️ Pasul 2: Am primit codul, vreau să șterg!
                                </button>
                            </div>
                        ) : (
                            <div style={{background:'#fef2f2', padding:'15px', borderRadius:'10px'}}>
                                <label style={{fontWeight:'bold', color:'#dc2626'}}>Introdu Codul de Confirmare:</label>
                                <input className="login-input" placeholder="Ex: A3F2..." value={deleteCode} onChange={e => setDeleteCode(e.target.value)} style={{borderColor:'#dc2626'}}/>
                                <div style={{display:'flex', gap:'10px', marginTop:'10px'}}>
                                    <button onClick={performDeletion} style={{flex:1, padding:'10px', background:'#dc2626', color:'white', border:'none', borderRadius:'8px', fontWeight:'bold', cursor:'pointer'}}>CONFIRMĂ ȘTERGEREA</button>
                                    {/* MODIFICAT: Buton cu clasa CSS */}
                                    <button onClick={() => {setShowDeleteInput(false); setDeleteCode('');}} className="btn-secondary">
                                        Anulează
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

// ==========================================
// 12. REGISTER
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
                    setMsg('✅ Cont creat! Redirectionare...');
                    setTimeout(onSwitchToLogin, 1500);
                } else {
                    setMsg('❌ ' + text);
                }
            })
            .catch(() => setMsg('❌ Eroare conexiune server.'))
            .finally(() => setLoading(false));
    };

    return (
        <div className="login-container"><div className="login-box" style={{width:'400px', maxHeight:'90vh', overflowY:'auto'}}>
            <div style={{marginBottom:'20px'}}><AwanaLogo width="180px"/></div>
            <h2 style={{marginBottom:'20px'}}>Inregistrare</h2>
            <div style={{display:'flex', gap:'5px', marginBottom:'15px'}}>
                {['CHILD','LEADER','DIRECTOR'].map(t => (
                    <button key={t} type="button" onClick={()=>{setRoleType(t); setSelectedDepts(new Set());}}
                            style={{flex:1, padding:'8px', background:roleType===t?'#eee':'transparent', fontWeight:'bold', border:'1px solid #eee', borderRadius:'5px'}}>
                        {t==='CHILD'?'Copil':t==='LEADER'?'Lider':'Director'}
                    </button>
                ))}
            </div>
            <form onSubmit={doRegister}>
                <input placeholder="Prenume" className="login-input" onChange={e=>setForm({...form, name:e.target.value})} required/>
                <input placeholder="Nume" className="login-input" onChange={e=>setForm({...form, surname:e.target.value})} required/>
                <input type="password" placeholder="Parola" className="login-input" onChange={e=>setForm({...form, pass:e.target.value})} required/>

                {roleType !== 'CHILD' && (
                    <input placeholder="🔒 Cod de Acces (de la Director)" className="login-input" style={{borderColor:'red'}} onChange={e=>setForm({...form, regCode:e.target.value})} required/>
                )}

                {roleType === 'CHILD' ? (
                    <>
                        <label style={{fontSize:'0.8rem', display:'block', textAlign:'left'}}>Data Nasterii:</label>
                        <input type="date" className="login-input" onChange={e=>setForm({...form, birthDate:e.target.value})} required/>
                        <input placeholder="Nume Parinte" className="login-input" onChange={e=>setForm({...form, parentName:e.target.value})} required/>
                        <input placeholder="Telefon Parinte" className="login-input" onChange={e=>setForm({...form, phone:e.target.value})} required/>
                    </>
                ) : (
                    <>
                        <input placeholder="Telefonul Tau" className="login-input" onChange={e=>setForm({...form, phone:e.target.value})} required/>
                        <div style={{textAlign:'left', marginTop:'15px'}}>
                            <label style={{fontWeight:'bold', fontSize:'0.9rem'}}>Departamente:</label>
                            <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'8px', marginTop:'8px', maxHeight:'150px', overflowY:'auto', background:'#f9f9f9', padding:'10px', borderRadius:'10px', border:'1px solid #eee'}}>
                                {departments.map(d => (
                                    <div key={d.id} onClick={() => toggleDept(d.id)} style={{cursor:'pointer', display:'flex', alignItems:'center', gap:'5px', fontSize:'0.85rem'}}>
                                        <input type="checkbox" checked={selectedDepts.has(d.id)} readOnly style={{width:'16px', height:'16px'}}/>{d.name}
                                    </div>
                                ))}
                            </div>
                        </div>
                    </>
                )}
                <p style={{color: msg.startsWith('✅')?'green':'red', fontWeight:'bold', marginTop:'10px'}}>{msg}</p>
                <button type="submit" disabled={loading} style={{width:'100%', padding:'12px', background:'var(--accent)', color:'white', borderRadius:'8px', fontWeight:'bold', marginTop:'15px', cursor: loading ? 'wait' : 'pointer'}}>
                    {loading ? 'Se proceseaza...' : 'Creeaza Cont'}
                </button>
            </form>
            <p onClick={onSwitchToLogin} style={{marginTop:'15px', cursor:'pointer', color:'var(--accent)'}}>Ai deja cont? Logheaza-te</p>
        </div></div>
    );
};

// ==========================================
// 13. LOGIN
// ==========================================
const Login = ({ onLogin, onSwitchToRegister }) => {
    const [form, setForm] = useState({ user:'', pass:'', role: 'LEADER' });
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
                username: form.user,
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
                    else setErr(data.message || 'Date gresite!');
                } else {
                    const text = await r.text();
                    setErr(text || 'Date gresite!');
                }
            })
            .catch((error) => {
                clearTimeout(timeoutId);
                if (error.name === 'AbortError') setErr('❌ Serverul nu raspunde (Timeout).');
                else setErr('❌ Server offline sau eroare conexiune.');
            })
            .finally(() => setLoading(false));
    };

    return (
        <div className="login-container">
            <div className="login-box" style={{maxWidth:'400px'}}>
                <div style={{marginBottom:'20px'}}><AwanaLogo width="180px"/></div>
                <h2 style={{marginBottom:'20px'}}>Autentificare</h2>

                <div style={{display:'flex', gap:'5px', marginBottom:'15px'}}>
                    {['CHILD','LEADER','DIRECTOR'].map(t => (
                        <button key={t} type="button"
                                onClick={()=>setForm({...form, role:t})}
                                style={{
                                    flex:1, padding:'8px',
                                    background: form.role === t ? 'var(--accent)' : '#eee',
                                    color: form.role === t ? 'white' : 'black',
                                    fontWeight:'bold', border:'none', borderRadius:'5px', cursor: 'pointer',
                                    transition: 'all 0.2s'
                                }}>
                            {t==='CHILD'?'Copil':t==='LEADER'?'Lider':'Director'}
                        </button>
                    ))}
                </div>

                <form onSubmit={doLogin}>
                    <input
                        placeholder={form.role === 'CHILD' ? "Numele tau" : "Telefon sau Nume"}
                        className="login-input"
                        value={form.user}
                        onChange={e=>setForm({...form, user:e.target.value})}
                        required
                    />
                    <input type="password" placeholder="Parola" className="login-input" value={form.pass} onChange={e=>setForm({...form, pass:e.target.value})} required/>

                    {err && <div style={{color:'red', background:'#fee2e2', padding:'10px', borderRadius:'5px', marginBottom:'15px', fontSize:'0.9rem'}}>{err}</div>}

                    <button type="submit" disabled={loading} style={{width:'100%', padding:'12px', background:'var(--accent)', color:'white', borderRadius:'8px', fontWeight:'bold', marginTop:'10px', cursor: loading ? 'wait' : 'pointer'}}>
                        {loading ? 'Se verifica...' : 'Intra in Cont'}
                    </button>
                </form>

                <p onClick={onSwitchToRegister} style={{marginTop:'20px', cursor:'pointer', color:'var(--accent)', borderTop:'1px solid #eee', paddingTop:'15px'}}>
                    Nu ai cont? Inregistreaza-te
                </p>
            </div>
        </div>
    );
};

// ==========================================
// 14. APP MAIN (FINAL & STABILIZAT)
// ==========================================
function App() {
    const [loading, setLoading] = useState(true);
    const [user, setUser] = useState(null);
    const [register, setRegister] = useState(false);
    const [page, setPage] = useState('dashboard');
    const [theme, setTheme] = useState('light');

    useEffect(() => { setTimeout(() => setLoading(false), 2000); }, []);

    const toggleTheme = () => { const t = theme === 'light' ? 'dark' : 'light'; setTheme(t); document.documentElement.setAttribute('data-theme', t); };

    if (loading) return <SplashScreen />;

    if (!user) return register ? <Register onSwitchToLogin={() => setRegister(false)} /> : <Login onLogin={setUser} onSwitchToRegister={() => setRegister(true)} />;

    const isChild = user.hasOwnProperty('parentPhone');
    const isDirector = user.role === 'DIRECTOR' || user.role === 'COORDONATOR';

    return (
        <div className="app-container">
            <div className="sidebar">
                <div style={{marginBottom:'30px', paddingLeft:'10px'}}><AwanaLogo width="160px" /></div>
                <button className={`nav-btn ${page==='dashboard'?'active':''}`} onClick={()=>setPage('dashboard')}>📊 Dashboard</button>
                <button className={`nav-btn ${page==='profile'?'active':''}`} onClick={()=>setPage('profile')}>👤 Contul Meu</button>

                {!isChild && (
                    <>
                        <div style={{margin:'20px 0 5px 15px', fontSize:'0.8rem', color:'var(--text-secondary)', fontWeight:'bold'}}>MANAGEMENT</div>
                        <button className={`nav-btn ${page==='calendar'?'active':''}`} onClick={()=>setPage('calendar')}>📅 Calendar & Sesiuni</button>
                        <button className={`nav-btn ${page==='departments'?'active':''}`} onClick={()=>setPage('departments')}>🏢 Departamente</button>
                        <button className={`nav-btn ${page==='registry'?'active':''}`} onClick={()=>setPage('registry')}>🗂️ Registru Copii</button>

                        {isDirector && (
                            <button className={`nav-btn ${page==='leaders'?'active':''}`} onClick={()=>setPage('leaders')}>👔 Registru Lideri</button>
                        )}
                    </>
                )}

                <div style={{marginTop: 'auto'}}>
                    <button className="theme-toggle" onClick={toggleTheme} style={{width:'100%', marginBottom:'10px'}}>{theme==='light' ? '🌙' : '☀️'}</button>

                    {/* AICI ESTE MODIFICAREA CRITICĂ: Butonul de Ieșire cu RELOAD */}
                    <button
                        className="nav-btn"
                        onClick={() => {
                            setUser(null);
                            window.location.reload(); // <--- ASTA REZOLVĂ CRASH-URILE
                        }}
                        style={{color:'var(--danger)'}}
                    >
                        🚪 Iesire
                    </button>
                </div>
            </div>

            <div className="main-content">
                {page === 'dashboard' && <Dashboard user={user} />}
                {page === 'profile' && <MyProfile user={user} onUpdateUser={setUser} />}
                {!isChild && page === 'calendar' && <CalendarManager user={user} />}
                {!isChild && page === 'departments' && <DepartmentsList user={user} />}
                {!isChild && page === 'registry' && <Registry user={user} />}
                {!isChild && page === 'leaders' && isDirector && <LeadersRegistry />}
            </div>
        </div>
    );
}

export default App;