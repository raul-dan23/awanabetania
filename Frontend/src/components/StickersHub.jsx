import React, { useState, useEffect } from 'react';
import { API_URL } from '../config';
import StickerMap from './StickerMap';

const StickersHub = ({ user }) => {
    const [children, setChildren] = useState([]);
    const [totalStickers, setTotalStickers] = useState(0);
    const [selectedChild, setSelectedChild] = useState(null);
    const [loading, setLoading] = useState(true);
    const [childSearch, setChildSearch] = useState('');

    useEffect(() => {
        Promise.all([
            fetch(`${API_URL}/children`).then(r => r.ok ? r.json() : []),
            fetch(`${API_URL}/stickers`).then(r => r.ok ? r.json() : [])
        ]).then(([childData, stickerData]) => {
            setChildren(childData.sort((a,b) => a.name.localeCompare(b.name)));
            setTotalStickers(stickerData.length);
            setLoading(false);
        }).catch(() => setLoading(false));
    }, []);

    const refreshChild = (childId) => {
        fetch(`${API_URL}/children/${childId}`).then(r=>r.json()).then(setSelectedChild);
    };

    const handleUnlockSticker = (childId) => {
        if(!window.confirm("Confirm: Copilul a spus lectia si deschidem lacatul?")) return;
        fetch(`${API_URL}/children/${childId}/unlock-next`, { method: 'POST' }).then(async res => {
            if(res.ok) {
                alert(await res.text());
                refreshChild(childId);
            }
            else alert("Eroare");
        });
    };

    if (loading) return <div className="animate-in" style={{padding:'20px', color:'var(--text-secondary)'}}>Se incarca albumele...</div>;

    // Vedere detaliu album
    if (selectedChild) {
        const progress = selectedChild.progress ? selectedChild.progress.lastStickerId : 0;
        return (
            <div className="animate-in">
                <div className="db-hero" style={{marginBottom:'24px', alignItems:'center'}}>
                    <div className="db-hero-left">
                        <span className="db-greeting">Albumul lui</span>
                        <h1 className="db-name">{selectedChild.name} {selectedChild.surname}</h1>
                        <span className="db-role-pill">{progress}{totalStickers > 0 ? ` din ${totalStickers}` : ''} stickere</span>
                    </div>
                    <button onClick={() => setSelectedChild(null)} style={{
                        background:'rgba(255,255,255,0.14)', border:'1.5px solid rgba(255,255,255,0.28)',
                        color:'white', padding:'9px 18px', borderRadius:'12px', fontWeight:'700',
                        fontSize:'0.88rem', flexShrink:0
                    }}>
                        Inapoi
                    </button>
                </div>
                <div className="card">
                    <StickerMap child={selectedChild} user={user} onUnlock={handleUnlockSticker} />
                </div>
            </div>
        );
    }

    // Vedere lista
    const filtered = children.filter(c =>
        childSearch === '' ||
        `${c.name} ${c.surname}`.toLowerCase().includes(childSearch.toLowerCase())
    );
    const totalCollected = children.reduce((sum, c) => sum + (c.progress ? c.progress.lastStickerId : 0), 0);

    return (
        <div className="animate-in">
            <div className="db-hero" style={{marginBottom:'24px', alignItems:'center'}}>
                <div className="db-hero-left">
                    <span className="db-greeting">Colectia Clubului</span>
                    <h1 className="db-name">Albume Stickere</h1>
                    <span className="db-role-pill">{children.length} copii inscrisi</span>
                </div>
                <div style={{background:'rgba(255,255,255,0.14)', borderRadius:'16px', padding:'12px 20px', textAlign:'center', border:'1.5px solid rgba(255,255,255,0.22)', flexShrink:0}}>
                    <div style={{color:'rgba(255,255,255,0.7)', fontSize:'0.72rem', fontWeight:'700', textTransform:'uppercase', letterSpacing:'1px'}}>Total Colectate</div>
                    <div style={{color:'white', fontSize:'1.8rem', fontWeight:'900', lineHeight:1.1}}>{totalCollected}</div>
                </div>
            </div>

            <input
                className="login-input"
                placeholder="Cauta copil dupa nume..."
                value={childSearch}
                onChange={e => setChildSearch(e.target.value)}
                style={{marginBottom:'20px'}}
            />

            <div style={{display:'grid', gridTemplateColumns:'repeat(auto-fill, minmax(200px, 1fr))', gap:'16px'}}>
                {filtered.map(c => {
                    const progress = c.progress ? c.progress.lastStickerId : 0;
                    const pct = totalStickers > 0 ? (progress / totalStickers) * 100 : 0;
                    const initials = `${c.name?.charAt(0) || ''}${c.surname?.charAt(0) || ''}`;
                    const isFull = totalStickers > 0 && progress >= totalStickers;

                    return (
                        <div
                            key={c.id}
                            onClick={() => setSelectedChild(c)}
                            style={{
                                background:'white', padding:'20px', borderRadius:'16px',
                                border:'1px solid var(--border-color)', cursor:'pointer',
                                boxShadow:'0 2px 8px rgba(0,0,0,0.05)',
                                transition:'transform 0.18s, box-shadow 0.18s'
                            }}
                            onMouseOver={e => { e.currentTarget.style.transform='translateY(-4px)'; e.currentTarget.style.boxShadow='0 8px 24px rgba(67,24,255,0.12)'; }}
                            onMouseOut={e => { e.currentTarget.style.transform='translateY(0)'; e.currentTarget.style.boxShadow='0 2px 8px rgba(0,0,0,0.05)'; }}
                        >
                            <div style={{display:'flex', alignItems:'center', gap:'12px', marginBottom:'14px'}}>
                                <div style={{
                                    width:'42px', height:'42px', borderRadius:'12px', flexShrink:0,
                                    background:'linear-gradient(135deg, var(--accent), #868cff)',
                                    display:'flex', alignItems:'center', justifyContent:'center',
                                    color:'white', fontWeight:'900', fontSize:'1rem'
                                }}>
                                    {initials}
                                </div>
                                <div style={{flex:1, minWidth:0}}>
                                    <div style={{fontWeight:'800', color:'var(--text-primary)', whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis'}}>{c.name} {c.surname}</div>
                                    <div style={{fontSize:'0.78rem', color:'var(--text-secondary)', marginTop:'2px'}}>
                                        {progress}{totalStickers > 0 ? ` / ${totalStickers}` : ''} stickere
                                    </div>
                                </div>
                                {isFull && (
                                    <span style={{background:'#dcfce7', color:'#15803d', fontSize:'0.68rem', fontWeight:'800', padding:'3px 8px', borderRadius:'20px', border:'1px solid #bbf7d0', flexShrink:0}}>COMPLET</span>
                                )}
                            </div>
                            {totalStickers > 0 && (
                                <div style={{background:'#f1f5f9', borderRadius:'999px', height:'6px', overflow:'hidden'}}>
                                    <div style={{
                                        width:`${pct}%`, height:'100%', borderRadius:'999px',
                                        background: isFull ? 'linear-gradient(90deg, #22c55e, #16a34a)' : 'linear-gradient(90deg, var(--accent), #868cff)',
                                        transition:'width 0.4s ease'
                                    }}/>
                                </div>
                            )}
                        </div>
                    );
                })}
                {filtered.length === 0 && (
                    <div style={{gridColumn:'1/-1', textAlign:'center', color:'var(--text-secondary)', padding:'40px 20px', fontStyle:'italic'}}>
                        Nu am gasit niciun copil.
                    </div>
                )}
            </div>
        </div>
    );
};

export default StickersHub;
