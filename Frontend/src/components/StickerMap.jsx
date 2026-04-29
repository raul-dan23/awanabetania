import React, { useState, useEffect } from 'react';
import { API_URL } from '../config';

const StickerMap = ({ child, user, onUnlock }) => {
    const [stickers, setStickers] = useState([]);
    const isLeader = user && !!user.role;

    useEffect(() => {
        fetch(`${API_URL}/stickers`)
            .then(r => r.ok ? r.json() : [])
            .then(data => setStickers(data.sort((a, b) => a.id - b.id)))
            .catch(() => setStickers([]));
    }, []);

    const currentLevel = child.progress ? child.progress.lastStickerId : 0;

    const LockIcon = ({ open }) => (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect x="5" y="11" width="14" height="10" rx="3" fill="currentColor" opacity="0.8"/>
            {open
                ? <path d="M8 11V7a4 4 0 0 1 8 0" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeDasharray="3 2"/>
                : <path d="M8 11V7a4 4 0 0 1 8 0v4" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round"/>
            }
        </svg>
    );

    const CheckIcon = () => (
        <svg width="26" height="26" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="12" cy="12" r="10" fill="#22c55e" opacity="0.15"/>
            <path d="M7 12.5l3.5 3.5 6.5-7" stroke="#15803d" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
    );

    return (
        <div style={{paddingTop:'4px'}}>
            <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'16px'}}>
                <p className="db-section-title" style={{margin:0}}>Colectia de Stickere</p>
                <span style={{
                    background: currentLevel > 0 && currentLevel === stickers.length ? '#dcfce7' : '#ede9fe',
                    color: currentLevel > 0 && currentLevel === stickers.length ? '#15803d' : '#6d28d9',
                    padding:'4px 12px', borderRadius:'20px', fontWeight:'800', fontSize:'0.82rem',
                    border:`1px solid ${currentLevel > 0 && currentLevel === stickers.length ? '#bbf7d0' : '#ddd6fe'}`
                }}>
                    {currentLevel} / {stickers.length} colectate
                </span>
            </div>

            {stickers.length > 0 && (
                <div style={{marginBottom:'20px', background:'#f1f5f9', borderRadius:'999px', height:'8px', overflow:'hidden'}}>
                    <div style={{
                        width:`${(currentLevel / stickers.length) * 100}%`,
                        height:'100%',
                        background:'linear-gradient(90deg, var(--accent), #868cff)',
                        borderRadius:'999px',
                        transition:'width 0.6s ease'
                    }}/>
                </div>
            )}

            <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(90px, 1fr))',
                gap: '10px',
                justifyItems: 'center'
            }}>
                {stickers.map((sticker, index) => {
                    const level = index + 1;
                    const isUnlocked = level <= currentLevel;
                    const isNext = level === currentLevel + 1;

                    let cardStyle = {
                        width: '100%', height: '100px', borderRadius: '12px',
                        display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center',
                        fontSize: '0.8rem', fontWeight: 'bold', position: 'relative',
                        transition: 'all 0.2s', overflow: 'hidden', cursor: 'default'
                    };

                    if (isUnlocked) {
                        cardStyle = { ...cardStyle, background: 'white', border: '2px solid #22c55e', color: '#15803d' };
                    } else if (isNext) {
                        cardStyle = {
                            ...cardStyle, background: '#fef3c7', border: '2px dashed #d97706', color: '#b45309',
                            transform: 'scale(1.05)', boxShadow: '0 0 14px rgba(245,158,11,0.35)',
                            cursor: isLeader ? 'pointer' : 'default', zIndex: 10
                        };
                    } else {
                        cardStyle = { ...cardStyle, background: '#f1f5f9', border: '1px solid #e2e8f0', color: '#94a3b8', opacity: 0.65 };
                    }

                    return (
                        <div key={sticker.id} style={cardStyle} onClick={() => { if(isLeader && isNext) onUnlock(child.id); }}>
                            {sticker.imagePath ? (
                                <>
                                    <img
                                        src={sticker.imagePath}
                                        alt={sticker.name}
                                        style={{
                                            width: '100%', height: '100%', objectFit: 'contain',
                                            filter: isUnlocked ? 'none' : 'grayscale(100%) blur(1px)',
                                            opacity: isUnlocked ? 1 : 0.45
                                        }}
                                    />
                                    {!isUnlocked && (
                                        <div style={{position:'absolute', top:0, left:0, width:'100%', height:'100%', display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', zIndex:2}}>
                                            <LockIcon open={isNext} />
                                            {isNext && <span style={{fontSize:'0.62rem', marginTop:'4px', fontWeight:'900'}}>APASA!</span>}
                                        </div>
                                    )}
                                </>
                            ) : (
                                isUnlocked ? (
                                    <div style={{display:'flex', flexDirection:'column', alignItems:'center', gap:'4px'}}>
                                        <CheckIcon />
                                        <span style={{fontSize:'0.7rem', textAlign:'center', padding:'0 4px'}}>{sticker.name}</span>
                                    </div>
                                ) : (
                                    <div style={{display:'flex', flexDirection:'column', alignItems:'center', gap:'5px', color: isNext ? '#b45309' : '#94a3b8'}}>
                                        <LockIcon open={isNext} />
                                        <span style={{fontSize:'0.68rem', fontWeight:'800'}}>{isNext ? 'Apasa!' : `#${level}`}</span>
                                    </div>
                                )
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default StickerMap;
