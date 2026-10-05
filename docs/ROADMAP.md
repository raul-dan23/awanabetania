# ROADMAP — ce mai e de făcut (oct. 2026)

Lista de lucru pentru ca AwanaBetania să fie sigură și organizată profesionist. E scrisă pentru
**Claude Code** (rulat din terminalul IntelliJ), dar se citește și de om. Contextul tehnic și
convențiile sunt în `CLAUDE.md`; pașii de server sunt în `docs/OPERATIONS.md`.

**Cum pornești o sesiune** (în terminalul din IntelliJ, în folderul proiectului):

```text
claude
> Citește docs/ROADMAP.md și fă task-ul S1, după regulile din secțiunea „Cum lucrezi”.
  La final spune-mi ce trebuie să fac eu pe server.
```

Legendă: `[ ]` de făcut · `[x]` gata · 🧑 pas manual (server, GitHub, decizie), nu cod ·
mărime **S** (sub o oră) / **M** (câteva ore) / **L** (o zi sau mai mult).

---

## Cum lucrezi (reguli pentru Claude Code)

1. **Un task = un branch = un PR**, pornit din `main` actualizat (`git fetch origin main`).
   Zonele mari din Etapa 2 pot avea un PR pe zonă.
2. **Respectă convențiile din `CLAUDE.md`, secțiunea „FAZA 2”:** Controller → Service → Repository,
   DTO-uri `record` cu `@Valid`, `ApiException` pentru erori (niciodată 401 pentru erori de business),
   puncte și stări doar prin `UPDATE` condiționat, identitatea doar din `AuthUser.current()`.
3. **Schema:** doar migrări noi `V<n>__*.sql`, doar adăugări, un `ALTER` pe fișier. Nu edita o migrare
   existentă. Rândurile noi din tabelele cu sezon primesc `seasonId` (secțiunea „SEZOANE”).
4. **Păstrează forma JSON** pe care o citește frontend-ul. Dacă o schimbi, schimbă și ecranul în același PR.
5. **Teste:** fiecare task adaugă teste (MockMvc pe H2; pe MySQL în Docker pentru concurență și migrări).
   Pentru o reparație: întâi testul care pică, apoi reparația.
6. **Înainte de commit:** `./mvnw verify` (cu Docker pornit), apoi `cd Frontend && npm run lint && npm run build`.
   Lint-ul trebuie să aibă 0 erori.
7. **Nu atinge** `/var/www/html/application.properties` și nu pune secrete în git. O setare nouă
   primește o valoare implicită sigură în `application.properties`, plus o linie în `.env.example`.
8. **La final:** bifează task-ul aici, actualizează `CLAUDE.md` și `docs/OPERATIONS.md` dacă se
   schimbă ceva la deploy, iar în descrierea PR-ului scrie ce trebuie făcut pe server.

---

## Etapa 0 — Pași manuali 🧑 (nu sunt cod)

- [ ] **0.1 Verifică ACUM că serverul web nu dă fișierele din `/var/www/html`.** Repository-ul e
  clonat chiar în folderul implicit al serverului web, iar `index.html` de la rădăcină e pagina
  implicită Ubuntu/Apache. Dacă serverul web servește fișiere de acolo, oricine poate descărca
  `application.properties` (parolele) sau `.git`. De pe calculatorul tău:
  ```bash
  curl -s https://awana.betania-tm.ro/application.properties | head -3
  curl -s -o /dev/null -w '%{http_code}\n' https://awana.betania-tm.ro/.git/config
  ```
  Nu trebuie să apară nicio linie `spring.datasource...`, iar al doilea cod trebuie să fie 403 sau 404,
  nu 200. Dacă apare ceva:
  - configurează serverul web să trimită **tot** traficul la aplicație (`proxy_pass http://127.0.0.1:8080;`),
    fără `root /var/www/html` servit direct, și blochează fișierele care încep cu punct;
  - schimbă imediat toate secretele: parola MySQL, `JWT_SECRET`, `ADMIN_PIN`, `NFC_TOKEN`.

  Indiferent de rezultat: `chmod 600 /var/www/html/application.properties`.
- [ ] **0.2 Schimbă parola MySQL.** Parola veche apare în istoricul git public (5 commit-uri).
  Actualizeaz-o în `application.properties`, apoi `sudo systemctl restart awanabetania`.
- [ ] **0.3 PIN-ul admin de 6–8 cifre** (`admin.pin`). Acum are 4 cifre, adică 10 000 de variante.
- [ ] **0.4 Decizie: repository privat?** E public și conține istoric cu secrete vechi. Dacă rămâne public:
  GitHub → Settings → Code security → *Secret scanning* și *Push protection* activate.
- [ ] **0.5 Protecție pe `main`:** GitHub → Settings → Branches → regulă pentru `main`:
  - PR obligatoriu;
  - verificările CI obligatorii (*Backend*, *Frontend*, *NFC bridge*);
  - fără force-push.
- [ ] **0.6 Securizarea serverului:**
  - `ufw`: doar 22, 80, 443 deschise; 8080 și 3306 închise din exterior;
  - SSH doar cu cheie (`PasswordAuthentication no`) și `fail2ban`;
  - `unattended-upgrades` pentru actualizări automate;
  - MySQL ascultă doar pe `127.0.0.1` (`bind-address`).
- [ ] **0.7 HTTPS:** certificat Let's Encrypt (`certbot`) și redirect http→https. Verifică:
  ```bash
  curl -sI http://awana.betania-tm.ro | grep -i location     # → https://...
  curl -sI https://awana.betania-tm.ro | grep -i strict-transport
  ```
- [ ] **0.8 Client ID propriu pentru rclone**, altfel copia pe Google Drive se oprește odată cu accesul
  comun al rclone (`docs/OPERATIONS.md` §4).
- [ ] **0.9 Monitorizare:** UptimeRobot pe `https://awana.betania-tm.ro/actuator/health`. O dată pe
  săptămână: `systemctl --failed` și `journalctl -u awanabetania-backup -n 5`.
- [ ] **0.10 Exercițiu de restaurare** o dată pe trimestru, într-o bază separată (`docs/OPERATIONS.md` §4).
- [ ] **0.11 Autentificarea cu Google**, când vrei (`docs/OPERATIONS.md` §8).

---

## Etapa 1 — Securitate (în ordinea asta)

- [ ] **S1 — Limitarea încercărilor** (M). Acum nimic nu oprește ghicirea parolelor sau a PIN-urilor.
  - **Unde:** login, `/api/admin/verify-pin` și orice `X-Admin-Pin` greșit, `/api/meetings/check-pin`,
    `/api/auth/register`, `/api/auth/google`, rutele publice Olimpiada.
  - **Cum:** un singur filtru, de exemplu Bucket4j în memorie (aplicația rulează într-o singură
    instanță), cu cheie pe IP (IP-ul real vine prin `forward-headers-strategy=native`) și, la login,
    și pe username. Răspunsul e 429 cu `Retry-After`, iar frontend-ul arată „Prea multe încercări,
    reîncearcă peste X minute”.
  - **Gata când:** testele arată că a 6-a încercare greșită într-un minut dă 429, iar un utilizator
    corect, de pe alt IP, nu e afectat.
- [ ] **S2 — PIN-ul întâlnirii** (S).
  - `MeetingController.checkPin` răspunde cu **401 la PIN greșit**, iar frontend-ul tratează 401 ca
    sesiune expirată și **deloghează liderul**. Același bug a fost reparat deja la PIN-ul admin.
    Trebuie 403.
  - Compararea se face cu `equals`; trebuie `SharedSecrets.matches` (timp constant).
  - Corpul cererii e un `Map`; trebuie un DTO.
  - Se acoperă și cu S1.
- [ ] **S3 — Identitatea și rolul din token** (S/M).
  - `FeedbackController.saveFeedback` ia `directorId` din corpul cererii, deci oricine poate semna
    evaluări în numele directorului. Trebuie luat din `AuthUser.current()`.
  - Evaluarea liderilor ar trebui permisă doar Director/Coordonator. Acum o poate face orice lider.
  - `DepartmentController` `/assign` și `/nominate`: orice lider poate repartiza pe oricine, inclusiv
    la Secretariat, care generează PIN-ul întâlnirii. 🧑 **Decide** cine are voie (director sau șeful
    departamentului), apoi aplică regula pe server.
- [ ] **S4 — O parolă schimbată închide sesiunile vechi** (M). Token-urile JWT rămân valabile 12 ore
  după o resetare sau o schimbare de parolă.
  - Adaugă `password_changed_at` (migrare) pe `children` și `leaders`, cu claim-ul corespunzător în token.
  - `JwtAuthFilter` respinge token-urile emise înainte de schimbare.
  - Aplică la resetare, la schimbarea parolei și la editarea profilului cu parolă nouă.
- [ ] **S5 — Limite pe valori** (S).
  - `extraPoints` în `ScoreController`: acum orice valoare, inclusiv -999999.
  - `TeamController` `/add-manual-points`.
  - Olimpiada `/extra`: e publică, iar două valori `Integer.MAX` dau un total negativ. Limita:
    `0 < puncte ≤ 10000`, nume de arbitru de 1–50 caractere.
  - Se face cu DTO-uri validate, iar o valoare în afara limitelor dă 400.
- [ ] **S6 — Antete de securitate** (S). Din `SecurityConfig.headers(...)`:
  - `Content-Security-Policy`:
    - `default-src 'self'`;
    - `script-src 'self' https://accounts.google.com/gsi/client`;
    - `frame-src https://accounts.google.com`;
    - `connect-src 'self' https://accounts.google.com`;
    - `style-src 'self' 'unsafe-inline'` (interfața folosește stiluri inline);
    - `img-src 'self' data: https:`;
  - `Referrer-Policy: strict-origin-when-cross-origin`;
  - `Permissions-Policy` restrictiv.

  Verifică în browser că login-ul, Control Center și butonul Google merg, fără erori CSP în consolă.
- [ ] **S7 — Telefoanele directorilor nu ajung la conturile de copil** (S). Oricine își poate face cont
  de copil, iar `/api/dashboard/stats` le trimite telefoanele directorilor (`DashboardController`).
  Copiii primesc doar numărul de directori; lista de contacte rămâne pentru lideri.
- [ ] **S8 — Înscrierea copiilor** (decizie 🧑, apoi M). Acum oricine creează un cont de copil, fără
  verificare. Recomandare: contul nou e „în așteptare” până îl aprobă directorul din Control Center.
  Alternativa e un cod pentru părinți.
- [ ] **S9 — NFC bridge** (M). Serverul WebSocket de pe `localhost:7000` acceptă orice `Origin`, deci
  orice site deschis pe laptopul contabilului poate citi UID-urile cardurilor.
  - Acceptă doar originile aplicației (producție și `localhost:5173`) și respinge restul.
  - Cod în `nfc-bridge/src`, cu test.
  - De știut: UID-ul unui card se poate clona, deci cardul identifică copilul, dar nu e o parolă.
    Aprobarea bonului de către contabil rămâne verificarea reală.
- [ ] **S10 — Endpoint-uri nefolosite** (S). `/api/products` și `/api/nfc/**` nu sunt apelate nici de
  frontend, nici de bridge (verificat cu grep în oct. 2026; verifică din nou).
  - Șterge `NfcController` și `ProductController`, cu serviciile și testele lor.
  - Șterge `permitAll` pentru `/api/nfc/**` și setarea `nfc.token`.
  - Tabelul `products` rămâne în BD: tabelele se șterg doar cu o migrare separată, mai târziu.
  - Mai puțină suprafață de atac.
- [ ] **S11 — Date personale în cod** (S). `DataInitializer.java:61` are telefonul directorului, iar
  repository-ul e public. Scoate-l (telefonul se pune din aplicație), apoi verifică ce mai creează
  `DataInitializer` în producție.
- [ ] **S12 — Scoaterea migrării AES** (S, după o verificare 🧑). Când interogarea de mai jos dă 0:
  - șterge `AESUtil` și ramura veche din `matchesAndUpgrade`;
  - șterge `aes.secret.key` / `AES_SECRET_KEY`.

  ```sql
  SELECT (SELECT COUNT(*) FROM children WHERE password IS NOT NULL AND password NOT LIKE '$2%')
       + (SELECT COUNT(*) FROM leaders  WHERE password IS NOT NULL AND password NOT LIKE '$2%');
  ```
- [ ] **S13 — Token-ul în cookie `httpOnly`** (L, opțional, după S6). Acum stă în `localStorage`, unde
  un XSS îl poate citi.
  - Alternativa: cookie `httpOnly` + `SameSite=Strict` + protecție CSRF.
  - Schimbă `Frontend/src/auth.js` și `JwtAuthFilter`.
- [ ] **S14 — Backup-uri criptate pe Drive** (opțional 🧑): un remote `rclone crypt` peste `gdrive:`.

---

## Etapa 2 — Faza 2: restul aplicației pe straturi

Modelul de urmat: zona Magazin (`BonService`, `ShopApiTest`, `ShopConcurrencyTest`) și Sezoane.
Pentru fiecare zonă:
- servicii cu `@Transactional` și injecție prin constructor;
- DTO-uri în loc de `Map` și de entități în `@RequestBody`. Azi sunt 14 `Map`-uri în 7 controllere,
  6 entități JPA primite direct și 3 clase de request fără validare (`LoginRequest`,
  `RegisterRequest`, `ScoreRequest`);
- `ApiException`, astfel încât input-ul invalid să dea 400, nu 500;
- teste API (forma JSON, 400/403/404/409), plus un test de concurență pe MySQL acolo unde se modifică puncte.

Testele vechi din `src/test/.../Controller/` sunt comentate integral și scrise pentru codul de dinainte
de JWT. Se șterg pe măsură ce zona lor primește teste noi.

- [ ] **F1 — Scorare și Întâlniri** (L): `ScoreController`, `MeetingController`, `TeamController`.
  - **Punctele se adaugă cu citește-modifică-salvează** (`addScore`): două scorări simultane pot pierde
    puncte. Trebuie un `UPDATE ... SET season_points = season_points + :p`.
  - **`closeMeeting` încarcă și salvează toți copiii.** Trebuie înlocuit cu `UPDATE`-uri în bloc
    (puncte zilnice, echipe, streak pentru absenți).
  - **Scorul ia data de azi**, nu data întâlnirii.
  - Include S2 și S5. Teste: scorare, dubla scorare (409), închiderea întâlnirii, concurență pe MySQL.
- [ ] **F2 — Copii, Lideri, Cont** (L): `ChildController`, `LeaderController`, `AuthController`
  (7 `@Autowired` pe câmpuri), `AccountController` (`requestDeletion` primește un `Map`), plus
  `AdminController.verifyPin` și `allUsers` (`Map`, întoarce entități).
  - **„Atribuie Manual” nu face nimic:** apelează `POST /api/children/{id}/assign-manual`, care nu
    există. Refă-l cu `seasonId`, ca manualele să țină de sezon, sau scoate butonul.
  - **`addChild` primește entitatea `Child` întreagă**, deci se pot trimite puncte sau NFC UID.
    Trebuie un DTO doar cu câmpurile de înscriere.
  - **`updateChild`/`updateLeader`:** validează username-ul (nu gol, unic, format).
    `updateLeader` acceptă orice departamente; decide cine le poate schimba.
  - Dacă S8 e decis, intră aici.
- [ ] **F3 — Olimpiada** (M): `OlimpiadaController` (3 `Map`-uri). Include S5 pentru `/extra`; rutele
  publice rămân publice, dar cu limite (S1).
- [ ] **F4 — Departamente, Feedback, Notificări, Dashboard, Avertismente, Stickere** (L):
  `DepartmentController` (3 `Map`-uri), `FeedbackController` (S3), `NotificationController`
  (primește entitatea), `DashboardController` (răspuns `Map`, S7), `WarningController` (primește
  entitatea), `StickerController`.

---

## Etapa 3 — Organizare profesională

- [ ] **Q1 — Curățenie în repository** (S).
  - Șterge `index.html` de la rădăcină (pagina implicită Ubuntu, adăugată din greșeală).
  - Scoate din git bundle-ul vechi din `src/main/resources/static/`: `deploy.sh` îl reconstruiește
    din `Frontend/` la fiecare deploy. Adaugă folderul în `.gitignore`. Verifică întâi `SpaController`
    și testele care cer `/`, pentru că folderul acela servește `index.html`.
  - Mută `explicatie_proiect.md` în `docs/` și actualizează-l, sau unește-l cu README.
- [ ] **Q2 — Șablon de PR, `CODEOWNERS`, `CONTRIBUTING.md`** (S).
  - `CONTRIBUTING.md`: cum rulezi local, convenția de commit (`feat(zona): ...`, `fix(...)`), regulile
    din „Cum lucrezi”.
  - Șablonul de PR are secțiunile: Ce, De ce, Teste, Pași pe server.
- [ ] **Q3 — Formatare automată** (M):
  - Spotless pentru Java (google-java-format sau palantir) și Prettier pentru frontend;
  - `.editorconfig`;
  - verificare în CI (`spotless:check`, `prettier --check`).

  Un singur PR de formatare, fără alte schimbări în el.
- [ ] **Q4 — Analiză de securitate în CI** (S):
  - workflow CodeQL (Java + JavaScript);
  - `npm audit --audit-level=high` în jobul de frontend;
  - opțional, OWASP dependency-check pentru Maven.
- [ ] **Q5 — Teste de frontend** (M):
  - Vitest + React Testing Library pentru `auth.js` (401 doar cu token, transformarea erorilor),
    `Login`, `ChangePassword`, `SeasonsPanel`;
  - `npm test` în CI.
- [ ] **Q6 — Test end-to-end în CI** (M): Playwright, cu un scenariu scurt pe MySQL + backend +
  frontend (login director, PIN, Sezoane, bon nou, aprobare), rulat la fiecare PR.
- [ ] **Q7 — Versiuni și istoric** (S):
  - tag `vYYYY.MM.DD` la fiecare deploy reușit (`deploy.sh` îl poate crea);
  - `CHANGELOG.md` cu ce s-a schimbat pentru utilizatori;
  - `/actuator/info` arată deja commit-ul.
- [ ] **Q8 — Jurnal de acțiuni de admin** (M): tabel `audit_log` (cine, ce, când, pe ce cont) pentru
  resetări de parolă, lideri adăugați, sezon nou, ștergeri, carduri NFC. Vizibil în Control Center.
- [ ] **Q9 — Frontend** (L, pe rând):
  - Error Boundary: o eroare în React să nu lase ecranul alb;
  - React Router: URL-uri, butonul Back;
  - stare globală prin Context în loc de prop drilling;
  - polling-ul la 5 s din `TeamsManager` înlocuit cu SSE sau WebSocket;
  - stilurile hardcodate din Control Center pe variabilele de temă (dark mode);
  - TypeScript, treptat (opțional, la final).
- [ ] **Q10 — Java 21 LTS** (S, opțional): `<java.version>21</java.version>`, Java 21 în CI și pe server.
- [ ] **Q11 — Documentație API** (S, opțional): springdoc-openapi, doar în dezvoltare (în producție oprit).

---

## Ordinea recomandată

1. **Etapa 0**, mai ales 0.1–0.3: pași manuali, dar cei mai importanți.
2. **S1, S2, S3, S5, S7**: reparații mici cu efect mare.
3. **Q1, Q2, Q4**: curățenie și plasa de siguranță în CI, înainte de refactorizările mari.
4. **F1, F2, F3, F4**, una câte una, fiecare cu PR-ul și testele ei. S4, S6, S8–S12 intră în zona potrivită.
5. Restul Etapei 3, în funcție de timp.

## Ce înseamnă „gata” pentru un task

- testele noi trec, iar `./mvnw verify` și `npm run lint && npm run build` sunt verzi și în CI;
- schimbarea e verificată în browser, dacă atinge interfața;
- `CLAUDE.md` (și, dacă e cazul, `docs/OPERATIONS.md`) descrie noua stare;
- task-ul e bifat aici;
- PR-ul spune ce trebuie făcut pe server sau „nimic, doar `./deploy.sh`”.
