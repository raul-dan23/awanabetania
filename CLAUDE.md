# CLAUDE.md — AwanaBetania

## Stack
- **Backend:** Spring Boot 3.5.16, Java 17, JPA/Hibernate, Flyway (MySQL 8)
- **Frontend:** React + Vite (folder `Frontend/`)
- **Server:** Ubuntu Linux (remote)
- **IDE:** IntelliJ IDEA

## Structura backend
```
src/main/java/com/awanabetania/awanabetania/
├── Controller/  → doar HTTP: primeste DTO (@Valid), cheama serviciul, intoarce DTO
├── Service/     → regulile de business + @Transactional          (Faza 2, in curs)
├── Dto/         → record-uri request/response (forma JSON exacta)  (Faza 2, in curs)
├── Exception/   → ApiException + GlobalExceptionHandler (ProblemDetail)
├── Security/    → JWT, AuthUser, AdminPinVerifier, SharedSecrets
├── Model/       → entitati JPA: Child, Score, Meeting, Leader, Bon, BonStatus...
├── Repository/  → Spring Data JPA
└── AwanaBetaniaApplication.java
```
Conventiile pentru cod nou sunt in sectiunea „FAZA 2” de mai jos.

## Modele cheie
- **Child** — `id`, `name`, `surname`, `seasonPoints`, `dailyPoints`, relatii cu `Score`, `ChildProgress`, `ChildManual`
- **Score** — punctajul unui copil per intalnire (`attended`, `hasBible`, `lesson`, `extraPoints`, `total`, `date`)
- **Meeting** — o seara/intalnire a clubului

---

## SISTEM NFC TÂRG FINAL DE SEZON — IMPLEMENTAT ✅

### Principiu cheie — cardul stochează DOAR UID-ul
Nu scriem nimic pe card. Fiecare card NFC are un UID hardware unic din fabrică (ex: `A1B2C3D4`).
Citim UID-ul și îl mapăm la copil în BD. Toate punctele rămân în BD, nu pe card.

### Arhitectura finală
```
[Card NFC fizic]
       │
[Cititor USB NFC - PC/SC]
       │ javax.smartcardio
[nfc-bridge.jar — rulează local]  ←── java -jar nfc-bridge.jar
       │ WebSocket ws://localhost:7000
[Browser React - aplicația web]
       │ HTTP REST
[Ubuntu Server - Spring Boot]
       │
[MySQL — scade punctele din Child.seasonPoints]
```

### Flux la târg
1. **Înainte de târg** — Admin deschide Control Center → tab "Carduri NFC", pune cardul pe cititor → UID se completează automat, apasă Asociază
2. **La târg — Lider vânzător** (telefon) → Magazin → Bon Nou → caută copil → bifează produse → Generează bon
3. **La târg — Lider contabil** (laptop cu cititor NFC) → Magazin → Contabil → pune cardul copilului → se filtrează bonul lui → Aprobă → puncte scăzute

---

## CE ESTE IMPLEMENTAT

### Backend
| Fișier | Descriere |
|---|---|
| `Model/Child.java` | Câmp `nfc_uid` (unique) |
| `Controller/NfcController.java` | `POST /api/nfc/register`, `GET /api/nfc/{uid}`, `POST /api/nfc/{uid}/spend` — protejate cu `X-NFC-Token` |
| `Model/Product.java` | Produse magazin: `name`, `pointPrice`, `category`, `available` |
| `Model/Bon.java` | Bon de cumpărături: `child`, `leaderName`, `items` (JSON), `totalPoints`, `status` |
| `Controller/ProductController.java` | CRUD produse — GET public, POST/PUT/DELETE cu `X-Admin-Pin` |
| `Controller/BonController.java` | `POST /api/bons`, `GET /api/bons/pending`, `GET /api/bons/all`, `POST /api/bons/{id}/approve`, `POST /api/bons/{id}/reject` |
| `Controller/AdminController.java` | `POST /api/admin/nfc-register`, `DELETE /api/admin/nfc-remove/{childId}` — cu `X-Admin-Pin` |
| `Repository/ProductRepository.java` | Acces BD produse |
| `Repository/BonRepository.java` | `findBySeasonIdAndStatusOrderByCreatedAtDesc`, `findBySeasonIdOrderByCreatedAtDesc` (doar sezonul curent) |

**Schema:** tabelele `products` și `bons` sunt în `db/migration/V1__baseline.sql` (vezi secțiunea INFRASTRUCTURĂ).

### Frontend
| Fișier | Descriere |
|---|---|
| `Frontend/src/components/Magazin.jsx` | Secțiune nouă în sidebar cu 3 tab-uri: Produse / Bon Nou / Contabil |
| `Frontend/src/hooks/useNfcBridge.js` | Hook WebSocket cu reconectare automată la 3s |
| `Frontend/src/AdminDashboard.jsx` | Tab nou "Carduri NFC" — asociere/eliminare card per copil, status bridge live |
| `Frontend/src/App.jsx` | "Magazin Târg" în sidebar pentru toți liderii |

**Acces pe roluri:**
- Copii: nu văd Magazin
- Lideri: Bon Nou + Contabil
- Director/Coordonator: + tab Produse (cu admin PIN)
- User ID=1: + Control Center → Carduri NFC

### NFC Bridge (proiect separat)
**Locație:** `nfc-bridge/` — Maven project, JAR executabil

```bash
# Build
cd nfc-bridge && mvn package

# Rulare normală (cu cititor fizic)
java -jar nfc-bridge/target/nfc-bridge.jar

# Rulare în mod test (fără hardware — trimiți UID-uri manual din terminal)
java -jar nfc-bridge/target/nfc-bridge.jar --test
```

**Cum funcționează:**
- Polling PC/SC la 300ms (javax.smartcardio — built-in în Java)
- APDU pentru UID: `FF CA 00 00 00`
- WebSocket server pe `ws://localhost:7000`
- Trimite `{"uid":"A1B2C3D4"}` la toate browserele conectate
- Browserul auto-completează UID-ul în formularul activ

---

## OLIMPIADA AWANA — IMPLEMENTAT ✅

### Principiu
Sistem de arbitraj independent pentru competitia anuala a clubului. 2 arbitri scoreaza aceleasi 4 echipe independent (ca doi arbitri), la final se compara totalurile. Complet izolat de datele clubului (nu atinge Child, Leader, Score etc.).

### Echipe fixe
Mereu exact 4: `ROSU`, `GALBEN`, `ALBASTRU`, `VERDE`

### Punctaj per loc
- Locul 1 = **1500 pct**
- Locul 2 = **1000 pct**
- Locul 3 = **500 pct**
- Locul 4 = **300 pct**

_(valori definite în `OlimpiadaController.BASE_POINTS`)_

### Flux utilizare
1. **Director** → Olimpiada → Sesiuni → PIN admin → creeaza sesiune cu cod scurt (ex: `OLM26`)
2. **Arbitru cu cont** → Olimpiada → Scorare → introduce codul → scoreaza runde
3. **Arbitru fara cont** → Login → "Intru ca arbitru" → cod sesiune + numele sau → scoreaza runde
4. **Director** → Olimpiada → Comparatie → vede totaluri per echipa per arbitru, diferentele marcate rosu, clasament final

### Backend — fisiere noi
| Fisier | Descriere |
|---|---|
| `Model/OlimpiadaSession.java` | Sesiune: `name`, `code` (unique, max 10 car), `status` (ACTIVE/CLOSED), `createdAt` |
| `Model/OlimpiadaScore.java` | Scor: `sessionId`, `roundNumber`, `team`, `arbiterName`, `place` (1-4), `points` |
| `Repository/OlimpiadaSessionRepository.java` | `findByCode`, `findAllByOrderByCreatedAtDesc` |
| `Repository/OlimpiadaScoreRepository.java` | `findBySessionId`, `deleteBySessionIdAndRoundNumberAndArbiterName`, `existsBySessionIdAndRoundNumberAndArbiterName` |
| `Controller/OlimpiadaController.java` | Toate endpoint-urile Olimpiada |

**Endpoint-uri:**
- `POST /api/olimpiada/sessions` (X-Admin-Pin) — creeaza sesiune
- `GET /api/olimpiada/sessions` (X-Admin-Pin) — lista sesiuni
- `POST /api/olimpiada/sessions/{id}/close` (X-Admin-Pin) — inchide sesiunea
- `GET /api/olimpiada/session/{code}` — info sesiune (public)
- `GET /api/olimpiada/session/{code}/round-status` — verifica daca arbitrul a trimis deja un tur
- `POST /api/olimpiada/session/{code}/score` — trimite scorurile unui tur (public)
- `GET /api/olimpiada/session/{code}/compare` — totaluri + clasament (public)
- `DELETE /api/olimpiada/session/{code}/round/{round}/arbiter/{name}` (X-Admin-Pin) — sterge un tur

**Tabele:** `olimpiada_sessions`, `olimpiada_scores` (în `V1__baseline.sql`)

### Frontend — fisiere noi/modificate
| Fisier | Descriere |
|---|---|
| `Frontend/src/components/Olimpiada.jsx` | Componenta principala cu 3 taburi |
| `Frontend/src/components/Login.jsx` | Buton "Intru ca arbitru" cu form cod+nume |
| `Frontend/src/App.jsx` | `olimpiadaGuest` state, buton Olimpiada in sidebar, routing |

**Tab-uri Olimpiada.jsx:**
- **Sesiuni** — necesita PIN admin; creeaza/inchide sesiuni
- **Scorare** — introduce cod sesiune, apoi 4 carduri echipa cu butoane Loc 1/2/3/4 (stil identic cu ScoringWidget), submit per runda
- **Comparatie** — tabel totaluri per echipa per arbitru, diferente marcate rosu, clasament final cu medalii

**Acces pe roluri:**
- Arbitru guest (fara cont): doar tabul Scorare
- Lideri/Directori cu cont: toate 3 taburile (Sesiuni necesita PIN)

### Deploy
Pe server: `cd /var/www/html && ./deploy.sh`. Pasii: pull → backup BD → build →
pre-flight (JAR-ul nou pornit fara web: migrari + validare schema, cu site-ul inca pe
versiunea veche) → restart → health + verificarea commit-ului → rollback automat daca
nu porneste. Detalii si cazuri de eroare: `docs/OPERATIONS.md`.

**Infrastructura (important):**
- Aplicatia ruleaza ca serviciu systemd: `awanabetania.service`, `Restart=always`
- `WorkingDirectory=/var/www/html` (setat prin drop-in in
  `/etc/systemd/system/awanabetania.service.d/override.conf`)
- Configuratia de productie sta in `/var/www/html/application.properties` —
  **netrackat in git**, contine credentialele reale. Spring il citeste din
  directorul de lucru si suprascrie valorile din JAR. Daca lipseste, aplicatia
  nu porneste. Backup: `~/application.properties.backup`
- `deploy.sh` NU porneste procesul direct. Versiunea veche facea `pkill` +
  `nohup java -jar`, ceea ce crea un proces orfan in afara systemd — site-ul
  a fost servit 12 zile de un astfel de orfan, iar systemd repornea in bucla
  fara sa poata ocupa portul.

Comenzi utile:
```bash
sudo systemctl status awanabetania
sudo journalctl -u awanabetania -f
pgrep -cf 'awanabetania.*jar'      # trebuie sa fie exact 1
curl -s localhost:8080/actuator/info   # commit-ul care ruleaza
```

---

## INFRASTRUCTURĂ & LIVRARE (Faza 1, oct. 2026) — IMPLEMENTAT ✅

### Schema bazei de date — Flyway
- Schema o modifica DOAR fisierele `src/main/resources/db/migration/V<n>__*.sql`.
  Hibernate are `ddl-auto=validate`: verifica la pornire si refuza sa porneasca la nepotrivire.
- **Camp nou intr-o entitate ⇒ migrare noua**, altfel pica `DatabaseMigrationTest`.
- **Nu edita o migrare care a rulat** (Flyway ii verifica checksum-ul). Nu edita `V1__baseline.sql`.
- `V1__baseline.sql` = schema creata de ddl-auto=update (SHOW CREATE TABLE, MySQL 8.0).
  In productie NU ruleaza: `baseline-on-migrate=true` marcheaza schema existenta ca v1.
  Verificat prin reluarea istoricului entitatilor (toate cele 10 versiuni din git) cu
  ddl-auto=update, apoi validare cu versiunea noua.
- Doar adauga in acelasi deploy (expand/contract): versiunea veche trebuie sa mearga pe
  schema noua, pentru pre-flight si rollback. Un singur ALTER pe fisier (MySQL nu anuleaza DDL).

### Actuator
- Publice: `GET /actuator/health` (UP/DOWN, fara detalii) si `GET /actuator/info` (git commit
  + build). Restul endpoint-urilor nu sunt expuse; lista `/actuator` e dezactivata.
- `server.forward-headers-strategy=native`: in spatele proxy-ului, aplicatia vede HTTPS
  (deci trimite HSTS) si IP-ul real al clientului.

### Configurare
- Baza (`application.properties`) = productie, sigura implicit; secretele din variabile de mediu.
- Local: `cp .env.example .env` (importat optional, ignorat de git). Lista completa a variabilelor
  si explicatiile sunt in `.env.example`.
- Teste: profilul `test` (H2, Flyway oprit). NU adauga `application-dev.properties` in git: e in
  `.gitignore` si git suprascrie fara avertisment fisierele ignorate la pull.
- Frontend: `VITE_API_URL` in `Frontend/.env.development` (`/api`, proxy Vite spre :8080) si
  `Frontend/.env.production` (URL-ul de productie). Override local: `.env.development.local`.

### CI — `.github/workflows/ci.yml`
- backend: `./mvnw verify` (inclusiv MySQL 8 in Docker prin Testcontainers);
  frontend: `npm ci`, `lint`, `build` (Node din `Frontend/.nvmrc`); nfc-bridge: build.
- Dependabot saptamanal (`.github/dependabot.yml`), minor/patch grupate.
- Lint-ul frontend trebuie sa aiba 0 erori (warning-urile `exhaustive-deps` raman; corectarea lor
  orbeste poate crea bucle de request-uri).

### Backup — `scripts/`
- `backup-db.sh [eticheta]` → `~/awanabetania-backups/*.sql.gz`, scriere atomica, credentialele din
  `application.properties` (fara parola in linia de comanda).
- Setari in `/var/www/html/application.properties` (citite de script, deci valabile pentru orice
  backup): `backup.rclone-remote` (copie pe Google Drive cu rclone) si `backup.keep-local`
  (cate raman pe server, implicit 10; cel nou ramane mereu).
- Cod de iesire 3 = backup facut pe server, copia pe Drive a esuat: `deploy.sh`, `restore-db.sh` si
  sezonul nou continua cu un avertisment. Orice alt cod != 0 = niciun backup, deci se opresc.
- `restore-db.sh <fisier>` → confirmare, backup pre-restore, stop serviciu, inlocuire tabele, start.
  Restaureaza dintr-o copie temporara: backup-ul pre-restore poate sterge fisierul sursa (keep-local).
- Automat: inainte de deploy (`pre-deploy`), inainte de un sezon nou (`pre-season`, din aplicatie:
  `Service/BackupService`, proprietatea `backup.script`; esec → 503 si sezonul nu porneste) si,
  optional, zilnic: `ops/systemd/awanabetania-backup.{service,timer}` (03:17). Instalare: `docs/OPERATIONS.md`.

---

## FAZA 2 — ARHITECTURA PE STRATURI (in curs, oct. 2026)

Se face pe zone, cate un PR, fiecare cu teste. **Gata: Magazin** (bonuri, produse, carduri NFC,
endpoint-urile NFC din Control Center). Urmeaza: Scorare/Intalniri, Copii/Lideri/Cont,
Olimpiada, Departamente/Echipe/Feedback/Notificari/Dashboard/Avertismente.

### Conventii (obligatorii pentru cod nou sau refacut)
- **Controller → Service → Repository.** Controllerul nu contine logica; serviciul are
  `@Transactional`. Injectie prin constructor (`@RequiredArgsConstructor`), nu `@Autowired` pe campuri.
- **DTO-uri** (record-uri in `Dto/`): request cu `@Valid` + `@NotNull/@Positive/@Size`; response cu
  `from(entity)`. Nu intoarce entitati JPA. **Pastreaza forma JSON** pe care o citeste frontend-ul.
  Fara `Map<String,Object>` in `@RequestBody` (cast-urile dadeau 500 la input gresit).
- **Erori:** arunca `ApiException.badRequest/forbidden/notFound/conflict(...)`.
  `GlobalExceptionHandler` le transforma in ProblemDetail (`application/problem+json`, cu `detail`);
  input invalid/JSON stricat → 400, niciodata 500. `Frontend/src/auth.js` transforma raspunsul in
  textul din `detail`, deci `res.text()` din ecrane arata mesajul normal.
- **Niciodata 401 pentru erori de business** (PIN gresit etc.): frontend-ul trateaza 401 ca sesiune
  expirata si delogheaza. PIN gresit = 403.
- **PIN admin:** `AdminPinVerifier.verify(pin)`; secretele se compara cu `SharedSecrets.matches`
  (timp constant). Header-ul PIN e `required = false`, ca lipsa lui sa dea 403, nu 400.
- **Puncte si stari:** niciodata citeste-modifica-salveaza. `PointsService.spend` face un singur
  `UPDATE ... WHERE season_points >= :suma`; tranzitiile de stare (bon PENDING→APPROVED) sunt
  `UPDATE ... WHERE status = 'PENDING'` si se verifica numarul de randuri. Verificat pe MySQL:
  logica veche aproba acelasi bon de 8 ori din 8 la cereri simultane.
- **Enum-uri** in loc de string-uri magice (`BonStatus`). Pe coloane VARCHAR existente:
  `@Enumerated(STRING) @JdbcTypeCode(SqlTypes.VARCHAR)`, altfel `validate` cere coloana ENUM.

### Teste
- `Shop/ShopApiTest` (H2, MockMvc): forma JSON, 400/403/404/409, rollback la sold insuficient.
- `Shop/ShopConcurrencyTest` (MySQL 8 in Docker): aprobari simultane, sold niciodata negativ.
- `Account/GoogleSignInTest`, `Account/PasswordResetTest`: vezi sectiunea CONTURI.
- `Season/SeasonTest`, `Season/SeasonMySqlTest`, `Season/SeasonBackupTest`: vezi sectiunea SEZOANE.
- `Service/BackupServiceTest`: codurile de iesire ale scriptului de backup (cu scripturi de proba).
- Total: 78 de teste (`./mvnw verify`).

### De decis
- `/api/products` si `/api/nfc/**` nu sunt folosite de frontend (Magazinul lucreaza cu calculator,
  bridge-ul doar cu WebSocket). Candidati la stergere: mai putina suprafata de atac.

---

## AUTENTIFICARE JWT — IMPLEMENTAT ✅

### Principiu
Fiecare cerere catre API poarta un token semnat pe server. Rolul nu mai vine din
`localStorage` (unde utilizatorul il putea edita), ci din token, unde e semnat criptografic.

```
Login ──> POST /api/auth/login ──> { token, user }
                                      │
  toate cererile ulterioare: Authorization: Bearer <token>
                                      │
              JwtAuthFilter verifica semnatura si expirarea
                                      │
          SecurityConfig decide: public / autentificat
```

### Backend — fisiere noi
| Fisier | Descriere |
|---|---|
| `Security/JwtService.java` | Emite si valideaza token-uri (HMAC-SHA256, expirare 12h) |
| `Security/JwtAuthFilter.java` | Citeste `Authorization: Bearer`, populeaza contextul de securitate |
| `Security/SecurityConfig.java` | Regulile de acces + CORS + bean-ul BCrypt |

### Ce s-a schimbat
- **Parole:** BCrypt in loc de AES reversibil. Conturile vechi se migreaza automat la
  primul login (`matchesAndUpgrade`) — nimeni nu trebuie sa-si schimbe parola.
- **Raspunsul de login:** nu mai contine parola (`@JsonProperty(WRITE_ONLY)` pe `Child` si `Leader`).
- **Coduri de inregistrare:** eliminate. Copiii se inscriu singuri; liderii sunt adaugati de
  director si intra cu Google (vezi sectiunea CONTURI). Codurile vechi (`AWANA2024`, `BETANIA`,
  `DIRECTOR_KEY`) erau publice pe GitHub.
- **CORS:** `@CrossOrigin(origins="*")` sters din toate cele 17 controllere; acum
  o singura configuratie centrala, limitata la `CORS_ALLOWED_ORIGINS`.
- **Default deny:** orice ruta nelistata ca publica cere token. Un controller
  adaugat maine e protejat automat.

### Rute publice (singurele)
```
POST /api/auth/login, /api/auth/register (doar copii), /api/auth/google
GET  /api/auth/config                             (Client ID-ul Google, public)
GET  /api/olimpiada/session/{code}              ─┐
GET  /api/olimpiada/session/{code}/round-status  │ arbitru invitat,
GET  /api/olimpiada/session/{code}/compare       │ nu are cont
POST /api/olimpiada/session/{code}/score         │
POST /api/olimpiada/session/{code}/extra        ─┘
     /api/nfc/**                                  (are propriul X-NFC-Token)
```

### Autorizare pe roluri (audit oct. 2026)
Oricine isi poate crea cont de copil fara cod, deci „autentificat” nu inseamna „de incredere”.
- `ROLE_CHILD` ajunge DOAR la: `GET /api/stickers`, `GET /api/dashboard/stats`,
  `GET|PUT|DELETE /api/children/{id}` (doar propriul id), `POST /api/account/request-deletion`,
  `POST /api/account/password`.
- `/api/admin/**` — doar Director/Coordonator (plus PIN).
- Restul — `ROLE_LEADER`.
- Identitatea vine din token: `AuthUser.current()`. Nu folosi niciodata `id`/`role`/`leaderId`
  trimise de client. Id-urile de copil si lider se suprapun — compara mereu si `kind`.
- `JwtAuthFilter` verifica la fiecare cerere ca contul exista; rolul liderului vine din BD.
- Editare/stergere lider: doar proprietarul sau un director. Codurile master
  (`AWANA2024`, `BETANIA`, `ADMIN`) au fost eliminate.
- `deletionCode` e `@JsonIgnore` pe `Child` si `Leader`.
- Teste: `Security/SecurityAuditTest.java` (14 scenarii de atac).

### Frontend
| Fisier | Descriere |
|---|---|
| `Frontend/src/auth.js` | Wrapper global peste `fetch`: ataseaza tokenul la fiecare apel catre API, iar la 401 curata sesiunea si reincarca |
| `Frontend/src/main.jsx` | Instaleaza wrapper-ul inainte de montarea React |

Wrapper-ul global evita modificarea celor ~80 de apeluri `fetch` individuale si face ca
orice apel adaugat ulterior sa fie autentificat automat.

### ⚠️ Variabile de mediu NOI — obligatorii la deploy
Fara ele aplicatia **nu porneste**:
```bash
export JWT_SECRET='...'                  # minim 32 caractere
export CORS_ALLOWED_ORIGINS='https://awana.betania-tm.ro'
```
Optional: `GOOGLE_CLIENT_ID` (`auth.google.client-id`); fara el butonul Google nu apare.
`AUTH_REGISTRATION_CODES` nu mai e folosita.
`AES_SECRET_KEY` ramane necesara — migreaza parolele vechi. Se poate scoate dupa ce
toti utilizatorii s-au logat macar o data.

### Teste
`src/test/java/.../Security/SecurityIntegrationTest.java` — 9 teste pe H2 in-memory:
acces anonim respins, token falsificat respins, login functional, parola absenta din
raspuns, migrare AES→BCrypt, liderii nu se pot inregistra singuri, rutele de arbitru
invitat inca publice. Ruleaza cu `./mvnw verify`.

---

## CONTURI: GOOGLE PENTRU LIDERI, RESETARE PAROLA — IMPLEMENTAT ✅

### Google (doar lideri)
- Flux: butonul Google Identity Services din browser → `credential` (ID token, JWT semnat de
  Google) → `POST /api/auth/google` → serverul il verifica → acelasi `{ token, user }` ca la login.
- Verificare (`Security/GoogleIdTokenVerifier`): semnatura cu cheile publice Google (JWKS),
  `iss`, `aud` = Client ID-ul nostru, expirare, `email_verified`. Doar Client ID, fara client secret.
- **Google e optional.** Fara `auth.google.client-id`: nu apare nimic despre Google (buton, card in
  profil, rand in Control Center — `Frontend/src/googleConfig.js`, `useGoogleSignIn()`), iar
  `/api/auth/google` da 404.
- **Doar pe invitatie:** directorul adauga liderul (`POST /api/admin/leaders`). Cu Google configurat
  si adresa data: fara parola. Altfel raspunsul contine `temporaryPassword` (aratata o data in cardul
  liderului, schimbata obligatoriu la prima intrare).
  Prima intrare leaga `google_sub` (id-ul permanent Google); urmatoarele cauta dupa el.
  Cont Google necunoscut → 403. Liderii existenti isi leaga contul din „Contul Meu”
  (`POST /api/account/google`) sau directorul le pune adresa (`PUT /api/admin/leaders/{id}/email`,
  care sterge legatura veche). Parola lor veche continua sa mearga.
- `/api/auth/register` accepta doar copii; liderii primesc 403.
- Copiii nu folosesc Google (multi nu au cont Google propriu).
- Pasii din Google Cloud Console + depanare: `docs/OPERATIONS.md`, sectiunea 8.

### Resetare parola (in loc de „arata parola”)
- BCrypt nu se decripteaza, deci parolele nu se pot afisa; `/api/admin/decrypt-password` e sters.
- `POST /api/admin/reset-password` (director + PIN) → parola temporara `xxxx-xxxx`, aratata o data
  in Control Center. Contul primeste `password_change_required`; login-ul intoarce
  `mustChangePassword` si `Login.jsx` cere parola noua (`ChangePassword.jsx`) inainte de aplicatie.
- `POST /api/account/password` — schimbarea propriei parole, cu parola curenta (gresita = 403).
- O singura regula pentru parole: 6–64 caractere (`PasswordService`).

### Fisiere
| Fisier | Descriere |
|---|---|
| `Security/GoogleIdTokenVerifier.java`, `GoogleSignInConfig.java` | Verificarea ID token-ului Google |
| `Service/GoogleSignInService.java` | Intrare cu Google + legarea contului din profil |
| `Service/LeaderAccountService.java` | Adaugare lider pe invitatie, schimbarea adresei |
| `Service/PasswordService.java` | Hash, regula de lungime, resetare, schimbare |
| `Frontend/src/components/GoogleSignInButton.jsx` | Butonul oficial Google (script incarcat o data) |
| `Frontend/src/components/ChangePassword.jsx` | Parola noua dupa o resetare |
| Migrari `V2`–`V4` | `password_change_required`; `leaders.email` + `leaders.google_sub` (unice) |

### Teste
`Account/GoogleSignInTest` (token-uri semnate cu o cheie RSA generata, verificate cu validatorul
din productie: semnatura straina, alt `aud`, expirat, email neverificat, cont neinvitat, legare;
copiii nu vad adresele Google ale directorilor in `/api/dashboard/stats`), `Account/NoGoogleTest`
(aplicatia fara Client ID: liderul adaugat primeste parola temporara) si `Account/PasswordResetTest`.


---

## SEZOANE — IMPLEMENTAT ✅

### Principiu
Un sezon = un an de club. Copiii si liderii raman de la un sezon la altul; datele de sezon
pornesc de la zero, iar sezonul incheiat ramane de citit (nu se sterge nimic).
- Tabel `seasons` (`name` unic, `start_date`, `end_date`, `status` ACTIVE/CLOSED). Mereu exact un ACTIVE.
- `season_id` pe `scores`, `meetings`, `leader_evaluations`, `warnings`, `bons`. Orice cod nou care
  creeaza randuri in tabelele astea pune `seasonId = seasonService.currentId()`, iar listele filtreaza
  pe sezonul curent. Nu folosi interogari fara sezon pe ele (cele vechi au fost sterse).
- Contoarele tinute pe `Child` (puncte, streak, prezente...) se copiaza in `season_child_results` la
  inchidere, apoi se pun pe zero. Punctele castigate/cheltuite si avertismentele se numara din
  randurile sezonului, care raman in BD.

### Sezon nou (Control Center → Sezoane, director + PIN)
`SeasonService.startNew`: intai backup-ul bazei (`BackupService`, eticheta `pre-season`; daca esueaza,
503 si nu se schimba nimic), apoi o singura tranzactie:
- **Se reseteaza:** `season_points`, `daily_points`, streak, prezente, lectii, `last_attendance_date`,
  insigne, `has_manual/has_shirt/has_hat`, suspendari, echipa; `child_progress.manuals_count`;
  lista `child_manual` (copiata ca JSON in arhiva); `leaders.rating`; notificarile FEEDBACK,
  SHIRT_ELIGIBLE, HAT_ELIGIBLE (ascunse); bonurile PENDING (respinse).
- **Raman:** copiii, liderii, datele de contact, conturile, cardurile NFC, stickerele
  (`child_progress.last_sticker_id`), anunturile, cererile de stergere a contului.
- Intalnirile neinchise trec in sezonul nou. **Blocat** cat timp o intalnire neinchisa are punctaje.
- Protectie la dublu-click / doi directori: cererea poarta `currentSeasonId`; inchiderea e
  `UPDATE ... WHERE status = 'ACTIVE'` cu verificarea numarului de randuri (vezi conventiile Faza 2).

### Primul sezon si deploy-ul
- Migrarile V5–V11 doar adauga (coloane `season_id` nullable). Primul sezon il creeaza
  `SeasonStartup` la pornire (inclusiv in pre-flight): numit dupa anul de club al primei intalniri
  (ex. `2025–2026`), cu toate datele existente. Directorul il poate redenumi.
- La fiecare pornire, randurile fara sezon (scrise de versiunea veche in pre-flight sau dupa un
  rollback) intra in sezonul activ.

### Endpoint-uri
- `GET /api/seasons` — lista (lideri); `GET /api/seasons/{id}/results` — clasamentul unui sezon incheiat
  (409 cat ruleaza).
- `GET /api/admin/seasons/preview`, `POST /api/admin/seasons` `{currentSeasonId, name}`,
  `PUT /api/admin/seasons/{id}` `{name}` — director + PIN.
- `GET /api/dashboard/stats` contine `season` (numele sezonului curent).

### Fisiere
| Fisier | Descriere |
|---|---|
| `Model/Season.java`, `SeasonStatus.java`, `SeasonChildResult.java` | Sezonul si arhiva per copil |
| `Service/SeasonService.java`, `SeasonStartup.java` | Sezon nou, arhiva, primul sezon, preluarea randurilor fara sezon |
| `Controller/SeasonController.java`, `SeasonAdminController.java` | Citire (lideri) / administrare (director + PIN) |
| `Frontend/src/components/SeasonsPanel.jsx` | Control Center → Sezoane: sezonul curent, sezon nou, rezultate |

### Teste
- `Season/SeasonTest` (H2): scenariu complet prin endpoint-uri (punctaj, avertisment, evaluare, bon)
  → sezon nou → tot ce trebuie e pe zero, nimic nu e sters, arhiva corecta; dublu-click; intalnire
  deschisa; PIN/rol/nume; copiii nu vad sezoanele; preluarea randurilor fara sezon.
- `Season/SeasonMySqlTest` (MySQL 8): 4 directori simultan → un singur sezon nou, arhiva o singura data.

---

## Referință: proiectul Awana-2 (C# WinForms)
Locație: `~/Downloads/Awana-2/`
Același concept dar mai vechi: stoca punctele PE CARD. Fișiere relevante pentru APDU: `AWANAcard.cs`, `CardInfo.cs`.

---

## Probleme de îmbunătățit

### ✅ REZOLVATE

#### Query-uri de performanță (findAll → query direct)
Toate `findAll()` + stream filter din controlleri au fost înlocuite cu query-uri JPA directe:

| Controller | Fix aplicat |
|---|---|
| `ScoreController` | `findByChildIdAndMeetingId` + `findByChildIdAndSeasonIdOrderByMeeting_DateDesc` |
| `MeetingController` | `findByMeetingId` + `findBySuspensionTrueAndRemainingMeetingsGreaterThan(0)` |
| `AuthController` | `findByNameIgnoreCase` (child + leader) în loc de `findAll().stream().filter()` |
| `DashboardController` | `findByRoleIgnoreCaseIn(List.of("director","coordonator"))` |
| `TeamController` | `findAvailableChildren()` + `findByCurrentTeamIgnoreCase()` + `findByIsCompletedFalseOrderByDateAsc()` |

Repository-uri noi adăugate: `ScoreRepository.findByMeetingId`, `WarningRepository.findBySuspensionTrueAndRemainingMeetingsGreaterThan`, `LeaderRepository.findByNameIgnoreCase` + `findByRoleIgnoreCaseIn`, `ChildRepository.findByNameIgnoreCase` + `findByCurrentTeamIgnoreCase` + `findAvailableChildren`.

---

### 🔴 CRITIC

#### 1. ~~Nicio autentificare pe endpoint-uri~~ — REZOLVAT
Spring Security + JWT, vezi sectiunea "AUTENTIFICARE JWT" de mai sus.

#### 2. Secrete expuse in istoricul git (repo public) — DE FACUT DE MANA
Doua lucruri sunt in istoricul public si nu pot fi sterse prin cod:
- **Parola MySQL** `application.properties`, comentata, prezenta in 5 commit-uri.
  → schimb-o pe server; rotatia e singurul fix real.
- **Codurile de inregistrare** vechi, din `beta1.0` incoace.
  → codurile de inregistrare au fost eliminate cu totul; nu mai deschid nimic.

Optional: `git filter-repo` pentru curatarea istoricului, sau trecerea repo-ului pe privat.

---

### 🔵 ÎMBUNĂTĂȚIRI VIITOARE

#### Ramase din auditul de securitate (oct. 2026)
- **Fara limitare de incercari** la login si la PIN-ul admin (PIN-ul are doar 4 cifre). Adauga rate limit.
- **Olimpiada** — `/extra` public accepta orice valoare si orice nume de arbitru; doua
  valori `Integer.MAX` dau total negativ (overflow). Pune o limita (ex. 0 < puncte ≤ 10000).
- **`extraPoints` negativ nelimitat** in `ScoreController` — un lider poate scadea
  -999999 dintr-o greseala de tastare.
- **NFC bridge** — WebSocket pe localhost fara verificare de `Origin`: orice site deschis
  pe laptopul contabilului poate citi UID-urile. UID-ul se poate clona; nu e autentificare puternica.
- **Input invalid → 500** in controllerele inca nerefacute (`Map` in `@RequestBody`: Olimpiada,
  echipe, departamente...). Rezolvat pentru Magazin; restul se rezolva pe masura ce trec in Faza 2.
- **Telefonul directorului** e hardcodat in `DataInitializer` (repo public).
- **Registru Copii → „Atribuie Manual”** apeleaza `POST /api/children/{id}/assign-manual`, care nu
  mai exista in backend (disparut in „awanabetania 3.0”): butonul nu face nimic, fara niciun mesaj.
  De refacut (cu `seasonId`, ca manualele sa tina de sezon) sau de scos.

#### Prioritate înaltă
- ~~JWT / Autentificare reală~~ — implementat.
- **HTTPS forțat** — obligatoriu pentru producție.
- **Reactivarea testelor de controller** — cele 6 fisiere din `src/test/.../Controller/`
  sunt comentate integral (incep cu `/**`) si nu ruleaza.

#### Prioritate medie
- **React Router** — navigare pe URL; suportă butonul Back și link-uri directe.
- **Context API sau Zustand** — starea globală (`user`, `page`) fără prop drilling.
- **Error Boundaries** — fallback vizibil la crash React în loc de ecran alb.
- **WebSockets** — înlocuiește polling-ul din TeamsManager (acum 5s).

#### Prioritate scăzută
- **TypeScript** — type safety pentru props și răspunsuri API.
- **Separare completă componente** — App.jsx mai conține logică de routing.
