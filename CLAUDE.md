# CLAUDE.md — AwanaBetania

## Stack
- **Backend:** Spring Boot 3.5.9, Java 17, JPA/Hibernate
- **Frontend:** React + Vite (folder `Frontend/`)
- **Server:** Ubuntu Linux (remote)
- **IDE:** IntelliJ IDEA

## Structura backend
```
src/main/java/com/awanabetania/awanabetania/
├── Model/       → Child, Score, Meeting, Leader, Department, Sticker, Warning, etc.
├── Controller/  → REST endpoints
├── Repository/  → JPA repositories
└── AwanaBetaniaApplication.java
```

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
| `Repository/BonRepository.java` | `findByStatusOrderByCreatedAtDesc`, `findAllByOrderByCreatedAtDesc` |

**application.properties:** `ddl-auto=update` (creează automat tabelele `products` și `bons`)

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

**Tabele noi create automat:** `olimpiada_sessions`, `olimpiada_scores`

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
Procesul corect (frontend e bunduit in JAR):
```bash
cd Frontend && npm run build
cp -r dist/* ../src/main/resources/static/
cd .. && mvn package -DskipTests
# Uploadeaza target/*.jar pe server si restartezi Spring Boot
```

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
- **Coduri de inregistrare:** mutate din sursa in `AUTH_REGISTRATION_CODES`.
  Cele vechi (`AWANA2024`, `BETANIA`, `DIRECTOR_KEY`) erau publice pe GitHub — nu le mai folosi.
- **CORS:** `@CrossOrigin(origins="*")` sters din toate cele 17 controllere; acum
  o singura configuratie centrala, limitata la `CORS_ALLOWED_ORIGINS`.
- **Default deny:** orice ruta nelistata ca publica cere token. Un controller
  adaugat maine e protejat automat.

### Rute publice (singurele)
```
POST /api/auth/login, /api/auth/register
GET  /api/olimpiada/session/{code}              ─┐
GET  /api/olimpiada/session/{code}/round-status  │ arbitru invitat,
GET  /api/olimpiada/session/{code}/compare       │ nu are cont
POST /api/olimpiada/session/{code}/score         │
POST /api/olimpiada/session/{code}/extra        ─┘
     /api/nfc/**                                  (are propriul X-NFC-Token)
```

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
export AUTH_REGISTRATION_CODES='COD1,COD2'
export CORS_ALLOWED_ORIGINS='https://awana.betania-tm.ro'
```
`AES_SECRET_KEY` ramane necesara — migreaza parolele vechi. Se poate scoate dupa ce
toti utilizatorii s-au logat macar o data.

### Teste
`src/test/java/.../Security/SecurityIntegrationTest.java` — 9 teste pe H2 in-memory:
acces anonim respins, token falsificat respins, login functional, parola absenta din
raspuns, migrare AES→BCrypt, cod de inregistrare vechi respins, rutele de arbitru
invitat inca publice. Ruleaza cu `mvn test`.


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
| `ScoreController` | `findByChildIdAndMeetingId` + `findByChildIdOrderByMeeting_DateDesc` |
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
  → deja inlocuite cu `AUTH_REGISTRATION_CODES`; cele vechi nu mai functioneaza.

Optional: `git filter-repo` pentru curatarea istoricului, sau trecerea repo-ului pe privat.

---

### 🔵 ÎMBUNĂTĂȚIRI VIITOARE

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
