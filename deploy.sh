#!/bin/bash
# Deploy pe serverul de productie.
#
# Aplicatia este gestionata de systemd (awanabetania.service), deci scriptul NU
# porneste procesul el insusi. Versiunea veche facea `pkill` + `nohup java -jar`,
# ceea ce lasa un proces orfan in afara systemd: systemd il repornea in bucla
# fara sa poata ocupa portul, iar traficul era servit de procesul orfan. Acela
# nu supravietuia unui reboot.
#
# Configuratia de productie (credentiale DB, chei) sta in
# /var/www/html/application.properties — netrackat in git, citit de Spring din
# directorul de lucru al serviciului. Scriptul verifica prezenta lui inainte de
# repornire.

set -euo pipefail

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

SERVICE=awanabetania
APP_DIR=/var/www/html
CONFIG="$APP_DIR/application.properties"

cd "$APP_DIR"

# ===== PAS 0: verificari preliminare =====
if [ ! -f "$CONFIG" ]; then
    echo -e "${RED}Lipseste $CONFIG — aplicatia nu ar porni. Opresc.${NC}"
    exit 1
fi

# ===== PAS 1: codul nou de pe Git =====
echo -e "${GREEN}Pas1: Aduc codul de pe Git…${NC}"
git fetch --all
git reset --hard origin/main

# ===== PAS 2: Build React =====
echo -e "${GREEN}Pas2: Build React…${NC}"
cd Frontend
npm install
npm run build
cd ..

mkdir -p src/main/resources/static
cp -R Frontend/dist/* src/main/resources/static/

# ===== PAS 3: Build backend =====
echo -e "${GREEN}Pas3: Build backend…${NC}"
chmod +x mvnw
./mvnw clean package -DskipTests

JAR_FILE=$(ls -t target/*.jar | head -n 1)
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}Eroare: nu s-a creat jar!${NC}"
    exit 1
fi

# ===== PAS 4: Repornire prin systemd =====
echo -e "${GREEN}Pas4: Repornesc serviciul…${NC}"
sudo systemctl restart "$SERVICE"

# ===== PAS 5: Verificare =====
echo -e "${GREEN}Pas5: Astept sa raspunda…${NC}"
for _ in $(seq 1 45); do
    sleep 1
    if [ "$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 http://localhost:8080/ || true)" = "200" ]; then
        echo -e "${GREEN}GATA! Aplicatia raspunde.${NC}"
        echo -e "Log: ${GREEN}sudo journalctl -u $SERVICE -f${NC}"
        exit 0
    fi
done

echo -e "${RED}Aplicatia nu raspunde dupa 45s. Ultimele linii din jurnal:${NC}"
sudo journalctl -u "$SERVICE" --no-pager -n 30
exit 1
