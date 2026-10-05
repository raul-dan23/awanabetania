import React, { useCallback, useEffect, useState } from 'react';
import { toast } from 'sonner';
import { API_URL } from '../config';

const card = { background: 'white', border: '1px solid #e2e8f0', borderRadius: '16px', padding: '18px 20px', marginBottom: '14px' };
const muted = { color: '#64748b', fontSize: '0.85rem' };

/** 2026-09-14 → 14.09.2026 */
const formatDate = (iso) => (iso ? iso.split('-').reverse().join('.') : '');

/** "2025–2026" → "2026–2027"; any other name gives no suggestion. */
const suggestNextName = (name) => {
    const m = /^(\d{4})\s*[–-]\s*(\d{4})$/.exec(name || '');
    return m ? `${Number(m[1]) + 1}–${Number(m[2]) + 1}` : '';
};

/**
 * Control Center → Sezoane. Shows the current season, starts a new one after showing
 * exactly what goes back to zero, and opens the record of each closed season.
 *
 * @param {Object} props
 * @param {string} props.pin - The admin PIN the Control Center was unlocked with
 */
const SeasonsPanel = ({ pin }) => {
    const [seasons, setSeasons] = useState([]);
    const [preview, setPreview] = useState(null);
    const [starting, setStarting] = useState(false);
    const [newName, setNewName] = useState('');
    const [understood, setUnderstood] = useState(false);
    const [busy, setBusy] = useState(false);
    const [openResults, setOpenResults] = useState(null);   // season id
    const [results, setResults] = useState({});             // season id → results

    const load = useCallback(() => {
        fetch(`${API_URL}/seasons`)
            .then(r => (r.ok ? r.json() : Promise.reject()))
            .then(setSeasons)
            .catch(() => toast.error('Sezoanele nu au putut fi incarcate.'));
        fetch(`${API_URL}/admin/seasons/preview`, { headers: { 'X-Admin-Pin': pin } })
            .then(r => (r.ok ? r.json() : Promise.reject()))
            .then(p => { setPreview(p); setNewName(n => n || suggestNextName(p.current.name)); })
            .catch(() => toast.error('Sezonul curent nu a putut fi incarcat.'));
    }, [pin]);

    useEffect(() => { load(); }, [load]);

    const rename = (season) => {
        const name = window.prompt('Numele sezonului:', season.name);
        if (name === null || !name.trim() || name.trim() === season.name) return;
        fetch(`${API_URL}/admin/seasons/${season.id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', 'X-Admin-Pin': pin },
            body: JSON.stringify({ name: name.trim() }),
        })
            .then(r => (r.ok ? r.json() : Promise.reject(r.status)))
            .then(() => { toast.success('Sezonul a fost redenumit.'); load(); })
            .catch(status => toast.error(status === 409 ? 'Exista deja un sezon cu acest nume.' : 'Numele nu e valid (maxim 60 de caractere).'));
    };

    const startSeason = (e) => {
        e.preventDefault();
        const name = newName.trim();
        if (!name || !understood || !preview) return;
        setBusy(true);
        fetch(`${API_URL}/admin/seasons`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-Admin-Pin': pin },
            body: JSON.stringify({ currentSeasonId: preview.current.id, name }),
        })
            .then(async r => {
                if (r.ok) return r.json();
                const text = await r.text();
                return Promise.reject({ status: r.status, text });
            })
            .then(next => {
                toast.success(`Sezonul ${next.name} a inceput. ${preview.current.name} e arhivat mai jos.`);
                setStarting(false);
                setUnderstood(false);
                setNewName('');
                load();
            })
            .catch(err => {
                const text = (err && err.text) || '';
                // 503 first: its message ("...nothing was changed") must not read as "the season changed"
                if (err && err.status === 503) toast.error('Backup-ul bazei de date a esuat, asa ca sezonul nu a fost pornit si nimic nu s-a schimbat. Detalii: sudo journalctl -u awanabetania');
                else if (text.includes('already exists')) toast.error('Exista deja un sezon cu acest nume.');
                else if (text.includes('still open')) toast.error('O intalnire cu punctaje e inca deschisa. Inchide-o, apoi incearca din nou.');
                else if (text.includes('changed')) { toast.error('Sezonul s-a schimbat intre timp. Am reincarcat pagina.'); load(); }
                else toast.error('Sezonul nou nu a putut fi pornit.');
            })
            .finally(() => setBusy(false));
    };

    const toggleResults = (season) => {
        if (openResults === season.id) { setOpenResults(null); return; }
        setOpenResults(season.id);
        if (results[season.id]) return;
        fetch(`${API_URL}/seasons/${season.id}/results`)
            .then(r => (r.ok ? r.json() : Promise.reject()))
            .then(res => setResults(prev => ({ ...prev, [season.id]: res })))
            .catch(() => toast.error('Rezultatele nu au putut fi incarcate.'));
    };

    const current = preview?.current;
    const closed = seasons.filter(s => !s.active);

    return (
        <div>
            {/* Current season */}
            {current && (
                <div style={{ ...card, borderLeft: '4px solid #16a34a' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <div style={{ flex: 1, minWidth: 0 }}>
                            <div style={{ ...muted, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.04em', fontSize: '0.72rem' }}>Sezonul curent</div>
                            <div style={{ fontWeight: 900, fontSize: '1.3rem', color: '#1e293b', overflowWrap: 'anywhere' }}>{current.name}</div>
                            <div style={muted}>din {formatDate(current.startDate)}</div>
                        </div>
                        <button onClick={() => rename(current)} style={{ padding: '8px 14px', borderRadius: '10px', border: '1px solid #e2e8f0', background: '#f8fafc', cursor: 'pointer', fontWeight: 700, flexShrink: 0 }}>Redenumeste</button>
                    </div>
                </div>
            )}

            {/* New season */}
            {preview && !starting && (
                <button onClick={() => setStarting(true)} style={{ width: '100%', padding: '13px', borderRadius: '12px', border: '1.5px dashed #fca5a5', background: '#fef2f2', color: '#b91c1c', fontWeight: 800, cursor: 'pointer', marginBottom: '20px' }}>
                    Incheie sezonul si incepe unul nou…
                </button>
            )}

            {preview && starting && (
                <form onSubmit={startSeason} style={{ ...card, borderColor: '#fecaca' }}>
                    <div style={{ fontWeight: 900, color: '#1e293b', marginBottom: '10px' }}>Sezon nou</div>

                    <div style={{ fontWeight: 800, fontSize: '0.85rem', color: '#b91c1c', marginBottom: '6px' }}>Pornesc de la zero:</div>
                    <ul style={{ margin: '0 0 12px', paddingLeft: '20px', color: '#334155', fontSize: '0.88rem', lineHeight: 1.6 }}>
                        <li>punctele: {preview.childrenWithPoints} copii au acum {preview.pointsBalance.toLocaleString('ro-RO')} puncte in total</li>
                        <li>streak-ul, prezentele, lectiile, insignele, manualele si uniforma primita, pentru toti cei {preview.children} copii</li>
                        <li>suspendarile in curs ({preview.suspensions})</li>
                        <li>rating-ul liderilor si comentariile primite ({preview.evaluations} evaluari)</li>
                        {preview.pendingBons > 0 && <li>{preview.pendingBons} bonuri neaprobate se anuleaza</li>}
                    </ul>

                    <div style={{ fontWeight: 800, fontSize: '0.85rem', color: '#15803d', marginBottom: '6px' }}>Raman:</div>
                    <ul style={{ margin: '0 0 12px', paddingLeft: '20px', color: '#334155', fontSize: '0.88rem', lineHeight: 1.6 }}>
                        <li>copiii si liderii, cu datele lor de contact, conturile si cardurile NFC</li>
                        <li>stickerele de pe harta</li>
                        <li>tot sezonul {current.name} ({preview.scores} punctaje), de citit mai jos la „Sezoane incheiate”</li>
                        {preview.plannedMeetings > 0 && <li>{preview.plannedMeetings} intalniri planificate trec in sezonul nou</li>}
                    </ul>

                    <div style={{ fontSize: '0.85rem', color: '#334155', marginBottom: '12px' }}>
                        {preview.autoBackup
                            ? 'Inainte de pornire se face automat un backup al bazei de date. Daca nu reuseste, sezonul nu porneste.'
                            : 'Fa un backup al bazei de date inainte (scripts/backup-db.sh pe server).'}
                    </div>

                    {preview.openMeetingDate ? (
                        <div style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#b91c1c', borderRadius: '10px', padding: '10px 12px', fontSize: '0.88rem', fontWeight: 700, marginBottom: '12px' }}>
                            Intalnirea din {formatDate(preview.openMeetingDate)} e inca deschisa si are punctaje. Inchide-o, apoi revino aici.
                        </div>
                    ) : (
                        <>
                            <label style={{ display: 'block', fontWeight: 700, fontSize: '0.85rem', color: '#334155', marginBottom: '4px' }}>Numele sezonului nou</label>
                            <input className="login-input" required maxLength={60} placeholder="ex: 2026–2027" value={newName} onChange={e => setNewName(e.target.value)} style={{ marginBottom: '10px' }} />
                            <label style={{ display: 'flex', gap: '8px', alignItems: 'flex-start', fontSize: '0.88rem', color: '#334155', marginBottom: '12px', cursor: 'pointer' }}>
                                {/* Sized here: the global input style would stretch it to the full width */}
                                <input type="checkbox" checked={understood} onChange={e => setUnderstood(e.target.checked)}
                                       style={{ width: '18px', height: '18px', flex: '0 0 auto', margin: '1px 0 0', padding: 0 }} />
                                <span>Am inteles: ce e mai sus porneste de la zero pentru toti copiii si liderii.</span>
                            </label>
                        </>
                    )}

                    <div style={{ display: 'flex', gap: '8px' }}>
                        <button type="button" onClick={() => { setStarting(false); setUnderstood(false); }} style={{ flex: 1, padding: '10px', borderRadius: '10px', border: '1px solid #e2e8f0', background: '#f8fafc', cursor: 'pointer', fontWeight: 700 }}>Renunta</button>
                        <button type="submit" disabled={busy || !understood || !newName.trim() || !!preview.openMeetingDate}
                                style={{ flex: 2, padding: '10px', borderRadius: '10px', border: 'none', background: (busy || !understood || !newName.trim() || preview.openMeetingDate) ? '#fca5a5' : '#dc2626', color: 'white', cursor: 'pointer', fontWeight: 800 }}>
                            {busy ? 'Se porneste…' : `Incheie ${current.name} si incepe sezonul nou`}
                        </button>
                    </div>
                </form>
            )}

            {/* Closed seasons */}
            <div style={{ fontWeight: 900, color: '#1e293b', margin: '6px 0 10px' }}>Sezoane incheiate</div>
            {closed.length === 0 && <div style={{ ...muted, marginBottom: '14px' }}>Niciun sezon incheiat inca.</div>}
            {closed.map(s => (
                <div key={s.id} style={card}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
                        <div style={{ flex: 1, minWidth: '140px' }}>
                            <div style={{ fontWeight: 900, color: '#1e293b', overflowWrap: 'anywhere' }}>{s.name}</div>
                            <div style={muted}>{formatDate(s.startDate)} – {formatDate(s.endDate)}</div>
                        </div>
                        <button onClick={() => rename(s)} style={{ padding: '7px 12px', borderRadius: '10px', border: '1px solid #e2e8f0', background: '#f8fafc', cursor: 'pointer', fontWeight: 700, fontSize: '0.8rem' }}>Redenumeste</button>
                        <button onClick={() => toggleResults(s)} style={{ padding: '7px 12px', borderRadius: '10px', border: 'none', background: '#eef2ff', color: '#4338ca', cursor: 'pointer', fontWeight: 800, fontSize: '0.8rem' }}>
                            {openResults === s.id ? 'Ascunde' : 'Vezi rezultatele'}
                        </button>
                    </div>
                    {openResults === s.id && <SeasonResults data={results[s.id]} />}
                </div>
            ))}
        </div>
    );
};

const th = { textAlign: 'left', padding: '6px 8px', fontSize: '0.72rem', color: '#64748b', fontWeight: 800, textTransform: 'uppercase', whiteSpace: 'nowrap' };
const td = { padding: '7px 8px', fontSize: '0.85rem', color: '#1e293b', borderTop: '1px solid #f1f5f9', whiteSpace: 'nowrap' };
const medal = (rank) => (rank === 1 ? '🥇' : rank === 2 ? '🥈' : rank === 3 ? '🥉' : rank);

/** The record of one closed season: final standings and leader ratings. */
const SeasonResults = ({ data }) => {
    if (!data) return <div style={{ ...muted, marginTop: '12px' }}>Se incarca…</div>;
    return (
        <div style={{ marginTop: '14px' }}>
            <div style={{ ...muted, marginBottom: '8px' }}>{data.meetings} intalniri · {data.children.length} copii</div>

            {/* The table scrolls inside the card on narrow screens */}
            <div style={{ overflowX: 'auto', margin: '0 -4px' }}>
                <table style={{ borderCollapse: 'collapse', width: '100%' }}>
                    <thead>
                        <tr>
                            <th style={th}>#</th>
                            <th style={th}>Copil</th>
                            <th style={{ ...th, textAlign: 'right' }} title="Puncte castigate la intalniri">Castigate</th>
                            <th style={{ ...th, textAlign: 'right' }} title="Puncte cheltuite la targ">Cheltuite</th>
                            <th style={{ ...th, textAlign: 'right' }} title="Puncte ramase la final">Ramase</th>
                            <th style={{ ...th, textAlign: 'right' }}>Prez.</th>
                            <th style={{ ...th, textAlign: 'right' }}>Lectii</th>
                            <th style={th}>Uniforma</th>
                            <th style={{ ...th, textAlign: 'right' }} title="Avertismente">Avert.</th>
                        </tr>
                    </thead>
                    <tbody>
                        {data.children.map(c => (
                            <tr key={c.id}>
                                <td style={td}>{medal(c.rank)}</td>
                                <td style={{ ...td, fontWeight: 700 }}>{c.name} {c.surname}</td>
                                <td style={{ ...td, textAlign: 'right', fontWeight: 800 }}>{c.earnedPoints.toLocaleString('ro-RO')}</td>
                                <td style={{ ...td, textAlign: 'right' }}>{c.spentPoints.toLocaleString('ro-RO')}</td>
                                <td style={{ ...td, textAlign: 'right' }}>{c.pointsLeft.toLocaleString('ro-RO')}</td>
                                <td style={{ ...td, textAlign: 'right' }}>{c.attendance}</td>
                                <td style={{ ...td, textAlign: 'right' }}>{c.lessons}</td>
                                <td style={td}>{[c.shirt && 'tricou', c.hat && 'caciula', c.manual && 'manual'].filter(Boolean).join(', ') || '—'}</td>
                                <td style={{ ...td, textAlign: 'right', color: c.warnings ? '#b91c1c' : '#1e293b' }}>{c.warnings}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            <div style={{ fontWeight: 800, color: '#1e293b', margin: '16px 0 6px' }}>Lideri</div>
            {data.leaders.length === 0 ? (
                <div style={muted}>Nicio evaluare in acest sezon.</div>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                    {data.leaders.map(l => (
                        <div key={l.id} style={{ display: 'flex', gap: '8px', fontSize: '0.88rem', color: '#1e293b' }}>
                            <span style={{ flex: 1, fontWeight: 700 }}>{l.name} {l.surname}</span>
                            <span style={{ fontWeight: 800, color: '#b45309' }}>★ {l.rating.toFixed(1)}</span>
                            <span style={{ ...muted, width: '90px', textAlign: 'right' }}>{l.evaluations} evaluari</span>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default SeasonsPanel;
