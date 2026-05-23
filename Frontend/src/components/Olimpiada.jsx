import React, { useState, useEffect, useCallback } from 'react';
import { toast } from 'sonner';
import { API_URL } from '../config';

const TEAMS = ['ROSU', 'GALBEN', 'ALBASTRU', 'VERDE'];

const TEAM_COLORS = {
    ROSU:     { bg: '#fde8e8', border: '#e53e3e', text: '#c53030', dot: '#e53e3e' },
    GALBEN:   { bg: '#fefce8', border: '#d69e2e', text: '#92620a', dot: '#d69e2e' },
    ALBASTRU: { bg: '#ebf8ff', border: '#3182ce', text: '#1a4c8c', dot: '#3182ce' },
    VERDE:    { bg: '#f0fff4', border: '#38a169', text: '#22623e', dot: '#38a169' },
};

const PLACE_POINTS = { 1: 1000, 2: 500, 3: 300, 4: 100 };

function ScoringTab({ arbiterName, sessionCode }) {
    const [placements, setPlacements] = useState({});
    const [round, setRound] = useState(1);
    const [submitting, setSubmitting] = useState(false);
    const [history, setHistory] = useState([]);

    const allAssigned = TEAMS.every(t => placements[t] != null);

    const handleTeamPress = (team) => {
        setPlacements(prev => {
            const next = { ...prev };
            if (next[team] != null) {
                delete next[team];
            } else {
                const taken = new Set(Object.values(next));
                for (let p = 1; p <= 4; p++) {
                    if (!taken.has(p)) { next[team] = p; break; }
                }
            }
            return next;
        });
    };

    const submitRound = async () => {
        if (!allAssigned) return;
        setSubmitting(true);
        const scores = TEAMS.map(team => ({ team, place: placements[team] }));
        try {
            const r = await fetch(`${API_URL}/olimpiada/session/${sessionCode}/score`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ arbiterName, roundNumber: round, scores }),
            });
            if (!r.ok) { toast.error(await r.text()); return; }
            toast.success(`Runda ${round} salvata!`);
            setHistory(h => [...h, { round, placements: { ...placements } }]);
            setRound(r2 => r2 + 1);
            setPlacements({});
        } finally {
            setSubmitting(false);
        }
    };

    const teamLabel = t => t.charAt(0) + t.slice(1).toLowerCase();

    return (
        <div className="animate-in">
            {/* Header runda */}
            <div className="card" style={{ marginBottom: 16, borderLeft: '4px solid var(--accent)', padding: '16px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div>
                    <div style={{ fontWeight: 800, fontSize: '1.1rem', color: 'var(--text-primary)' }}>Runda {round}</div>
                    <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', marginTop: 2 }}>Arbitru: {arbiterName}</div>
                </div>
                <div style={{ textAlign: 'right', color: 'var(--accent)', fontWeight: 900, fontSize: '1.8rem', lineHeight: 1 }}>
                    {Object.keys(placements).length}<span style={{ fontSize: '1rem' }}>/4</span>
                    <div style={{ fontSize: '0.7rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>echipe</div>
                </div>
            </div>

            {/* Grid 2x2 echipe — exact ca ScoringWidget */}
            <div className="card" style={{ marginBottom: 16 }}>
                <p className="db-section-title" style={{ marginBottom: 14 }}>
                    Apasa echipele in ordinea locurilor (primul apasat = Loc 1)
                </p>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                    {TEAMS.map(team => {
                        const c = TEAM_COLORS[team];
                        const place = placements[team];
                        const isActive = place != null;
                        return (
                            <div
                                key={team}
                                onClick={() => handleTeamPress(team)}
                                style={{
                                    padding: '14px 16px', borderRadius: 14, cursor: 'pointer',
                                    border: `2px solid ${isActive ? c.border : 'var(--border-color)'}`,
                                    background: isActive ? c.bg : 'white',
                                    transition: 'all 0.15s',
                                    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                }}
                            >
                                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                                    <div style={{ width: 10, height: 10, borderRadius: '50%', background: c.dot, flexShrink: 0 }} />
                                    <span style={{ fontWeight: 800, fontSize: '0.95rem', color: isActive ? c.text : 'var(--text-primary)' }}>
                                        {teamLabel(team)}
                                    </span>
                                </div>
                                <span style={{ fontSize: '0.85rem', fontWeight: 700, color: isActive ? c.text : 'var(--text-secondary)', opacity: isActive ? 1 : 0.4 }}>
                                    {isActive ? `Loc ${place}` : '—'}
                                </span>
                            </div>
                        );
                    })}
                </div>

                {/* Rezumat puncte */}
                {Object.keys(placements).length > 0 && (
                    <div style={{ marginTop: 14, display: 'flex', flexDirection: 'column', gap: 4 }}>
                        {TEAMS.filter(t => placements[t] != null)
                            .sort((a, b) => placements[a] - placements[b])
                            .map(team => {
                                const c = TEAM_COLORS[team];
                                return (
                                    <div key={team} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.83rem', color: 'var(--text-secondary)', padding: '2px 4px' }}>
                                        <span style={{ color: c.text, fontWeight: 600 }}>Loc {placements[team]} — Echipa {teamLabel(team)}</span>
                                        <span style={{ fontWeight: 700, color: 'var(--text-primary)' }}>+{PLACE_POINTS[placements[team]].toLocaleString()} pct</span>
                                    </div>
                                );
                            })}
                    </div>
                )}
            </div>

            {/* Buton salveaza */}
            <button
                onClick={submitRound}
                disabled={!allAssigned || submitting}
                style={{
                    width: '100%', padding: 16, background: allAssigned ? '#15803d' : '#d1d5db',
                    color: allAssigned ? 'white' : '#9ca3af', border: 'none', borderRadius: 14,
                    fontWeight: 800, fontSize: '1rem', cursor: allAssigned ? 'pointer' : 'not-allowed',
                    boxShadow: allAssigned ? '0 4px 14px rgba(21,128,61,0.3)' : 'none',
                    marginBottom: 16, transition: 'all 0.15s',
                }}>
                {submitting ? 'Se salveaza...' : `Salveaza Runda ${round} — ${allAssigned ? TEAMS.reduce((s, t) => s + PLACE_POINTS[placements[t]], 0).toLocaleString() + ' pct' : '?'}`}
            </button>

            {/* Istoric */}
            {history.length > 0 && (
                <div className="card">
                    <p className="db-section-title" style={{ marginBottom: 10 }}>Runde trimise</p>
                    {history.map(h => (
                        <div key={h.round} style={{
                            padding: '10px 0', borderBottom: '1px solid var(--border-color)',
                            display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap',
                            fontSize: '0.83rem'
                        }}>
                            <span style={{ fontWeight: 800, color: 'var(--text-primary)', minWidth: 65 }}>Runda {h.round}</span>
                            {TEAMS.sort((a, b) => h.placements[a] - h.placements[b]).map(t => (
                                <span key={t} style={{
                                    background: TEAM_COLORS[t].bg, border: `1px solid ${TEAM_COLORS[t].border}`,
                                    borderRadius: 8, padding: '2px 10px',
                                    color: TEAM_COLORS[t].text, fontWeight: 600
                                }}>
                                    {teamLabel(t)} — loc {h.placements[t]}
                                </span>
                            ))}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

function CompareTab({ sessionCode }) {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(false);

    const load = useCallback(async () => {
        if (!sessionCode) return;
        setLoading(true);
        try {
            const r = await fetch(`${API_URL}/olimpiada/session/${sessionCode}/compare`);
            if (r.ok) setData(await r.json());
            else toast.error('Eroare la incarcare');
        } finally {
            setLoading(false);
        }
    }, [sessionCode]);

    useEffect(() => { load(); }, [load]);

    if (loading) return <div style={{ color: '#6b7280', padding: 20, textAlign: 'center' }}>Se incarca...</div>;
    if (!data) return <div style={{ color: '#6b7280', padding: 20, textAlign: 'center' }}>Alege o sesiune pentru comparatie.</div>;

    const { arbiters, totals, roundCounts, teams } = data;

    if (arbiters.length === 0) {
        return <div style={{ color: '#6b7280', padding: 20, textAlign: 'center' }}>Niciun arbitru nu a trimis scoruri inca.</div>;
    }

    const teamLabel = t => t.charAt(0) + t.slice(1).toLowerCase();

    // Clasament final (suma totals per team)
    const finalTotals = teams.map(team => {
        const total = arbiters.reduce((sum, a) => sum + (totals[team]?.[a] || 0), 0);
        return { team, total };
    }).sort((a, b) => b.total - a.total);

    return (
        <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
                <div style={{ fontWeight: 700, fontSize: '1.05rem' }}>{data.sessionName}</div>
                <button onClick={load} style={{
                    background: '#f3f4f6', border: '1px solid #e5e7eb',
                    borderRadius: 8, padding: '5px 12px', cursor: 'pointer',
                    fontSize: '0.82rem', color: '#374151'
                }}>
                    Actualizeaza
                </button>
            </div>

            {/* Tabel punctaje */}
            <div style={{ overflowX: 'auto', marginBottom: 20 }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.88rem' }}>
                    <thead>
                        <tr style={{ background: '#f9fafb' }}>
                            <th style={{ padding: '8px 12px', textAlign: 'left', border: '1px solid #e5e7eb', fontWeight: 600 }}>Echipa</th>
                            {arbiters.map(a => (
                                <th key={a} style={{ padding: '8px 12px', textAlign: 'center', border: '1px solid #e5e7eb', fontWeight: 600 }}>
                                    {a}
                                    <div style={{ fontWeight: 400, fontSize: '0.75rem', color: '#6b7280' }}>
                                        {roundCounts[a] || 0} runde
                                    </div>
                                </th>
                            ))}
                            {arbiters.length >= 2 && (
                                <th style={{ padding: '8px 12px', textAlign: 'center', border: '1px solid #e5e7eb', fontWeight: 600, color: '#6b7280' }}>
                                    Diferenta
                                </th>
                            )}
                        </tr>
                    </thead>
                    <tbody>
                        {teams.map(team => {
                            const c = TEAM_COLORS[team];
                            const arbiterValues = arbiters.map(a => totals[team]?.[a] || 0);
                            const diff = arbiters.length >= 2
                                ? Math.abs(arbiterValues[0] - arbiterValues[1])
                                : null;
                            const hasDiff = diff !== null && diff > 0;
                            return (
                                <tr key={team}>
                                    <td style={{ padding: '8px 12px', border: '1px solid #e5e7eb' }}>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                                            <div style={{ width: 10, height: 10, borderRadius: '50%', background: c.dot }} />
                                            <span style={{ fontWeight: 600, color: c.text }}>Echipa {teamLabel(team)}</span>
                                        </div>
                                    </td>
                                    {arbiters.map(a => (
                                        <td key={a} style={{ padding: '8px 12px', textAlign: 'center', border: '1px solid #e5e7eb', fontWeight: 600 }}>
                                            {totals[team]?.[a] || 0}
                                        </td>
                                    ))}
                                    {diff !== null && (
                                        <td style={{
                                            padding: '8px 12px', textAlign: 'center',
                                            border: '1px solid #e5e7eb',
                                            fontWeight: hasDiff ? 700 : 400,
                                            color: hasDiff ? '#dc2626' : '#9ca3af',
                                            background: hasDiff ? '#fef2f2' : 'transparent'
                                        }}>
                                            {hasDiff ? `+${diff}` : '—'}
                                        </td>
                                    )}
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>

            {/* Clasament final */}
            <div style={{ fontWeight: 600, fontSize: '0.9rem', color: '#374151', marginBottom: 10 }}>
                Clasament Final (suma arbitri)
            </div>
            {finalTotals.map((item, idx) => {
                const c = TEAM_COLORS[item.team];
                const medals = ['I', 'II', 'III', 'IV'];
                return (
                    <div key={item.team} style={{
                        display: 'flex', alignItems: 'center', gap: 12,
                        padding: '10px 14px', borderRadius: 10, marginBottom: 8,
                        background: c.bg, border: `1.5px solid ${c.border}`,
                    }}>
                        <div style={{
                            width: 28, height: 28, borderRadius: '50%',
                            background: idx === 0 ? '#f59e0b' : idx === 1 ? '#9ca3af' : idx === 2 ? '#b45309' : '#e5e7eb',
                            color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center',
                            fontWeight: 700, fontSize: '0.75rem', flexShrink: 0,
                        }}>
                            {medals[idx]}
                        </div>
                        <div style={{ width: 10, height: 10, borderRadius: '50%', background: c.dot, flexShrink: 0 }} />
                        <span style={{ fontWeight: 600, color: c.text, flex: 1 }}>
                            Echipa {item.team.charAt(0) + item.team.slice(1).toLowerCase()}
                        </span>
                        <span style={{ fontWeight: 700, fontSize: '1.05rem', color: c.text }}>
                            {item.total} pct
                        </span>
                    </div>
                );
            })}
        </div>
    );
}

function SessionsTab({ adminPin }) {
    const [sessions, setSessions] = useState([]);
    const [form, setForm] = useState({ name: '', code: '' });
    const [saving, setSaving] = useState(false);
    const [loading, setLoading] = useState(false);

    const load = async () => {
        setLoading(true);
        try {
            const r = await fetch(`${API_URL}/olimpiada/sessions`, {
                headers: { 'X-Admin-Pin': adminPin }
            });
            if (r.ok) setSessions(await r.json());
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { if (adminPin) load(); }, [adminPin]);

    const create = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            const r = await fetch(`${API_URL}/olimpiada/sessions`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-Admin-Pin': adminPin },
                body: JSON.stringify(form),
            });
            if (r.ok) {
                toast.success('Sesiune creata!');
                setForm({ name: '', code: '' });
                load();
            } else {
                toast.error(await r.text());
            }
        } finally {
            setSaving(false);
        }
    };

    const closeSession = async (id) => {
        const r = await fetch(`${API_URL}/olimpiada/sessions/${id}/close`, {
            method: 'POST',
            headers: { 'X-Admin-Pin': adminPin }
        });
        if (r.ok) { toast.success('Sesiune inchisa'); load(); }
    };

    return (
        <div>
            <form onSubmit={create} style={{ background: '#f9fafb', border: '1px solid #e5e7eb', borderRadius: 10, padding: 16, marginBottom: 20 }}>
                <div style={{ fontWeight: 600, marginBottom: 12, fontSize: '0.9rem' }}>Sesiune noua</div>
                <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                    <input
                        placeholder="Nume (ex: Olimpiada 2026)"
                        value={form.name}
                        onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                        required
                        style={{ flex: 2, minWidth: 160, padding: '8px 12px', borderRadius: 8, border: '1px solid #d1d5db', fontSize: '0.9rem' }}
                    />
                    <input
                        placeholder="Cod (ex: OLM26)"
                        value={form.code}
                        onChange={e => setForm(f => ({ ...f, code: e.target.value.toUpperCase() }))}
                        required
                        maxLength={10}
                        style={{ flex: 1, minWidth: 100, padding: '8px 12px', borderRadius: 8, border: '1px solid #d1d5db', fontSize: '0.9rem' }}
                    />
                    <button type="submit" disabled={saving} style={{
                        padding: '8px 18px', background: '#4f46e5', color: '#fff',
                        border: 'none', borderRadius: 8, fontWeight: 600, cursor: 'pointer', fontSize: '0.9rem'
                    }}>
                        {saving ? '...' : 'Creeaza'}
                    </button>
                </div>
            </form>

            {loading ? (
                <div style={{ color: '#6b7280', textAlign: 'center', padding: 20 }}>Se incarca...</div>
            ) : sessions.length === 0 ? (
                <div style={{ color: '#6b7280', textAlign: 'center', padding: 20 }}>Nicio sesiune creata inca.</div>
            ) : (
                sessions.map(s => (
                    <div key={s.id} style={{
                        display: 'flex', alignItems: 'center', gap: 12,
                        padding: '12px 14px', border: '1px solid #e5e7eb',
                        borderRadius: 10, marginBottom: 8, background: '#fff'
                    }}>
                        <div style={{ flex: 1 }}>
                            <div style={{ fontWeight: 600, fontSize: '0.95rem' }}>{s.name}</div>
                            <div style={{ fontSize: '0.8rem', color: '#6b7280', marginTop: 2 }}>
                                Cod: <strong>{s.code}</strong>
                            </div>
                        </div>
                        <span style={{
                            fontSize: '0.75rem', fontWeight: 600, padding: '3px 10px', borderRadius: 20,
                            background: s.status === 'ACTIVE' ? '#dcfce7' : '#f3f4f6',
                            color: s.status === 'ACTIVE' ? '#15803d' : '#6b7280',
                        }}>
                            {s.status}
                        </span>
                        {s.status === 'ACTIVE' && (
                            <button onClick={() => closeSession(s.id)} style={{
                                padding: '5px 12px', background: '#fef2f2', color: '#dc2626',
                                border: '1px solid #fecaca', borderRadius: 8, cursor: 'pointer', fontSize: '0.8rem'
                            }}>
                                Inchide
                            </button>
                        )}
                    </div>
                ))
            )}
        </div>
    );
}

export default function Olimpiada({ user, guestArbiter, onExitGuest }) {
    const isDirector = user && (user.role === 'DIRECTOR' || user.role === 'COORDONATOR');
    const isGuest = !!guestArbiter;

    const [tab, setTab] = useState(isGuest ? 'scorare' : 'sesiuni');
    const [adminPin, setAdminPin] = useState('');
    const [pinUnlocked, setPinUnlocked] = useState(false);
    const [pinInput, setPinInput] = useState('');
    const [pinErr, setPinErr] = useState('');

    // Sesiune selectata pentru scorare
    const [codeInput, setCodeInput] = useState(guestArbiter?.code || '');
    const [activeSession, setActiveSession] = useState(null);
    const [sessionErr, setSessionErr] = useState('');
    const [loadingSession, setLoadingSession] = useState(false);

    // Arbiter name
    const arbiterName = isGuest ? guestArbiter.name : (user?.name + (user?.surname ? ' ' + user.surname : ''));

    const joinSession = async () => {
        if (!codeInput.trim()) return;
        setLoadingSession(true);
        setSessionErr('');
        try {
            const r = await fetch(`${API_URL}/olimpiada/session/${codeInput.trim().toUpperCase()}`);
            if (r.ok) {
                const s = await r.json();
                if (s.status === 'CLOSED') { setSessionErr('Aceasta sesiune este inchisa.'); return; }
                setActiveSession(s);
                if (!isGuest) setTab('scorare');
            } else {
                setSessionErr('Codul nu a fost gasit.');
            }
        } finally {
            setLoadingSession(false);
        }
    };

    const verifyPin = async (e) => {
        e.preventDefault();
        const r = await fetch(`${API_URL}/admin/verify-pin`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ pin: pinInput })
        });
        if (r.ok) {
            setAdminPin(pinInput);
            setPinUnlocked(true);
            setPinErr('');
        } else {
            setPinErr('PIN incorect');
        }
    };

    const tabs = [
        ...(!isGuest ? [{ id: 'sesiuni', label: 'Sesiuni', color: '#0284c7' }] : []),
        { id: 'scorare', label: 'Scorare', color: '#16a34a' },
        { id: 'comparatie', label: 'Comparatie', color: '#7c3aed' },
    ];

    return (
        <div style={{ maxWidth: 640, margin: '0 auto', padding: '20px 16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
                <div>
                    <h2 style={{ margin: 0, fontSize: '1.3rem', fontWeight: 700 }}>Olimpiada Awana</h2>
                    {isGuest && (
                        <div style={{ fontSize: '0.82rem', color: '#6b7280', marginTop: 3 }}>
                            Arbitru invitat: <strong>{guestArbiter.name}</strong>
                        </div>
                    )}
                </div>
                {isGuest && (
                    <button onClick={onExitGuest} style={{
                        background: '#f3f4f6', border: '1px solid #e5e7eb',
                        borderRadius: 8, padding: '5px 12px', cursor: 'pointer',
                        fontSize: '0.82rem', color: '#374151'
                    }}>
                        Iesi
                    </button>
                )}
            </div>

            {/* Tabs */}
            <div style={{ display: 'flex', gap: 8, marginBottom: 20, background: '#f4f7fe', padding: 4, borderRadius: 14 }}>
                {tabs.map(t => (
                    <button key={t.id} onClick={() => setTab(t.id)} style={{
                        flex: 1, padding: '10px', border: 'none', cursor: 'pointer', borderRadius: 10,
                        fontWeight: 800, fontSize: '0.85rem', transition: 'all 0.18s',
                        background: tab === t.id ? 'white' : 'transparent',
                        color: tab === t.id ? t.color : '#64748b',
                        boxShadow: tab === t.id ? '0 2px 8px rgba(0,0,0,0.08)' : 'none',
                    }}>
                        {t.label}
                    </button>
                ))}
            </div>

            {/* Sesiuni tab — necesita PIN */}
            {tab === 'sesiuni' && !isGuest && (
                <div>
                    {!pinUnlocked ? (
                        <form onSubmit={verifyPin} style={{ maxWidth: 320 }}>
                            <div style={{ fontWeight: 600, marginBottom: 10, fontSize: '0.9rem' }}>Acces admin</div>
                            <div style={{ display: 'flex', gap: 8 }}>
                                <input
                                    type="password"
                                    placeholder="PIN admin"
                                    value={pinInput}
                                    onChange={e => setPinInput(e.target.value)}
                                    style={{ flex: 1, padding: '8px 12px', borderRadius: 8, border: '1px solid #d1d5db', fontSize: '0.9rem' }}
                                />
                                <button type="submit" style={{
                                    padding: '8px 16px', background: '#4f46e5', color: '#fff',
                                    border: 'none', borderRadius: 8, fontWeight: 600, cursor: 'pointer'
                                }}>
                                    Intra
                                </button>
                            </div>
                            {pinErr && <div style={{ color: '#dc2626', fontSize: '0.82rem', marginTop: 6 }}>{pinErr}</div>}
                        </form>
                    ) : (
                        <SessionsTab adminPin={adminPin} />
                    )}
                </div>
            )}

            {/* Scorare tab */}
            {tab === 'scorare' && (
                <div>
                    {!activeSession ? (
                        <div>
                            <div style={{ fontWeight: 600, marginBottom: 10, fontSize: '0.9rem' }}>
                                {isGuest ? 'Sesiune' : 'Alege sesiunea'}
                            </div>
                            <div style={{ display: 'flex', gap: 8 }}>
                                <input
                                    placeholder="Cod sesiune (ex: OLM26)"
                                    value={codeInput}
                                    onChange={e => setCodeInput(e.target.value.toUpperCase())}
                                    style={{ flex: 1, padding: '9px 12px', borderRadius: 8, border: '1px solid #d1d5db', fontSize: '0.9rem' }}
                                />
                                <button onClick={joinSession} disabled={loadingSession} style={{
                                    padding: '9px 18px', background: '#4f46e5', color: '#fff',
                                    border: 'none', borderRadius: 8, fontWeight: 600, cursor: 'pointer'
                                }}>
                                    {loadingSession ? '...' : 'Intra'}
                                </button>
                            </div>
                            {sessionErr && <div style={{ color: '#dc2626', fontSize: '0.82rem', marginTop: 8 }}>{sessionErr}</div>}
                        </div>
                    ) : (
                        <div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
                                <div style={{
                                    background: '#f0fdf4', border: '1px solid #bbf7d0',
                                    borderRadius: 8, padding: '5px 12px', fontSize: '0.82rem', color: '#15803d'
                                }}>
                                    {activeSession.name} ({activeSession.code})
                                </div>
                                <button onClick={() => setActiveSession(null)} style={{
                                    background: 'none', border: 'none', cursor: 'pointer',
                                    color: '#6b7280', fontSize: '0.8rem', textDecoration: 'underline'
                                }}>
                                    Schimba
                                </button>
                            </div>
                            <ScoringTab arbiterName={arbiterName} sessionCode={activeSession.code} />
                        </div>
                    )}
                </div>
            )}

            {/* Comparatie tab */}
            {tab === 'comparatie' && (
                <div>
                    {!activeSession ? (
                        <div>
                            <div style={{ fontWeight: 600, marginBottom: 10, fontSize: '0.9rem' }}>Alege sesiunea pentru comparatie</div>
                            <div style={{ display: 'flex', gap: 8 }}>
                                <input
                                    placeholder="Cod sesiune (ex: OLM26)"
                                    value={codeInput}
                                    onChange={e => setCodeInput(e.target.value.toUpperCase())}
                                    style={{ flex: 1, padding: '9px 12px', borderRadius: 8, border: '1px solid #d1d5db', fontSize: '0.9rem' }}
                                />
                                <button onClick={joinSession} disabled={loadingSession} style={{
                                    padding: '9px 18px', background: '#4f46e5', color: '#fff',
                                    border: 'none', borderRadius: 8, fontWeight: 600, cursor: 'pointer'
                                }}>
                                    {loadingSession ? '...' : 'Incarca'}
                                </button>
                            </div>
                            {sessionErr && <div style={{ color: '#dc2626', fontSize: '0.82rem', marginTop: 8 }}>{sessionErr}</div>}
                        </div>
                    ) : (
                        <div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
                                <div style={{
                                    background: '#f0fdf4', border: '1px solid #bbf7d0',
                                    borderRadius: 8, padding: '5px 12px', fontSize: '0.82rem', color: '#15803d'
                                }}>
                                    {activeSession.name} ({activeSession.code})
                                </div>
                                <button onClick={() => setActiveSession(null)} style={{
                                    background: 'none', border: 'none', cursor: 'pointer',
                                    color: '#6b7280', fontSize: '0.8rem', textDecoration: 'underline'
                                }}>
                                    Schimba
                                </button>
                            </div>
                            <CompareTab sessionCode={activeSession.code} />
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
