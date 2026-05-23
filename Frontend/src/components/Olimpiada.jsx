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

const PLACE_POINTS = { 1: 1500, 2: 1000, 3: 500, 4: 300 };
const teamLabel = t => t.charAt(0) + t.slice(1).toLowerCase();
const medals = ['I', 'II', 'III', 'IV'];

// ─── SCORING TAB ────────────────────────────────────────────────────────────

function ScoringTab({ arbiterName, sessionCode }) {
    const [placements, setPlacements] = useState({});
    const [round, setRound] = useState(1);
    const [submitting, setSubmitting] = useState(false);
    const [history, setHistory] = useState([]);
    const [isDouble, setIsDouble] = useState(false);

    // Extra points state
    const [extraTeam, setExtraTeam] = useState('ROSU');
    const [extraPoints, setExtraPoints] = useState('');
    const [extraNote, setExtraNote] = useState('');
    const [submittingExtra, setSubmittingExtra] = useState(false);

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
                body: JSON.stringify({ arbiterName, roundNumber: round, isDouble, scores }),
            });
            if (!r.ok) { toast.error(await r.text()); return; }
            toast.success(`Runda ${round}${isDouble ? ' (dublu)' : ''} salvata!`);
            setHistory(h => [...h, { round, placements: { ...placements }, isDouble }]);
            setRound(r2 => r2 + 1);
            setPlacements({});
            setIsDouble(false);
        } finally {
            setSubmitting(false);
        }
    };

    const submitExtra = async (e) => {
        e.preventDefault();
        const pts = parseInt(extraPoints);
        if (!pts || pts <= 0) { toast.error('Puncte invalide'); return; }
        setSubmittingExtra(true);
        try {
            const r = await fetch(`${API_URL}/olimpiada/session/${sessionCode}/extra`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ arbiterName, team: extraTeam, points: pts, note: extraNote }),
            });
            if (!r.ok) { toast.error(await r.text()); return; }
            toast.success(`+${pts} pct acordate echipei ${teamLabel(extraTeam)}!`);
            setExtraPoints('');
            setExtraNote('');
        } finally {
            setSubmittingExtra(false);
        }
    };

    const effectivePoints = (place) => isDouble ? PLACE_POINTS[place] * 2 : PLACE_POINTS[place];

    return (
        <div className="animate-in">
            {/* Header runda */}
            <div className="card" style={{ marginBottom: 16, borderLeft: `4px solid ${isDouble ? '#f59e0b' : 'var(--accent)'}`, padding: '16px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div>
                    <div style={{ fontWeight: 800, fontSize: '1.1rem', color: 'var(--text-primary)' }}>
                        Runda {round}
                        {isDouble && <span style={{ marginLeft: 8, fontSize: '0.78rem', background: '#fef3c7', color: '#92400e', borderRadius: 6, padding: '2px 8px', fontWeight: 700 }}>x2</span>}
                    </div>
                    <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', marginTop: 2 }}>Arbitru: {arbiterName}</div>
                </div>
                <div style={{ textAlign: 'right', color: isDouble ? '#f59e0b' : 'var(--accent)', fontWeight: 900, fontSize: '1.8rem', lineHeight: 1 }}>
                    {Object.keys(placements).length}<span style={{ fontSize: '1rem' }}>/4</span>
                    <div style={{ fontSize: '0.7rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>echipe</div>
                </div>
            </div>

            {/* Grid 2x2 echipe */}
            <div className="card" style={{ marginBottom: 16 }}>
                <p className="db-section-title" style={{ marginBottom: 14 }}>
                    Apasa echipele in ordinea locurilor (primul = Loc 1)
                </p>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                    {TEAMS.map(team => {
                        const c = TEAM_COLORS[team];
                        const place = placements[team];
                        const isActive = place != null;
                        return (
                            <div key={team} onClick={() => handleTeamPress(team)} style={{
                                padding: '14px 16px', borderRadius: 14, cursor: 'pointer',
                                border: `2px solid ${isActive ? c.border : 'var(--border-color)'}`,
                                background: isActive ? c.bg : 'white',
                                transition: 'all 0.15s',
                                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                            }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                                    <div style={{ width: 10, height: 10, borderRadius: '50%', background: c.dot, flexShrink: 0 }} />
                                    <span style={{ fontWeight: 800, fontSize: '0.95rem', color: isActive ? c.text : 'var(--text-primary)' }}>
                                        {teamLabel(team)}
                                    </span>
                                </div>
                                <div style={{ textAlign: 'right' }}>
                                    <div style={{ fontSize: '0.85rem', fontWeight: 700, color: isActive ? c.text : 'var(--text-secondary)', opacity: isActive ? 1 : 0.4 }}>
                                        {isActive ? `Loc ${place}` : '—'}
                                    </div>
                                    {isActive && (
                                        <div style={{ fontSize: '0.72rem', fontWeight: 600, color: c.text, opacity: 0.75 }}>
                                            +{effectivePoints(place).toLocaleString()}
                                        </div>
                                    )}
                                </div>
                            </div>
                        );
                    })}
                </div>

                {Object.keys(placements).length > 0 && (
                    <div style={{ marginTop: 14, display: 'flex', flexDirection: 'column', gap: 4 }}>
                        {TEAMS.filter(t => placements[t] != null).sort((a, b) => placements[a] - placements[b]).map(team => {
                            const c = TEAM_COLORS[team];
                            return (
                                <div key={team} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.83rem', padding: '2px 4px' }}>
                                    <span style={{ color: c.text, fontWeight: 600 }}>Loc {placements[team]} — Echipa {teamLabel(team)}</span>
                                    <span style={{ fontWeight: 700, color: 'var(--text-primary)' }}>+{effectivePoints(placements[team]).toLocaleString()} pct</span>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>

            {/* Toggle double points */}
            <div onClick={() => setIsDouble(d => !d)} className="card" style={{
                marginBottom: 16, padding: '14px 20px', cursor: 'pointer',
                border: `2px solid ${isDouble ? '#f59e0b' : 'var(--border-color)'}`,
                background: isDouble ? '#fffbeb' : 'white',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                transition: 'all 0.15s',
            }}>
                <span style={{ fontWeight: 800, fontSize: '0.95rem', color: isDouble ? '#92400e' : 'var(--text-primary)' }}>
                    Runda cu puncte duble
                </span>
                <span style={{ fontWeight: 700, fontSize: '0.85rem', opacity: isDouble ? 1 : 0.4, color: '#92400e' }}>
                    {isDouble ? 'ACTIV — x2' : 'x2'}
                </span>
            </div>

            {/* Buton salveaza runda */}
            <button onClick={submitRound} disabled={!allAssigned || submitting} style={{
                width: '100%', padding: 16, background: allAssigned ? '#15803d' : '#d1d5db',
                color: allAssigned ? 'white' : '#9ca3af', border: 'none', borderRadius: 14,
                fontWeight: 800, fontSize: '1rem', cursor: allAssigned ? 'pointer' : 'not-allowed',
                boxShadow: allAssigned ? '0 4px 14px rgba(21,128,61,0.3)' : 'none',
                marginBottom: 20, transition: 'all 0.15s',
            }}>
                {submitting ? 'Se salveaza...' : `Salveaza Runda ${round}${isDouble ? ' (x2)' : ''}`}
            </button>

            {/* Puncte extra */}
            <div className="card" style={{ marginBottom: 16 }}>
                <p className="db-section-title" style={{ marginBottom: 14 }}>Puncte Extra</p>
                <form onSubmit={submitExtra} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                        {TEAMS.map(team => {
                            const c = TEAM_COLORS[team];
                            const isSelected = extraTeam === team;
                            return (
                                <div key={team} onClick={() => setExtraTeam(team)} style={{
                                    padding: '10px 14px', borderRadius: 12, cursor: 'pointer',
                                    border: `2px solid ${isSelected ? c.border : 'var(--border-color)'}`,
                                    background: isSelected ? c.bg : 'white',
                                    display: 'flex', alignItems: 'center', gap: 8, transition: 'all 0.12s',
                                }}>
                                    <div style={{ width: 8, height: 8, borderRadius: '50%', background: c.dot }} />
                                    <span style={{ fontWeight: 700, fontSize: '0.88rem', color: isSelected ? c.text : 'var(--text-primary)' }}>
                                        {teamLabel(team)}
                                    </span>
                                </div>
                            );
                        })}
                    </div>
                    <input
                        type="number"
                        className="login-input"
                        placeholder="Puncte (ex: 300)"
                        value={extraPoints}
                        onChange={e => setExtraPoints(e.target.value)}
                        min="1"
                        required
                        style={{ marginBottom: 0 }}
                    />
                    <input
                        type="text"
                        className="login-input"
                        placeholder="Motiv (optional)"
                        value={extraNote}
                        onChange={e => setExtraNote(e.target.value)}
                        style={{ marginBottom: 0 }}
                    />
                    <button type="submit" disabled={submittingExtra} style={{
                        padding: '12px', background: 'var(--accent)', color: 'white',
                        border: 'none', borderRadius: 12, fontWeight: 800, cursor: 'pointer', fontSize: '0.9rem',
                    }}>
                        {submittingExtra ? 'Se salveaza...' : `Acorda puncte echipei ${teamLabel(extraTeam)}`}
                    </button>
                </form>
            </div>

            {/* Istoric */}
            {history.length > 0 && (
                <div className="card">
                    <p className="db-section-title" style={{ marginBottom: 10 }}>Runde trimise</p>
                    {history.map(h => (
                        <div key={h.round} style={{
                            padding: '10px 0', borderBottom: '1px solid var(--border-color)',
                            display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', fontSize: '0.83rem'
                        }}>
                            <span style={{ fontWeight: 800, color: 'var(--text-primary)', minWidth: 65 }}>
                                Runda {h.round}
                                {h.isDouble && <span style={{ marginLeft: 4, fontSize: '0.7rem', background: '#fef3c7', color: '#92400e', borderRadius: 4, padding: '1px 5px' }}>x2</span>}
                            </span>
                            {[...TEAMS].sort((a, b) => h.placements[a] - h.placements[b]).map(t => (
                                <span key={t} style={{
                                    background: TEAM_COLORS[t].bg, border: `1px solid ${TEAM_COLORS[t].border}`,
                                    borderRadius: 8, padding: '2px 10px', color: TEAM_COLORS[t].text, fontWeight: 600
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

// ─── COMPARE TAB ────────────────────────────────────────────────────────────

function ArbiterDosar({ arbiterName, arbiterData }) {
    const { rounds, extras } = arbiterData;

    return (
        <div style={{ marginTop: 12, borderTop: '1px solid var(--border-color)', paddingTop: 12 }}>
            {rounds.length === 0 && extras.length === 0 && (
                <div style={{ color: 'var(--text-secondary)', fontSize: '0.83rem', padding: '8px 0' }}>
                    Nicio runda trimisa inca.
                </div>
            )}
            {rounds.map(r => (
                <div key={r.round} style={{ marginBottom: 10 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
                        <span style={{ fontWeight: 800, fontSize: '0.85rem', color: 'var(--text-primary)' }}>Runda {r.round}</span>
                        {r.isDouble && <span style={{ fontSize: '0.72rem', background: '#fef3c7', color: '#92400e', borderRadius: 4, padding: '1px 6px', fontWeight: 700 }}>x2</span>}
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                        {TEAMS.filter(t => r.scores[t])
                            .sort((a, b) => r.scores[a].place - r.scores[b].place)
                            .map(team => {
                                const c = TEAM_COLORS[team];
                                const s = r.scores[team];
                                return (
                                    <div key={team} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '4px 10px', borderRadius: 8, background: c.bg, border: `1px solid ${c.border}` }}>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                                            <div style={{ width: 7, height: 7, borderRadius: '50%', background: c.dot }} />
                                            <span style={{ fontWeight: 600, fontSize: '0.83rem', color: c.text }}>Loc {s.place} — {teamLabel(team)}</span>
                                        </div>
                                        <span style={{ fontWeight: 700, fontSize: '0.83rem', color: c.text }}>+{s.points.toLocaleString()}</span>
                                    </div>
                                );
                            })}
                    </div>
                </div>
            ))}
            {extras.length > 0 && (
                <div style={{ marginTop: 8 }}>
                    <div style={{ fontWeight: 700, fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: 6, textTransform: 'uppercase', letterSpacing: '0.5px' }}>Puncte extra</div>
                    {extras.map((ex, i) => {
                        const c = TEAM_COLORS[ex.team];
                        return (
                            <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '4px 10px', borderRadius: 8, background: c.bg, border: `1px solid ${c.border}`, marginBottom: 4 }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                                    <div style={{ width: 7, height: 7, borderRadius: '50%', background: c.dot }} />
                                    <span style={{ fontWeight: 600, fontSize: '0.83rem', color: c.text }}>
                                        {teamLabel(ex.team)}{ex.note ? ` — ${ex.note}` : ''}
                                    </span>
                                </div>
                                <span style={{ fontWeight: 700, fontSize: '0.83rem', color: c.text }}>+{ex.points.toLocaleString()}</span>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}

function CompareTab({ sessionCode }) {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [openDosare, setOpenDosare] = useState(new Set());

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

    const toggleDosar = (arbiter) => {
        setOpenDosare(prev => {
            const next = new Set(prev);
            if (next.has(arbiter)) next.delete(arbiter);
            else next.add(arbiter);
            return next;
        });
    };

    if (loading) return <div style={{ color: 'var(--text-secondary)', padding: 20, textAlign: 'center' }}>Se incarca...</div>;
    if (!data) return <div style={{ color: 'var(--text-secondary)', padding: 20, textAlign: 'center' }}>Alege o sesiune pentru comparatie.</div>;

    const { arbiters, arbiterData } = data;

    if (arbiters.length === 0) {
        return <div style={{ color: 'var(--text-secondary)', padding: 20, textAlign: 'center' }}>Niciun arbitru nu a trimis scoruri inca.</div>;
    }

    return (
        <div className="animate-in">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
                <div style={{ fontWeight: 700, fontSize: '1.05rem', color: 'var(--text-primary)' }}>{data.sessionName}</div>
                <button onClick={load} style={{
                    background: 'var(--bg-primary)', border: '1px solid var(--border-color)',
                    borderRadius: 8, padding: '5px 12px', cursor: 'pointer',
                    fontSize: '0.82rem', color: 'var(--text-secondary)'
                }}>
                    Actualizeaza
                </button>
            </div>

            {/* Un card per arbitru */}
            {arbiters.map(arbiter => {
                const ad = arbiterData[arbiter];
                const isOpen = openDosare.has(arbiter);
                return (
                    <div key={arbiter} className="card" style={{ marginBottom: 16 }}>
                        {/* Header arbitru */}
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
                            <div>
                                <p className="db-section-title" style={{ margin: 0 }}>{arbiter}</p>
                                <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', marginTop: 2 }}>
                                    {ad.roundCount} runde{ad.extras.length > 0 ? ` + ${ad.extras.length} extra` : ''}
                                </div>
                            </div>
                            <button onClick={() => toggleDosar(arbiter)} style={{
                                padding: '6px 14px', borderRadius: 8, border: '1px solid var(--border-color)',
                                background: isOpen ? 'var(--accent)' : 'white',
                                color: isOpen ? 'white' : 'var(--text-secondary)',
                                fontWeight: 700, fontSize: '0.8rem', cursor: 'pointer', transition: 'all 0.15s',
                            }}>
                                {isOpen ? 'Inchide dosar' : 'Deschide dosar'}
                            </button>
                        </div>

                        {/* Leaderboard per arbitru */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                            {ad.leaderboard.map((item, idx) => {
                                const c = TEAM_COLORS[item.team];
                                return (
                                    <div key={item.team} style={{
                                        display: 'flex', alignItems: 'center', gap: 12,
                                        padding: '10px 14px', borderRadius: 12,
                                        background: c.bg, border: `1.5px solid ${c.border}`,
                                    }}>
                                        <div style={{
                                            width: 26, height: 26, borderRadius: '50%', flexShrink: 0,
                                            background: idx === 0 ? '#f59e0b' : idx === 1 ? '#9ca3af' : idx === 2 ? '#b45309' : '#e5e7eb',
                                            color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center',
                                            fontWeight: 800, fontSize: '0.72rem',
                                        }}>
                                            {medals[idx]}
                                        </div>
                                        <div style={{ width: 8, height: 8, borderRadius: '50%', background: c.dot, flexShrink: 0 }} />
                                        <span style={{ fontWeight: 700, color: c.text, flex: 1, fontSize: '0.9rem' }}>
                                            Echipa {teamLabel(item.team)}
                                        </span>
                                        <span style={{ fontWeight: 900, fontSize: '1.05rem', color: c.text }}>
                                            {item.total.toLocaleString()}
                                        </span>
                                    </div>
                                );
                            })}
                        </div>

                        {/* Dosar expandabil */}
                        {isOpen && <ArbiterDosar arbiterName={arbiter} arbiterData={ad} />}
                    </div>
                );
            })}

            {/* Comparatie rapida intre arbitri (daca sunt >= 2) */}
            {arbiters.length >= 2 && (
                <div className="card">
                    <p className="db-section-title" style={{ marginBottom: 14 }}>Diferente intre arbitri</p>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                        {TEAMS.map(team => {
                            const c = TEAM_COLORS[team];
                            const vals = arbiters.map(a => {
                                const lb = arbiterData[a]?.leaderboard?.find(x => x.team === team);
                                return lb ? lb.total : 0;
                            });
                            const max = Math.max(...vals);
                            const min = Math.min(...vals);
                            const diff = max - min;
                            return (
                                <div key={team} style={{ padding: '10px 14px', borderRadius: 12, background: diff > 0 ? '#fef2f2' : c.bg, border: `1.5px solid ${diff > 0 ? '#fecaca' : c.border}` }}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: diff > 0 ? 6 : 0 }}>
                                        <div style={{ width: 8, height: 8, borderRadius: '50%', background: c.dot }} />
                                        <span style={{ fontWeight: 700, color: c.text, flex: 1, fontSize: '0.88rem' }}>Echipa {teamLabel(team)}</span>
                                        {diff > 0
                                            ? <span style={{ fontWeight: 800, color: '#dc2626', fontSize: '0.85rem' }}>diferenta: {diff.toLocaleString()}</span>
                                            : <span style={{ fontWeight: 600, color: '#15803d', fontSize: '0.82rem' }}>identic</span>
                                        }
                                    </div>
                                    {diff > 0 && (
                                        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                                            {arbiters.map((a, i) => (
                                                <span key={a} style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', fontWeight: 600 }}>
                                                    {a}: <strong style={{ color: 'var(--text-primary)' }}>{vals[i].toLocaleString()}</strong>
                                                </span>
                                            ))}
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
}

// ─── SESSIONS TAB ────────────────────────────────────────────────────────────

function SessionsTab({ adminPin }) {
    const [sessions, setSessions] = useState([]);
    const [form, setForm] = useState({ name: '', code: '' });
    const [saving, setSaving] = useState(false);
    const [loading, setLoading] = useState(false);

    const load = async () => {
        setLoading(true);
        try {
            const r = await fetch(`${API_URL}/olimpiada/sessions`, { headers: { 'X-Admin-Pin': adminPin } });
            if (r.ok) setSessions(await r.json());
        } finally { setLoading(false); }
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
            if (r.ok) { toast.success('Sesiune creata!'); setForm({ name: '', code: '' }); load(); }
            else toast.error(await r.text());
        } finally { setSaving(false); }
    };

    const closeSession = async (id) => {
        const r = await fetch(`${API_URL}/olimpiada/sessions/${id}/close`, { method: 'POST', headers: { 'X-Admin-Pin': adminPin } });
        if (r.ok) { toast.success('Sesiune inchisa'); load(); }
    };

    const deleteSession = async (id, name) => {
        if (!window.confirm(`Stergi sesiunea "${name}"? Se sterg si toate scorurile.`)) return;
        const r = await fetch(`${API_URL}/olimpiada/sessions/${id}`, { method: 'DELETE', headers: { 'X-Admin-Pin': adminPin } });
        if (r.ok) { toast.success('Sesiune stearsa'); load(); }
        else toast.error(await r.text());
    };

    return (
        <div>
            <form onSubmit={create} className="card" style={{ marginBottom: 20 }}>
                <p className="db-section-title" style={{ marginBottom: 12 }}>Sesiune noua</p>
                <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                    <input placeholder="Nume (ex: Olimpiada 2026)" value={form.name}
                        onChange={e => setForm(f => ({ ...f, name: e.target.value }))} required
                        className="login-input" style={{ flex: 2, minWidth: 160, marginBottom: 0 }} />
                    <input placeholder="Cod (ex: OLM26)" value={form.code}
                        onChange={e => setForm(f => ({ ...f, code: e.target.value.toUpperCase() }))} required maxLength={10}
                        className="login-input" style={{ flex: 1, minWidth: 100, marginBottom: 0 }} />
                    <button type="submit" disabled={saving} className="btn-primary" style={{ padding: '10px 18px', border: 'none', borderRadius: 10, fontWeight: 700, cursor: 'pointer' }}>
                        {saving ? '...' : 'Creeaza'}
                    </button>
                </div>
            </form>

            {loading ? (
                <div style={{ color: 'var(--text-secondary)', textAlign: 'center', padding: 20 }}>Se incarca...</div>
            ) : sessions.length === 0 ? (
                <div style={{ color: 'var(--text-secondary)', textAlign: 'center', padding: 20 }}>Nicio sesiune creata inca.</div>
            ) : sessions.map(s => (
                <div key={s.id} className="card" style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 16px', marginBottom: 10 }}>
                    <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--text-primary)' }}>{s.name}</div>
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: 2 }}>Cod: <strong>{s.code}</strong></div>
                    </div>
                    <span style={{ fontSize: '0.75rem', fontWeight: 700, padding: '3px 10px', borderRadius: 20, background: s.status === 'ACTIVE' ? '#dcfce7' : '#f3f4f6', color: s.status === 'ACTIVE' ? '#15803d' : '#6b7280' }}>
                        {s.status}
                    </span>
                    {s.status === 'ACTIVE' && (
                        <button onClick={() => closeSession(s.id)} style={{ padding: '5px 12px', background: '#fef2f2', color: '#dc2626', border: '1px solid #fecaca', borderRadius: 8, cursor: 'pointer', fontSize: '0.8rem' }}>
                            Inchide
                        </button>
                    )}
                    <button onClick={() => deleteSession(s.id, s.name)} style={{ padding: '5px 12px', background: '#fef2f2', color: '#dc2626', border: '1px solid #fecaca', borderRadius: 8, cursor: 'pointer', fontSize: '0.8rem' }}>
                        Sterge
                    </button>
                </div>
            ))}
        </div>
    );
}

// ─── MAIN COMPONENT ──────────────────────────────────────────────────────────

export default function Olimpiada({ user, guestArbiter, onExitGuest }) {
    const isGuest = !!guestArbiter;

    const [tab, setTab] = useState(isGuest ? 'scorare' : 'sesiuni');
    const [adminPin, setAdminPin] = useState('');
    const [pinUnlocked, setPinUnlocked] = useState(false);
    const [pinInput, setPinInput] = useState('');
    const [pinErr, setPinErr] = useState('');

    const [codeInput, setCodeInput] = useState(guestArbiter?.code || '');
    const [activeSession, setActiveSession] = useState(null);
    const [sessionErr, setSessionErr] = useState('');
    const [loadingSession, setLoadingSession] = useState(false);

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
            } else {
                setSessionErr('Codul nu a fost gasit.');
            }
        } finally { setLoadingSession(false); }
    };

    const verifyPin = async (e) => {
        e.preventDefault();
        const r = await fetch(`${API_URL}/admin/verify-pin`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ pin: pinInput })
        });
        if (r.ok) { setAdminPin(pinInput); setPinUnlocked(true); setPinErr(''); }
        else setPinErr('PIN incorect');
    };

    const tabs = [
        ...(!isGuest ? [{ id: 'sesiuni', label: 'Sesiuni', color: '#0284c7' }] : []),
        { id: 'scorare', label: 'Scorare', color: '#16a34a' },
        { id: 'comparatie', label: 'Comparatie', color: '#7c3aed' },
    ];

    const codePickerJsx = (
        <div>
            <p className="db-section-title" style={{ marginBottom: 10 }}>Alege sesiunea</p>
            <div style={{ display: 'flex', gap: 8 }}>
                <input placeholder="Cod sesiune (ex: OLM26)" value={codeInput}
                    onChange={e => setCodeInput(e.target.value.toUpperCase())}
                    className="login-input" style={{ flex: 1, marginBottom: 0 }} />
                <button onClick={joinSession} disabled={loadingSession} className="btn-primary"
                    style={{ padding: '10px 18px', border: 'none', borderRadius: 10, fontWeight: 700, cursor: 'pointer', whiteSpace: 'nowrap' }}>
                    {loadingSession ? '...' : 'Intra'}
                </button>
            </div>
            {sessionErr && <div style={{ color: '#dc2626', fontSize: '0.82rem', marginTop: 8 }}>{sessionErr}</div>}
        </div>
    );

    return (
        <div style={{ maxWidth: 640, margin: '0 auto', padding: '20px 16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
                <div>
                    <h2 style={{ margin: 0, fontSize: '1.3rem', fontWeight: 800, color: 'var(--text-primary)' }}>Olimpiada Awana</h2>
                    {isGuest && <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', marginTop: 3 }}>Arbitru invitat: <strong>{guestArbiter.name}</strong></div>}
                </div>
                {isGuest && (
                    <button onClick={onExitGuest} style={{ background: 'var(--bg-primary)', border: '1px solid var(--border-color)', borderRadius: 8, padding: '5px 12px', cursor: 'pointer', fontSize: '0.82rem', color: 'var(--text-secondary)' }}>
                        Iesi
                    </button>
                )}
            </div>

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

            {/* TAB SESIUNI */}
            {tab === 'sesiuni' && !isGuest && (
                !pinUnlocked ? (
                    <div className="card">
                        <p className="db-section-title" style={{ marginBottom: 12 }}>Acces admin</p>
                        <form onSubmit={verifyPin} style={{ display: 'flex', gap: 8 }}>
                            <input type="password" placeholder="PIN admin" value={pinInput}
                                onChange={e => setPinInput(e.target.value)}
                                className="login-input" style={{ flex: 1, marginBottom: 0 }} />
                            <button type="submit" className="btn-primary" style={{ padding: '10px 16px', border: 'none', borderRadius: 10, fontWeight: 700, cursor: 'pointer' }}>
                                Intra
                            </button>
                        </form>
                        {pinErr && <div style={{ color: '#dc2626', fontSize: '0.82rem', marginTop: 8 }}>{pinErr}</div>}
                    </div>
                ) : <SessionsTab adminPin={adminPin} />
            )}

            {/* TAB SCORARE */}
            {tab === 'scorare' && (
                !activeSession ? (
                    <div className="card">{codePickerJsx}</div>
                ) : (
                    <div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
                            <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: 8, padding: '5px 12px', fontSize: '0.82rem', color: '#15803d', fontWeight: 600 }}>
                                {activeSession.name} ({activeSession.code})
                            </div>
                            <button onClick={() => setActiveSession(null)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)', fontSize: '0.8rem', textDecoration: 'underline' }}>
                                Schimba
                            </button>
                        </div>
                        <ScoringTab arbiterName={arbiterName} sessionCode={activeSession.code} />
                    </div>
                )
            )}

            {/* TAB COMPARATIE */}
            {tab === 'comparatie' && (
                !activeSession ? (
                    <div className="card">{codePickerJsx}</div>
                ) : (
                    <div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
                            <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: 8, padding: '5px 12px', fontSize: '0.82rem', color: '#15803d', fontWeight: 600 }}>
                                {activeSession.name} ({activeSession.code})
                            </div>
                            <button onClick={() => setActiveSession(null)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)', fontSize: '0.8rem', textDecoration: 'underline' }}>
                                Schimba
                            </button>
                        </div>
                        <CompareTab sessionCode={activeSession.code} />
                    </div>
                )
            )}
        </div>
    );
}
