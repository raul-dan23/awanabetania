import React, { useState, useEffect } from 'react';

const API_URL = 'http://awana.betania-tm.ro/api';


const AdminDashboard = ({ currentUser }) => {
    // Stari
    const [isUnlocked, setIsUnlocked] = useState(false);
    const [pin, setPin] = useState('');
    const [data, setData] = useState({ leaders: [], children: [] });
    const [viewMode, setViewMode] = useState('LEADERS'); // 'LEADERS' sau 'CHILDREN'
    const [searchTerm, setSearchTerm] = useState(''); // Cautare
    const [visiblePasswords, setVisiblePasswords] = useState({}); // Parole decriptate

    // PIN-ul tau secret
    const MASTER_PIN = "0000";

    useEffect(() => {
        if (isUnlocked) {
            fetchData();
        }
    }, [isUnlocked]);

    const fetchData = () => {
        fetch(`${API_URL}/admin/all-users`)
            .then(res => res.json())
            .then(data => setData(data))
            .catch(err => alert("Eroare la incarcarea datelor! Verifica API_URL."));
    };

    const handleUnlock = () => {
        if (pin === MASTER_PIN) {
            setIsUnlocked(true);
        } else {
            alert("PIN Incorect!");
            setPin('');
        }
    };

    const revealPassword = (id, encryptedPass) => {
        if (visiblePasswords[id]) {
            const newVis = { ...visiblePasswords };
            delete newVis[id];
            setVisiblePasswords(newVis);
            return;
        }

        fetch(`${API_URL}/admin/decrypt-password`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ password: encryptedPass })
        })
            .then(res => res.json())
            .then(resp => {
                setVisiblePasswords(prev => ({ ...prev, [id]: resp.realPassword }));
            })
            .catch(() => alert("Eroare decriptare."));
    };

    // --- FILTRARE PENTRU CAUTARE ---
    const getFilteredList = () => {
        const list = viewMode === 'LEADERS' ? data.leaders : data.children;
        if (!searchTerm) return list;
        return list.filter(user =>
            user.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
            user.surname.toLowerCase().includes(searchTerm.toLowerCase())
        );
    };

    const displayList = getFilteredList();

    // --- ECRAN BLOCAT (PIN) ---
    if (!isUnlocked) {
        return (
            <div style={{ height: '80vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', color: '#333' }}>
                <div style={{fontSize:'4rem', marginBottom:'10px'}}>🛡️</div>
                <h2>Control Center</h2>
                <p style={{color:'#666'}}>Zona restrictionata Admin.</p>
                <input
                    type="password" inputMode="numeric" pattern="[0-9]*"
                    value={pin} onChange={(e) => setPin(e.target.value)}
                    placeholder="PIN Master"
                    style={{ padding: '15px', fontSize: '1.5rem', textAlign: 'center', borderRadius: '10px', margin: '20px', border: '2px solid #ddd', width:'200px' }}
                />
                <button onClick={handleUnlock} style={{ padding: '15px 40px', background: '#e11d48', color: 'white', fontWeight: 'bold', border: 'none', borderRadius: '10px', fontSize: '1.2rem' }}>
                    DESCHIDE
                </button>
            </div>
        );
    }

    return (
        <div className="animate-in" style={{ padding: '10px', maxWidth: '800px', margin: '0 auto', paddingBottom:'100px' }}>

            {/* HEADER */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <h2 style={{margin:0}}>🎛️ Admin</h2>
                <button onClick={() => setIsUnlocked(false)} style={{ background: '#333', color: 'white', padding: '8px 15px', borderRadius:'8px', border:'none' }}>🔒 Iesi</button>
            </div>

            {/* TAB-URI (LIDERI vs COPII) */}
            <div style={{ display: 'flex', gap: '10px', marginBottom: '15px' }}>
                <button
                    onClick={() => setViewMode('LEADERS')}
                    style={{ flex: 1, padding: '12px', background: viewMode === 'LEADERS' ? '#0284c7' : '#e2e8f0', color: viewMode === 'LEADERS' ? 'white' : 'black', border: 'none', borderRadius: '8px', fontWeight: 'bold' }}>
                    Lideri ({data.leaders.length})
                </button>
                <button
                    onClick={() => setViewMode('CHILDREN')}
                    style={{ flex: 1, padding: '12px', background: viewMode === 'CHILDREN' ? '#16a34a' : '#e2e8f0', color: viewMode === 'CHILDREN' ? 'white' : 'black', border: 'none', borderRadius: '8px', fontWeight: 'bold' }}>
                    Copii ({data.children.length})
                </button>
            </div>

            {/* CAUTARE */}
            <input
                className="login-input"
                placeholder={`🔍 Cauta ${viewMode === 'LEADERS' ? 'lider' : 'copil'}...`}
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
                style={{marginBottom:'20px', padding:'12px', fontSize:'1rem'}}
            />

            {/* LISTA TIP "DOSAR" (CARDURI) */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                {displayList.map(user => (
                    <div key={user.id} style={{
                        background: 'white',
                        padding: '20px',
                        borderRadius: '15px',
                        boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
                        borderLeft: viewMode === 'LEADERS' ? '5px solid #0284c7' : '5px solid #16a34a'
                    }}>
                        {/* HEADER DOSAR */}
                        <div style={{display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:'15px', borderBottom:'1px solid #f1f5f9', paddingBottom:'10px'}}>
                            <div>
                                <h3 style={{margin:0, fontSize:'1.2rem', color:'#1e293b'}}>{user.name} {user.surname}</h3>
                                <span style={{fontSize:'0.85rem', color:'#64748b'}}>ID: #{user.id}</span>
                            </div>
                            <span className="badge" style={{
                                background: viewMode === 'LEADERS' ? '#e0f2fe' : '#dcfce7',
                                color: viewMode === 'LEADERS' ? '#0369a1' : '#15803d'
                            }}>
                                {viewMode === 'LEADERS' ? (user.role || 'LIDER') : 'COPIL'}
                            </span>
                        </div>

                        {/* DETALII */}
                        <div style={{display:'grid', gap:'8px', fontSize:'0.95rem', color:'#334155'}}>
                            {viewMode === 'LEADERS' ? (
                                <div>📞 <strong>Tel:</strong> <a href={`tel:${user.phoneNumber}`}>{user.phoneNumber || '-'}</a></div>
                            ) : (
                                <>
                                    <div>🎂 <strong>Varsta:</strong> {user.age} ani</div>
                                    <div>👤 <strong>Parinte:</strong> {user.parentName}</div>
                                    <div>📞 <strong>Tel:</strong> <a href={`tel:${user.parentPhone}`}>{user.parentPhone}</a></div>
                                </>
                            )}
                        </div>

                        {/* ZONA PAROLA */}
                        <div style={{marginTop:'15px', background:'#f8fafc', padding:'10px', borderRadius:'8px', display:'flex', justifyContent:'space-between', alignItems:'center'}}>
                            <div style={{fontFamily:'monospace', fontSize:'1.1rem', color: visiblePasswords[user.id] ? '#dc2626' : '#94a3b8', fontWeight:'bold'}}>
                                {visiblePasswords[user.id] ? visiblePasswords[user.id] : '••••••••'}
                            </div>
                            <button
                                onClick={() => revealPassword(user.id, user.password)}
                                style={{
                                    background: visiblePasswords[user.id] ? '#fee2e2' : 'white',
                                    color: visiblePasswords[user.id] ? '#dc2626' : '#333',
                                    border: '1px solid #cbd5e1',
                                    padding: '8px 12px',
                                    borderRadius: '8px',
                                    cursor: 'pointer',
                                    fontWeight: 'bold',
                                    fontSize: '0.9rem'
                                }}
                            >
                                {visiblePasswords[user.id] ? 'Ascunde' : '👁️ Vezi Parola'}
                            </button>
                        </div>

                    </div>
                ))}

                {displayList.length === 0 && (
                    <div style={{textAlign:'center', color:'#94a3b8', padding:'20px'}}>
                        Nu am gasit niciun rezultat.
                    </div>
                )}
            </div>
        </div>
    );
};

export default AdminDashboard;