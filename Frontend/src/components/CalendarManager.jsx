import React, { useState, useEffect } from 'react';
import { API_URL } from '../config';
import DepartmentsPlan from './DepartmentsPlan';
import ScoringWidget from './ScoringWidget';
import TeamsManager from './TeamsManager';

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

const CalendarManager = ({ user }) => {
    const [meetings, setMeetings] = useState([]);
    const [activeSession, setActiveSession] = useState(null);
    const [showFeedbackModal, setShowFeedbackModal] = useState(false);
    const [activeTab, setActiveTab] = useState('plan');
    const [isOrganized, setIsOrganized] = useState(false);
    const [newDate, setNewDate] = useState('');
    const [newDesc, setNewDesc] = useState('');
    const [showAddForm, setShowAddForm] = useState(false);

    const isDirector = user.role === 'DIRECTOR' || user.role === 'COORDONATOR';

    const loadMeetings = () => { fetch(`${API_URL}/meetings`).then(r => r.ok?r.json():[]).then(setMeetings).catch(()=>{}); };
    useEffect(() => { loadMeetings(); }, []);

    const addMeeting = () => {
        if(!newDate) return alert("Alege o data!");
        fetch(`${API_URL}/meetings/add`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({ date: newDate, description: newDesc }) })
            .then(() => { loadMeetings(); setNewDate(''); setNewDesc(''); setShowAddForm(false); });
    };

    const finalizeClose = () => {
        fetch(`${API_URL}/meetings/close/${activeSession.id}`, { method: 'POST' }).then(async res => {
            if(res.ok) { alert(await res.text()); setShowFeedbackModal(false); setActiveSession(null); loadMeetings(); }
            else alert("Eroare la inchidere.");
        });
    };

    if (showFeedbackModal) return <MeetingFeedback meeting={activeSession} user={user} onComplete={finalizeClose} onCancel={() => setShowFeedbackModal(false)} />;

    /* ── SESIUNE ACTIVA ─────────────────────────────────────── */
    if (activeSession) {
        const tabs = [
            { key: 'plan', label: 'Organizare' },
            ...((isOrganized || !isDirector) ? [
                { key: 'scoring', label: 'Scoring' },
                { key: 'teams', label: 'Echipe' },
            ] : []),
        ];

        return (
            <div className="animate-in">
                {/* Hero sesiune */}
                <div className="db-hero" style={{marginBottom:'20px', alignItems:'center'}}>
                    <div className="db-hero-left">
                        <span className="db-greeting">Sedinta activa</span>
                        <h1 className="db-name" style={{marginBottom:'10px'}}>{activeSession.date}</h1>
                        <span className="db-role-pill">{activeSession.description || 'Standard'}</span>
                    </div>
                    <div style={{display:'flex', flexDirection:'column', gap:'8px', alignItems:'flex-end', flexShrink:0}}>
                        <button onClick={() => setActiveSession(null)} style={{
                            background:'rgba(255,255,255,0.14)', border:'1.5px solid rgba(255,255,255,0.28)',
                            color:'white', padding:'9px 18px', borderRadius:'12px', fontWeight:'700', fontSize:'0.88rem'
                        }}>
                            Inapoi
                        </button>
                        {isDirector && (
                            <button onClick={() => { if(!window.confirm("Inchizi seara? Actiunea este ireversibila.")) return; setShowFeedbackModal(true); }} style={{
                                background:'rgba(239,68,68,0.25)', border:'1.5px solid rgba(239,68,68,0.6)',
                                color:'#fca5a5', padding:'9px 18px', borderRadius:'12px', fontWeight:'700', fontSize:'0.88rem'
                            }}>
                                Incheie Seara
                            </button>
                        )}
                    </div>
                </div>

                {/* Pill tabs */}
                <div style={{display:'flex', background:'var(--bg-primary)', borderRadius:'14px', padding:'4px', gap:'2px', marginBottom:'22px', border:'1px solid var(--border-color)'}}>
                    {tabs.map(t => (
                        <button key={t.key} onClick={() => setActiveTab(t.key)} style={{
                            flex:1, padding:'10px 8px', borderRadius:'10px', border:'none', fontWeight:'700',
                            fontSize:'0.88rem', transition:'all 0.18s', cursor:'pointer',
                            background: activeTab===t.key ? 'white' : 'transparent',
                            color: activeTab===t.key ? 'var(--accent)' : 'var(--text-secondary)',
                            boxShadow: activeTab===t.key ? '0 2px 8px rgba(0,0,0,0.08)' : 'none',
                        }}>{t.label}</button>
                    ))}
                </div>

                {activeTab === 'plan'    && <DepartmentsPlan meetingId={activeSession.id} user={user} isConfirmed={isOrganized} onConfirm={() => { setIsOrganized(!isOrganized); if(!isOrganized) setActiveTab('scoring'); }} />}
                {activeTab === 'scoring' && <ScoringWidget />}
                {activeTab === 'teams'   && <TeamsManager />}
            </div>
        );
    }

    /* ── LISTA SEDINTE ──────────────────────────────────────── */
    const nextMeeting = meetings[0];

    return (
        <div className="animate-in">

            {/* Hero — urmatoarea sedinta */}
            <div className="db-hero" style={{marginBottom:'24px', alignItems:'center'}}>
                <div className="db-hero-left">
                    <span className="db-greeting">Urmatoarea sedinta</span>
                    <h1 className="db-name" style={{marginBottom:'10px'}}>
                        {nextMeeting ? nextMeeting.date : 'Nicio sedinta'}
                    </h1>
                    <span className="db-role-pill">
                        {nextMeeting ? (nextMeeting.description || 'Standard') : 'Adauga o sedinta mai jos'}
                    </span>
                </div>
                <div style={{textAlign:'right', flexShrink:0, zIndex:1}}>
                    <div className="db-date-day">{meetings.length}</div>
                    <div className="db-date-month">planificate</div>
                </div>
            </div>

            {/* Form adaugare (director) */}
            {isDirector && (
                <div className="card" style={{marginBottom:'20px'}}>
                    <div
                        onClick={() => setShowAddForm(f => !f)}
                        style={{display:'flex', justifyContent:'space-between', alignItems:'center', cursor:'pointer'}}
                    >
                        <p className="db-section-title" style={{margin:0}}>Programeaza Sedinta</p>
                        <span style={{
                            width:'28px', height:'28px', borderRadius:'8px',
                            background:'var(--bg-primary)', border:'1px solid var(--border-color)',
                            display:'flex', alignItems:'center', justifyContent:'center',
                            fontWeight:'800', fontSize:'1.1rem', color:'var(--accent)',
                            transition:'transform 0.2s',
                            transform: showAddForm ? 'rotate(45deg)' : 'none'
                        }}>+</span>
                    </div>
                    {showAddForm && (
                        <div className="animate-in" style={{marginTop:'16px', display:'flex', gap:'10px', flexWrap:'wrap'}}>
                            <input type="date" className="login-input" value={newDate} onChange={e=>setNewDate(e.target.value)} style={{marginBottom:0, flex:'1 1 140px'}}/>
                            <input type="text" className="login-input" placeholder="Descriere (optional)" value={newDesc} onChange={e=>setNewDesc(e.target.value)} style={{marginBottom:0, flex:'2 1 200px'}}/>
                            <button onClick={addMeeting} style={{background:'var(--accent)', color:'white', padding:'0 24px', borderRadius:'12px', fontWeight:'700', fontSize:'0.92rem', flexShrink:0}}>
                                Adauga
                            </button>
                        </div>
                    )}
                </div>
            )}

            {/* Lista sedinte */}
            {meetings.length === 0 ? (
                <div className="card" style={{textAlign:'center', padding:'40px 20px', color:'var(--text-secondary)'}}>
                    <div style={{fontSize:'2.5rem', marginBottom:'12px', opacity:0.3}}>
                        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>
                    </div>
                    <p style={{fontWeight:'600', margin:0}}>Nu exista sedinte planificate.</p>
                </div>
            ) : (
                <div style={{display:'flex', flexDirection:'column', gap:'12px'}}>
                    {meetings.map((m, i) => (
                        <div key={m.id} className="card" style={{
                            display:'flex', alignItems:'center', gap:'18px', padding:'18px 22px',
                            borderLeft:`4px solid ${i===0 ? 'var(--accent)' : 'var(--border-color)'}`,
                            marginBottom:0, transition:'border-color 0.2s, box-shadow 0.2s'
                        }}>
                            <div style={{flex:1, minWidth:0}}>
                                <div style={{fontWeight:'800', fontSize:'1.05rem', color:'var(--text-primary)'}}>{m.date}</div>
                                <div style={{color:'var(--text-secondary)', fontSize:'0.88rem', marginTop:'3px'}}>{m.description || 'Standard'}</div>
                            </div>
                            <div style={{display:'flex', gap:'8px', flexShrink:0}}>
                                {isDirector && (
                                    <button onClick={() => { setActiveSession(m); setActiveTab('plan'); }} style={{
                                        background:'var(--bg-primary)', color:'var(--accent)',
                                        border:'1.5px solid var(--border-color)', padding:'8px 16px',
                                        borderRadius:'10px', fontWeight:'700', fontSize:'0.85rem'
                                    }}>
                                        Planifica
                                    </button>
                                )}
                                <button onClick={() => setActiveSession(m)} style={{
                                    background:'var(--accent)', color:'white',
                                    border:'none', padding:'9px 20px',
                                    borderRadius:'10px', fontWeight:'700', fontSize:'0.9rem'
                                }}>
                                    Intra
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default CalendarManager;
