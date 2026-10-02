# Conexiunea la baza de date, comuna pentru backup-db.sh si restore-db.sh (se include cu
# `source`, nu se ruleaza direct).
#
# db_connect seteaza DB_HOST, DB_PORT, DB_NAME si scrie in fisierul temporar $DB_CNF un
# fisier de optiuni MySQL cu utilizatorul si parola, ca parola sa nu apara niciodata in
# linia de comanda (ar fi vizibila in `ps`). Apelantul sterge $DB_CNF la iesire.
#
# Sursa credentialelor, in ordine:
#   1. BACKUP_MYSQL_CNF — fisier de optiuni MySQL ([client] user=... password=...), chmod 600
#   2. aceleasi setari ca aplicatia: spring.datasource.* din $APP_DIR/application.properties,
#      cu valori ${VAR} rezolvate din mediu (DB_URL, DB_USERNAME, DB_PASSWORD)
#
# shellcheck shell=bash
# shellcheck disable=SC2034  # DB_HOST, DB_PORT, DB_NAME, DB_CNF sunt citite de scripturile apelante

APP_DIR="${APP_DIR:-/var/www/html}"
PROPS="$APP_DIR/application.properties"

die() { echo "$(basename "$0"): $*" >&2; exit 1; }

# Valoarea unei chei din application.properties, citita ca in Java: ultima aparitie
# castiga, iar \x inseamna x (deci \\ e un singur backslash).
prop() {
    [[ -f "$PROPS" ]] || return 0
    sed -n "s/^[[:space:]]*$1[[:space:]]*[=:][[:space:]]*//p" "$PROPS" | tail -n 1 \
        | sed 's/\\\(.\)/\1/g'
}

# Rezolva ${VAR} si ${VAR:implicit} din mediu, ca Spring.
resolve() {
    local value="$1"
    if [[ "$value" =~ ^\$\{([A-Za-z_][A-Za-z0-9_]*)(:(.*))?\}$ ]]; then
        local name="${BASH_REMATCH[1]}" fallback="${BASH_REMATCH[3]}"
        value="${!name:-$fallback}"
    fi
    printf '%s' "$value"
}

db_connect() {
    local url user pass
    url="$(resolve "$(prop 'spring\.datasource\.url')")"
    url="${url:-${DB_URL:-jdbc:mysql://localhost:3306/awana_club_manager}}"

    # jdbc:mysql://host[:port]/baza[?parametri]
    [[ "$url" =~ ^jdbc:mysql://([^:/?]+)(:([0-9]+))?/([^?]+) ]] \
        || die "nu pot interpreta URL-ul bazei de date: $url"
    DB_HOST="${BASH_REMATCH[1]}"
    DB_PORT="${BASH_REMATCH[3]:-3306}"
    DB_NAME="${BASH_REMATCH[4]}"

    DB_CNF="$(mktemp)"
    chmod 600 "$DB_CNF"
    if [[ -n "${BACKUP_MYSQL_CNF:-}" ]]; then
        [[ -r "$BACKUP_MYSQL_CNF" ]] || die "nu pot citi $BACKUP_MYSQL_CNF"
        cp "$BACKUP_MYSQL_CNF" "$DB_CNF"
        return
    fi

    user="$(resolve "$(prop 'spring\.datasource\.username')")"
    pass="$(resolve "$(prop 'spring\.datasource\.password')")"
    user="${user:-${DB_USERNAME:-}}"
    pass="${pass:-${DB_PASSWORD:-}}"
    [[ -n "$user" ]] || die "nu gasesc utilizatorul BD (application.properties, DB_USERNAME sau BACKUP_MYSQL_CNF)"

    # In fisierele de optiuni MySQL, ghilimelele permit # si spatii; \ si " se escapeaza.
    pass="${pass//\\/\\\\}"
    pass="${pass//\"/\\\"}"
    printf '[client]\nuser="%s"\npassword="%s"\n' "$user" "$pass" > "$DB_CNF"
}
