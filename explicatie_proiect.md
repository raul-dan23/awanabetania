# Explicație completă — Awana Betania Web Application

> Acest document este un prompt structurat pentru orice GenAI (sau om) care trebuie să înțeleagă proiectul de la zero, să poată face update-uri, să depaneze bug-uri sau să adauge funcționalitate nouă. Citește-l de la cap la coadă înainte să atingi codul.

---

## 1. CE ESTE APLICAȚIA — CONTEXT

**Awana Betania** este o aplicație web full-stack construită pentru clubul de copii Awana din Timișoara (organizație creștină non-profit). Clubul se întâlnește săptămânal (seara). Sunt ~100 de utilizatori activi: copii (6-18 ani) și lideri adulți voluntari.

Aplicația înlocuiește complet procesele manuale (foi de hârtie, Excel) cu o platformă centralizată. Este live la [awanabetania.eu](https://awanabetania.eu) și rulează non-stop pe un server Ubuntu self-hosted.

---

## 2. STACK TEHNIC

| Layer | Tehnologie |
|---|---|
| Backend | Java 17, Spring Boot 3.x, JPA/Hibernate, Maven |
| Frontend | React 18, Vite, CSS inline/module |
| Database | MySQL (tabele create automat via `ddl-auto=update`) |
| Criptare | AES-128 simetric (cheia din env var `AES_SECRET_KEY`) |
| NFC | Java `javax.smartcardio` (PC/SC), WebSocket, JAR separat |
| Server | Ubuntu Linux, Spring Boot rulează ca serviciu systemd |
| Deploy | Git hook → shell script → rebuild JAR → restart service |

**Structura fișiere backend:**
```
src/main/java/com/awanabetania/awanabetania/
├── AwanaBetaniaApplication.java   — entry point Spring Boot
├── DataInitializer.java           — seed data la startup (departamente, stickere, admin)
├── Model/                         — entități JPA + clase ajutătoare
├── Controller/                    — REST endpoints (@RestController)
└── Repository/                    — Spring Data JPA interfaces
```

**Structura fișiere frontend:**
```
Frontend/src/
├── App.jsx                        — componenta root, gestionează starea globală și routing
├── AdminDashboard.jsx             — Control Center (doar user ID=1)
├── config.js                      — API_URL (diferit local vs producție)
├── hooks/useNfcBridge.js          — WebSocket hook pentru cititor NFC
└── components/
    ├── Login.jsx                  — ecran de autentificare + buton arbitru guest
    ├── Register.jsx               — înregistrare cont nou
    ├── Dashboard.jsx              — pagina principală după login
    ├── Registry.jsx               — registrul copiilor
    ├── LeadersRegistry.jsx        — registrul liderilor
    ├── ScoringWidget.jsx          — widget de punctare per copil
    ├── CalendarManager.jsx        — management întâlniri (seri de club)
    ├── DepartmentsList.jsx        — lista departamente
    ├── DepartmentsPlan.jsx        — planificarea departamentelor pe seri
    ├── TeamsManager.jsx           — manager echipe (polling 5s)
    ├── StickerMap.jsx             — harta stickerelor (gamification progres)
    ├── StickersHub.jsx            — hub stickere (admin)
    ├── MyProfile.jsx              — profil personal
    ├── Magazin.jsx                — Magazin Târg (NFC fair)
    └── Olimpiada.jsx              — sistem arbitraj olimpiadă
```

---

## 3. AUTENTIFICARE ȘI ROLURI

### Cum funcționează login-ul
Nu există JWT, nu există Spring Security activ. Autentificarea este simplă:

1. Browserul trimite `POST /api/auth/login` cu `{ username, password, role }` (plaintext JSON).
2. Serverul caută userul în BD după `username` (sau fallback după `name` pentru conturi vechi).
3. Parola introdusă este criptată cu AES și comparată cu ce e stocat în BD.
4. Dacă match → serverul returnează obiectul complet (Child sau Leader) ca JSON.
5. Browserul salvează obiectul în `localStorage` cu cheia `awanaLoggedUser`.
6. La fiecare refresh, App.jsx citește din `localStorage` și restaurează sesiunea.

**VULNERABILITATE CUNOSCUTĂ:** Nu există token de sesiune cu expirare. Oricine are acces la localStorage poate impersona orice user. JWT este planificat dar neimplementat.

### Criptarea parolelor
Clasa `AESUtil.java` face AES-128 ECB (nu cel mai sigur mod, dar e ce e). Cheia vine din env var `AES_SECRET_KEY` (16 caractere). La înregistrare parola e criptată și stocată. La login parola introdusă e criptată și comparată.

**Compatibilitate backward:** Dacă parola din BD nu e criptată (cont vechi), serverul acceptă parola plaintext și face upgrade automat (salvează versiunea criptată).

### Roluri și tipuri de utilizatori
Există **două tabele separate**: `children` și `leaders`. Nu există un tabel comun de utilizatori.

| Rol | Tabel | Câmp `role` | Ce poate face |
|---|---|---|---|
| `CHILD` | children | N/A | Vede propria fișă, puncte, progres |
| `Lider` | leaders | `"Lider"` | Scorare, bon nou, olimpiadă scoring |
| `Coordonator` | leaders | `"Coordonator"` | Ca Lider + management extins |
| `Director` | leaders | `"Director"` | Toate funcțiile + admin PIN features |
| User ID=1 | leaders | orice | Control Center complet (NFC admin) |

**Important:** Verificarea rolului se face în **frontend** prin `user.role` din localStorage. Nu există verificare server-side pe roluri (cu excepția admin PIN și NFC token).

### Admin PIN
Unele acțiuni sensibile necesită un PIN de admin, configurat în `application.properties` ca `admin.pin`. Endpoints-urile respective verifică header-ul `X-Admin-Pin`. PIN-ul este introdus de user în browser la fiecare acțiune (nu e stocat în localStorage).

### Cod de înregistrare lideri
La înregistrare un lider nou trebuie să introducă un cod fix. Codurile valide sunt hardcodate în `AuthController.java`: `"AWANA2024"`, `"BETANIA"`, `"DIRECTOR_KEY"`.

---

## 4. MODELELE DE DATE (ENTITĂȚI JPA)

### Child (`children`)
Copilul din club. Câmpuri cheie:
- `id`, `name`, `surname`, `username` (unic, generat automat din nume+prenume)
- `password` (AES encrypted)
- `birthDate`, `parentName`, `parentPhone`
- `seasonPoints` — totalul punctelor acumulate în tot sezonul. **Acesta e soldul pentru târg.**
- `dailyPoints` — punctele din seara curentă (resetate la 0 când se închide o întâlnire)
- `attendanceStreak` — câte seri consecutive a fost prezent (resetat la 0 dacă lipsește)
- `totalAttendance` — total prezențe istorice
- `lessonsCompleted` — total lecții recitate
- `lastAttendanceDate` — data ultimei prezențe (folosit la close meeting pentru reset streak)
- `hasManual`, `hasShirt`, `hasHat`, `badgesCount` — inventar fizic
- `currentTeam` — echipa din seara curentă (resetată la close meeting)
- `nfcUid` — UID-ul cardului NFC asociat (unic, poate fi null)
- `isSuspended` — dacă copilul e suspendat (nu poate intra la întâlniri)
- `progressPercent` — procent progres manual Awana
- `progress` (OneToOne → ChildProgress) — starea stickerelor

### Leader (`leaders`)
Liderul/voluntarul. Câmpuri cheie:
- `id`, `name`, `surname`, `username`, `password`
- `role` — string: `"Lider"`, `"Coordonator"`, `"Director"`
- `departments` (ManyToMany → Department) — departamentele la care e alocat
- `rating` — nota medie ca lider
- `phoneNumber`, `notes`

### Score (`scores`)
Punctajul unui copil la O singură seară. Câmpuri:
- `child` (ManyToOne), `meeting` (ManyToOne)
- `attended`, `hasBible`, `hasHandbook`, `lesson`, `friend`, `hasUniform` — boolean
- `extraPoints` — puncte bonus date manual
- `total`, `individualPoints`, `teamPoints`
- `date`, `details` (text generat, ex: `"Prezenta, Biblie, Lectie"`)

**Formula punctaj (calculatePoints în ScoreController):**
```
Prezență:   +1.000 pct
Biblie:     +500 pct
Manual:     +500 pct
Lecție:     +1.000 pct
Prieten:    +1.000 pct
Uniformă:   +10.000 pct   ← DA, uniforma valorează mult
Extra:      +X pct (oricât)
```

### Meeting (`meetings`)
O seară de club. Câmpuri:
- `date`, `description`
- `isCompleted` — false = seara e activă/în desfășurare, true = s-a încheiat
- `directorDay` (ManyToOne → Leader) — liderul responsabil
- `meetingPin` — PIN generat la asignarea secretariatului (folosit pentru acces rapid la scoring)
- `generalRating`, `generalFeedback`

### Department (`departments`)
Departamentele clubului (Lecție, Jocuri, Media, etc.). Create la startup de `DataInitializer`.

### Bon (`bons`)
Bonul de cumpărături de la târg. Câmpuri:
- `child` (ManyToOne) — cine cumpără
- `leaderName` — liderul vânzător (text, nu FK)
- `items` — JSON string cu produsele selectate (ex: `[{"name":"Ciocolată","points":500}]`)
- `totalPoints` — suma în puncte
- `status` — `"PENDING"` | `"APPROVED"` | `"REJECTED"`
- `createdAt`, `approvedAt`

### Product (`products`)
Produsele din magazinul de târg:
- `name`, `pointPrice`, `category`, `available` (boolean)

### OlimpiadaSession (`olimpiada_sessions`)
O sesiune de olimpiadă:
- `name`, `code` (unic, max 10 caractere, uppercase)
- `status` — `"ACTIVE"` | `"CLOSED"`
- `createdAt`

### OlimpiadaScore (`olimpiada_scores`)
Un scor dintr-o rundă a olimpiadei:
- `sessionId`, `roundNumber`, `team` (ROSU/GALBEN/ALBASTRU/VERDE)
- `arbiterName`, `place` (1-4, sau 0 pentru puncte extra)
- `points` — calculat din loc sau acordat manual
- `isDouble` — dacă runda are dublare de puncte
- `note` — notă opțională (doar pentru puncte extra)
- `createdAt`

### Alte modele secundare
- `ChildProgress` — OneToOne cu Child, ține `lastStickerId` și `manualsCount`
- `ChildManual` — câte manuale a primit un copil și statusul lor (`ACTIVE`/`COMPLETED`)
- `Sticker` — stickerele din jocul de progres (create la startup)
- `Warning` — avertizări/suspendări ale copiilor
- `TeamGamePoint` — puncte pe echipe per seară
- `Notification` — notificări interne (ex: copil eligibil pentru tricou)
- `MeetingAssignment` — asignarea liderilor la departamente per seară
- `LeaderEvaluation` — evaluarea liderilor după fiecare seară

---

## 5. CICLUL DE VIAȚĂ AL UNEI SERI DE CLUB

Aceasta este flow-ul principal al aplicației. Tot se învârte în jurul acestui ciclu:

```
1. ADMIN creează o Meeting (POST /api/meetings/add)
   → isCompleted = false
   → devine "seara activă"

2. SECRETAR asignează departamente pentru seara respectivă
   → se generează un meetingPin (SecureRandom) și se salvează pe Meeting
   → PIN-ul e distribuit liderilor pentru acces la scoring

3. LIDERI scorează copiii (POST /api/scores/add)
   → ScoreController găsește Meeting cu isCompleted=false și data cea mai apropiată
   → Calculează punctele
   → Adaugă la child.seasonPoints și child.dailyPoints
   → Dacă copilul e prezent: streak++, totalAttendance++
   → Dacă streak ajunge la 5: notificare "eligibil tricou"
   → Dacă streak ajunge la 10: notificare "eligibil căciulă"

4. DIRECTOR încheie seara (POST /api/meetings/close/{id})
   → isCompleted = true
   → Gestionare suspendări: pentru fiecare copil suspendat prezent, scade remainingMeetings
   → Dacă remainingMeetings ajunge la 0: ridică suspendarea
   → Pentru TOȚI copiii: dailyPoints = 0, currentTeam = null
   → Pentru copiii ABSENȚI (lastAttendanceDate != meetingDate): attendanceStreak = 0
```

**Punct critic de înțeles:** Detecția absenților la close se bazează pe `child.lastAttendanceDate` comparată cu `meeting.date`. De aceea, la scoring, se setează `child.lastAttendanceDate = meeting.getDate()` (data ședinței, nu `LocalDate.now()`). Asta permite directorului să închidă seara și a doua zi fără să reseteze greșit streak-urile.

---

## 6. SISTEMUL NFC — TÂRGUL DE FINAL DE SEZON

### Principiu fundamental
Cardul NFC stochează DOAR UID-ul hardware (ex: `A1B2C3D4`). Nu scriem nimic pe card. Toate datele (puncte, asocieri) sunt în BD.

### Componentele sistemului

**1. nfc-bridge.jar** (proiect Maven separat în `nfc-bridge/`)
- Rulează LOCAL pe laptopul contabilului
- Polling la 300ms pe cititorul PC/SC via `javax.smartcardio`
- APDU pentru citire UID: `FF CA 00 00 00`
- Când detectează un card nou: trimite `{"uid":"A1B2C3D4"}` via WebSocket la `ws://localhost:7000`
- Mod `--test`: nu necesită hardware, poți introduce UID-uri manual din terminal

**2. useNfcBridge.js** (hook React)
- Se conectează la `ws://localhost:7000`
- Reconectare automată la fiecare 3 secunde dacă conexiunea cade
- Expune `{ lastUid, connected }` — componentele îl folosesc pentru auto-completare

**3. Backend NFC endpoints** (protejate cu `X-NFC-Token` din `application.properties`)
- `POST /api/nfc/register` — asociază UID cu un copil
- `GET /api/nfc/{uid}` — returnează datele copilului după UID
- `POST /api/nfc/{uid}/spend` — scade puncte din `seasonPoints`

**4. Backend Admin endpoints** (protejate cu `X-Admin-Pin`)
- `POST /api/admin/nfc-register` — asociază card (folosit din Control Center)
- `DELETE /api/admin/nfc-remove/{childId}` — dezasociază cardul

### Flow complet la târg

**Înainte de târg (Control Center → tab "Carduri NFC"):**
1. Admin pune cardul pe cititor → UID apare automat în câmpul din UI (via WebSocket)
2. Selectează copilul din dropdown
3. Apasă "Asociază" → `POST /api/admin/nfc-register`

**La târg — LIDER VÂNZĂTOR (telefon, fără cititor NFC):**
1. Deschide Magazin → tab "Bon Nou"
2. Caută copilul (după nume)
3. Bifează produsele dorite
4. Apasă "Generează Bon" → `POST /api/bons` cu `status: "PENDING"`

**La târg — LIDER CONTABIL (laptop cu cititor NFC):**
1. Deschide Magazin → tab "Contabil"
2. Copilul pune cardul pe cititor → UID detectat via WebSocket
3. Contabilul vede bonurile PENDING ale copilului respectiv (filtrate după childId)
4. Verifică lista de produse și punctele totale
5. Apasă "Aprobă" → `POST /api/bons/{id}/approve`
   - Serverul verifică dacă `child.seasonPoints >= bon.totalPoints`
   - Dacă da: `child.seasonPoints -= bon.totalPoints`, `bon.status = "APPROVED"`
   - Dacă nu: eroare "Puncte insuficiente"

---

## 7. SISTEMUL OLIMPIADĂ

### Scop
Sistem independent de arbitraj pentru competiția anuală. Doi arbitri (sau mai mulți) scorează aceleași 4 echipe independent, la final se compară totalurile. **Complet izolat** de restul aplicației — nu atinge Child, Score, Meeting etc.

### Punctaj per loc (valori în `OlimpiadaController.BASE_POINTS`)
```
Locul 1: 1.500 pct
Locul 2: 1.000 pct
Locul 3:   500 pct
Locul 4:   300 pct
```
*(Notă: în CLAUDE.md sunt valori diferite — 1000/500/300/100. Valorile reale din cod sunt 1500/1000/500/300.)*

Există și opțiunea **"Rundă Dublă"** (`isDouble: true`) care dublează toate punctele rundei respective.

Există și **"Puncte Extra"** (endpoint `/extra`) care adaugă puncte libere unei echipe fără plasament (roundNumber = 0, place = 0).

### Fluxul de utilizare

**Director creează sesiune:**
1. Olimpiada → tab "Sesiuni" → introduce PIN admin
2. Completează numele și un cod scurt (ex: `OLM26`)
3. `POST /api/olimpiada/sessions` → sesiune cu status `ACTIVE`

**Arbitru cu cont normal:**
1. Login normal → Olimpiada → tab "Scorare"
2. Introduce codul sesiunii
3. Vede interfața cu 4 carduri echipă + butoane Loc 1/2/3/4
4. Pentru fiecare rundă: setează plasamentele și apasă Submit
5. Serverul validează: toate 4 echipe prezente, locuri unice, echipe unice

**Arbitru fără cont (guest):**
1. Pe ecranul de Login: apasă "Intru ca arbitru"
2. Introduce codul sesiunii și numele său
3. App.jsx setează `olimpiadaGuest = { code, arbiterName }` și randează direct `<Olimpiada guestArbiter={...} />`
4. Vede doar tab-ul "Scorare", fără Sesiuni sau Comparație

**Director vede comparația:**
1. Tab "Comparație" → selectează sesiunea
2. `GET /api/olimpiada/session/{code}/compare`
3. Răspuns structurat: per fiecare arbitru → leaderboard + runde detaliate + extra
4. Frontend afișează tabelul, marchează diferențele cu roșu, arată clasamentul final

**Resubmit rundă:** Dacă un arbitru trimite din nou aceeași rundă, serverul șterge scorurile anterioare ale acelui arbitru pentru acea rundă (`deleteBySessionIdAndRoundNumberAndArbiterName`) și salvează noile.

---

## 8. FRONTEND — ARHITECTURA ȘI STATE MANAGEMENT

### Nu există React Router
Navigarea se face complet prin `useState` în App.jsx. Variabila `page` (string) determină ce se randează. Nu există URL-uri per pagină — butonul Back al browserului nu funcționează pentru navigarea internă.

### Starea globală (App.jsx)
```javascript
const [user, setUser] = useState(...)        // null sau obiectul Leader/Child
const [page, setPage] = useState(...)        // "dashboard" | "registry" | "scoring" | etc.
const [olimpiadaGuest, setOlimpiadaGuest] = useState(null) // arbitru guest fără cont
const [register, setRegister] = useState(false) // arată ecranul de înregistrare
```

Persistență:
- `user` → `localStorage.awanaLoggedUser` (rămâne între sesiuni de browser)
- `page` → `localStorage.awanaCurrentPage` (rămâne la refresh)
- Splash screen (logo animat) → `sessionStorage.hasSeenLogo` (o singură dată per tab)

### Cum se verifică rolul în frontend
```javascript
// Verificare simplă după câmpul role din obiectul user
const isDirector = user?.role === 'Director' || user?.role === 'Coordonator';
const isChild = !user?.role; // copiii nu au câmpul role
const isAdmin = user?.id === 1; // doar pentru Control Center
```

### API_URL
Definit în `Frontend/src/config.js`. Local: `http://localhost:8080`. Producție: URL-ul serverului. La deploy, frontend-ul e bunduit în JAR și servit de Spring Boot — deci nu există CORS în producție (aceeași origine).

---

## 9. DEPLOYMENT — PROCESUL CORECT

**Frontend-ul NU este un server separat.** La producție, React este compilat și rezultatul (`dist/`) este copiat în `src/main/resources/static/` al proiectului Spring Boot. Spring Boot servește fișierele statice. Există un `SpaController.java` care redirecționează toate rutele necunoscute la `index.html` (necesar pentru SPA).

```bash
# 1. Build frontend
cd Frontend && npm run build

# 2. Copiază în resources
cp -r dist/* ../src/main/resources/static/

# 3. Build JAR
cd .. && mvn package -DskipTests

# 4. Uploadează pe server și restart
# (procesul exact depinde de scripturile de pe server)
```

**Deploy automat:** Există un Git hook pe server care face pașii de mai sus automat la push pe main.

---

## 10. SECURITATE — STAREA ACTUALĂ ȘI LIMITĂRI

### Ce există
- Parole criptate AES (nu plaintext în BD)
- Admin PIN pentru acțiuni sensibile (creare sesiuni olimpiadă, management produse)
- NFC Token pentru endpoints NFC
- Coduri de înregistrare hardcodate pentru lideri noi

### Ce LIPSEȘTE (vulnerabilități cunoscute)
- **Nu există JWT/token de sesiune** — serverul nu știe cine e logat. Orice request poate veni de la oricine.
- **`@CrossOrigin(origins = "*")`** pe toți controllerii — permite requests din orice origine.
- **Rolul nu e verificat server-side** — un lider poate accesa endpoints de director dacă știe URL-urile.
- **User complet în localStorage** — dacă cineva are acces la browser-ul tău, poate citi sau modifica datele de sesiune.
- **HTTPS nu e forțat** — configurat la nivel de server/nginx, nu în aplicație.

**Planificat:** Spring Security + JWT. Este un task mare neînceput.

---

## 11. CONFIGURARE — application.properties

Variabilele sensibile sunt citite din environment variables pe server:

```properties
spring.datasource.url=...
spring.datasource.username=...
spring.datasource.password=...
spring.jpa.hibernate.ddl-auto=update    # Creează/actualizează tabelele automat

aes.secret.key=${AES_SECRET_KEY}        # 16 caractere, pentru criptare parole
admin.pin=${ADMIN_PIN}                   # PIN-ul pentru acțiuni admin
nfc.token=${NFC_TOKEN}                   # Token pentru endpoints NFC
```

---

## 12. DataInitializer — CE FACE LA STARTUP

`DataInitializer.java` rulează la fiecare pornire a aplicației (`implements CommandLineRunner`). Face lucruri IDEMPOTENTE (verifică dacă există înainte să creeze):

1. **Creează departamentele** dacă nu există: Lecție, Jocuri, Media, Social Media, Sală, Materiale, Secretariat, Agapă — fiecare cu număr minim/maxim de lideri
2. **Creează contul de admin** (Raul Macovei, DIRECTOR) dacă nu există
3. **Creează stickerele** pentru jocul de progres (gamification)
4. **Migrare username-uri** — pentru conturi vechi care nu aveau username, generează unul din `generateCleanUsername(name, surname)`: elimină diacritice, lowercase, concatenează

---

## 13. COMPONENTE FRONTEND — DETALII CHEIE

### ScoringWidget.jsx
Widget-ul de punctare per copil. Afișează un card per copil cu toggle-uri pentru fiecare criteriu. La submit: `POST /api/scores/add`. Folosit în cadrul CalendarManager sau dintr-o pagină dedicată de scoring.

### CalendarManager.jsx
Gestionează întâlnirile (seri de club). Permite:
- Crearea unei noi seri
- Vizualizarea serii active
- Asignarea departamentelor și liderilor
- Închiderea serii (buton "Încheie seara")

### TeamsManager.jsx
Gestionează echipele din seara curentă. Face polling la fiecare 5 secunde (`setInterval`) pentru a actualiza punctajul echipelor în timp real. Aceasta este singura componentă cu polling activ — restul sunt one-shot requests.

### Magazin.jsx
Componenta pentru târg. Are 3 tab-uri:
- **Produse** — management produse (doar Director/Coordonator, necesită PIN)
- **Bon Nou** — liderul vânzător creează bonuri pentru copii
- **Contabil** — liderul contabil aprobă bonuri, cu auto-completare UID via NFC bridge

### Olimpiada.jsx
Componenta pentru olimpiadă. Are 3 tab-uri:
- **Sesiuni** — creare/închidere sesiuni (necesită PIN)
- **Scorare** — arbitrul scorează rundele
- **Comparație** — vizualizarea rezultatelor comparate

### AdminDashboard.jsx
Control Center complet, vizibil doar pentru `user.id === 1`. Include:
- Tab "Carduri NFC" — asociere/dezasociere card per copil
- Status bridge live (conectat/deconectat)
- Alte funcții administrative

---

## 14. PATTERNS ȘI CONVENȚII IMPORTANTE

### Pattern pentru endpoint-uri protejate
```java
@Value("${admin.pin}")
private String adminPin;

private boolean isPinValid(String pin) {
    return adminPin != null && adminPin.equals(pin);
}

@PostMapping("/ceva")
public ResponseEntity<?> doSomething(@RequestHeader(value = "X-Admin-Pin", required = false) String pin) {
    if (!isPinValid(pin)) return ResponseEntity.status(401).body("PIN incorect");
    // logica
}
```

### Pattern pentru BON approval (verifica mereu punctele ÎNAINTE de deducere)
```java
int current = child.getSeasonPoints() != null ? child.getSeasonPoints() : 0;
if (bon.getTotalPoints() > current)
    return ResponseEntity.badRequest().body("Puncte insuficiente. Sold: " + current);
child.setSeasonPoints(current - bon.getTotalPoints());
```

### Username generation
`DataInitializer.generateCleanUsername(name, surname)`:
- Elimină diacritice (ș→s, ț→t, ă→a, â→a, î→i)
- Lowercase
- Elimină caracterele non-alfanumerice
- Concatenează: `"popescuion"` din `("Ion", "Popescu")`

### Detecție seară activă
**Mereu** se face cu: `meetingRepository.findByIsCompletedFalseOrderByDateAsc().stream().findFirst()`
— prima întâlnire nesfinalizată cu data cea mai apropiată. Nu există un flag explicit "activeNow".

---

## 15. UNDE SĂ CAUȚI CÂND FACI MODIFICĂRI

| Vrei să modifici | Fișier principal |
|---|---|
| Formula de punctaj | `ScoreController.java` → `calculatePoints()` |
| Logica de streak/prezență | `ScoreController.java` → `addScore()` |
| Logica de close seară | `MeetingController.java` → `closeMeeting()` |
| Deducere puncte târg | `BonController.java` → `approve()` |
| Punctajul olimpiadei | `OlimpiadaController.java` → `BASE_POINTS` constant |
| Asociere card NFC | `NfcController.java` + `AdminController.java` |
| Ecranul de login | `Frontend/src/components/Login.jsx` |
| Routing/stare globală | `Frontend/src/App.jsx` |
| URL API | `Frontend/src/config.js` |
| Tabele BD (structură) | Modelele din `Model/` (ddl-auto=update le recreează) |
| Date inițiale | `DataInitializer.java` |
| Criptare parole | `Model/AESUtil.java` |

---

## 16. LUCRURI DE ȘTIUT ÎNAINTE SĂ ADAUGI FUNCȚIONALITATE

1. **Nu există React Router** — dacă adaugi o pagină nouă, adaugi un string nou în `page` state-ul din App.jsx și un `if (page === 'noua-pagina')` în render.

2. **ddl-auto=update** — dacă adaugi un câmp nou la un model JPA, Hibernate îl adaugă automat în BD la restart. Dacă schimbi tipul sau constrângerea unui câmp existent, poate eșua — în acel caz trebuie migrare manuală sau `ddl-auto=create-drop` (ATENȚIE: șterge datele).

3. **Frontend în JAR** — la orice modificare frontend, trebuie rebuildit și deploiat JAR-ul complet. Nu există hot reload în producție.

4. **Starea user-ului vine din localStorage** — dacă modifici structura unui model (ex: adaugi un câmp la Child), userul deja logat are obiectul vechi în localStorage. El se actualizează doar la next login. Dacă câmpul nou e critic, poate fi nevoie de un endpoint de refresh profil.

5. **AES_SECRET_KEY trebuie să fie exact 16 caractere** — altfel criptarea eșuează silențios și parola devine `null`.

6. **Olimpiada BASE_POINTS** — valorile din cod (1500/1000/500/300) diferă de cele din documentație (1000/500/300/100). Codul e cel corect.

7. **Rundă dublă la olimpiadă** — `isDouble=true` dublează punctele la salvare (nu la afișare). Deci în BD sunt deja stocate valorile duble.
