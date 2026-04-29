import React, { useState, useEffect } from 'react';
import { API_URL } from '../config';

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

    const scoreItems = [
        { k: 'attended',    label: 'Prezent',       pts: 1000,  full: true },
        { k: 'hasBible',    label: 'Biblie',         pts: 500,   full: false },
        { k: 'friend',      label: 'Prieten',        pts: 1000,  full: false },
        { k: 'hasHandbook', label: 'Manual',         pts: 500,   full: false },
        { k: 'lesson',      label: 'Lectie gata',    pts: 1000,  full: false },
        { k: 'hasUniform',  label: 'Costum special', pts: 10000, full: true },
    ];

    return (
        <div className="animate-in">

            {/* Cautare copil — mereu vizibila sus */}
            <div className="card" style={{marginBottom:'16px'}}>
                <p className="db-section-title" style={{marginBottom:'10px'}}>Cauta Copil</p>
                <div style={{position:'relative'}}>
                    <input
                        className="login-input"
                        placeholder="Scrie numele..."
                        value={searchTerm}
                        onChange={e => { setSearchTerm(e.target.value); setIsSearching(true); }}
                        onFocus={() => setIsSearching(true)}
                        style={{marginBottom:0, paddingRight: selectedChild ? '90px' : '14px'}}
                    />
                    {selectedChild && (
                        <button onClick={() => { setSelectedChild(null); setSearchTerm(''); }} style={{
                            position:'absolute', right:'8px', top:'50%', transform:'translateY(-50%)',
                            background:'var(--bg-primary)', border:'1px solid var(--border-color)',
                            color:'var(--text-secondary)', borderRadius:'8px', padding:'4px 10px',
                            fontWeight:'700', fontSize:'0.8rem'
                        }}>Schimba</button>
                    )}
                </div>
                {isSearching && !selectedChild && searchTerm.length > 0 && (
                    <div className="animate-in" style={{marginTop:'6px', border:'1px solid var(--border-color)', borderRadius:'12px', overflow:'hidden', maxHeight:'220px', overflowY:'auto'}}>
                        {getFilteredChildren().length === 0
                            ? <div style={{padding:'14px', color:'var(--text-secondary)', textAlign:'center', fontSize:'0.9rem'}}>Niciun rezultat.</div>
                            : getFilteredChildren().map(c => (
                                <div key={c.id} onClick={() => { setSelectedChild(c); setIsSearching(false); }} style={{
                                    padding:'12px 16px', cursor:'pointer', display:'flex', justifyContent:'space-between', alignItems:'center',
                                    borderBottom:'1px solid var(--border-color)', fontWeight:'700', color:'var(--text-primary)',
                                    transition:'background 0.12s'
                                }}>
                                    <span>{c.name} {c.surname}</span>
                                    <span style={{color:'var(--accent)', fontSize:'0.82rem', fontWeight:'800'}}>Alege</span>
                                </div>
                            ))
                        }
                    </div>
                )}
            </div>

            {/* Punctaj — apare cand e selectat un copil */}
            {selectedChild && (
                <div className="animate-in">
                    {/* Nume copil selectat */}
                    <div className="card" style={{marginBottom:'16px', borderLeft:'4px solid var(--accent)', padding:'16px 20px', display:'flex', alignItems:'center', justifyContent:'space-between'}}>
                        <div>
                            <div style={{fontWeight:'800', fontSize:'1.1rem', color:'var(--text-primary)'}}>{selectedChild.name} {selectedChild.surname}</div>
                            <div style={{fontSize:'0.82rem', color:'var(--text-secondary)', marginTop:'2px'}}>Acorda punctele pentru aceasta sedinta</div>
                        </div>
                        <div style={{
                            textAlign:'right', flexShrink:0,
                            color:'var(--accent)', fontWeight:'900', fontSize:'1.8rem', lineHeight:1
                        }}>
                            {calculateTotal().toLocaleString()}
                            <div style={{fontSize:'0.7rem', fontWeight:'600', color:'var(--text-secondary)', textTransform:'uppercase', letterSpacing:'0.5px'}}>puncte</div>
                        </div>
                    </div>

                    {/* Butoane toggle */}
                    <div className="card" style={{marginBottom:'16px'}}>
                        <p className="db-section-title" style={{marginBottom:'14px'}}>Categorii</p>
                        <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'10px'}}>
                            {scoreItems.map(item => (
                                <div
                                    key={item.k}
                                    onClick={() => togglePoint(item.k)}
                                    style={{
                                        gridColumn: item.full ? 'span 2' : 'span 1',
                                        padding:'14px 16px', borderRadius:'14px', cursor:'pointer',
                                        border: `2px solid ${pointsData[item.k] ? 'var(--accent)' : 'var(--border-color)'}`,
                                        background: pointsData[item.k] ? 'var(--accent)' : 'white',
                                        color: pointsData[item.k] ? 'white' : 'var(--text-primary)',
                                        transition:'all 0.15s', display:'flex', justifyContent:'space-between', alignItems:'center'
                                    }}
                                >
                                    <span style={{fontWeight:'800', fontSize:'0.95rem'}}>{item.label}</span>
                                    <span style={{
                                        fontSize:'0.8rem', fontWeight:'700',
                                        opacity: pointsData[item.k] ? 0.85 : 0.45
                                    }}>+{item.pts.toLocaleString()}</span>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Puncte extra */}
                    <div className="card" style={{marginBottom:'16px'}}>
                        <p className="db-section-title" style={{marginBottom:'10px'}}>Puncte Extra</p>
                        <input
                            type="number"
                            className="login-input"
                            value={extraPoints}
                            onChange={e => setExtraPoints(e.target.value)}
                            placeholder="Ex: 1000 (pentru a 2-a lectie)"
                            style={{marginBottom:0}}
                        />
                    </div>

                    {/* Salveaza */}
                    <button onClick={handleSaveScore} style={{
                        width:'100%', padding:'16px', background:'#15803d', color:'white',
                        border:'none', borderRadius:'14px', fontWeight:'800', fontSize:'1rem',
                        boxShadow:'0 4px 14px rgba(21,128,61,0.3)', cursor:'pointer'
                    }}>
                        Salveaza — {calculateTotal().toLocaleString()} puncte
                    </button>
                </div>
            )}

            {/* Empty state */}
            {!selectedChild && searchTerm.length === 0 && (
                <div className="card" style={{textAlign:'center', padding:'40px 20px', color:'var(--text-secondary)'}}>
                    <div style={{opacity:0.3, marginBottom:'12px'}}>
                        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
                    </div>
                    <p style={{fontWeight:'600', margin:0}}>Cauta un copil pentru a acorda puncte.</p>
                </div>
            )}
        </div>
    );
};

export default ScoringWidget;
