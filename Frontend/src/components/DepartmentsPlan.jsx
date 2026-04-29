import React, { useState, useEffect } from 'react';
import { API_URL } from '../config';

const DepartmentsPlan = ({ meetingId, user, onConfirm, isConfirmed }) => {
    const [meetings, setMeetings] = useState([]);
    const [selectedMeetingId, setSelectedMeetingId] = useState(meetingId);
    const [departments, setDepartments] = useState([]);
    const [leaders, setLeaders] = useState([]);
    const [plan, setPlan] = useState({ assignments: {}, directorDay: null });
    const [leaderSearch, setLeaderSearch] = useState('');
    const [activeSearchDept, setActiveSearchDept] = useState(null);
    const [directorSearch, setDirectorSearch] = useState('');
    const [directorDropOpen, setDirectorDropOpen] = useState(false);

    const isDirector = user && (user.role === 'DIRECTOR' || user.role === 'COORDONATOR');
    const directorsList = leaders.filter(l => l.role === 'DIRECTOR' || l.role === 'COORDONATOR');

    useEffect(() => { fetch(`${API_URL}/meetings`).then(r=>r.ok?r.json():[]).then(setMeetings); fetch(`${API_URL}/departments`).then(r=>r.ok?r.json():[]).then(setDepartments); fetch(`${API_URL}/leaders`).then(r=>r.ok?r.json():[]).then(setLeaders); }, []);

    useEffect(() => { const idToUse = meetingId || selectedMeetingId; if(!idToUse) return; fetch(`${API_URL}/departments/plan/${idToUse}`).then(r=>r.ok?r.json():null).then(d=>{ if(d) setPlan(d); }); }, [meetingId, selectedMeetingId]);

    const assignLeader = (deptId, leaderIdStr) => { if(!leaderIdStr) return; const idToUse = meetingId || selectedMeetingId; const leaderId = parseInt(leaderIdStr); fetch(`${API_URL}/departments/assign`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({ meetingId: idToUse, deptId, leaderId }) }).then(res => { if (res.ok) { const newPlan = { ...plan }; if(!newPlan.assignments[deptId]) newPlan.assignments[deptId] = []; const foundLeader = leaders.find(l=>l.id===leaderId); newPlan.assignments[deptId].push({ leader: foundLeader, id: 'temp' + Date.now() }); setPlan(newPlan); } else alert("Eroare la asignare."); }); };
    const removeAssignment = (deptId, leaderId) => { const idToUse = meetingId || selectedMeetingId; fetch(`${API_URL}/departments/remove?meetingId=${idToUse}&deptId=${deptId}&leaderId=${leaderId}`, { method: 'DELETE' }).then(res => { if (res.ok) { const newPlan = { ...plan }; if (newPlan.assignments[deptId]) { newPlan.assignments[deptId] = newPlan.assignments[deptId].filter(item => item.leader && item.leader.id !== leaderId); setPlan(newPlan); } } }); };
    const setDirector = (leaderId) => { if(!leaderId) return; const idToUse = meetingId || selectedMeetingId; fetch(`${API_URL}/departments/director/${idToUse}`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: parseInt(leaderId) }).then(res => { if(res.ok) setPlan(prev => ({...prev, directorDay: leaders.find(l=>l.id===parseInt(leaderId))})); }); };

    return (
        <div className="animate-in">

            {/* Header */}
            <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'20px'}}>
                <p className="db-section-title" style={{margin:0}}>Planificare Departamente</p>
                {meetingId && isDirector && (
                    <button onClick={onConfirm} style={{
                        background: isConfirmed ? 'var(--bg-primary)' : 'var(--success)',
                        color: isConfirmed ? 'var(--text-secondary)' : 'white',
                        border: isConfirmed ? '1.5px solid var(--border-color)' : 'none',
                        padding:'9px 20px', borderRadius:'12px', fontWeight:'700', fontSize:'0.88rem'
                    }}>
                        {isConfirmed ? 'Editeaza' : 'Salveaza & Continua'}
                    </button>
                )}
            </div>

            {/* Selector sedinta (cand e folosit standalone) */}
            {!meetingId && (
                <div className="card" style={{marginBottom:'20px'}}>
                    <p className="db-section-title">Alege Sedinta</p>
                    <select className="login-input" style={{marginBottom:0}} onChange={e => setSelectedMeetingId(e.target.value)}>
                        <option value="">-- Alege data --</option>
                        {meetings.map(m => <option key={m.id} value={m.id}>{m.date}{m.description ? ` — ${m.description}` : ''}</option>)}
                    </select>
                </div>
            )}

            {(meetingId || selectedMeetingId) && (
                <div style={{opacity: (isConfirmed && meetingId) ? 0.65 : 1, pointerEvents: (isConfirmed && meetingId) ? 'none' : 'auto', transition:'opacity 0.2s'}}>

                    {/* Director de Zi */}
                    <div className="card" style={{marginBottom:'20px', borderLeft:'4px solid var(--accent)'}}>
                        <p className="db-section-title" style={{marginBottom:'12px'}}>Director de Zi</p>
                        {plan.directorDay ? (
                            <div style={{display:'flex', alignItems:'center', gap:'14px'}}>
                                <div style={{
                                    width:'46px', height:'46px', borderRadius:'12px',
                                    background:'linear-gradient(135deg, var(--accent), #868cff)',
                                    display:'flex', alignItems:'center', justifyContent:'center',
                                    color:'white', fontWeight:'900', fontSize:'1rem', flexShrink:0
                                }}>
                                    {plan.directorDay.name?.charAt(0)}{plan.directorDay.surname?.charAt(0)}
                                </div>
                                <div style={{flex:1}}>
                                    <div style={{fontWeight:'800', color:'var(--text-primary)'}}>{plan.directorDay.name} {plan.directorDay.surname}</div>
                                    <div style={{fontSize:'0.8rem', color:'var(--text-secondary)', textTransform:'uppercase', letterSpacing:'0.5px', marginTop:'2px'}}>Director de Zi</div>
                                </div>
                                {isDirector && (
                                    <button onClick={() => { setPlan(p => ({...p, directorDay: null})); setDirectorSearch(''); setDirectorDropOpen(true); }} style={{background:'var(--bg-primary)', border:'1px solid var(--border-color)', color:'var(--text-secondary)', borderRadius:'8px', padding:'5px 10px', fontWeight:'700', fontSize:'0.8rem'}}>
                                        Schimba
                                    </button>
                                )}
                            </div>
                        ) : isDirector ? (
                            <div style={{position:'relative'}}>
                                <input
                                    className="login-input"
                                    placeholder="Cauta director..."
                                    value={directorSearch}
                                    autoFocus={directorDropOpen}
                                    onChange={e => { setDirectorSearch(e.target.value); setDirectorDropOpen(true); }}
                                    onFocus={() => setDirectorDropOpen(true)}
                                    style={{marginBottom:0}}
                                />
                                {directorDropOpen && (
                                    <div className="animate-in" style={{position:'absolute', top:'calc(100% + 4px)', left:0, right:0, background:'white', border:'1px solid var(--border-color)', borderRadius:'12px', zIndex:20, maxHeight:'200px', overflowY:'auto', boxShadow:'0 8px 24px rgba(0,0,0,0.1)'}}>
                                        {directorsList
                                            .filter(l => directorSearch === '' || `${l.name} ${l.surname}`.toLowerCase().includes(directorSearch.toLowerCase()))
                                            .map(l => (
                                                <div key={l.id} onMouseDown={() => { setDirector(l.id); setDirectorDropOpen(false); setDirectorSearch(''); }} style={{padding:'11px 14px', cursor:'pointer', fontWeight:'700', color:'var(--text-primary)', borderBottom:'1px solid var(--border-color)', display:'flex', justifyContent:'space-between', alignItems:'center'}}>
                                                    <span>{l.name} {l.surname}</span>
                                                    <span style={{fontSize:'0.75rem', color:'var(--accent)', fontWeight:'800'}}>Alege</span>
                                                </div>
                                            ))
                                        }
                                        {directorsList.filter(l => `${l.name} ${l.surname}`.toLowerCase().includes(directorSearch.toLowerCase())).length === 0 && (
                                            <div style={{padding:'12px 14px', color:'var(--text-secondary)', fontSize:'0.88rem'}}>Niciun rezultat.</div>
                                        )}
                                    </div>
                                )}
                            </div>
                        ) : (
                            <p style={{color:'var(--text-secondary)', margin:0, fontSize:'0.9rem'}}>Neselectat</p>
                        )}
                    </div>

                    {/* Grid departamente */}
                    <div className="departments-grid" style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'16px'}}>
                        {departments.map(dept => {
                            const assigned = plan.assignments[dept.id] || [];
                            const isFull = assigned.length >= dept.maxLeaders;
                            return (
                                <div key={dept.id} className="card" style={{
                                    marginBottom:0, padding:'18px',
                                    borderLeft:`4px solid ${isFull ? '#10b981' : 'var(--border-color)'}`,
                                    transition:'border-color 0.2s'
                                }}>
                                    <div style={{display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:'14px'}}>
                                        <div style={{fontWeight:'800', fontSize:'1rem', color:'var(--text-primary)'}}>{dept.name}</div>
                                        <span style={{
                                            fontSize:'0.75rem', fontWeight:'700', padding:'3px 9px', borderRadius:'20px',
                                            background: isFull ? '#dcfce7' : 'var(--bg-primary)',
                                            color: isFull ? '#15803d' : 'var(--text-secondary)',
                                            border: `1px solid ${isFull ? '#bbf7d0' : 'var(--border-color)'}`,
                                            flexShrink:0, marginLeft:'8px'
                                        }}>
                                            {assigned.length}/{dept.maxLeaders}
                                        </span>
                                    </div>

                                    {/* Lideri asignati ca chips */}
                                    <div style={{display:'flex', flexWrap:'wrap', gap:'6px', minHeight:'32px', marginBottom:'12px'}}>
                                        {assigned.map(a => {
                                            const l = a.leader;
                                            if (!l) return null;
                                            return (
                                                <span key={a.id || l.id} style={{
                                                    display:'inline-flex', alignItems:'center', gap:'6px',
                                                    background:'#ede9fe', color:'#6d28d9',
                                                    padding:'4px 10px 4px 8px', borderRadius:'20px',
                                                    fontSize:'0.82rem', fontWeight:'700'
                                                }}>
                                                    {l.name} {l.surname}
                                                    {isDirector && (
                                                        <button onClick={() => removeAssignment(dept.id, l.id)} style={{
                                                            background:'none', border:'none', color:'#7c3aed',
                                                            cursor:'pointer', padding:'0', lineHeight:1,
                                                            fontWeight:'900', fontSize:'0.9rem'
                                                        }}>×</button>
                                                    )}
                                                </span>
                                            );
                                        })}
                                        {assigned.length === 0 && (
                                            <span style={{color:'var(--text-secondary)', fontSize:'0.82rem', fontStyle:'italic'}}>Niciun lider asignat</span>
                                        )}
                                    </div>

                                    {isDirector && (
                                        <div style={{position:'relative'}}>
                                            <input
                                                className="login-input"
                                                placeholder="Cauta lider..."
                                                value={activeSearchDept === dept.id ? leaderSearch : ''}
                                                onFocus={() => { setActiveSearchDept(dept.id); setLeaderSearch(''); }}
                                                onChange={e => setLeaderSearch(e.target.value)}
                                                onBlur={() => setTimeout(() => setActiveSearchDept(null), 150)}
                                                style={{marginBottom:0, fontSize:'0.88rem'}}
                                            />
                                            {activeSearchDept === dept.id && (
                                                <div className="animate-in" style={{position:'absolute', top:'calc(100% + 4px)', left:0, right:0, background:'white', border:'1px solid var(--border-color)', borderRadius:'12px', zIndex:20, maxHeight:'180px', overflowY:'auto', boxShadow:'0 8px 24px rgba(0,0,0,0.1)'}}>
                                                    {leaders
                                                        .filter(l => !plan.assignments[dept.id]?.some(a => a.leader?.id === l.id))
                                                        .filter(l => leaderSearch === '' || `${l.name} ${l.surname}`.toLowerCase().includes(leaderSearch.toLowerCase()))
                                                        .map(l => (
                                                            <div key={l.id} onMouseDown={() => { assignLeader(dept.id, l.id); setActiveSearchDept(null); setLeaderSearch(''); }} style={{padding:'10px 14px', cursor:'pointer', fontWeight:'700', color:'var(--text-primary)', borderBottom:'1px solid var(--border-color)', display:'flex', justifyContent:'space-between', alignItems:'center'}}>
                                                                <span>{l.name} {l.surname}</span>
                                                                <span style={{fontSize:'0.75rem', color:'var(--accent)', fontWeight:'800'}}>+</span>
                                                            </div>
                                                        ))
                                                    }
                                                    {leaders.filter(l => !plan.assignments[dept.id]?.some(a => a.leader?.id === l.id) && `${l.name} ${l.surname}`.toLowerCase().includes(leaderSearch.toLowerCase())).length === 0 && (
                                                        <div style={{padding:'11px 14px', color:'var(--text-secondary)', fontSize:'0.85rem'}}>Niciun lider disponibil.</div>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                    )}

                                    {dept.headLeader && (
                                        <div style={{marginTop:'10px', fontSize:'0.75rem', color:'var(--accent)', fontWeight:'700'}}>
                                            Responsabil: {dept.headLeader.name} {dept.headLeader.surname}
                                        </div>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                </div>
            )}
        </div>
    );
};

export default DepartmentsPlan;
