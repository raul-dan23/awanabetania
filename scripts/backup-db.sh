#!/usr/bin/env bash
# Backup al bazei de date MySQL a aplicatiei.
#
#   scripts/backup-db.sh [eticheta]        # eticheta: daily (implicit), pre-deploy, manual...
#
# Rezultat: $BACKUP_DIR/awana-<baza>-<data>-<eticheta>.sql.gz, vizibil doar pentru
# utilizatorul care l-a creat. Fisierul e scris intai ca .partial si redenumit doar dupa
# ce dump-ul s-a terminat complet, deci un fisier .sql.gz e intotdeauna un backup intreg.
#
# Credentialele: aceleasi ca ale aplicatiei, vezi scripts/lib/db-connection.sh.
#
# Setari, din $APP_DIR/application.properties (o variabila de mediu cu acelasi rol are prioritate):
#   backup.rclone-remote  BACKUP_RCLONE_REMOTE  copie in afara serverului, ex. gdrive:awana-backups
#                                               (necesita rclone configurat pentru acest utilizator)
#   backup.keep-local     BACKUP_KEEP_LOCAL     cate backup-uri raman pe server (implicit 10);
#                                               cele mai vechi se sterg, cel nou ramane mereu
# Alte variabile: APP_DIR (implicit /var/www/html), BACKUP_DIR (implicit ~/awanabetania-backups).
#
# Cod de iesire: 0 = gata; 3 = backup-ul de pe server e facut, dar copia in afara serverului
# a esuat (deploy-ul si sezonul nou continua cu un avertisment); altceva = niciun backup.

set -euo pipefail
umask 077

# shellcheck source=lib/db-connection.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib/db-connection.sh"

LABEL="${1:-daily}"
BACKUP_DIR="${BACKUP_DIR:-$HOME/awanabetania-backups}"
REMOTE="${BACKUP_RCLONE_REMOTE:-$(resolve "$(prop 'backup\.rclone-remote')")}"
KEEP_LOCAL="${BACKUP_KEEP_LOCAL:-$(resolve "$(prop 'backup\.keep-local')")}"
KEEP_LOCAL="${KEEP_LOCAL:-10}"

[[ "$LABEL" =~ ^[A-Za-z0-9_-]+$ ]] || die "eticheta invalida: '$LABEL' (doar litere, cifre, - si _)"
[[ "$KEEP_LOCAL" =~ ^[1-9][0-9]*$ ]] || die "backup.keep-local trebuie sa fie un numar de la 1 in sus, nu '$KEEP_LOCAL'"
command -v mysqldump >/dev/null || die "lipseste mysqldump (sudo apt install mysql-client)"

db_connect
trap 'rm -f "$DB_CNF"' EXIT

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
FILE="$BACKUP_DIR/awana-$DB_NAME-$(date +%Y%m%d-%H%M%S)-$LABEL.sql.gz"

# --single-transaction: copie consistenta fara a bloca aplicatia (InnoDB)
# --no-tablespaces: nu cere privilegiul PROCESS, pe care utilizatorul aplicatiei nu il are
OPTS=(--single-transaction --quick --no-tablespaces --default-character-set=utf8mb4)
if mysqldump --help 2>/dev/null | grep -q -- '--set-gtid-purged'; then
    OPTS+=(--set-gtid-purged=OFF)   # restaurabil si pe un server fara GTID
fi

mysqldump --defaults-extra-file="$DB_CNF" --host="$DB_HOST" --port="$DB_PORT" "${OPTS[@]}" "$DB_NAME" \
    | gzip -9 > "$FILE.partial" \
    || { rm -f "$FILE.partial"; die "mysqldump a esuat; niciun backup nou nu a fost scris"; }

# mysqldump scrie "-- Dump completed" doar daca a ajuns la final.
if ! gzip -t "$FILE.partial" || ! zcat "$FILE.partial" | tail -n 1 | grep -q "Dump completed"; then
    rm -f "$FILE.partial"
    die "dump incomplet; l-am sters"
fi
mv "$FILE.partial" "$FILE"
echo "Backup: $FILE ($(du -h "$FILE" | cut -f1))"

REMOTE_FAILED=0
if [[ -n "$REMOTE" ]]; then
    if ! command -v rclone >/dev/null; then
        echo "$(basename "$0"): backup.rclone-remote e setat, dar rclone nu e instalat" >&2
        REMOTE_FAILED=1
    elif rclone copy "$FILE" "$REMOTE"; then
        echo "Copiat in $REMOTE"
    else
        echo "$(basename "$0"): copia in $REMOTE a esuat; backup-ul de pe server exista" >&2
        REMOTE_FAILED=1
    fi
fi

# Pe server raman doar cele mai noi $KEEP_LOCAL, dupa data fisierului; cel abia facut e mereu
# printre ele.
find "$BACKUP_DIR" -maxdepth 1 -name 'awana-*.sql.gz' -printf '%T@ %p\n' | sort -rn | cut -d' ' -f2- \
    | tail -n +"$((KEEP_LOCAL + 1))" | while read -r old; do
        rm -f -- "$old"
        echo "Sters de pe server (raman ultimele $KEEP_LOCAL): $(basename "$old")"
    done

[[ "$REMOTE_FAILED" -eq 0 ]] || exit 3
