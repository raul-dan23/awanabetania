import React, { useState, useEffect, useRef } from 'react';
import { toast } from 'sonner';
import { API_URL } from '../config';
import { useNfcBridge } from '../hooks/useNfcBridge';

/**
 * End-of-season fair store component.
 *
 *   - Bon Nou: a leader picks a child and uses a price calculator to add the
 *     line amounts the child is spending (free prices, no fixed catalogue).
 *     The receipt is sent to the cashiers in PENDING state. A child can come
 *     back several times — each visit is a separate bon.
 *   - Contabil: cashier view (one of up to 3 laptops, each with its own NFC
 *     reader + local bridge). Polls pending bons every 3 s, can filter by the
 *     tapped NFC card, and approves/rejects. Approval atomically deducts
 *     seasonPoints; the backend guards against double-approval by status.
 *   - Carduri (directors only): assign NFC cards to children, phone-first via
 *     continuous Web NFC scan, with USB bridge fallback. PIN-protected.
 *
 * @param {Object} props
 * @param {Object} props.user - Currently logged-in user; role controls tab visibility.
 */
const Magazin = ({ user }) => {
    const isDirector = user?.role === 'DIRECTOR' || user?.role === 'COORDONATOR';
    const [tab, setTab] = useState('bon');

    // Shop currency drops the last digit of a child's real points: 100000 → 10000.
    // Real points stay canonical (in the DB and in bons); we only divide for display,
    // and multiply a leader's typed shop price by 10 when storing a bon.
    const toShop = (v) => Math.floor((v || 0) / 10);

    // ── COPII ────────────────────────────────────────────────────
    const [children, setChildren] = useState([]);

    // ── BON NOU (calculator) ─────────────────────────────────────
    const [childSearch, setChildSearch] = useState('');
    const [selectedChild, setSelectedChild] = useState(null);
    const [calcAmount, setCalcAmount] = useState('');   // digits currently being typed
    const [calcLabel, setCalcLabel] = useState('');     // optional short label for the line
    const [bonLines, setBonLines] = useState([]);       // [{ name, pointPrice, qty }]

    // ── CONTABIL ─────────────────────────────────────────────────
    const [pendingBons, setPendingBons] = useState([]);
    const [allBons, setAllBons] = useState([]);
    const [contabilTab, setContabilTab] = useState('pending');
    const [nfcChild, setNfcChild] = useState(null);

    // ── CARDURI (asociere NFC, doar directori) ───────────────────
    const [editPinInput, setEditPinInput] = useState('');
    const [editPinVerified, setEditPinVerified] = useState(false);
    const [nfcScanActive, setNfcScanActive] = useState(false);
    const [detectedUid, setDetectedUid] = useState(null);
    const [cardSearch, setCardSearch] = useState('');   // search inside the "assign detected card" panel
    const [searchCard, setSearchCard] = useState('');   // search inside the full children list
    const ndefAbortRef = useRef(null);

    // The bridge / Web NFC callback is tab-aware: on the "Carduri" tab a tap
    // arms the UID for assignment; on "Contabil" it filters bons by child.
    const nfcBridgeConnected = useNfcBridge((uid) => {
        if (tab === 'carduri') {
            setDetectedUid(uid);
            const existing = children.find(c => c.nfcUid === uid);
            toast.success(existing ? `Card ${uid} (acum: ${existing.name})` : `Card nou: ${uid}`);
            return;
        }
        const found = children.find(c => c.nfcUid === uid);
        if (found) {
            setNfcChild(found);
            toast.success(`Card: ${found.name} ${found.surname}`);
        } else {
            toast.error('Card necunoscut sau neînregistrat.');
        }
    });

    useEffect(() => { fetchChildren(); }, []);

    useEffect(() => {
        if (tab === 'contabil') {
            fetchPendingBons();
            const iv = setInterval(fetchPendingBons, 3000);
            return () => clearInterval(iv);
        }
    }, [tab]);

    const fetchChildren = () =>
        fetch(`${API_URL}/children`)
            .then(r => r.json())
            .then(d => setChildren(d.sort((a, b) => a.name.localeCompare(b.name))))
            .catch(() => {});

    const fetchPendingBons = () =>
        fetch(`${API_URL}/bons/pending`).then(r => r.json()).then(setPendingBons).catch(() => {});

    const fetchAllBons = () =>
        fetch(`${API_URL}/bons/all`).then(r => r.json()).then(setAllBons).catch(() => {});

    // ── BON NOU: CALCULATOR ──────────────────────────────────────
    const pressDigit = (d) =>
        setCalcAmount(a => (a + d).replace(/^0+(?=\d)/, '').slice(0, 6));

    const backspace = () => setCalcAmount(a => a.slice(0, -1));

    const addLine = () => {
        const val = parseInt(calcAmount, 10);
        if (!val || val <= 0) { toast.error('Introdu o sumă.'); return; }
        setBonLines(l => [...l, { name: calcLabel.trim() || 'Cumpărături', pointPrice: val, qty: 1 }]);
        setCalcAmount('');
        setCalcLabel('');
    };

    const removeLine = (i) => setBonLines(l => l.filter((_, idx) => idx !== i));

    const bonTotal = bonLines.reduce((s, x) => s + x.pointPrice, 0)
        + (parseInt(calcAmount, 10) > 0 ? parseInt(calcAmount, 10) : 0);

    const resetBon = () => {
        setSelectedChild(null); setBonLines([]); setCalcAmount(''); setCalcLabel(''); setChildSearch('');
    };

    const createBon = () => {
        if (!selectedChild) { toast.error('Selectează un copil.'); return; }
        // Fold any typed-but-not-yet-added amount into the lines so it isn't lost
        let lines = bonLines;
        const typed = parseInt(calcAmount, 10);
        if (typed > 0) lines = [...bonLines, { name: calcLabel.trim() || 'Cumpărături', pointPrice: typed, qty: 1 }];
        if (lines.length === 0) { toast.error('Adaugă cel puțin o sumă.'); return; }
        // lines hold shop prices; store the bon in real points (×10) so the
        // backend deduction stays correct without any backend change.
        const realLines = lines.map(l => ({ ...l, pointPrice: l.pointPrice * 10 }));
        const realTotal = realLines.reduce((s, x) => s + x.pointPrice, 0);
        fetch(`${API_URL}/bons`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                childId: selectedChild.id,
                leaderName: user.name + ' ' + (user.surname || ''),
                items: JSON.stringify(realLines),
                totalPoints: realTotal
            })
        })
        .then(r => r.ok ? r.json() : Promise.reject())
        .then(() => { toast.success(`Bon trimis! Total: ${toShop(realTotal)} pct`); resetBon(); })
        .catch(() => toast.error('Eroare la creare bon.'));
    };

    // ── CONTABIL ─────────────────────────────────────────────────
    const [approvingAll, setApprovingAll] = useState(false);

    const approveBon = (id) => {
        // Optimistic removal so the bon disappears immediately on this station,
        // shrinking the window where another cashier could double-tap it.
        setPendingBons(b => b.filter(x => x.id !== id));
        fetch(`${API_URL}/bons/${id}/approve`, { method: 'POST' })
            .then(r => r.ok ? r.json() : r.text().then(t => Promise.reject(t)))
            .then(data => { toast.success(`Aprobat! Sold nou: ${toShop(data.remainingPoints)} pct`); fetchPendingBons(); fetchChildren(); })
            .catch(err => { toast.error(typeof err === 'string' ? err : 'Eroare la aprobare.'); fetchPendingBons(); });
    };

    /**
     * Approves every pending bon of the tapped child in one action.
     * Guards against the grand total exceeding the live balance, then approves
     * each bon sequentially against the existing per-bon endpoint (no backend
     * change needed). The balance is read from the freshest poll (childPoints).
     */
    const approveAllForChild = async (child, bons, balance) => {
        if (bons.length === 0) return;
        const total = bons.reduce((s, b) => s + b.totalPoints, 0);
        if (total > balance) {
            toast.error(`Total ${toShop(total)} pct depășește soldul (${toShop(balance)}). Nu se poate aproba.`);
            return;
        }
        setApprovingAll(true);
        const ids = bons.map(b => b.id);
        setPendingBons(b => b.filter(x => !ids.includes(x.id)));   // optimistic
        let ok = 0, lastBalance = balance;
        for (const b of bons) {
            try {
                const r = await fetch(`${API_URL}/bons/${b.id}/approve`, { method: 'POST' });
                if (r.ok) { const d = await r.json(); ok++; lastBalance = d.remainingPoints; }
            } catch { /* keep going; refetch reconciles below */ }
        }
        setApprovingAll(false);
        toast.success(`${ok}/${bons.length} bonuri aprobate · sold nou: ${toShop(lastBalance)} pct`);
        setNfcChild(null);
        fetchPendingBons();
        fetchChildren();
    };

    const rejectBon = (id) => {
        setPendingBons(b => b.filter(x => x.id !== id));
        fetch(`${API_URL}/bons/${id}/reject`, { method: 'POST' })
            .then(r => r.ok ? r.json() : Promise.reject())
            .then(() => { toast.success('Bon respins.'); fetchPendingBons(); })
            .catch(() => { toast.error('Eroare.'); fetchPendingBons(); });
    };

    // ── CARDURI ──────────────────────────────────────────────────
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

    /**
     * Starts a continuous Web NFC scan (Chrome on Android). Unlike a one-shot
     * read, the listener stays active so the director can tap many cards in a
     * row — each tap arms `detectedUid` to be assigned to a child.
     */
    const startCardScan = async () => {
        if (!('NDEFReader' in window)) {
            toast.error('Web NFC indisponibil. Condiții: Chrome 89+ pe Android, site pe HTTPS, Chrome deschis direct (nu din WhatsApp/Gmail). Pe laptop folosește cititorul USB.');
            return;
        }
        try {
            const ctrl = new AbortController();
            const ndef = new window.NDEFReader();
            await ndef.scan({ signal: ctrl.signal });
            ndefAbortRef.current = ctrl;
            ndef.onreading = ({ serialNumber }) => {
                const uid = (serialNumber || '').replace(/:/g, '').toUpperCase();
                if (!uid) return;
                setDetectedUid(uid);
                const existing = children.find(c => c.nfcUid === uid);
                toast.success(existing ? `Card ${uid} (acum: ${existing.name})` : `Card nou: ${uid}`);
            };
            setNfcScanActive(true);
        } catch (err) {
            toast.error('Eroare NFC: ' + (err.message || 'permisiune refuzată sau NFC indisponibil'));
        }
    };

    const stopCardScan = () => {
        ndefAbortRef.current?.abort();
        ndefAbortRef.current = null;
        setNfcScanActive(false);
    };

    // Abort any in-flight Web NFC scan on unmount
    useEffect(() => () => ndefAbortRef.current?.abort(), []);

    /**
     * Assigns the currently detected UID to a child via the admin endpoint,
     * then clears the armed UID so the next tap starts a fresh assignment.
     */
    const assignCard = (childId) => {
        if (!detectedUid) { toast.error('Atinge întâi un card.'); return; }
        fetch(`${API_URL}/admin/nfc-register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-Admin-Pin': editPinInput },
            body: JSON.stringify({ childId, uid: detectedUid })
        })
        .then(r => r.ok ? r.json() : r.text().then(t => Promise.reject(t)))
        .then(() => { toast.success('Card atribuit!'); setDetectedUid(null); setCardSearch(''); fetchChildren(); })
        .catch(err => toast.error(typeof err === 'string' ? err : 'Eroare la atribuire.'));
    };

    const removeCard = (childId) => {
        if (!window.confirm('Sigur elimini cardul acestui copil?')) return;
        fetch(`${API_URL}/admin/nfc-remove/${childId}`, { method: 'DELETE', headers: { 'X-Admin-Pin': editPinInput } })
        .then(r => r.ok ? r.json() : Promise.reject())
        .then(() => { toast.success('Card eliminat.'); fetchChildren(); })
        .catch(() => toast.error('Eroare.'));
    };

    // ── DERIVATE ─────────────────────────────────────────────────
    const filteredChildren = childSearch.length >= 2
        ? children.filter(c => `${c.name} ${c.surname}`.toLowerCase().includes(childSearch.toLowerCase()))
        : [];

    const cardChildren = cardSearch.length >= 1
        ? children.filter(c => `${c.name} ${c.surname}`.toLowerCase().includes(cardSearch.toLowerCase()))
        : [];

    const tabs = [
        { id: 'bon', label: 'Bon Nou' },
        { id: 'contabil', label: `Contabil${pendingBons.length > 0 ? ` (${pendingBons.length})` : ''}` },
        ...(isDirector ? [{ id: 'carduri', label: 'Carduri' }] : [])
    ];

    // Reusable keypad key style
    const keyStyle = {
        padding: '16px 0', border: '1px solid #e2e8f0', borderRadius: '12px',
        background: 'white', cursor: 'pointer', fontWeight: '800', fontSize: '1.4rem', color: '#1e293b'
    };

    return (
        <div style={{ maxWidth: '800px', margin: '0 auto', padding: '10px', paddingBottom: '120px' }}>

            {/* HERO */}
            <div className="db-hero" style={{ marginBottom: '24px' }}>
                <div className="db-hero-left">
                    <span className="db-greeting">Târg Awana</span>
                    <h1 className="db-name">Magazin</h1>
                    <span className="db-role-pill">{pendingBons.length} bonuri în așteptare</span>
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
                                    <div style={{ color: '#16a34a', fontWeight: '700', fontSize: '0.88rem', marginTop: '2px' }}>{toShop(selectedChild.seasonPoints)} puncte disponibile</div>
                                </div>
                                <button onClick={resetBon} style={{ padding: '8px 14px', border: '1px solid #86efac', borderRadius: '8px', background: 'white', cursor: 'pointer', fontWeight: '700', fontSize: '0.8rem', color: '#15803d' }}>
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
                                                <span style={{ fontWeight: '800', color: '#f59e0b', fontSize: '0.85rem', flexShrink: 0, marginLeft: '8px' }}>{toShop(c.seasonPoints)} pct</span>
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

                    {/* Pasul 2: Calculator */}
                    {selectedChild && (
                        <>
                            <div style={{ background: 'white', borderRadius: '16px', padding: '18px', marginBottom: '14px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #e2e8f0' }}>
                                <div style={{ fontWeight: '900', color: '#64748b', fontSize: '0.78rem', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: '12px' }}>2. Trece prețurile</div>

                                {/* Linii adăugate */}
                                {bonLines.length > 0 && (
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginBottom: '12px' }}>
                                        {bonLines.map((ln, i) => (
                                            <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '9px 12px', background: '#f8fafc', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
                                                <span style={{ fontWeight: '700', color: '#1e293b', fontSize: '0.9rem', minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{ln.name}</span>
                                                <span style={{ display: 'flex', alignItems: 'center', gap: '10px', flexShrink: 0, marginLeft: '8px' }}>
                                                    <span style={{ fontWeight: '800', color: '#f59e0b' }}>{ln.pointPrice} pct</span>
                                                    <button onClick={() => removeLine(i)} style={{ width: '26px', height: '26px', borderRadius: '7px', border: '1px solid #fecaca', background: '#fff5f5', color: '#dc2626', cursor: 'pointer', fontWeight: '900', lineHeight: 1 }}>✕</button>
                                                </span>
                                            </div>
                                        ))}
                                    </div>
                                )}

                                {/* Display sumă curentă */}
                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 18px', borderRadius: '12px', background: '#f4f7fe', border: '2px solid #e0e7ff', marginBottom: '10px' }}>
                                    <span style={{ color: '#94a3b8', fontWeight: '800', fontSize: '0.8rem', textTransform: 'uppercase' }}>Sumă</span>
                                    <span style={{ fontWeight: '900', fontSize: '2rem', color: '#4318ff', lineHeight: 1, fontFamily: 'monospace' }}>
                                        {calcAmount || '0'}<span style={{ fontSize: '0.9rem', marginLeft: '4px' }}>pct</span>
                                    </span>
                                </div>

                                {/* Etichetă opțională */}
                                <input
                                    className="login-input"
                                    placeholder="Etichetă (opțional, ex: jucărie)"
                                    value={calcLabel}
                                    onChange={e => setCalcLabel(e.target.value)}
                                    style={{ marginBottom: '10px' }}
                                />

                                {/* Keypad */}
                                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px' }}>
                                    {['1','2','3','4','5','6','7','8','9'].map(d => (
                                        <button key={d} onClick={() => pressDigit(d)} style={keyStyle}>{d}</button>
                                    ))}
                                    <button onClick={backspace} style={{ ...keyStyle, fontSize: '1.2rem', color: '#dc2626' }}>⌫</button>
                                    <button onClick={() => pressDigit('0')} style={keyStyle}>0</button>
                                    <button onClick={addLine} style={{ ...keyStyle, background: 'linear-gradient(135deg, #16a34a, #4ade80)', color: 'white', border: 'none', fontSize: '0.95rem' }}>+ Adaugă</button>
                                </div>
                            </div>

                            {/* Total + Trimite */}
                            {bonTotal > 0 && (
                                <div style={{
                                    position: 'sticky', bottom: '80px',
                                    background: 'white', borderRadius: '16px', padding: '16px 20px',
                                    boxShadow: '0 -4px 24px rgba(0,0,0,0.12)', border: '2px solid #4318ff',
                                    display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '12px'
                                }}>
                                    <div>
                                        <div style={{ color: '#94a3b8', fontWeight: '700', fontSize: '0.72rem', textTransform: 'uppercase' }}>Total</div>
                                        <div style={{ fontWeight: '900', fontSize: '1.6rem', color: '#4318ff', lineHeight: 1.1 }}>{bonTotal} <span style={{ fontSize: '0.9rem' }}>pct</span></div>
                                        {bonTotal > toShop(selectedChild.seasonPoints) && (
                                            <div style={{ color: '#dc2626', fontWeight: '700', fontSize: '0.75rem', marginTop: '2px' }}>
                                                Depășește soldul ({toShop(selectedChild.seasonPoints)} disponibile)
                                            </div>
                                        )}
                                    </div>
                                    <button onClick={createBon} style={{
                                        padding: '14px 22px', border: 'none', borderRadius: '12px',
                                        background: 'linear-gradient(135deg, #4318ff, #868cff)',
                                        color: 'white', fontWeight: '900', cursor: 'pointer', fontSize: '1rem', flexShrink: 0
                                    }}>Trimite la secretari →</button>
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

                    {/* Card copil identificat prin NFC + Aprobă tot */}
                    {nfcChild && contabilTab === 'pending' && (() => {
                        const childBons = pendingBons.filter(b => b.childId === nfcChild.id);
                        // Live balance from the freshest poll, falling back to the initial list
                        const balance = childBons.length ? (childBons[0].childPoints ?? nfcChild.seasonPoints ?? 0) : (nfcChild.seasonPoints ?? 0);
                        const total = childBons.reduce((s, b) => s + b.totalPoints, 0);
                        const overBalance = total > balance;
                        return (
                            <div style={{ padding: '16px 18px', borderRadius: '14px', background: 'linear-gradient(135deg, #f0fdf4, #dcfce7)', border: '2px solid #86efac', marginBottom: '4px' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '10px' }}>
                                    <div>
                                        <div style={{ fontWeight: '900', color: '#15803d', fontSize: '1.15rem' }}>{nfcChild.name} {nfcChild.surname}</div>
                                        <div style={{ fontSize: '0.82rem', color: '#16a34a', fontWeight: '700', marginTop: '2px' }}>{toShop(balance)} puncte disponibile</div>
                                    </div>
                                    <div style={{ textAlign: 'right', flexShrink: 0 }}>
                                        <div style={{ fontWeight: '900', color: '#15803d', fontSize: '1.5rem', lineHeight: 1 }}>{toShop(total)}</div>
                                        <div style={{ fontSize: '0.68rem', color: '#16a34a', fontWeight: '800' }}>TOTAL DE PLATĂ</div>
                                    </div>
                                </div>
                                {childBons.length > 0 ? (
                                    <>
                                        {overBalance && (
                                            <div style={{ marginTop: '8px', color: '#dc2626', fontWeight: '800', fontSize: '0.8rem' }}>
                                                Totalul depășește soldul — nu se poate aproba tot.
                                            </div>
                                        )}
                                        <button
                                            onClick={() => approveAllForChild(nfcChild, childBons, balance)}
                                            disabled={approvingAll || overBalance}
                                            style={{
                                                width: '100%', marginTop: '12px', padding: '15px', border: 'none', borderRadius: '12px',
                                                background: (approvingAll || overBalance) ? '#cbd5e1' : 'linear-gradient(135deg, #16a34a, #4ade80)',
                                                color: 'white', fontWeight: '900', cursor: (approvingAll || overBalance) ? 'not-allowed' : 'pointer', fontSize: '1.05rem'
                                            }}>
                                            {approvingAll ? 'Se aprobă...' : `Aprobă tot (${childBons.length} ${childBons.length === 1 ? 'bon' : 'bonuri'}) → −${toShop(total)} pct`}
                                        </button>
                                        <div style={{ textAlign: 'center', marginTop: '6px', fontSize: '0.78rem', color: '#16a34a', fontWeight: '700' }}>
                                            Sold după aprobare: {overBalance ? '—' : toShop(balance - total)} pct
                                        </div>
                                    </>
                                ) : (
                                    <div style={{ marginTop: '8px', fontSize: '0.82rem', color: '#16a34a', fontWeight: '700' }}>
                                        Niciun bon în așteptare pentru acest copil.
                                    </div>
                                )}
                            </div>
                        );
                    })()}

                    {/* Bonuri în așteptare */}
                    {contabilTab === 'pending' && (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                            {pendingBons.filter(b => !nfcChild || b.childId === nfcChild.id).length === 0 && pendingBons.length === 0 && (
                                <div style={{ textAlign: 'center', padding: '60px 20px', color: '#94a3b8' }}>
                                    <div style={{ fontSize: '2.5rem', marginBottom: '8px', color: '#86efac' }}>—</div>
                                    <div style={{ fontWeight: '700' }}>Niciun bon în așteptare.</div>
                                    <div style={{ fontSize: '0.85rem', marginTop: '4px' }}>Se actualizează automat.</div>
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
                                                <div style={{ fontWeight: '900', color: '#f59e0b', fontSize: '1.4rem', lineHeight: 1 }}>{toShop(bon.totalPoints)}</div>
                                                <div style={{ fontSize: '0.7rem', color: '#94a3b8', fontWeight: '700' }}>PUNCTE</div>
                                            </div>
                                        </div>
                                        <div style={{ padding: '12px 18px', borderBottom: '1px solid #f1f5f9' }}>
                                            {items.map((item, i) => (
                                                <div key={i} style={{ display: 'flex', justifyContent: 'space-between', padding: '3px 0', fontSize: '0.9rem' }}>
                                                    <span style={{ fontWeight: '600', color: '#1e293b' }}>{item.qty > 1 ? `${item.qty}× ` : ''}{item.name}</span>
                                                    <span style={{ fontWeight: '800', color: '#f59e0b' }}>{toShop(item.pointPrice * (item.qty || 1))} pct</span>
                                                </div>
                                            ))}
                                        </div>
                                        <div style={{ padding: '12px 18px', display: 'flex', gap: '8px' }}>
                                            <button onClick={() => approveBon(bon.id)} style={{ flex: 1, padding: '13px', border: 'none', borderRadius: '10px', background: 'linear-gradient(135deg, #16a34a, #4ade80)', color: 'white', fontWeight: '900', cursor: 'pointer', fontSize: '1rem' }}>
                                                Aprobă
                                            </button>
                                            <button onClick={() => rejectBon(bon.id)} style={{ padding: '13px 20px', border: '1px solid #fecaca', borderRadius: '10px', background: 'white', cursor: 'pointer', fontWeight: '800', fontSize: '0.9rem', color: '#dc2626' }}>
                                                Respinge
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
                                                    {items.map(i => `${i.qty > 1 ? `${i.qty}× ` : ''}${i.name} (${toShop(i.pointPrice * (i.qty || 1))})`).join(', ')}
                                                </div>
                                                <div style={{ fontSize: '0.75rem', color: '#94a3b8', marginTop: '2px' }}>
                                                    {bon.leaderName} · {new Date(bon.createdAt).toLocaleString('ro-RO', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })}
                                                </div>
                                            </div>
                                            <div style={{ textAlign: 'right', flexShrink: 0 }}>
                                                <div style={{ fontWeight: '900', color: sc }}>{toShop(bon.totalPoints)} pct</div>
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

            {/* ════════════ TAB: CARDURI ════════════ */}
            {tab === 'carduri' && isDirector && (
                <div>
                    {!editPinVerified ? (
                        <div style={{ background: 'white', borderRadius: '16px', padding: '20px', marginBottom: '16px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #e2e8f0' }}>
                            <p style={{ margin: '0 0 10px', color: '#64748b', fontSize: '0.85rem', fontWeight: '600' }}>Introdu PIN-ul admin pentru a gestiona cardurile:</p>
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
                        <>
                            {/* Sursă scanare: Web NFC (telefon) + status bridge USB */}
                            <div style={{ background: 'white', borderRadius: '16px', padding: '16px 18px', marginBottom: '12px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1px solid #e2e8f0' }}>
                                <div style={{ fontWeight: '900', color: '#64748b', fontSize: '0.78rem', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: '10px' }}>Scanare carduri</div>
                                <button onClick={nfcScanActive ? stopCardScan : startCardScan} style={{
                                    width: '100%', padding: '13px', border: 'none', borderRadius: '12px', marginBottom: '10px',
                                    background: nfcScanActive ? 'linear-gradient(135deg, #dc2626, #f87171)' : 'linear-gradient(135deg, #7c3aed, #a78bfa)',
                                    color: 'white', fontWeight: '800', cursor: 'pointer', fontSize: '0.95rem'
                                }}>
                                    {nfcScanActive ? '■ Oprește scanarea (telefon)' : '📱 Pornește scanarea cu telefonul'}
                                </button>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.78rem', color: '#64748b' }}>
                                    <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: (nfcScanActive || nfcBridgeConnected) ? '#16a34a' : '#94a3b8', flexShrink: 0 }} />
                                    <span style={{ fontWeight: '700' }}>
                                        {nfcScanActive ? 'Telefon: atinge cardurile pe rând' : nfcBridgeConnected ? 'Cititor USB conectat — pune cardul pe cititor' : 'Pornește scanarea sau conectează cititorul USB'}
                                    </span>
                                </div>
                            </div>

                            {/* Card detectat → alege copilul */}
                            {detectedUid && (
                                <div style={{ background: 'linear-gradient(135deg, #faf5ff, #f3e8ff)', borderRadius: '16px', padding: '16px 18px', marginBottom: '12px', border: '2px solid #d8b4fe' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                                        <div>
                                            <div style={{ fontSize: '0.72rem', color: '#7c3aed', fontWeight: '800', textTransform: 'uppercase' }}>Card detectat</div>
                                            <div style={{ fontFamily: 'monospace', fontWeight: '900', color: '#6d28d9', fontSize: '1.3rem', letterSpacing: '0.05em' }}>{detectedUid}</div>
                                            {(() => {
                                                const cur = children.find(c => c.nfcUid === detectedUid);
                                                return cur ? <div style={{ fontSize: '0.8rem', color: '#9333ea', fontWeight: '700', marginTop: '2px' }}>Atribuit acum: {cur.name} {cur.surname}</div> : null;
                                            })()}
                                        </div>
                                        <button onClick={() => { setDetectedUid(null); setCardSearch(''); }} style={{ padding: '7px 12px', border: '1px solid #d8b4fe', borderRadius: '8px', background: 'white', cursor: 'pointer', fontWeight: '700', fontSize: '0.8rem', color: '#7c3aed' }}>
                                            Anulează
                                        </button>
                                    </div>
                                    <input
                                        className="login-input"
                                        placeholder="Caută copilul pentru atribuire..."
                                        value={cardSearch}
                                        onChange={e => setCardSearch(e.target.value)}
                                        autoFocus
                                        style={{ marginBottom: cardChildren.length > 0 ? '8px' : 0 }}
                                    />
                                    {cardChildren.length > 0 && (
                                        <div style={{ border: '1px solid #e9d5ff', borderRadius: '12px', overflow: 'hidden', maxHeight: '260px', overflowY: 'auto', background: 'white' }}>
                                            {cardChildren.slice(0, 30).map((c, i) => (
                                                <button key={c.id} onClick={() => assignCard(c.id)} style={{
                                                    width: '100%', padding: '12px 16px', border: 'none',
                                                    borderBottom: i < Math.min(cardChildren.length, 30) - 1 ? '1px solid #f5f3ff' : 'none',
                                                    background: 'white', cursor: 'pointer', textAlign: 'left',
                                                    display: 'flex', justifyContent: 'space-between', alignItems: 'center'
                                                }}>
                                                    <span style={{ fontWeight: '700', color: '#1e293b' }}>{c.name} {c.surname}</span>
                                                    {c.nfcUid
                                                        ? <span style={{ fontSize: '0.72rem', color: '#7c3aed', fontWeight: '800', fontFamily: 'monospace', flexShrink: 0, marginLeft: '8px' }}>are card</span>
                                                        : <span style={{ fontSize: '0.72rem', color: '#94a3b8', fontWeight: '700', flexShrink: 0, marginLeft: '8px' }}>fără card</span>}
                                                </button>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            )}

                            {/* Listă copii + status card */}
                            <input
                                className="login-input"
                                placeholder="Caută copil în listă..."
                                value={searchCard}
                                onChange={e => setSearchCard(e.target.value)}
                                style={{ marginBottom: '10px' }}
                            />
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                {children
                                    .filter(c => !searchCard || `${c.name} ${c.surname}`.toLowerCase().includes(searchCard.toLowerCase()))
                                    .sort((a, b) => (a.nfcUid ? 1 : 0) - (b.nfcUid ? 1 : 0) || a.name.localeCompare(b.name))
                                    .map(c => (
                                    <div key={c.id} style={{ background: 'white', borderRadius: '14px', padding: '12px 16px', boxShadow: '0 2px 6px rgba(0,0,0,0.04)', border: '1px solid #e2e8f0', borderLeft: `4px solid ${c.nfcUid ? '#7c3aed' : '#e2e8f0'}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '10px' }}>
                                        <div style={{ flex: 1, minWidth: 0 }}>
                                            <div style={{ fontWeight: '900', color: '#1e293b' }}>{c.name} {c.surname}</div>
                                            <div style={{ fontSize: '0.78rem', marginTop: '2px' }}>
                                                {c.nfcUid
                                                    ? <span style={{ fontFamily: 'monospace', color: '#7c3aed', fontWeight: '800' }}>{c.nfcUid}</span>
                                                    : <span style={{ color: '#94a3b8' }}>Fără card</span>}
                                                {' · '}<span style={{ color: '#f59e0b', fontWeight: '700' }}>{toShop(c.seasonPoints)} pct</span>
                                            </div>
                                        </div>
                                        {c.nfcUid && (
                                            <button onClick={() => removeCard(c.id)} style={{ padding: '6px 11px', border: '1px solid #fecaca', borderRadius: '8px', background: '#fff5f5', cursor: 'pointer', fontWeight: '700', fontSize: '0.78rem', color: '#dc2626', flexShrink: 0 }}>
                                                Elimină
                                            </button>
                                        )}
                                    </div>
                                ))}
                            </div>
                        </>
                    )}
                </div>
            )}
        </div>
    );
};

export default Magazin;
