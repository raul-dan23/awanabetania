#!/usr/bin/env bash
# Restaureaza baza de date dintr-un backup facut cu backup-db.sh.
#
#   scripts/restore-db.sh ~/awanabetania-backups/awana-awana_club_manager-...-daily.sql.gz
#
# Pasii, in ordine:
#   1. cere confirmare: tastezi numele bazei de date
#   2. face un backup "pre-restore" al starii curente, ca sa poti reveni
#   3. opreste aplicatia (systemctl stop), daca ruleaza
#   4. sterge toate tabelele din baza si incarca backup-ul
#   5. porneste aplicatia la loc
#
# Contul folosit trebuie sa poata sterge si crea tabele (contul aplicatiei poate).
# Credentialele: aceleasi ca ale aplicatiei, vezi scripts/lib/db-connection.sh.
# RESTORE_CONFIRM=<nume baza> sare peste intrebare (pentru scripturi).

set -euo pipefail
umask 077

SCRIPTS="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/db-connection.sh
source "$SCRIPTS/lib/db-connection.sh"

SERVICE="${SERVICE:-awanabetania}"
FILE="${1:-}"

[[ -n "$FILE" ]] || die "folosire: $0 <backup.sql.gz>"
[[ -r "$FILE" ]] || die "nu pot citi $FILE"
command -v mysql >/dev/null || die "lipseste clientul mysql (sudo apt install mysql-client)"
gzip -t "$FILE" || die "$FILE nu e o arhiva gzip valida"
zcat "$FILE" | tail -n 1 | grep -q "Dump completed" || die "$FILE e un dump incomplet"

db_connect
trap 'rm -f "$DB_CNF"' EXIT
sql() { mysql --defaults-extra-file="$DB_CNF" --host="$DB_HOST" --port="$DB_PORT" "$@"; }

echo "Restaurez $FILE"
echo "in baza '$DB_NAME' de pe $DB_HOST:$DB_PORT. Datele curente vor fi INLOCUITE."
if [[ "${RESTORE_CONFIRM:-}" != "$DB_NAME" ]]; then
    read -r -p "Tasteaza numele bazei ($DB_NAME) ca sa continui: " answer
    [[ "$answer" == "$DB_NAME" ]] || die "anulat"
fi

echo "1/4 Backup al starii curente..."
"$SCRIPTS/backup-db.sh" pre-restore

restart=false
if command -v systemctl >/dev/null && systemctl is-active --quiet "$SERVICE" 2>/dev/null; then
    echo "2/4 Opresc $SERVICE..."
    sudo systemctl stop "$SERVICE"
    restart=true
else
    echo "2/4 $SERVICE nu ruleaza; nu e nimic de oprit."
fi

echo "3/4 Inlocuiesc tabelele..."
tables="$(sql -N -B -e "SELECT CONCAT('\`', table_name, '\`') FROM information_schema.tables
                        WHERE table_schema = '$DB_NAME' AND table_type = 'BASE TABLE'" | paste -sd, -)"
if [[ -n "$tables" ]]; then
    sql "$DB_NAME" -e "SET FOREIGN_KEY_CHECKS = 0; DROP TABLE $tables; SET FOREIGN_KEY_CHECKS = 1;"
fi
if ! zcat "$FILE" | sql "$DB_NAME"; then
    echo "Restaurarea a esuat la jumatate; aplicatia ramane oprita." >&2
    echo "Revino la starea de dinainte cu cel mai nou backup *-pre-restore.sql.gz:" >&2
    echo "  $0 \$(ls -t ${BACKUP_DIR:-$HOME/awanabetania-backups}/*-pre-restore.sql.gz | head -1)" >&2
    exit 1
fi

if $restart; then
    echo "4/4 Pornesc $SERVICE..."
    sudo systemctl start "$SERVICE"
else
    echo "4/4 Gata (aplicatia nu rula, n-am pornit-o)."
fi
echo "Restaurare reusita."
