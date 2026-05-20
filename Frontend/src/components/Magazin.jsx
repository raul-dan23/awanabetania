import React, { useState, useEffect } from 'react';
import { toast } from 'sonner';
import { API_URL } from '../config';
import { useNfcBridge } from '../hooks/useNfcBridge';

const Magazin = ({ user }) => {
    const isDirector = user?.role === 'DIRECTOR' || user?.role === 'COORDONATOR';
    const [tab, setTab] = useState('bon');

    // ── PRODUSE ──────────────────────────────────────────────────
    const [products, setProducts] = useState([]);
    const [editPinInput, setEditPinInput] = useState('');
    const [editPinVerified, setEditPinVerified] = useState(false);
    const [newProduct, setNewProduct] = useState({ name: '', pointPrice: '', category: '' });
    const [showAddForm, setShowAddForm] = useState(false);

    // ── BON NOU ──────────────────────────────────────────────────
    const [children, setChildren] = useState([]);
    const [childSearch, setChildSearch] = useState('');
    const [selectedChild, setSelectedChild] = useState(null);
    const [quantities, setQuantities] = useState({});

    // ── CONTABIL ─────────────────────────────────────────────────
    const [pendingBons, setPendingBons] = useState([]);
    const [allBons, setAllBons] = useState([]);
    const [contabilTab, setContabilTab] = useState('pending');
    const [nfcChild, setNfcChild] = useState(null);

    const nfcBridgeConnected = useNfcBridge((uid) => {
        const found = children.find(c => c.nfcUid === uid);
        if (found) {
            setNfcChild(found);
            toast.success(`Card: ${found.name} ${found.surname}`);
        } else {
            toast.error('Card necunoscut sau neînregistrat.');
        }
    });

    useEffect(() => { fetchProducts(); fetchChildren(); }, []);

    useEffect(() => {
        if (tab === 'contabil') {
            fetchPendingBons();
            const iv = setInterval(fetchPendingBons, 3000);
            return () => clearInterval(iv);
        }
    }, [tab]);

    const fetchProducts = () =>
        fetch(`${API_URL}/products`).then(r => r.json()).then(setProducts).catch(() => {});

    const fetchChildren = () =>
        fetch(`${API_URL}/children`)
            .then(r => r.json())
            .then(d => setChildren(d.sort((a, b) => a.name.localeCompare(b.name))))
            .catch(() => {});

    const fetchPendingBons = () =>
        fetch(`${API_URL}/bons/pending`).then(r => r.json()).then(setPendingBons).catch(() => {});

    const fetchAllBons = () =>
        fetch(`${API_URL}/bons/all`).then(r => r.json()).then(setAllBons).catch(() => {});

    // ── OPERATII PRODUSE ─────────────────────────────────────────
    const verifyEditPin = () => {
        fetch(`${API_URL}/admin/verify-pin`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ pin: editPinInput })
        })
        .then(r => {
            if (r.ok) setEditPinVerified(true);
            else { toast.error('PIN incorect!'); setEditPinInput(''); }
        })
        .catch(() => toast.error('Eroare conexiune.'));
    };

    const addProduct = () => {
        if (!newProduct.name || !newProduct.pointPrice) { toast.error('Completează numele și prețul.'); return; }
        fetch(`${API_URL}/products`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-Admin-Pin': editPinInput },
            body: JSON.stringify({ name: newProduct.name, pointPrice: parseInt(newProduct.pointPrice), category: newProduct.category, available: true })
        })
        .then(r => r.ok ? r.json() : Promise.reject())
        .then(() => { toast.success('Produs adăugat!'); setNewProduct({ name: '', pointPrice: '', category: '' }); setShowAddForm(false); fetchProducts(); })
        .catch(() => toast.error('Eroare la adăugare.'));
    };

    const toggleAvailable = (p) => {
        fetch(`${API_URL}/products/${p.id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', 'X-Admin-Pin': editPinInput },
            body: JSON.stringify({ ...p, available: !p.available })
        })
        .then(r => r.ok ? fetchProducts() : Promise.reject())
        .catch(() => toast.error('Eroare.'));
    };

    const deleteProduct = (id) => {
        if (!window.confirm('Sigur ștergi acest produs?')) return;
        fetch(`${API_URL}/products/${id}`, { method: 'DELETE', headers: { 'X-Admin-Pin': editPinInput } })
            .then(r => { if (r.ok) { toast.success('Produs șters.'); fetchProducts(); } else toast.error('Eroare.'); })
            .catch(() => toast.error('Eroare.'));
    };

    // ── OPERATII BON ─────────────────────────────────────────────
    const getTotalPoints = () =>
        Object.entries(quantities).reduce((sum, [pid, qty]) => {
            const p = products.find(x => x.id === parseInt(pid));
            return sum + (p ? p.pointPrice * qty : 0);
        }, 0);

    const getSelectedItems = () =>
        Object.entries(quantities)
            .filter(([, qty]) => qty > 0)
            .map(([pid, qty]) => {
                const p = products.find(x => x.id === parseInt(pid));
                return { id: p.id, name: p.name, pointPrice: p.pointPrice, qty };
            });

    const createBon = () => {
        const items = getSelectedItems();
        if (!selectedChild) { toast.error('Selectează un copil.'); return; }
        if (items.length === 0) { toast.error('Alege cel puțin un produs.'); return; }
        fetch(`${API_URL}/bons`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                childId: selectedChild.id,
                leaderName: user.name + ' ' + (user.surname || ''),
                items: JSON.stringify(items),
                totalPoints: getTotalPoints()
            })
        })
        .then(r => r.ok ? r.json() : Promise.reject())
        .then(() => {
            toast.success(`Bon trimis! Total: ${getTotalPoints()} puncte`);
            setSelectedChild(null); setQuantities({}); setChildSearch('');
        })
        .catch(() => toast.error('Eroare la creare bon.'));
    };

    // ── OPERATII CONTABIL ────────────────────────────────────────
    const approveBon = (id) => {
        fetch(`${API_URL}/bons/${id}/approve`, { method: 'POST' })
            .then(r => r.ok ? r.json() : r.text().then(t => Promise.reject(t)))
            .then(data => { toast.success(`Aprobat! Sold nou: ${data.remainingPoints} puncte`); fetchPendingBons(); })
            .catch(err => toast.error(typeof err === 'string' ? err : 'Eroare la aprobare.'));
    };

    const rejectBon = (id) => {
        fetch(`${API_URL}/bons/${id}/reject`, { method: 'POST' })
            .then(r => r.ok ? r.json() : Promise.reject())
            .then(() => { toast.success('Bon respins.'); fetchPendingBons(); })
            .catch(() => toast.error('Eroare.'));
    };

    const filteredChildren = childSearch.length >= 2
        ? children.filter(c => `${c.name} ${c.surname}`.toLowerCase().includes(childSearch.toLowerCase()))
        : [];

    const tabs = [
        ...(isDirector ? [{ id: 'produse', label: 'Produse' }] : []),
        { id: 'bon', label: 'Bon Nou' },
        { id: 'contabil', label: `Contabil${pendingBons.length > 0 ? ` (${pendingBons.length})` : ''}` }
    ];

    return (
        <div style={{ maxWidth: '800px', margin: '0 auto', padding: '10px', paddingBottom: '120px' }}>

            {/* HERO */}
            <div className="db-hero" style={{ marginBottom: '24px' }}>
                <div className="db-hero-left">
                    <span className="db-greeting">Târg Awana</span>
                    <h1 className="db-name">Magazin</h1>
                    <span className="db-role-pill">{products.filter(p => p.available).length} produse disponibile</span>
                </div>
            </div>

            {/* TABS */}
            <div style={{ display: 'flex', gap: '8px', marginBottom: '20px', background: '#f4f7fe', padding: '4px', borderRadius: '14px' }}>
                {tabs.map(t => (
                    <button key={t.id} onClick={() => setTab(t.id)} style={{
                        flex: 1, padding: '10px', border: 'none', cursor: 'pointer', borderRadius: '10px',
                        fontWeight: '800', fontSize: '0.88rem', transition: 'all 0.18s',
                        background: tab === t.id ? 'white' : 'transparent',
                        color: tab === t.id ? '#4318ff' : '#64748b',
                        boxShadow: tab === t.id ? '0 2px 8px rgba(0,0,0,0.08)' : 'none'
                    }}>{t.label}</button>
                ))}
            </div>

            {/* ════════════ TAB: PRODUSE ════════════ */}
            {tab === 'produse' && (
                <div>
                    {!editPinVerified ? (
                        <div style={{ background: 'white', borderRadius: '16px', padding: '20px', marginBottom: '16px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #e2e8f0' }}>
                            <p style={{ margin: '0 0 10px', color: '#64748b', fontSize: '0.85rem', fontWeight: '600' }}>Introdu PIN-ul admin pentru a gestiona produsele:</p>
                            <div style={{ display: 'flex', gap: '8px' }}>
                                <input
                                    type="password" inputMode="numeric"
                                    value={editPinInput}
                                    onChange={e => setEditPinInput(e.target.value)}
                                    onKeyDown={e => e.key === 'Enter' && verifyEditPin()}
                                    placeholder="PIN"
                                    style={{ flex: 1, padding: '10px 14px', borderRadius: '10px', border: '2px solid #e2e8f0', fontSize: '1.2rem', textAlign: 'center', fontFamily: 'monospace', outline: 'none' }}
                                />
                                <button onClick={verifyEditPin} style={{
                                    padding: '10px 18px', borderRadius: '10px', border: 'none',
                                    background: 'linear-gradient(135deg, #4318ff, #868cff)',
                                    color: 'white', fontWeight: '800', cursor: 'pointer'
                                }}>Deblochează</button>
                            </div>
                        </div>
                    ) : (
                        <button onClick={() => setShowAddForm(v => !v)} style={{
                            width: '100%', padding: '13px', border: 'none', borderRadius: '12px', marginBottom: '16px',
                            background: 'linear-gradient(135deg, #16a34a, #4ade80)',
                            color: 'white', fontWeight: '800', cursor: 'pointer', fontSize: '0.95rem'
                        }}>+ Adaugă produs</button>
                    )}

                    {showAddForm && editPinVerified && (
                        <div style={{ background: 'white', borderRadius: '16px', padding: '20px', marginBottom: '16px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '2px solid #dcfce7' }}>
                            <h3 style={{ margin: '0 0 14px', color: '#16a34a', fontWeight: '900' }}>Produs nou</h3>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                                <input className="login-input" placeholder="Nume produs *" value={newProduct.name} onChange={e => setNewProduct(p => ({ ...p, name: e.target.value }))} />
                                <input className="login-input" placeholder="Preț în puncte *" type="number" min="1" value={newProduct.pointPrice} onChange={e => setNewProduct(p => ({ ...p, pointPrice: e.target.value }))} />
                                <input className="login-input" placeholder="Categorie (opțional)" value={newProduct.category} onChange={e => setNewProduct(p => ({ ...p, category: e.target.value }))} />
                                <div style={{ display: 'flex', gap: '8px' }}>
                                    <button onClick={addProduct} style={{ flex: 1, padding: '12px', border: 'none', borderRadius: '10px', background: '#16a34a', color: 'white', fontWeight: '800', cursor: 'pointer' }}>Adaugă</button>
                                    <button onClick={() => setShowAddForm(false)} style={{ padding: '12px 16px', border: '1px solid #e2e8f0', borderRadius: '10px', background: 'white', cursor: 'pointer', fontWeight: '700', color: '#64748b' }}>Anulează</button>
                                </div>
                            </div>
                        </div>
                    )}

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                        {products.map(p => (
                            <div key={p.id} style={{
                                background: 'white', borderRadius: '14px', padding: '14px 18px',
                                boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #e2e8f0',
                                borderLeft: `4px solid ${p.available ? '#16a34a' : '#cbd5e1'}`,
                                opacity: p.available ? 1 : 0.6
                            }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '10px' }}>
                                    <div style={{ flex: 1, minWidth: 0 }}>
                                        <div style={{ fontWeight: '900', color: '#1e293b', fontSize: '1rem' }}>{p.name}</div>
                                        <div style={{ display: 'flex', gap: '6px', marginTop: '4px', alignItems: 'center', flexWrap: 'wrap' }}>
                                            <span style={{ fontWeight: '800', color: '#f59e0b' }}>{p.pointPrice} pct</span>
                                            {p.category && <span style={{ fontSize: '0.72rem', color: '#64748b', background: '#f1f5f9', padding: '1px 8px', borderRadius: '20px', fontWeight: '700' }}>{p.category}</span>}
                                            <span style={{ fontSize: '0.72rem', fontWeight: '800', padding: '1px 8px', borderRadius: '20px', background: p.available ? '#dcfce7' : '#f1f5f9', color: p.available ? '#15803d' : '#94a3b8' }}>
                                                {p.available ? 'Disponibil' : 'Oprit'}
                                            </span>
                                        </div>
                                    </div>
                                    {editPinVerified && (
                                        <div style={{ display: 'flex', gap: '6px', flexShrink: 0 }}>
                                            <button onClick={() => toggleAvailable(p)} style={{ padding: '6px 11px', border: '1px solid #e2e8f0', borderRadius: '8px', background: 'white', cursor: 'pointer', fontWeight: '700', fontSize: '0.78rem', color: '#64748b' }}>
                                                {p.available ? 'Oprește' : 'Activează'}
                                            </button>
                                            <button onClick={() => deleteProduct(p.id)} style={{ padding: '6px 11px', border: '1px solid #fecaca', borderRadius: '8px', background: '#fff5f5', cursor: 'pointer', fontWeight: '700', fontSize: '0.78rem', color: '#dc2626' }}>
                                                Șterge
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                        {products.length === 0 && (
                            <div style={{ textAlign: 'center', color: '#94a3b8', padding: '50px', fontStyle: 'italic' }}>Niciun produs adăugat încă.</div>
                        )}
                    </div>
                </div>
            )}

            {/* ════════════ TAB: BON NOU ════════════ */}
            {tab === 'bon' && (
                <div>
                    {/* Pasul 1: Selectare copil */}
                    <div style={{ background: 'white', borderRadius: '16px', padding: '18px', marginBottom: '14px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #e2e8f0' }}>
                        <div style={{ fontWeight: '900', color: '#64748b', fontSize: '0.78rem', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: '12px' }}>1. Selectează copilul</div>
                        {selectedChild ? (
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '14px 16px', background: 'linear-gradient(135deg, #f0fdf4, #dcfce7)', borderRadius: '12px', border: '2px solid #86efac' }}>
                                <div>
                                    <div style={{ fontWeight: '900', color: '#15803d', fontSize: '1.1rem' }}>{selectedChild.name} {selectedChild.surname}</div>
                                    <div style={{ color: '#16a34a', fontWeight: '700', fontSize: '0.88rem', marginTop: '2px' }}>{selectedChild.seasonPoints || 0} puncte disponibile</div>
                                </div>
                                <button onClick={() => { setSelectedChild(null); setQuantities({}); }} style={{ padding: '8px 14px', border: '1px solid #86efac', borderRadius: '8px', background: 'white', cursor: 'pointer', fontWeight: '700', fontSize: '0.8rem', color: '#15803d' }}>
                                    Schimbă
                                </button>
                            </div>
                        ) : (
                            <>
                                <input
                                    className="login-input"
                                    placeholder="Caută după nume (min. 2 litere)..."
                                    value={childSearch}
                                    onChange={e => setChildSearch(e.target.value)}
                                    style={{ marginBottom: '8px' }}
                                />
                                {filteredChildren.length > 0 && (
                                    <div style={{ border: '1px solid #e2e8f0', borderRadius: '12px', overflow: 'hidden', maxHeight: '240px', overflowY: 'auto' }}>
                                        {filteredChildren.map((c, i) => (
                                            <button key={c.id} onClick={() => { setSelectedChild(c); setChildSearch(''); }} style={{
                                                width: '100%', padding: '12px 16px', border: 'none',
                                                borderBottom: i < filteredChildren.length - 1 ? '1px solid #f1f5f9' : 'none',
                                                background: 'white', cursor: 'pointer', textAlign: 'left',
                                                display: 'flex', justifyContent: 'space-between', alignItems: 'center'
                                            }}>
                                                <span style={{ fontWeight: '700', color: '#1e293b' }}>{c.name} {c.surname}</span>
                                                <span style={{ fontWeight: '800', color: '#f59e0b', fontSize: '0.85rem', flexShrink: 0, marginLeft: '8px' }}>{c.seasonPoints || 0} pct</span>
                                            </button>
                                        ))}
                                    </div>
                                )}
                                {childSearch.length >= 2 && filteredChildren.length === 0 && (
                                    <p style={{ color: '#94a3b8', fontSize: '0.85rem', textAlign: 'center', margin: '8px 0 0' }}>Nu a fost găsit niciun copil.</p>
                                )}
                            </>
                        )}
                    </div>

                    {/* Pasul 2: Produse */}
                    {selectedChild && (
                        <>
                            <div style={{ background: 'white', borderRadius: '16px', padding: '18px', marginBottom: '14px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #e2e8f0' }}>
                                <div style={{ fontWeight: '900', color: '#64748b', fontSize: '0.78rem', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: '12px' }}>2. Alege produsele</div>
                                {products.filter(p => p.available).length === 0 && (
                                    <p style={{ color: '#94a3b8', textAlign: 'center', padding: '20px 0' }}>Niciun produs disponibil.</p>
                                )}
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                    {products.filter(p => p.available).map(p => {
                                        const qty = quantities[p.id] || 0;
                                        return (
                                            <div key={p.id} style={{
                                                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                                padding: '12px 14px', borderRadius: '12px', border: `1px solid ${qty > 0 ? '#86efac' : '#e2e8f0'}`,
                                                background: qty > 0 ? '#f0fdf4' : '#f8fafc'
                                            }}>
                                                <div style={{ flex: 1, minWidth: 0 }}>
                                                    <div style={{ fontWeight: '800', color: '#1e293b' }}>{p.name}</div>
                                                    <div style={{ color: '#f59e0b', fontWeight: '700', fontSize: '0.82rem' }}>{p.pointPrice} pct{p.category ? ` · ${p.category}` : ''}</div>
                                                </div>
                                                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexShrink: 0 }}>
                                                    <button onClick={() => setQuantities(q => ({ ...q, [p.id]: Math.max(0, (q[p.id] || 0) - 1) }))} style={{ width: '34px', height: '34px', borderRadius: '8px', border: '1px solid #e2e8f0', background: 'white', cursor: 'pointer', fontWeight: '900', fontSize: '1.2rem', color: '#64748b', lineHeight: 1 }}>−</button>
                                                    <span style={{ fontWeight: '900', fontSize: '1.1rem', minWidth: '22px', textAlign: 'center', color: '#1e293b' }}>{qty}</span>
                                                    <button onClick={() => setQuantities(q => ({ ...q, [p.id]: (q[p.id] || 0) + 1 }))} style={{ width: '34px', height: '34px', borderRadius: '8px', border: 'none', background: 'linear-gradient(135deg, #16a34a, #4ade80)', cursor: 'pointer', fontWeight: '900', fontSize: '1.2rem', color: 'white', lineHeight: 1 }}>+</button>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>

                            {/* Total + Buton */}
                            {getTotalPoints() > 0 && (
                                <div style={{
                                    position: 'sticky', bottom: '80px',
                                    background: 'white', borderRadius: '16px', padding: '16px 20px',
                                    boxShadow: '0 -4px 24px rgba(0,0,0,0.12)', border: '2px solid #4318ff',
                                    display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '12px'
                                }}>
                                    <div>
                                        <div style={{ color: '#94a3b8', fontWeight: '700', fontSize: '0.72rem', textTransform: 'uppercase' }}>Total</div>
                                        <div style={{ fontWeight: '900', fontSize: '1.6rem', color: '#4318ff', lineHeight: 1.1 }}>{getTotalPoints()} <span style={{ fontSize: '0.9rem' }}>pct</span></div>
                                        {getTotalPoints() > (selectedChild.seasonPoints || 0) && (
                                            <div style={{ color: '#dc2626', fontWeight: '700', fontSize: '0.75rem', marginTop: '2px' }}>
                                                ⚠ Depășește soldul ({selectedChild.seasonPoints || 0} disponibile)
                                            </div>
                                        )}
                                    </div>
                                    <button onClick={createBon} style={{
                                        padding: '14px 22px', border: 'none', borderRadius: '12px',
                                        background: 'linear-gradient(135deg, #4318ff, #868cff)',
                                        color: 'white', fontWeight: '900', cursor: 'pointer', fontSize: '1rem', flexShrink: 0
                                    }}>Generează bon →</button>
                                </div>
                            )}
                        </>
                    )}
                </div>
            )}

            {/* ════════════ TAB: CONTABIL ════════════ */}
            {tab === 'contabil' && (
                <div>
                    <div style={{ display: 'flex', gap: '8px', marginBottom: '16px', background: '#f4f7fe', padding: '4px', borderRadius: '14px' }}>
                        {[
                            { id: 'pending', label: `În așteptare (${pendingBons.length})` },
                            { id: 'all', label: 'Istoric' }
                        ].map(t => (
                            <button key={t.id} onClick={() => { setContabilTab(t.id); if (t.id === 'all') fetchAllBons(); }} style={{
                                flex: 1, padding: '9px', border: 'none', cursor: 'pointer', borderRadius: '10px',
                                fontWeight: '800', fontSize: '0.85rem', transition: 'all 0.18s',
                                background: contabilTab === t.id ? 'white' : 'transparent',
                                color: contabilTab === t.id ? '#4318ff' : '#64748b',
                                boxShadow: contabilTab === t.id ? '0 2px 8px rgba(0,0,0,0.08)' : 'none'
                            }}>{t.label}</button>
                        ))}
                    </div>

                    {/* Status NFC Bridge */}
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 14px', borderRadius: '10px', background: nfcBridgeConnected ? '#f0fdf4' : '#f8fafc', border: `1px solid ${nfcBridgeConnected ? '#86efac' : '#e2e8f0'}`, marginBottom: '8px' }}>
                        <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: nfcBridgeConnected ? '#16a34a' : '#94a3b8', flexShrink: 0 }} />
                        <span style={{ fontSize: '0.82rem', fontWeight: '700', color: nfcBridgeConnected ? '#15803d' : '#64748b', flex: 1 }}>
                            {nfcBridgeConnected ? 'NFC activ — pune cardul pe cititor pentru identificare' : 'NFC Bridge deconectat — porneste nfc-bridge.jar'}
                        </span>
                        {nfcChild && (
                            <button onClick={() => setNfcChild(null)} style={{ padding: '3px 10px', border: '1px solid #86efac', borderRadius: '6px', background: 'white', cursor: 'pointer', fontSize: '0.75rem', fontWeight: '700', color: '#15803d', flexShrink: 0 }}>
                                Sterge filtru
                            </button>
                        )}
                    </div>

                    {/* Banner copil identificat prin NFC */}
                    {nfcChild && contabilTab === 'pending' && (
                        <div style={{ padding: '14px 18px', borderRadius: '14px', background: 'linear-gradient(135deg, #f0fdf4, #dcfce7)', border: '2px solid #86efac', marginBottom: '4px' }}>
                            <div style={{ fontWeight: '900', color: '#15803d', fontSize: '1.05rem' }}>
                                {nfcChild.name} {nfcChild.surname}
                            </div>
                            <div style={{ fontSize: '0.82rem', color: '#16a34a', fontWeight: '700', marginTop: '2px' }}>
                                {nfcChild.seasonPoints || 0} puncte disponibile · se afișează doar bonurile acestui copil
                            </div>
                        </div>
                    )}

                    {/* Bonuri în așteptare */}
                    {contabilTab === 'pending' && (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                            {pendingBons.filter(b => !nfcChild || b.childId === nfcChild.id).length === 0 && pendingBons.length === 0 && (
                                <div style={{ textAlign: 'center', padding: '60px 20px', color: '#94a3b8' }}>
                                    <div style={{ fontSize: '2.5rem', marginBottom: '8px' }}>✓</div>
                                    <div style={{ fontWeight: '700' }}>Niciun bon în așteptare.</div>
                                    <div style={{ fontSize: '0.85rem', marginTop: '4px' }}>Se actualizează automat.</div>
                                </div>
                            )}
                            {nfcChild && pendingBons.filter(b => b.childId === nfcChild.id).length === 0 && (
                                <div style={{ textAlign: 'center', padding: '30px 20px', color: '#94a3b8' }}>
                                    <div style={{ fontWeight: '700' }}>Niciun bon în așteptare pentru {nfcChild.name}.</div>
                                </div>
                            )}
                            {pendingBons.filter(b => !nfcChild || b.childId === nfcChild.id).map(bon => {
                                const items = (() => { try { return JSON.parse(bon.items || '[]'); } catch { return []; } })();
                                return (
                                    <div key={bon.id} style={{ background: 'white', borderRadius: '16px', overflow: 'hidden', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #e2e8f0', borderLeft: '4px solid #f59e0b' }}>
                                        <div style={{ padding: '14px 18px', borderBottom: '1px solid #f1f5f9', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                            <div>
                                                <div style={{ fontWeight: '900', color: '#1e293b', fontSize: '1.05rem' }}>{bon.childName}</div>
                                                <div style={{ fontSize: '0.78rem', color: '#64748b', marginTop: '2px' }}>
                                                    Lider: <strong>{bon.leaderName}</strong> · {new Date(bon.createdAt).toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' })}
                                                </div>
                                            </div>
                                            <div style={{ textAlign: 'right', flexShrink: 0, marginLeft: '10px' }}>
                                                <div style={{ fontWeight: '900', color: '#f59e0b', fontSize: '1.4rem', lineHeight: 1 }}>{bon.totalPoints}</div>
                                                <div style={{ fontSize: '0.7rem', color: '#94a3b8', fontWeight: '700' }}>PUNCTE</div>
                                            </div>
                                        </div>
                                        <div style={{ padding: '12px 18px', borderBottom: '1px solid #f1f5f9' }}>
                                            {items.map((item, i) => (
                                                <div key={i} style={{ display: 'flex', justifyContent: 'space-between', padding: '3px 0', fontSize: '0.9rem' }}>
                                                    <span style={{ fontWeight: '600', color: '#1e293b' }}>{item.qty}× {item.name}</span>
                                                    <span style={{ fontWeight: '800', color: '#f59e0b' }}>{item.pointPrice * item.qty} pct</span>
                                                </div>
                                            ))}
                                        </div>
                                        <div style={{ padding: '12px 18px', display: 'flex', gap: '8px' }}>
                                            <button onClick={() => approveBon(bon.id)} style={{ flex: 1, padding: '13px', border: 'none', borderRadius: '10px', background: 'linear-gradient(135deg, #16a34a, #4ade80)', color: 'white', fontWeight: '900', cursor: 'pointer', fontSize: '1rem' }}>
                                                ✓ Aprobă
                                            </button>
                                            <button onClick={() => rejectBon(bon.id)} style={{ padding: '13px 20px', border: '1px solid #fecaca', borderRadius: '10px', background: 'white', cursor: 'pointer', fontWeight: '800', fontSize: '0.9rem', color: '#dc2626' }}>
                                                ✗ Respinge
                                            </button>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}

                    {/* Istoric */}
                    {contabilTab === 'all' && (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                            {allBons.map(bon => {
                                const items = (() => { try { return JSON.parse(bon.items || '[]'); } catch { return []; } })();
                                const sc = bon.status === 'APPROVED' ? '#16a34a' : bon.status === 'REJECTED' ? '#dc2626' : '#f59e0b';
                                const sl = bon.status === 'APPROVED' ? 'Aprobat' : bon.status === 'REJECTED' ? 'Respins' : 'În așteptare';
                                return (
                                    <div key={bon.id} style={{ background: 'white', borderRadius: '14px', padding: '14px 18px', boxShadow: '0 2px 6px rgba(0,0,0,0.04)', border: '1px solid #e2e8f0', borderLeft: `4px solid ${sc}` }}>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '10px' }}>
                                            <div style={{ flex: 1, minWidth: 0 }}>
                                                <div style={{ fontWeight: '900', color: '#1e293b' }}>{bon.childName}</div>
                                                <div style={{ fontSize: '0.78rem', color: '#64748b', marginTop: '2px' }}>
                                                    {items.map(i => `${i.qty}× ${i.name}`).join(', ')}
                                                </div>
                                                <div style={{ fontSize: '0.75rem', color: '#94a3b8', marginTop: '2px' }}>
                                                    {bon.leaderName} · {new Date(bon.createdAt).toLocaleString('ro-RO', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })}
                                                </div>
                                            </div>
                                            <div style={{ textAlign: 'right', flexShrink: 0 }}>
                                                <div style={{ fontWeight: '900', color: sc }}>{bon.totalPoints} pct</div>
                                                <div style={{ fontSize: '0.72rem', color: sc, fontWeight: '800' }}>{sl}</div>
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                            {allBons.length === 0 && (
                                <div style={{ textAlign: 'center', color: '#94a3b8', padding: '50px', fontStyle: 'italic' }}>Niciun bon înregistrat.</div>
                            )}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default Magazin;
