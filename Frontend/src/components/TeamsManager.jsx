import React, { useState, useEffect } from 'react';
import { API_URL } from '../config';

const TeamsManager = () => {
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

    const teams =['red', 'blue', 'green', 'yellow'];
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

    const pickChild = (childId) => { fetch(`${API_URL}/teams/pick`, { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({ childId: childId, teamColor: myColor }) }).then(async res => { if(res.ok) refreshData(); else alert(await res.text()); }); };
    const removeChildFromTeam = (child) => { if(!window.confirm(`Îl scoți pe ${child.name} din echipa ${myColor.toUpperCase()}?`)) return; fetch(`${API_URL}/teams/remove`, { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({ childId: child.id }) }).then(async res => { if(res.ok) { refreshData(); } else { alert("Eroare: " + await res.text()); }}); };
    const handleTeamPress = (c) => { if(!ranking.includes(c)) setRanking([...ranking, c]); };
    const sendGamePoints = () => { if(ranking.length===0) return alert("Selecteaza ordinea!"); fetch(`${API_URL}/teams/game-round`, { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({ ranking, isDouble }) }).then(res => { if(res.ok) { alert("✅ Clasament salvat!"); setRanking([]); setIsDouble(false); refreshData(); } }); };
    const sendManualPoints = () => { if(!manualPoints || isNaN(manualPoints)) return alert("Introdu un număr valid!"); fetch(`${API_URL}/teams/add-manual-points`, { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({ teamColor: manualTeam, points: parseInt(manualPoints) }) }).then(async res => { if(res.ok) { alert(`✅ ${await res.text()}`); setManualPoints(''); refreshData(); } else { alert("Eroare server: " + await res.text()); } }); };
    const getPoints = (i) => { const pts=[1000,500,300,100]; return i<4 ? (isDouble?pts[i]*2:pts[i]) : 0; };
    const getFilteredAvailable = () => { if(!searchAvailable) return availableKids; const term = searchAvailable.toLowerCase(); return availableKids.filter(c => c.name.toLowerCase().includes(term) || c.surname.toLowerCase().includes(term)); };

    return (
        <div className="animate-in">

            {/* Modal PIN */}
            {showPinModal && (
                <div style={{position:'fixed', top:0, left:0, width:'100%', height:'100%', background:'rgba(15,23,42,0.75)', backdropFilter:'blur(4px)', zIndex:1000, display:'flex', justifyContent:'center', alignItems:'center'}}>
                    <div className="animate-in card" style={{width:'90%', maxWidth:'320px', textAlign:'center', padding:'32px'}}>
                        <div style={{width:'52px', height:'52px', borderRadius:'14px', background:'var(--bg-primary)', border:'1px solid var(--border-color)', display:'flex', alignItems:'center', justifyContent:'center', margin:'0 auto 16px'}}>
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                        </div>
                        <h3 style={{margin:'0 0 6px', color:'var(--text-primary)', fontWeight:'800'}}>Acces Restrictionat</h3>
                        <p style={{color:'var(--text-secondary)', marginBottom:'20px', fontSize:'0.9rem'}}>Verifica mesajele pentru codul serii.</p>
                        <input
                            type="password" inputMode="numeric" pattern="[0-9]*" maxLength="4" autoFocus placeholder="Cod PIN"
                            value={pinInput} onChange={e => setPinInput(e.target.value)}
                            onKeyDown={e => e.key === 'Enter' && verifyPin()}
                            style={{width:'100%', padding:'14px', fontSize:'1.6rem', textAlign:'center', letterSpacing:'8px', borderRadius:'12px', border:'2px solid var(--border-color)', marginBottom:'16px', fontWeight:'900'}}
                        />
                        <div style={{display:'flex', gap:'10px'}}>
                            <button onClick={() => setShowPinModal(false)} style={{flex:1, padding:'12px', background:'var(--bg-primary)', color:'var(--text-secondary)', border:'1px solid var(--border-color)', borderRadius:'10px', fontWeight:'700'}}>Anuleaza</button>
                            <button onClick={verifyPin} disabled={loadingPin} style={{flex:1, padding:'12px', background:'var(--accent)', color:'white', border:'none', borderRadius:'10px', fontWeight:'700'}}>
                                {loadingPin ? '...' : 'Confirma'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Pill tabs mod */}
            <div style={{display:'flex', background:'var(--bg-primary)', borderRadius:'14px', padding:'4px', gap:'2px', marginBottom:'22px', border:'1px solid var(--border-color)'}}>
                <button onClick={() => setMode('selection')} style={{flex:1, padding:'10px', borderRadius:'10px', border:'none', fontWeight:'700', fontSize:'0.88rem', cursor:'pointer', transition:'all 0.18s', background: mode==='selection' ? 'white' : 'transparent', color: mode==='selection' ? 'var(--accent)' : 'var(--text-secondary)', boxShadow: mode==='selection' ? '0 2px 8px rgba(0,0,0,0.08)' : 'none'}}>
                    Selectie Echipa
                </button>
                <button onClick={handleAccessGames} style={{flex:1, padding:'10px', borderRadius:'10px', border:'none', fontWeight:'700', fontSize:'0.88rem', cursor:'pointer', transition:'all 0.18s', background: mode==='games' ? 'white' : 'transparent', color: mode==='games' ? 'var(--accent)' : 'var(--text-secondary)', boxShadow: mode==='games' ? '0 2px 8px rgba(0,0,0,0.08)' : 'none', display:'flex', alignItems:'center', justifyContent:'center', gap:'6px'}}>
                    {mode !== 'games' && <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>}
                    Punctaje Jocuri
                </button>
            </div>

            {/* ── SELECTIE ECHIPA ── */}
            {mode === 'selection' && (
                <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'16px'}}>

                    {/* Selector culoare echipa */}
                    <div style={{gridColumn:'span 2', display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:'8px'}}>
                        {teams.map(c => {
                            const th = teamStyles[c];
                            const active = myColor === c;
                            return (
                                <button key={c} onClick={() => setMyColor(c)} style={{
                                    padding:'12px 8px', borderRadius:'12px', fontWeight:'800',
                                    textTransform:'uppercase', fontSize:'0.88rem', letterSpacing:'0.5px',
                                    border: `2px solid ${active ? th.border : 'var(--border-color)'}`,
                                    background: active ? th.border : 'white',
                                    color: active ? 'white' : 'var(--text-secondary)',
                                    transition:'all 0.15s', boxShadow: active ? `0 4px 12px ${th.border}55` : 'none'
                                }}>{c}</button>
                            );
                        })}
                    </div>

                    {/* Disponibili */}
                    <div className="card" style={{height:'480px', display:'flex', flexDirection:'column', padding:'16px', marginBottom:0}}>
                        <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'10px'}}>
                            <p className="db-section-title" style={{margin:0}}>Disponibili</p>
                            <span style={{background:'var(--bg-primary)', border:'1px solid var(--border-color)', borderRadius:'20px', padding:'2px 10px', fontSize:'0.8rem', fontWeight:'700', color:'var(--text-secondary)'}}>{availableKids.length}</span>
                        </div>
                        <input className="login-input" placeholder="Cauta nume..." value={searchAvailable} onChange={e => setSearchAvailable(e.target.value)} style={{marginBottom:'10px', padding:'8px 12px', fontSize:'0.88rem'}}/>
                        <div style={{overflowY:'auto', flex:1}}>
                            {getFilteredAvailable().map(c => (
                                <div key={c.id} onClick={() => pickChild(c.id)} style={{
                                    padding:'11px 12px', marginBottom:'6px', cursor:'pointer',
                                    display:'flex', justifyContent:'space-between', alignItems:'center',
                                    background:'var(--bg-primary)', borderRadius:'10px',
                                    border:'1px solid var(--border-color)', transition:'border-color 0.12s'
                                }}>
                                    <span style={{fontWeight:'700', color:'var(--text-primary)', fontSize:'0.92rem'}}>{c.name} {c.surname}</span>
                                    <span style={{
                                        width:'22px', height:'22px', borderRadius:'50%',
                                        background:'var(--accent)', color:'white',
                                        display:'flex', alignItems:'center', justifyContent:'center',
                                        fontWeight:'900', fontSize:'1rem', flexShrink:0
                                    }}>+</span>
                                </div>
                            ))}
                            {searchAvailable && getFilteredAvailable().length === 0 && (
                                <div style={{textAlign:'center', color:'var(--text-secondary)', padding:'16px', fontSize:'0.88rem'}}>Nu am gasit.</div>
                            )}
                        </div>
                    </div>

                    {/* Echipa mea */}
                    <div className="card" style={{display:'flex', flexDirection:'column', padding:'16px', marginBottom:0, border:`2px solid ${currentTheme.border}`, background: currentTheme.bg}}>
                        <p style={{margin:'0 0 4px', fontSize:'0.72rem', fontWeight:'800', textTransform:'uppercase', letterSpacing:'0.8px', color: currentTheme.text, opacity:0.7}}>Echipa</p>
                        <p style={{margin:'0 0 2px', fontWeight:'900', textTransform:'uppercase', fontSize:'1.1rem', color: currentTheme.text}}>{myColor}</p>
                        <div style={{margin:'8px 0 12px', padding:'10px 12px', background:'rgba(255,255,255,0.55)', borderRadius:'10px'}}>
                            <div style={{fontSize:'2rem', fontWeight:'900', color: currentTheme.text, lineHeight:1}}>{scores.total.toLocaleString()}</div>
                            <div style={{fontSize:'0.72rem', color: currentTheme.text, opacity:0.7, marginTop:'2px'}}>Indiv {scores.individual} + Joc {scores.game}</div>
                        </div>
                        <div style={{overflowY:'auto', flex:1}}>
                            {myTeamMembers.length === 0 && <div style={{padding:'10px', fontSize:'0.82rem', fontStyle:'italic', color: currentTheme.text, opacity:0.6}}>Niciun membru inca.</div>}
                            {myTeamMembers.map(c => (
                                <div key={c.id} onClick={() => removeChildFromTeam(c)} style={{
                                    padding:'8px 10px', marginBottom:'5px', cursor:'pointer',
                                    display:'flex', justifyContent:'space-between', alignItems:'center',
                                    background:'rgba(255,255,255,0.55)', borderRadius:'8px',
                                    border:`1px solid ${currentTheme.border}40`
                                }}>
                                    <span style={{fontWeight:'700', color: currentTheme.text, fontSize:'0.9rem'}}>{c.name} {c.surname}</span>
                                    <span style={{color: currentTheme.text, opacity:0.5, fontWeight:'800', fontSize:'0.9rem'}}>×</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            )}

            {/* ── PUNCTAJE JOCURI ── */}
            {mode === 'games' && (
                <div style={{display:'flex', flexDirection:'column', gap:'16px'}}>
                    <div className="card">
                        {/* Header */}
                        <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'20px'}}>
                            <p className="db-section-title" style={{margin:0}}>Clasament Runda</p>
                            <button onClick={() => setIsDouble(!isDouble)} style={{
                                padding:'8px 18px', borderRadius:'10px', fontWeight:'800', fontSize:'0.88rem', border:'none',
                                background: isDouble ? 'linear-gradient(135deg, #f59e0b, #d97706)' : 'var(--bg-primary)',
                                color: isDouble ? 'white' : 'var(--text-secondary)',
                                border: isDouble ? 'none' : '1px solid var(--border-color)',
                                boxShadow: isDouble ? '0 4px 12px rgba(245,158,11,0.4)' : 'none',
                                transition:'all 0.18s'
                            }}>
                                {isDouble ? 'DUBLU' : 'Normal'}
                            </button>
                        </div>

                        {/* Butoane echipe pentru ranking */}
                        <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'12px', marginBottom:'20px'}}>
                            {teams.map(c => {
                                const idx = ranking.indexOf(c);
                                const th = teamStyles[c];
                                const placed = idx !== -1;
                                return (
                                    <button key={c} onClick={() => handleTeamPress(c)} disabled={placed} style={{
                                        padding:'28px 16px', borderRadius:'16px', border:'none',
                                        fontWeight:'900', textTransform:'uppercase', fontSize:'1.3rem',
                                        letterSpacing:'1px', cursor: placed ? 'default' : 'pointer',
                                        background: placed ? '#1e293b' : th.border,
                                        color:'white', opacity: placed ? 0.75 : 1,
                                        boxShadow: placed ? 'none' : `0 6px 0 ${th.text}88`,
                                        transform: placed ? 'translateY(3px)' : 'none',
                                        transition:'all 0.15s', position:'relative', overflow:'hidden'
                                    }}>
                                        {c}
                                        {placed && (
                                            <div style={{fontSize:'0.85rem', fontWeight:'700', color:'#fbbf24', marginTop:'6px', letterSpacing:'0.5px'}}>
                                                Locul {idx + 1} · +{getPoints(idx).toLocaleString()}
                                            </div>
                                        )}
                                    </button>
                                );
                            })}
                        </div>

                        {/* Actiuni */}
                        <div style={{display:'flex', gap:'10px'}}>
                            <button onClick={() => setRanking([])} style={{flex:1, padding:'13px', background:'var(--bg-primary)', color:'#ef4444', border:'1.5px solid #fecaca', borderRadius:'12px', fontWeight:'700', fontSize:'0.9rem'}}>
                                Reseteaza
                            </button>
                            <button onClick={sendGamePoints} style={{flex:2, padding:'13px', background:'#15803d', color:'white', border:'none', borderRadius:'12px', fontWeight:'800', fontSize:'0.9rem', boxShadow:'0 4px 12px rgba(21,128,61,0.3)'}}>
                                Trimite Clasament
                            </button>
                        </div>
                        <div style={{marginTop:'30px', paddingTop:'20px', borderTop:'2px dashed #eee'}}>
                            <p className="db-section-title" style={{marginBottom:'14px'}}>Puncte Bonus</p>
                            <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'10px'}}>
                                {teams.map(t => {
                                    const th = teamStyles[t];
                                    const isSelected = manualTeam === t;
                                    return (
                                        <div key={t} style={{
                                            background: isSelected ? th.bg : 'white',
                                            border: `2px solid ${isSelected ? th.border : '#e2e8f0'}`,
                                            borderRadius:'14px', padding:'14px', transition:'all 0.15s'
                                        }}>
                                            <div style={{fontWeight:'800', textTransform:'uppercase', color: th.text, marginBottom:'8px', fontSize:'0.9rem'}}>{t}</div>
                                            {/* Preseturi rapide */}
                                            <div style={{display:'flex', gap:'5px', marginBottom:'8px', flexWrap:'wrap'}}>
                                                {[500, 1000, 2000].map(preset => (
                                                    <button key={preset} onClick={() => { setManualTeam(t); setManualPoints(String(preset)); }} style={{
                                                        padding:'4px 9px', borderRadius:'8px', border:`1px solid ${th.border}`,
                                                        background: (isSelected && manualPoints === String(preset)) ? th.border : 'white',
                                                        color: (isSelected && manualPoints === String(preset)) ? 'white' : th.text,
                                                        fontWeight:'700', fontSize:'0.78rem', cursor:'pointer'
                                                    }}>+{preset}</button>
                                                ))}
                                            </div>
                                            <div style={{display:'flex', gap:'6px'}}>
                                                <input
                                                    type="number"
                                                    placeholder="Pct"
                                                    value={manualTeam === t ? manualPoints : ''}
                                                    onChange={e => { setManualTeam(t); setManualPoints(e.target.value); }}
                                                    onFocus={() => setManualTeam(t)}
                                                    style={{flex:1, padding:'7px 10px', borderRadius:'8px', border:`1px solid ${th.border}`, fontSize:'0.88rem', margin:0, fontWeight:'700'}}
                                                />
                                                <button onClick={() => { setManualTeam(t); sendManualPoints(); }} style={{
                                                    background: th.border, color:'white', border:'none',
                                                    borderRadius:'8px', padding:'0 12px', fontWeight:'800',
                                                    fontSize:'0.88rem', cursor:'pointer', flexShrink:0
                                                }}>OK</button>
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default TeamsManager;
