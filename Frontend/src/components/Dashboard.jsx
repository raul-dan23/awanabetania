import React, { useState, useEffect } from 'react';
import { API_URL } from '../config';

const Dashboard = ({ user }) => {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const isChild = user && user.hasOwnProperty('parentPhone');

    useEffect(() => {
        const url = user ? `${API_URL}/dashboard/stats?leaderId=${user.id}` : `${API_URL}/dashboard/stats`;
        fetch(url).then(r => r.ok ? r.json() : null).then(data => { if(data) { setStats(data); setLoading(false); }}).catch(() => setLoading(false));
    }, [user]);

    const handleDeleteNotification = (id) => {
        if(!window.confirm("Ștergi notificarea?")) return;
        fetch(`${API_URL}/notifications/${id}`, { method: 'DELETE' })
            .then(res => {
                if(res.ok) setStats(prev => ({ ...prev, notifications: prev.notifications.filter(n => n.id !== id) }));
                else alert("Eroare la ștergere.");
            })
            .catch(() => alert("Eroare server."));
    };

    const getGreeting = () => {
        const h = new Date().getHours();
        if (h < 12) return 'Bună dimineața';
        if (h < 18) return 'Bună ziua';
        return 'Bună seara';
    };

    const getProgressItems = () => {
        if (!isChild) return [];
        const streak = user.attendanceStreak || 0;
        if (!user.hasShirt) {
            const remaining = Math.max(0, 5 - streak);
            return [{ icon: '👕', label: remaining === 0 ? 'Eligibil pentru Tricou!' : `${remaining} prezențe până la Tricou`, pct: Math.min((streak / 5) * 100, 100), eligible: remaining === 0 }];
        }
        if (!user.hasHat) {
            const target = 10;
            const remaining = Math.max(0, target - streak);
            return [{ icon: '🧢', label: remaining === 0 ? 'Eligibil pentru Căciulă!' : `${remaining} prezențe până la Căciulă`, pct: Math.min(((streak - 5) / 5) * 100, 100), eligible: remaining === 0 }];
        }
        return [];
    };

    if (loading) return (
        <div className="animate-in db-loading">
            <div className="db-loading-pulse" style={{height:'140px'}} />
            <div style={{display:'grid', gridTemplateColumns:'1fr 1fr 1fr 1fr', gap:'15px'}}>
                {[1,2,3,4].map(i => <div key={i} className="db-loading-pulse" style={{height:'100px'}} />)}
            </div>
            <div className="db-loading-pulse" style={{height:'80px'}} />
        </div>
    );
    if (!stats) return <div className="animate-in"><p>Eroare incarcare date.</p></div>;

    const progressItems = getProgressItems();
    const notificationsToDisplay = isChild ? [] : (stats.notifications || []);

    return (
        <div className="animate-in dashboard-wrapper">

            {/* HERO */}
            <div className="db-hero">
                <div className="db-hero-left">
                    <span className="db-greeting">{getGreeting()},</span>
                    <h1 className="db-name">{user?.name} {user?.surname}</h1>
                    <span className="db-role-pill">{isChild ? 'Copil' : (user.role || 'Lider')}</span>
                </div>
                <div className="db-hero-date">
                    <div className="db-date-day">{new Date().toLocaleDateString('ro-RO', { day: 'numeric' })}</div>
                    <div className="db-date-month">{new Date().toLocaleDateString('ro-RO', { month: 'long' })}</div>
                    <div className="db-date-weekday">{new Date().toLocaleDateString('ro-RO', { weekday: 'long' })}</div>
                </div>
            </div>

            {/* STAT CARDS */}
            <div className="db-stats">
                <div className="db-stat-card db-stat-blue">
                    <div className="db-stat-number">{stats.kidsCount}</div>
                    <div className="db-stat-label">Copii Înscriși</div>
                </div>
                <div className="db-stat-card db-stat-purple">
                    <div className="db-stat-number">{stats.leadersCount}</div>
                    <div className="db-stat-label">Lideri Activi</div>
                </div>
                {isChild ? (
                    <>
                        <div className="db-stat-card db-stat-amber">
                            <div className="db-stat-number">{(user.seasonPoints || 0).toLocaleString()}</div>
                            <div className="db-stat-label">Puncte Sezon</div>
                        </div>
                        <div className="db-stat-card db-stat-green">
                            <div className="db-stat-number">{user.attendanceStreak || 0}</div>
                            <div className="db-stat-label">Streak Prezențe</div>
                        </div>
                    </>
                ) : (
                    <>
                        <div className="db-stat-card db-stat-green">
                            <div className="db-stat-number">{(stats.notifications || []).length}</div>
                            <div className="db-stat-label">Notificări</div>
                        </div>
                        <div className="db-stat-card db-stat-amber">
                            <div className="db-stat-number">{(stats.directors || []).length}</div>
                            <div className="db-stat-label">Directori</div>
                        </div>
                    </>
                )}
            </div>

            {/* PROGRES COPIL */}
            {isChild && progressItems.length > 0 && (
                <div className="db-section">
                    <p className="db-section-title">🎯 Progres Recompense</p>
                    {progressItems.map((item, i) => (
                        <div key={i} className={`db-progress-card ${item.eligible ? 'db-progress-eligible' : ''}`}>
                            <div className="db-progress-top">
                                <span>{item.icon} {item.label}</span>
                                <span className="db-progress-pct">{Math.round(item.pct)}%</span>
                            </div>
                            <div className="db-progress-bar">
                                <div className="db-progress-fill" style={{ width: `${item.pct}%` }} />
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* DIRECTORI */}
            {!isChild && stats.directors && stats.directors.length > 0 && (
                <div className="db-section">
                    <p className="db-section-title">📞 Contacte Directori</p>
                    <div className="db-directors-row">
                        {stats.directors.map(dir => (
                            <a key={dir.id} href={`tel:${dir.phoneNumber}`} className="db-director-chip">
                                <div className="db-director-avatar">
                                    {dir.name?.charAt(0)}{dir.surname?.charAt(0)}
                                </div>
                                <div className="db-director-info">
                                    <div className="db-director-name">{dir.name} {dir.surname}</div>
                                    <div className="db-director-phone">{dir.phoneNumber || '-'}</div>
                                </div>
                            </a>
                        ))}
                    </div>
                </div>
            )}

            {/* NOTIFICARI */}
            <div className="db-section db-section-grow">
                <p className="db-section-title">{isChild ? '✨ Noutăți' : '🔔 Avizier Digital'}</p>
                <div className="db-notifs">
                    {notificationsToDisplay.length === 0 && (!isChild || progressItems.length === 0) ? (
                        <div className="db-empty">
                            <div className="db-empty-icon">📭</div>
                            <div>Nicio notificare momentan.</div>
                        </div>
                    ) : (
                        notificationsToDisplay.map(n => {
                            const isReward = n.type && n.type.includes('ELIGIBLE');
                            return (
                                <div key={n.id} className={`db-notif-card ${isReward ? 'db-notif-reward' : 'db-notif-info'}`}>
                                    <span className="db-notif-dot" />
                                    <span className="db-notif-msg">{n.message}</span>
                                    <button onClick={() => handleDeleteNotification(n.id)} className="db-notif-close">×</button>
                                </div>
                            );
                        })
                    )}
                </div>
            </div>

            {/* VERSE */}
            <div className="db-verse">
                „Isus Hristos este acelaşi ieri şi azi şi în veci!" <strong>Evrei 13:8</strong>
            </div>
        </div>
    );
};

export default Dashboard;
