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

## FEATURE DE IMPLEMENTAT: Sistem NFC pentru Targul de Final de Sezon

### Contextul complet
La final de sezon, copiii primesc **carduri NFC fizice** legate de contul lor din aplicatie.
Merg la **"tarabe"** (standuri cu recompense) si isi cumpara lucruri cu punctele acumulate peste sezon.
La taraba, copilul tasteaza cardul pe un cititor NFC USB → punctele se scad din baza de date.

### Principiu cheie — cardul stocheaza DOAR UID-ul
Nu scriem nimic pe card. Fiecare card NFC are un UID hardware unic din fabrica (ex: `A1B2C3D4`).
Citim acest UID si il mapam la copil in BD. Toate punctele raman in BD, nu pe card.
Daca cardul se pierde, adminul poate atribui un card nou aceluiasi copil.

### Arhitectura
```
[Card NFC fizic] → [Cititor USB ACS ACR1251 la taraba]
                          │
                   [Laptop la taraba]
                   (JAR Java local - proiect separat)
                          │ HTTP REST
                   [Ubuntu Server - Spring Boot]
                          │
                   [BD - scade punctele din Child.seasonPoints]
```

---

## PLAN DE IMPLEMENTARE

### PASUL 1 — Camp `nfcUid` in Child (backend)
**Fisier:** `src/main/java/com/awanabetania/awanabetania/Model/Child.java`

Adauga:
```java
@Column(name = "nfc_uid", unique = true)
private String nfcUid;
```

### PASUL 2 — NfcController (backend)
**Fisier nou:** `src/main/java/com/awanabetania/awanabetania/Controller/NfcController.java`

Trei endpoint-uri:
```
POST /api/nfc/register
     body: { "childId": 5, "uid": "A1B2C3D4" }
     → leaga un card de un copil (folosit de admin la inceput de sezon)

GET  /api/nfc/{uid}
     → returneaza datele copilului + punctele curente
     → folosit de JAR-ul de la taraba dupa scanare

POST /api/nfc/{uid}/spend
     body: { "amount": 50 }
     → scade punctele din Child.seasonPoints
     → returneaza noul sold
```

Protejeaza endpoint-urile cu un header secret simplu:
```
X-NFC-Token: <token configurat in application.properties>
```

### PASUL 3 — Pagina admin in React pentru inregistrare carduri
**In `Frontend/src/`** — pagina noua accesibila doar adminului:
- Input pentru ID-ul copilului
- Buton "Asteapta card" → apeleaza un endpoint care citeste UID-ul urmatorului card scanat
- La scanare, UID-ul se salveaza automat in BD

### PASUL 4 — JAR local pentru tarabe (proiect Java separat)
Un proiect Java simplu (Swing) care ruleaza pe laptopul de la taraba:
- Foloseste `javax.smartcardio` (built-in in Java, zero dependente externe)
- Polling la 500ms — asteapta un card
- La detectie: citeste UID cu comanda APDU `FF CA 00 00 00`
- Apeleaza `GET /api/nfc/{uid}` → afiseaza numele copilului si punctele
- Operatorul introduce suma produsului → apeleaza `POST /api/nfc/{uid}/spend`
- Afiseaza noul sold si confirmare

**Comanda APDU pentru citire UID (functioneaza pe orice card NFC, fara autentificare):**
```java
byte[] GET_UID = new byte[]{ (byte)0xFF, (byte)0xCA, 0x00, 0x00, 0x00 };
```

### PASUL 5 — (Optional, mai tarziu) Telefon Android ca si card
Android HCE (Host Card Emulation) — aplicatie Android separata.
Nu implementa acum.

---

## Referinta: proiectul Awana-2 (C# WinForms)
Locatie: `~/Downloads/Awana-2/`
Acelasi concept dar mai vechi: stoca punctele PE CARD (nu in BD) si folosea cititorul ACS ACR1251.
Fisierele relevante pentru referinta comenzilor APDU: `AWANAcard.cs`, `CardInfo.cs`.

---

## Ordinea de lucru recomandata
1. Incepe cu **Pasul 1 + Pasul 2** (backend pur, nu necesita hardware)
2. Testeaza endpoint-urile cu Postman/curl (simuleaza un UID hardcodat)
3. Fa **Pasul 3** (pagina admin React)
4. Fa **Pasul 4** (JAR taraba) — necesita cititorul NFC fizic pentru testare finala

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
