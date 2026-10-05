# Operare în producție

Ghid pentru serverul de producție: deploy, rollback, backup, restaurare, migrări, conturi
(autentificare cu Google, resetarea parolelor) și sezoane.
Aplicația rulează ca serviciul systemd `awanabetania`, din `/var/www/html`, ca utilizatorul `awana`.

---

## 1. Prima dată după actualizarea la Flyway (o singură dată)

Fă pașii în ordine, înainte de primul `./deploy.sh` cu versiunea nouă.

**1. Adu întâi scriptul nou, apoi rulează-l.** `deploy.sh` vechi își înlocuiește singur fișierul în
timp ce rulează, iar bash ar continua să citească din fișierul nou de la poziția greșită:

```bash
cd /var/www/html
git fetch origin main && git reset --hard origin/main
```

**2. Verifică configurația de pe server.** O linie `ddl-auto` în fișierul de pe server ar suprascrie
`validate` din aplicație, iar Hibernate ar modifica din nou schema pe ascuns:

```bash
grep -n "ddl-auto\|flyway" /var/www/html/application.properties   # nu trebuie să afișeze nimic
```

Dacă apare `spring.jpa.hibernate.ddl-auto=update`, șterge linia.

**3. Verifică JAR-ul pornit de systemd.** `deploy.sh` presupune
`/var/www/html/target/awanabetania-0.0.1-SNAPSHOT.jar`:

```bash
systemctl show -p ExecStart awanabetania
```

Dacă e alt fișier, rulează deploy-ul cu `APP_JAR=/calea/corecta.jar ./deploy.sh`.

**4. Testează backup-ul.** `deploy.sh` refuză să continue fără backup:

```bash
which mysqldump || sudo apt install mysql-client
scripts/backup-db.sh manual
ls -lh ~/awanabetania-backups/
```

Scriptul folosește aceleași credențiale ca aplicația (din `application.properties`).

**5. Pornește backup-ul zilnic** (03:17 în fiecare noapte):

```bash
sudo cp ops/systemd/awanabetania-backup.service ops/systemd/awanabetania-backup.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now awanabetania-backup.timer
systemctl list-timers awanabetania-backup.timer       # vezi următoarea rulare
```

**6. Primul deploy:**

```bash
./deploy.sh
```

La prima pornire, Flyway înregistrează schema existentă ca versiunea 1 („baseline”). Nu modifică
nicio tabelă și nu atinge datele.

---

## 2. Deploy

```bash
cd /var/www/html && ./deploy.sh
```

| Pas | Ce face | Dacă eșuează |
|---|---|---|
| 1 | `git fetch` + `reset` la `origin/main`, apoi rulează versiunea nouă a scriptului | nimic nu s-a schimbat |
| 2 | păstrează JAR-ul curent în `~/awanabetania-releases/previous.jar` | — |
| 3 | backup al bazei (`*-pre-deploy.sql.gz`) | se oprește; nimic nu s-a schimbat |
| 4 | build frontend + backend | JAR-ul vechi e pus la loc |
| 5 | **pre-flight**: pornește versiunea nouă fără server web → migrări Flyway + validarea schemei | JAR-ul vechi e pus la loc; site-ul n-a fost oprit |
| 6 | `systemctl restart awanabetania` | — |
| 7 | așteaptă `/actuator/health` = UP și verifică în `/actuator/info` că răspunde commit-ul nou | **rollback automat** la JAR-ul anterior |

Mesajele scriptului spun exact ce s-a întâmplat. Cazurile speciale:

- **„Versiunea nouă nu pornește … N-am repornit nimic”**: citește
  `~/awanabetania-releases/preflight.log`, repară, fă push și rulează din nou.
- **„O migrare a eșuat”**: MySQL nu poate anula schimbările de schemă (`ALTER`, `CREATE`). Ce a
  apucat să ruleze rămâne aplicat, iar Flyway marchează migrarea ca eșuată. Site-ul merge pe
  versiunea veche, dar următorul deploy e refuzat până cureți. Cea mai simplă variantă e să
  restaurezi backup-ul `pre-deploy` făcut chiar de acel deploy (vezi secțiunea 4).
- **„Rollback reușit”**: rulează versiunea anterioară. Codul din `/var/www/html` e însă la commit-ul
  nou: repară, fă push, rulează din nou.
- **„Portul e ocupat” / „Răspunde commit-ul X, nu Y”**: un proces java pornit în afara systemd ține
  portul 8080 (așa a fost servit site-ul 12 zile de un proces orfan):

  ```bash
  pgrep -af 'awanabetania.*jar'     # trebuie să fie exact unul, cel al serviciului
  sudo kill <pid-ul în plus>
  sudo systemctl restart awanabetania
  ```

**Urgență, fără backup:** `SKIP_BACKUP=1 ./deploy.sh`. Doar dacă știi sigur că nu se schimbă schema.

---

## 3. Rollback manual

Varianta corectă e un commit care anulează schimbarea (`git revert`), urmat de deploy. Codul de pe
GitHub rămâne astfel la fel cu ce rulează.

Dacă e urgent, repornește JAR-ul anterior:

```bash
cp ~/awanabetania-releases/previous.jar /var/www/html/target/awanabetania-0.0.1-SNAPSHOT.jar
sudo systemctl restart awanabetania
curl -s localhost:8080/actuator/info      # ce commit rulează acum
```

Versiunea veche merge pe schema nouă cât timp migrările doar *adaugă* tabele sau coloane (vezi
secțiunea 5).

---

## 4. Backup și restaurare

| | |
|---|---|
| Unde | `~/awanabetania-backups/awana-<baza>-<data>-<eticheta>.sql.gz` (doar utilizatorul `awana` le poate citi), plus, dacă e configurat, pe Google Drive |
| Când | înainte de fiecare deploy (`pre-deploy`), înainte de un sezon nou (`pre-season`, automat din aplicație), înainte de o restaurare (`pre-restore`), la cerere (`scripts/backup-db.sh manual`) și, opțional, zilnic la 03:17 (`daily`) |
| Câte pe server | ultimele 10 (`backup.keep-local`); pe Drive rămân toate |
| Integritate | un fișier `.sql.gz` e scris doar dacă dump-ul s-a terminat complet |

**Setări**, în `/var/www/html/application.properties` (le citește scriptul, deci se aplică oricărui
backup, inclusiv celor de la deploy și de la sezonul nou):

```properties
backup.rclone-remote=gdrive:awana-backups   # copie pe Google Drive (vezi mai jos)
backup.keep-local=3                         # câte backup-uri rămân pe server; cel nou rămâne mereu
```

Un backup are câteva zeci de KB, deci nici 10 nu ocupă aproape nimic. Dacă Google Drive nu răspunde,
backup-ul de pe server se face oricum, iar deploy-ul și sezonul nou merg mai departe cu un
avertisment (scriptul iese cu codul 3).

**Backup zilnic: opțional.** Datele se schimbă la fiecare întâlnire, deci un backup doar la deploy
poate rămâne în urmă cu săptămâni. Cu Drive configurat, cel zilnic nu ocupă loc pe server. Pornit/oprit:

```bash
sudo systemctl enable --now awanabetania-backup.timer    # pornește
sudo systemctl disable --now awanabetania-backup.timer   # oprește
```

**Restaurare** (oprește aplicația, înlocuiește toate tabelele, o pornește la loc):

```bash
ls -t ~/awanabetania-backups/                       # alege fișierul
scripts/restore-db.sh ~/awanabetania-backups/awana-awana_club_manager-....sql.gz
```

Scriptul cere numele bazei ca confirmare și face întâi un backup `pre-restore` al stării curente,
deci și restaurarea se poate anula.

**Copie pe Google Drive (recomandat).** Un backup aflat pe același disc nu te ajută dacă serverul
cade. Configurarea, o singură dată, ca utilizatorul `awana`:

1. Instalează [rclone](https://rclone.org) cu scriptul oficial; versiunea din `apt` poate fi prea
   veche pentru autorizarea Google:
   `sudo -v ; curl https://rclone.org/install.sh | sudo bash`
2. Serverul nu are browser, deci autorizarea o faci din browserul calculatorului, printr-un tunel.
   De pe calculator: `ssh -L 53682:localhost:53682 awana@<server>`, apoi `rclone config`:
   `n` (remote nou) → nume `gdrive` → `drive` → client_id și client_secret goale (sau ale tale,
   vezi mai jos) → scope `drive.file` (rclone vede doar fișierele create de el) → `n` la
   *advanced config* → `y` la *web browser*. Deschide link-ul afișat (`http://127.0.0.1:53682/...`)
   în browserul calculatorului și permite accesul, apoi `n` la *Shared Drive*, `y`, `q`.
   Mesajele `channel ... Connection refused` de după `Got code` sunt inofensive.
3. Pune `backup.rclone-remote=gdrive:awana-backups` în `application.properties` și testează:
   `scripts/backup-db.sh manual` trebuie să afișeze `Copiat in gdrive:awana-backups`.

Folosește contul Google al clubului: backup-urile conțin date ale copiilor; nu partaja folderul.
Restaurare din Drive: `rclone copy gdrive:awana-backups/<fisier> ~/awanabetania-backups/`, apoi
`scripts/restore-db.sh` ca mai sus (sau descarci fișierul de pe drive.google.com).

**Client ID propriu pentru rclone.** Accesul comun al rclone la Google Drive se oprește în 2026
(rclone afișează un `NOTICE`). Cu un Client ID propriu, în proiectul Google Cloud al clubului
(același ca la secțiunea 8): *APIs & Services → Library → Google Drive API → Enable*; ecranul de
consimțământ publicat (*Audience → Publish app*; în *Testing*, accesul expiră după 7 zile);
*Data Access* → scope `.../auth/drive.file`; *Clients → Create client → Desktop app*. Apoi, prin tunel:

```bash
rclone config update gdrive client_id=<CLIENT_ID> client_secret=<CLIENT_SECRET>
rclone config reconnect gdrive:
```

Cu `drive.file`, rclone nu mai vede fișierele urcate cu vechiul acces: pe Drive apare un al doilea
folder `awana-backups`; pe cel vechi îl poți șterge de pe drive.google.com. Dacă vreodată copia
pică cu `invalid_grant`, rulează din nou `rclone config reconnect gdrive:` prin tunel.

**Exercițiu de restaurare (o dată la câteva luni).** Un backup pe care nu l-ai restaurat niciodată
nu e un backup sigur. Restaurează-l într-o bază separată, nu în cea de producție (scriptul îți
arată numele bazei înainte să ceară confirmarea: trebuie să fie `awana_restore_test`):

```bash
mysql -u root -p -e "CREATE DATABASE awana_restore_test; GRANT ALL ON awana_restore_test.* TO '<user-aplicatie>'@'localhost';"
mkdir -p /tmp/restore-test
grep -v '^spring.datasource.url' /var/www/html/application.properties > /tmp/restore-test/application.properties
echo 'spring.datasource.url=jdbc:mysql://localhost:3306/awana_restore_test' >> /tmp/restore-test/application.properties
APP_DIR=/tmp/restore-test SERVICE=nimic scripts/restore-db.sh ~/awanabetania-backups/<fisier>.sql.gz
mysql -u root -p awana_restore_test -e "SELECT COUNT(*) FROM children; SELECT COUNT(*) FROM leaders;"
mysql -u root -p -e "DROP DATABASE awana_restore_test"; rm -rf /tmp/restore-test
```

---

## 5. Migrări (schimbări de schemă)

- Orice schimbare de schemă e un fișier nou `src/main/resources/db/migration/V<n>__descriere.sql`,
  cu `n` următorul număr liber.
- **Nu modifica niciodată o migrare care a rulat deja.** Flyway îi verifică suma de control și
  refuză să pornească dacă s-a schimbat.
- Hibernate doar verifică schema (`ddl-auto=validate`). Un câmp nou într-o entitate fără migrarea
  corespunzătoare pică testul `DatabaseMigrationTest` din CI, nu deploy-ul.
- **Doar adaugă, nu șterge în același deploy.** Pre-flight-ul aplică migrările cât timp rulează încă
  versiunea veche, iar un rollback o repornește tot pe schema nouă. Deci versiunea veche trebuie
  să meargă pe schema nouă:
  - coloană nouă: permite `NULL` sau are `DEFAULT`;
  - redenumire sau ștergere: întâi un deploy care nu mai folosește coloana veche, abia apoi un
    deploy cu migrarea care o șterge.
- Pe MySQL, pune o singură schimbare de structură pe fișier, ca o eroare să nu lase migrarea
  aplicată pe jumătate.

---

## 6. Monitorizare

| Adresă | Răspuns |
|---|---|
| `/actuator/health` | `{"status":"UP"}`, sau 503 dacă baza de date nu răspunde |
| `/actuator/info` | commit-ul și ora build-ului care rulează |

Un serviciu gratuit precum [UptimeRobot](https://uptimerobot.com) poate verifica
`https://awana.betania-tm.ro/actuator/health` la câteva minute și îți trimite e-mail dacă site-ul cade.

## 7. Comenzi utile

```bash
sudo systemctl status awanabetania
sudo journalctl -u awanabetania -f                    # jurnal live
curl -s localhost:8080/actuator/info                  # ce versiune rulează
pgrep -af 'awanabetania.*jar'                         # trebuie să fie exact un proces
systemctl list-timers awanabetania-backup.timer       # următorul backup
journalctl -u awanabetania-backup -n 20               # rezultatul ultimelor backup-uri
```

---

## 8. Autentificare cu Google

Liderii intră cu „Continuă cu Google”. Copiii rămân pe nume de utilizator și parolă.
Google confirmă *cine* e persoana; lista de lideri din Control Center decide *dacă* are acces.
Un cont Google care nu e în listă e refuzat, deci nu se poate crea nimeni singur cont de lider.

Serverul are nevoie doar de **Client ID**, care e public (apare oricum în pagina de login).
Nu se folosește niciun *client secret* și niciun redirect.
Google e opțional. Fără Client ID, aplicația nu arată nimic despre Google, toată lumea intră cu
utilizator și parolă, iar un lider adăugat din Control Center primește o parolă temporară (vezi 8.3).
Poți configura Google oricând mai târziu.

### 8.1 Client ID-ul în Google Cloud Console (o singură dată, ~10 minute)

1. Deschide <https://console.cloud.google.com> cu un cont Google **al clubului**, nu cu unul personal,
   ca accesul să nu depindă de o singură persoană. Alți administratori se adaugă din „IAM & Admin”.
2. **Proiect nou:** selectorul de proiect din bara de sus → *New project* → nume `Awana Betania` →
   *Create*. Apoi selectează proiectul. Nu e nevoie de cont de facturare: autentificarea e gratuită.
3. **Ecranul de consimțământ:** meniul ☰ → *APIs & Services* → *OAuth consent screen*
   (în consola nouă se numește *Google Auth Platform*: <https://console.cloud.google.com/auth/overview>)
   → *Get started*:
   - *App name*: `Awana Betania` (numele pe care îl văd liderii când aleg contul);
   - *User support email*: adresa clubului;
   - *Audience*: **External** (liderii au conturi Gmail obișnuite);
   - *Contact information*: adresa ta → bifează acordul → *Create*.
4. **Branding** (meniul din stânga):
   - *Application home page*: `https://awana.betania-tm.ro`;
   - *Authorized domains*: `betania-tm.ro`;
   - **nu încărca logo**: logo-ul cere verificarea aplicației de către Google, care durează zile
     sau săptămâni. Fără logo nu e nevoie de verificare. → *Save*.
5. **Audience** → *Publish app* → *Confirm*. Statusul devine **In production**.
   În starea *Testing* pot intra doar adresele trecute manual la *Test users*; ceilalți primesc
   „Access blocked”. Aplicația cere doar numele și adresa de e-mail, deci publicarea nu cere verificare.
6. **Data Access:** nu adăuga nimic. Se folosesc doar `openid`, `email` și `profile`, care sunt implicite.
7. **Clients** → *Create client*:
   - *Application type*: **Web application**;
   - *Name*: `Awana web`;
   - *Authorized JavaScript origins*: `https://awana.betania-tm.ro`
     (exact așa: cu `https`, fără `/` la final). Pentru dezvoltare locală adaugă și
     `http://localhost:5173` și `http://localhost`;
   - *Authorized redirect URIs*: **lasă gol**;
   - *Create*.
8. Copiază **Client ID**-ul, de forma `123456789012-abc…xyz.apps.googleusercontent.com`.
   *Client secret*-ul nu e folosit: nu-l copia nicăieri.

O origine nou adăugată poate avea nevoie de la câteva minute la câteva ore până funcționează.

### 8.2 Pe server

```bash
cd /var/www/html
nano application.properties
#   adaugă linia:  auth.google.client-id=123456789012-abc…xyz.apps.googleusercontent.com
#   linia auth.registration-codes nu mai e folosită și se poate șterge
cp application.properties ~/application.properties.backup

./deploy.sh                                  # sau, dacă versiunea e deja la zi:
                                             # sudo systemctl restart awanabetania
curl -s localhost:8080/api/auth/config       # {"googleClientId":"123456789012-…"}
```

Local, aceeași valoare se pune în `.env`, ca `GOOGLE_CLIENT_ID=…`.

### 8.3 Liderii

- **Lider nou:** Control Center → *Lideri* → *+ Adauga lider* → nume, adresa Gmail, rol.
  - Cu Google configurat: liderul intră cu „Continuă cu Google” pe adresa aceea. Nu primește parolă.
  - Fără Google (sau fără adresă): în cardul liderului apar username-ul și o parolă temporară,
    o singură dată. I le dai liderului; la prima intrare își alege parola lui. După ce
    configurezi Google, își poate lega contul din *Contul Meu*.
- **Lider existent** (are deja parolă), una din două:
  - singur: intră cu parola → *Contul Meu* → *Cont Google* → alege contul;
  - directorul: Control Center → *Lideri* → rândul *Google* → *Schimba* → adresa liderului.

  Parola veche continuă să meargă.
- La prima intrare, contul de lider se leagă de identificatorul permanent al contului Google.
  Dacă directorul schimbă apoi adresa, legătura se șterge și se reface la următoarea intrare.
- Pagina „Cont Nou” înscrie doar copii. Înainte, oricine cu un cod de lider își putea alege
  singur rolul de director.

### 8.4 Probleme frecvente

| Simptom | Cauză și rezolvare |
|---|---|
| Butonul Google nu apare la login | `curl -s localhost:8080/api/auth/config` arată `null`: lipsește `auth.google.client-id`. Dacă arată ID-ul, browserul blochează `accounts.google.com` (de ex. un adblocker). |
| Consola browserului (F12): „The given origin is not allowed for the given client ID” | Adresa site-ului lipsește din *Authorized JavaScript origins* sau e scrisă diferit. După corectare, așteaptă propagarea. |
| Google afișează „Access blocked” sau `access_denied` | Aplicația e încă în *Testing*: *Audience* → *Publish app*. |
| „Nu te-am putut autentifica cu acest cont Google…” | Adresa nu e în lista de lideri sau liderul a ales alt cont Google. Jurnalul arată adresa: `sudo journalctl -u awanabetania \| grep 'Google sign-in refused'`. |
| Jurnalul arată `Rejected Google ID token` | Cu `expired` sau `used before`: ceasul serverului e decalat (`timedatectl` trebuie să arate `System clock synchronized: yes`). Cu `JWK set`: serverul nu ajunge la Google (`curl -sI https://www.googleapis.com/oauth2/v3/certs`). |
| Fereastra Google se deschide și se închide fără efect | Proxy-ul trimite `Cross-Origin-Opener-Policy: same-origin`, care blochează fereastra. Folosește `same-origin-allow-popups` sau scoate header-ul. |

---

## 9. Parolă uitată

Parolele nu pot fi afișate: sunt salvate ca hash BCrypt, care nu se poate întoarce în text.
În schimb, se resetează:

1. Control Center → *Copii* (sau *Lideri*) → cardul persoanei → *Reseteaza parola* → confirmă.
2. Apare o parolă temporară, de forma `ab3k-7xmq`, cu butonul *Copiaza*. Trimite-o părintelui.
   Nu mai apare a doua oară; dacă se pierde, resetezi din nou.
3. La prima intrare cu ea, aplicația cere o parolă nouă (minimum 6 caractere) înainte de orice altceva.

Fiecare resetare rămâne în jurnal, cu cine a făcut-o:
`sudo journalctl -u awanabetania | grep 'Password reset'`.

---

## 10. Sezon nou

Un sezon e un an de club. La un sezon nou, copiii și liderii rămân, cu datele și conturile lor;
datele de sezon pornesc de la zero, iar sezonul încheiat rămâne de citit. Nu se șterge nimic.

### Prima dată

Deploy-ul care aduce sezoanele creează singur primul sezon, cu toate datele existente. Îl numește
după anul de club al primei întâlniri (de ex. `2025–2026`). Numele se poate schimba din
Control Center → *Sezoane* → *Redenumeste*.

### Pornirea unui sezon nou

1. Control Center → *Sezoane* → *Incheie sezonul si incepe unul nou…*
2. Pagina arată exact ce pornește de la zero și ce rămâne. Scrie numele sezonului nou, bifează
   *Am inteles* și confirmă.

Înainte de orice schimbare, aplicația face singură un backup al bazei de date (`…-pre-season.sql.gz`,
și pe Google Drive dacă e configurat). Dacă backup-ul nu reușește, sezonul nu pornește și nu se
schimbă nimic; cauza e în `sudo journalctl -u awanabetania | grep -i backup`.

Ce se întâmplă, într-o singură operație:

| Pornesc de la zero | Rămân |
|---|---|
| punctele, streak-ul, prezențele, lecțiile | copiii și liderii, datele de contact, conturile |
| insignele, manualele, uniforma primită | cardurile NFC |
| suspendările în curs | stickerele de pe hartă |
| rating-ul liderilor și comentariile primite | anunțurile și cererile de ștergere a contului |
| bonurile neaprobate (se anulează) | întâlnirile planificate (trec în sezonul nou) |

Sezonul încheiat apare la *Sezoane incheiate* → *Vezi rezultatele*: clasamentul copiilor (puncte
câștigate, cheltuite la târg, rămase, prezențe, lecții, uniformă, avertismente) și media fiecărui lider.

Pornirea e blocată cât timp o întâlnire neînchisă are deja punctaje: închide-o întâi.

Fiecare sezon nou rămâne în jurnal, cu cine l-a pornit:
`sudo journalctl -u awanabetania | grep 'started by leader'`.

### Dacă ai pornit un sezon din greșeală

Nu există buton de anulare. Datele sezonului vechi sunt toate în baza de date, dar punctele și
streak-urile copiilor au fost puse pe zero. Cel mai simplu: restaurează backup-ul făcut automat
chiar înainte, cel mai nou `*-pre-season.sql.gz` (`scripts/restore-db.sh`, vezi secțiunea 4).
Se pierde doar ce s-a scris după el.
