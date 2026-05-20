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

## Referință: proiectul Awana-2 (C# WinForms)
Locație: `~/Downloads/Awana-2/`
Același concept dar mai vechi: stoca punctele PE CARD. Fișiere relevante pentru APDU: `AWANAcard.cs`, `CardInfo.cs`.

---

## Probleme de îmbunătățit

### 🔴 CRITIC (de rezolvat urgent)

#### 1. Nicio autentificare pe endpoint-uri
- **Fișier:** Toți controllerii
- **Problemă:** `@CrossOrigin(origins = "*")` + niciun token de sesiune. Oricine știe URL-urile poate modifica date.
- **Fix:** Spring Security + JWT (task mare, de planificat separat)
---

---

### 🔵 ÎMBUNĂTĂȚIRI VIITOARE

#### Prioritate înaltă
- **JWT / Autentificare reală** — userul complet în `localStorage` e vulnerabil la impersonare. Token JWT cu expirare.
- **HTTPS forțat** — obligatoriu pentru producție (legat de #3).

#### Prioritate medie
- **React Router** — navigare pe URL; suportă butonul Back și link-uri directe.
- **Context API sau Zustand** — starea globală (`user`, `page`) fără prop drilling.
- **Error Boundaries** — fallback vizibil la crash React în loc de ecran alb.
- **WebSockets** — înlocuiește polling-ul din TeamsManager (acum 5s).

#### Prioritate scăzută
- **TypeScript** — type safety pentru props și răspunsuri API.
- **Separare completă componente** — App.jsx mai conține logică de routing.
