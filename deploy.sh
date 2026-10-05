#!/bin/bash
# Deploy pe serverul de productie:   cd /var/www/html && ./deploy.sh
#
# Pasii:
#   1. aduce codul de pe GitHub (main) si continua cu versiunea proaspat descarcata a
#      acestui script
#   2. pastreaza JAR-ul care ruleaza acum, pentru rollback
#   3. backup al bazei de date (scripts/backup-db.sh pre-deploy)
#   4. build frontend + backend
#   5. pre-flight: porneste JAR-ul nou fara server web. Asa se aplica migrarile Flyway si
#      se verifica schema si configuratia INAINTE de repornire. Daca esueaza, deploy-ul se
#      opreste si site-ul ramane pe versiunea veche, neatins.
#   6. repornire prin systemd
#   7. verificare: /actuator/health = UP si /actuator/info arata commit-ul tocmai construit.
#      Daca versiunea noua nu porneste, revine automat la JAR-ul anterior.
#
# Aplicatia e gestionata de systemd (awanabetania.service), deci scriptul NU porneste
# procesul el insusi. Versiunea veche facea `pkill` + `nohup java -jar`, ceea ce lasa un
# proces orfan in afara systemd, care a servit site-ul 12 zile. Pasul 7 prinde acum
# situatia: daca raspunde alt commit decat cel construit, deploy-ul esueaza.
#
# Configuratia de productie (credentiale DB, chei) sta in
# /var/www/html/application.properties — netrackat in git, citit de Spring din
# directorul de lucru al serviciului.
#
# Tot scriptul e in functia main: bash il citeste integral inainte sa-l ruleze, deci
# `git reset` poate inlocui fisierul in timpul rularii fara efecte neprevazute.
#
# Variabile optionale: SKIP_BACKUP=1 (doar in urgente), APP_DIR, SERVICE, APP_JAR,
# RELEASES_DIR, BASE_URL, JAVA.

set -euo pipefail

APP_DIR="${APP_DIR:-/var/www/html}"
SERVICE="${SERVICE:-awanabetania}"
CONFIG="$APP_DIR/application.properties"
# JAR-ul pornit de systemd (ExecStart din awanabetania.service). Numele vine din pom.xml
# (artifactId-version): daca schimbi versiunea acolo, actualizeaza si ExecStart.
APP_JAR="${APP_JAR:-$APP_DIR/target/awanabetania-0.0.1-SNAPSHOT.jar}"
RELEASES_DIR="${RELEASES_DIR:-$HOME/awanabetania-releases}"
PREVIOUS_JAR="$RELEASES_DIR/previous.jar"
BASE_URL="${BASE_URL:-http://localhost:8080}"
JAVA="${JAVA:-java}"

GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m'

step() { echo -e "${GREEN}$*${NC}"; }
warn() { echo -e "${YELLOW}$*${NC}"; }
fail() { echo -e "${RED}$*${NC}" >&2; exit 1; }

# Asteapta pana cand aplicatia raspunde UP, cel mult $1 secunde.
wait_healthy() {
    local _
    for _ in $(seq 1 "$1"); do
        if curl -fsS --max-time 3 "$BASE_URL/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
            return 0
        fi
        sleep 1
    done
    return 1
}

# Commit-ul procesului care raspunde pe port (din /actuator/info), sau gol.
running_commit() {
    curl -fsS --max-time 3 "$BASE_URL/actuator/info" 2>/dev/null \
        | sed -n 's/.*"commit":{"id":"\([0-9a-f]*\)".*/\1/p' || true
}

# Pune JAR-ul anterior inapoi acolo unde il porneste systemd.
restore_previous_jar() {
    [ -f "$PREVIOUS_JAR" ] && cp -p "$PREVIOUS_JAR" "$APP_JAR"
}

# Daca deploy-ul se opreste inainte de repornire, JAR-ul de pe disc trebuie sa fie tot
# cel care ruleaza: altfel o repornire ulterioara (crash, reboot) ar porni o versiune
# neverificata sau, dupa `mvnw clean`, niciuna.
RESTARTED=false
on_exit() {
    local rc=$?
    if [ "$rc" -ne 0 ] && [ "$RESTARTED" = false ] && restore_previous_jar; then
        warn "Am pus la loc JAR-ul anterior; site-ul ruleaza in continuare versiunea veche."
    fi
}

main() {
    cd "$APP_DIR"
    [ -f "$CONFIG" ] || fail "Lipseste $CONFIG — aplicatia nu ar porni. Opresc."

    # ===== PAS 1: codul nou, apoi versiunea noua a acestui script =====
    if [ "${1:-}" != "--updated" ]; then
        step "Pas 1: Aduc codul de pe Git…"
        # Versiunea care ruleaza acum; checkout-ul poate fi inaintea ei dupa un deploy esuat.
        PREVIOUS_COMMIT="$(running_commit)"
        PREVIOUS_COMMIT="${PREVIOUS_COMMIT:-$(git rev-parse --short HEAD)}"
        export PREVIOUS_COMMIT
        git fetch origin main
        git reset --hard origin/main
        exec bash "$APP_DIR/deploy.sh" --updated
    fi

    # Un singur deploy odata. Lacatul (fd 9) nu se da mai departe proceselor pornite de
    # aici (9>&-), altfel unul care ramane in viata l-ar tine blocat si dupa deploy.
    exec 9>"/tmp/$SERVICE-deploy.lock"
    flock -n 9 || fail "Alt deploy ruleaza deja."

    NEW_COMMIT="$(git rev-parse HEAD)"
    step "Deploy ${PREVIOUS_COMMIT:-?} → ${NEW_COMMIT:0:7}"

    # ===== PAS 2: JAR-ul care ruleaza acum, pentru rollback =====
    mkdir -p "$RELEASES_DIR"
    if [ -f "$APP_JAR" ]; then
        cp -p "$APP_JAR" "$PREVIOUS_JAR"
    else
        rm -f "$PREVIOUS_JAR"
        warn "Nu exista $APP_JAR: de data asta nu voi putea face rollback automat."
    fi
    trap on_exit EXIT

    # ===== PAS 3: backup al bazei de date, inainte de orice migrare =====
    step "Pas 3: Backup baza de date…"
    BACKUP_FILE=""
    if [ "${SKIP_BACKUP:-}" = "1" ]; then
        warn "SKIP_BACKUP=1: fara backup."
    else
        local out rc=0
        out="$("$APP_DIR/scripts/backup-db.sh" pre-deploy)" || rc=$?
        if [ "$rc" -eq 3 ]; then
            warn "Copia backup-ului in afara serverului a esuat; backup-ul de pe server exista, continui."
        elif [ "$rc" -ne 0 ]; then
            fail "Backup-ul a esuat; nu continui fara backup. (In urgente: SKIP_BACKUP=1 ./deploy.sh)"
        fi
        echo "$out"
        BACKUP_FILE="$(sed -n 's/^Backup: \(.*\) (.*/\1/p' <<< "$out")"
    fi

    # ===== PAS 4: build =====
    step "Pas 4: Build frontend…"
    (cd Frontend && npm ci --no-audit --no-fund && npm run build)
    # Doar build-ul curent: fisierele vechi, cu alt hash in nume, nu mai ajung in JAR.
    rm -rf src/main/resources/static
    mkdir -p src/main/resources/static
    cp -R Frontend/dist/. src/main/resources/static/

    step "Pas 4: Build backend…"
    chmod +x mvnw
    ./mvnw -B -q clean package -DskipTests
    [ -f "$APP_JAR" ] || fail "Build-ul nu a produs $APP_JAR. Daca ai schimbat versiunea in pom.xml, actualizeaza APP_JAR si ExecStart."

    # ===== PAS 5: pre-flight, cu site-ul inca pe versiunea veche =====
    step "Pas 5: Pre-flight — migrari Flyway si verificarea schemei…"
    local log="$RELEASES_DIR/preflight.log"
    if ! timeout 300 "$JAVA" -jar "$APP_JAR" \
            --spring.main.web-application-type=none --spring.main.banner-mode=off > "$log" 2>&1 9>&-; then
        tail -n 40 "$log" >&2
        if grep -q "Could not resolve placeholder" "$log"; then
            warn "Lipseste o valoare de configurare. Daca o tii in unit-ul systemd (Environment=),"
            warn "pre-flight-ul n-o vede: mut-o in $CONFIG."
        fi
        if grep -q "FlywayMigrateException" "$log"; then
            warn "O migrare a esuat. MySQL nu poate anula schimbarile de schema, deci ce a apucat"
            warn "sa ruleze a ramas aplicat, iar Flyway a marcat migrarea ca esuata: urmatorul deploy"
            warn "va fi refuzat pana cureti. Site-ul merge in continuare pe versiunea veche."
            [ -n "$BACKUP_FILE" ] && warn "  Simplu: scripts/restore-db.sh $BACKUP_FILE  (se pierde ce s-a scris de atunci)"
            warn "  Manual: anuleaza ce a facut migrarea, apoi: DELETE FROM flyway_schema_history WHERE success = 0;"
        fi
        if grep -q "Detected failed migration" "$log"; then
            warn "Un deploy anterior a lasat in baza o migrare esuata. Restaureaza backup-ul pre-deploy"
            warn "facut INAINTEA acelui deploy (nu pe cel de acum, care contine deja starea stricata):"
            warn "  ls -t ${BACKUP_DIR:-$HOME/awanabetania-backups}/*-pre-deploy.sql.gz"
            warn "  scripts/restore-db.sh <fisierul>"
            warn "sau anuleaza manual ce a facut migrarea, apoi: DELETE FROM flyway_schema_history WHERE success = 0;"
        fi
        fail "Versiunea noua nu porneste (jurnal complet: $log). N-am repornit nimic."
    fi

    # ===== PAS 6: repornire =====
    step "Pas 6: Repornesc serviciul…"
    RESTARTED=true
    sudo systemctl restart "$SERVICE" 9>&-

    # ===== PAS 7: verificare =====
    step "Pas 7: Astept sa raspunda…"
    if ! wait_healthy 90; then
        echo -e "${RED}Versiunea noua nu raspunde. Ultimele linii din jurnal:${NC}" >&2
        local journal
        journal="$(sudo journalctl -u "$SERVICE" --no-pager -n 40 2>&1 || true)"
        echo "$journal" >&2
        if grep -q "already in use" <<< "$journal"; then
            fail "Portul e ocupat de alt proces (probabil un java orfan, pornit in afara systemd). Verifica: pgrep -af 'awanabetania.*jar' — opreste-l si ruleaza din nou deploy.sh."
        fi
        if restore_previous_jar; then
            warn "ROLLBACK: repornesc versiunea anterioara (${PREVIOUS_COMMIT:-?})…"
            sudo systemctl restart "$SERVICE" 9>&-
            if wait_healthy 90; then
                fail "Rollback reusit: ruleaza din nou ${PREVIOUS_COMMIT:-versiunea anterioara}. Codul din $APP_DIR e la ${NEW_COMMIT:0:7}; repara problema si ruleaza din nou deploy.sh."
            fi
        fi
        [ -n "$BACKUP_FILE" ] && warn "Daca o migrare a stricat datele: scripts/restore-db.sh $BACKUP_FILE"
        fail "Nici versiunea anterioara nu raspunde. Verifica: sudo journalctl -u $SERVICE -n 100"
    fi

    local running
    running="$(running_commit)"
    if [ -z "$running" ]; then
        warn "Aplicatia raspunde, dar /actuator/info nu arata commit-ul; nu pot confirma versiunea."
    elif [[ "$NEW_COMMIT" != "$running"* ]]; then
        fail "Raspunde commit-ul $running, nu ${NEW_COMMIT:0:7}: alt proces tine portul. Verifica: pgrep -af 'awanabetania.*jar' (trebuie exact unul)"
    fi

    step "GATA! Ruleaza ${NEW_COMMIT:0:7}."
    echo -e "Log: ${GREEN}sudo journalctl -u $SERVICE -f${NC}"
}

main "$@"
