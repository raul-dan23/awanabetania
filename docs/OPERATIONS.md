# Operare în producție

Ghid pentru serverul de producție: deploy, rollback, backup, restaurare și migrări.
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
| Unde | `~/awanabetania-backups/awana-<baza>-<data>-<eticheta>.sql.gz` (doar utilizatorul `awana` le poate citi) |
| Când | zilnic la 03:17 (`daily`), înainte de fiecare deploy (`pre-deploy`), înainte de o restaurare (`pre-restore`) |
| Cât timp | 30 de zile (`BACKUP_RETENTION_DAYS`) |
| Integritate | un fișier `.sql.gz` e scris doar dacă dump-ul s-a terminat complet |

**Restaurare** (oprește aplicația, înlocuiește toate tabelele, o pornește la loc):

```bash
ls -t ~/awanabetania-backups/                       # alege fișierul
scripts/restore-db.sh ~/awanabetania-backups/awana-awana_club_manager-....sql.gz
```

Scriptul cere numele bazei ca confirmare și face întâi un backup `pre-restore` al stării curente,
deci și restaurarea se poate anula.

**Copie în afara serverului (recomandat).** Un backup aflat pe același disc nu te ajută dacă
serverul cade. Cu [rclone](https://rclone.org) poți trimite fiecare backup, de exemplu, pe Google Drive:

```bash
sudo apt install rclone
rclone config                                       # creează un remote, ex. „gdrive”
BACKUP_RCLONE_REMOTE=gdrive:awana-backups scripts/backup-db.sh manual
```

Apoi decomentează linia `Environment=BACKUP_RCLONE_REMOTE=...` din
`/etc/systemd/system/awanabetania-backup.service` și rulează `sudo systemctl daemon-reload`.

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
