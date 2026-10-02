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
# Variabile optionale:
#   APP_DIR                 directorul aplicatiei            (implicit /var/www/html)
#   BACKUP_DIR              unde se scriu backup-urile        (implicit ~/awanabetania-backups)
#   BACKUP_RETENTION_DAYS   backup-urile mai vechi se sterg   (implicit 30)
#   BACKUP_RCLONE_REMOTE    copie in afara serverului, ex. gdrive:awana-backups (necesita rclone)

set -euo pipefail
umask 077

# shellcheck source=lib/db-connection.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib/db-connection.sh"

LABEL="${1:-daily}"
BACKUP_DIR="${BACKUP_DIR:-$HOME/awanabetania-backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"

[[ "$LABEL" =~ ^[A-Za-z0-9_-]+$ ]] || die "eticheta invalida: '$LABEL' (doar litere, cifre, - si _)"
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

if [[ -n "${BACKUP_RCLONE_REMOTE:-}" ]]; then
    command -v rclone >/dev/null || die "BACKUP_RCLONE_REMOTE e setat, dar rclone nu e instalat"
    rclone copy "$FILE" "$BACKUP_RCLONE_REMOTE" || die "copia in $BACKUP_RCLONE_REMOTE a esuat (backup-ul local exista)"
    echo "Copiat in $BACKUP_RCLONE_REMOTE"
fi

find "$BACKUP_DIR" -maxdepth 1 -name 'awana-*.sql.gz' -mtime +"$RETENTION_DAYS" -print -delete \
    | sed 's/^/Sters (mai vechi de '"$RETENTION_DAYS"' zile): /'
