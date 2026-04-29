import React, { useState, useEffect } from 'react';
import './App.css';
import AwanaLogo from './AwanaLogo';
import AdminDashboard from './AdminDashboard';
import { API_URL } from './config';
import Dashboard from './components/Dashboard';
import MyProfile from './components/MyProfile';
import CalendarManager from './components/CalendarManager';
import DepartmentsList from './components/DepartmentsList';
import Registry from './components/Registry';
import LeadersRegistry from './components/LeadersRegistry';
import Register from './components/Register';
import Login from './components/Login';

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
const SplashScreen = () => (
    <div className="splash-screen">
        <div className="logo-anim"><AwanaLogo width="280px" /></div>
        <p style={{marginTop: '20px', color: 'white', opacity: 0.8}}>Se initializeaza sistemul...</p>
    </div>
);

// ==========================================
// 15. APP MAIN (FINAL SI CORECTAT)
// ==========================================
function App() {
    // 1. Logoul (tinut minte pe sesiune ca sa nu fie enervant)
    const [loading, setLoading] = useState(() => {
        return !sessionStorage.getItem('hasSeenLogo');
    });

    // 2. USER-UL: Il incarcam din memoria permanenta ca sa nu mai ceara login la refresh
    const [user, setUser] = useState(() => {
        const savedUser = localStorage.getItem('awanaLoggedUser');
        return savedUser ? JSON.parse(savedUser) : null;
    });

    const [register, setRegister] = useState(false);

    // 3. PAGINA: Tine minte unde erai inainte de refresh (Registru, Dashboard, etc.)
    const [page, setPage] = useState(() => {
        return localStorage.getItem('awanaCurrentPage') || 'dashboard';
    });

    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

    // ==========================================
    // EFECTE MAGICE: Salvare automata in fundal
    // ==========================================

    // Cand userul se schimba (ex: se logheaza sau delogheaza) -> actualizam memoria
    useEffect(() => {
        if (user) {
            localStorage.setItem('awanaLoggedUser', JSON.stringify(user));
        } else {
            // Daca a dat logout (user e null), stergem datele
            localStorage.removeItem('awanaLoggedUser');
            localStorage.removeItem('awanaCurrentPage');
        }
    }, [user]);

    // Cand schimba pagina -> o tinem minte imediat
    useEffect(() => {
        if (user) {
            localStorage.setItem('awanaCurrentPage', page);
        }
    }, [page, user]);

    // Animatia cu Logo Awana
    useEffect(() => {
        if (loading) {
            const timer = setTimeout(() => {
                setLoading(false);
                sessionStorage.setItem('hasSeenLogo', 'true');
            }, 2000);
            return () => clearTimeout(timer);
        }
    }, [loading]);

    // O functie mica pentru schimbarea paginii (probabil o ai deja mai jos, dar asigura-te ca arata cam asa)
    const navigateTo = (pageName) => {
        setPage(pageName);
        setMobileMenuOpen(false);
    };

    if (loading) return <SplashScreen />;

    if (!user) return register ? <Register onSwitchToLogin={() => setRegister(false)} /> : <Login onLogin={setUser} onSwitchToRegister={() => setRegister(true)} />;

    const isChild = user.hasOwnProperty('parentPhone');
    const isDirector = user.role === 'DIRECTOR' || user.role === 'COORDONATOR';

    return (
        <div className="app-container">

            {/* HEADER MOBIL */}
            <div className="mobile-header">
                <div style={{display:'flex', alignItems:'center', gap:'12px'}}>
                    <button className="hamburger-btn" onClick={() => setMobileMenuOpen(true)}>
                        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
                            <line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/>
                        </svg>
                    </button>
                    <span className="mobile-title">Awana Betania</span>
                </div>
                <div className="sb-avatar" style={{width:'34px', height:'34px', borderRadius:'9px', fontSize:'0.82rem'}}>
                    {user.name.charAt(0)}{user.surname?.charAt(0) || ''}
                </div>
            </div>

            {/* OVERLAY */}
            <div className={`mobile-overlay ${mobileMenuOpen ? 'visible' : ''}`} onClick={() => setMobileMenuOpen(false)}></div>

            {/* SIDEBAR */}
            <div className={`sidebar ${mobileMenuOpen ? 'open' : ''} ${sidebarCollapsed ? 'collapsed' : ''}`}>

                <button className="close-menu-btn" onClick={() => setMobileMenuOpen(false)}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
                        <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                </button>

                {/* LOGO */}
                <div className="sb-logo">
                    <div className="sb-logo-inner">
                        <AwanaLogo width="110px" />
                    </div>
                    <button className="sb-collapse-btn" onClick={() => setSidebarCollapsed(c => !c)} title="Toggle sidebar">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                            {sidebarCollapsed
                                ? <polyline points="9,18 15,12 9,6"/>
                                : <polyline points="15,18 9,12 15,6"/>
                            }
                        </svg>
                    </button>
                </div>

                {/* USER */}
                <div className="sb-user">
                    <div className="sb-avatar">{user.name.charAt(0)}{user.surname?.charAt(0) || ''}</div>
                    <div className="sb-user-info">
                        <div className="sb-user-name">{user.name} {user.surname}</div>
                        <div className="sb-user-role">{isChild ? 'Copil' : (user.role || 'Lider')}</div>
                    </div>
                </div>

                {/* NAV */}
                <nav className="sb-nav">
                    <span className="sb-label">Navigare</span>

                    <button className={`sb-btn ${page==='dashboard'?'active':''}`} onClick={()=>navigateTo('dashboard')}>
                        <svg className="sb-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                            <rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/>
                            <rect x="14" y="14" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/>
                        </svg>
                        <span className="sb-btn-text">Dashboard</span>
                    </button>

                    <button className={`sb-btn ${page==='profile'?'active':''}`} onClick={()=>navigateTo('profile')}>
                        <svg className="sb-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                            <circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/>
                        </svg>
                        <span className="sb-btn-text">Contul Meu</span>
                    </button>

                    {!isChild && (
                        <>
                            <button className={`sb-btn ${page==='calendar'?'active':''}`} onClick={()=>navigateTo('calendar')}>
                                <svg className="sb-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                                    <rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/>
                                </svg>
                                <span className="sb-btn-text">Calendar & Sesiuni</span>
                            </button>

                            <button className={`sb-btn ${page==='departments'?'active':''}`} onClick={()=>navigateTo('departments')}>
                                <svg className="sb-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-4h6v4"/>
                                </svg>
                                <span className="sb-btn-text">Departamente</span>
                            </button>

                            {/* Albume Stickere — ascuns temporar */}

                            <button className={`sb-btn ${page==='registry'?'active':''}`} onClick={()=>navigateTo('registry')}>
                                <svg className="sb-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                                    <circle cx="9" cy="7" r="4"/>
                                    <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>
                                </svg>
                                <span className="sb-btn-text">Registru Copii</span>
                            </button>

                            {isDirector && (
                                <button className={`sb-btn ${page==='leaders'?'active':''}`} onClick={()=>navigateTo('leaders')}>
                                    <svg className="sb-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                                        <rect x="2" y="7" width="20" height="14" rx="2"/>
                                        <path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/>
                                    </svg>
                                    <span className="sb-btn-text">Registru Lideri</span>
                                </button>
                            )}

                            {user.id === 1 && (
                                <button className={`sb-btn sb-btn-admin ${page==='admin'?'active':''}`} onClick={()=>navigateTo('admin')}>
                                    <svg className="sb-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                                    </svg>
                                    <span className="sb-btn-text">Control Center</span>
                                </button>
                            )}
                        </>
                    )}
                </nav>

                {/* LOGOUT */}
                <div className="sb-footer">
                    <button className="sb-btn sb-btn-logout" onClick={() => { setUser(null); window.location.reload(); }}>
                        <svg className="sb-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                            <polyline points="16,17 21,12 16,7"/><line x1="21" y1="12" x2="9" y2="12"/>
                        </svg>
                        <span className="sb-btn-text">Iesire</span>
                    </button>
                </div>
            </div>

            {/* CONTINUT PRINCIPAL */}
            <div className="main-content">
                {page === 'dashboard' && <Dashboard user={user} />}
                {page === 'profile' && <MyProfile user={user} onUpdateUser={setUser} />}
                {/* StickersHub ascuns temporar */}
                {!isChild && page === 'calendar' && <CalendarManager user={user} />}
                {!isChild && page === 'departments' && <DepartmentsList user={user} />}
                {!isChild && page === 'registry' && <Registry user={user} />}
                {!isChild && page === 'leaders' && isDirector && <LeadersRegistry />}

                {/* 👇 LINIA NOUA PENTRU ADMIN 👇 */}
                {!isChild && page === 'admin' && <AdminDashboard currentUser={user} />}
            </div>
        </div>
    );
}

export default App;
