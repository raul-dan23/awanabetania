#!/bin/bash

# Culori pentru output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

# ===== PAS 1: Oprim procesul Java =====
echo -e "${GREEN}Pas1: Oprim ce ruleaza Java…${NC}"
# Folosim sudo ca să avem permisiune
sudo pkill -f 'awanabetania' || true

# ===== PAS 2: Adaugam codul nou de pe Git =====
echo -e "${GREEN}Pas2: Punem noul cod de pe Git…${NC}"
git fetch --all
git reset --hard origin/main

# ===== PAS 3: Build React =====
echo -e "${GREEN}Pas3: Build React…${NC}"
cd Frontend || { echo -e "${RED}Nu exista folder Frontend!${NC}"; exit 1; }

echo -e "${GREEN}Instalam dependentele si build-ui…${NC}"
npm install
npm run build

# Verificam daca dist exista
if [ ! -d "dist" ]; then
    echo -e "${RED}Eroare: dist nu s-a creat!${NC}"
    exit 1
fi
cd ..

# ===== PAS 4: Mut dist in backend =====
echo -e "${GREEN}Pas4: Mut dist in backend Java…${NC}"
rm -rf src/main/resources/static
mkdir -p src/main/resources/static
cp -R Frontend/dist/* src/main/resources/static/

# ===== PAS 5: Build backend =====
echo -e "${GREEN}Pas5: Build backend…${NC}"
chmod +x mvnw
./mvnw clean package -DskipTests

# Verificam daca jar-ul exista
JAR_FILE=$(ls target/*.jar | head -n 1)
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}Eroare: nu s-a creat jar!${NC}"
    exit 1
fi

# ===== PAS 6: Pornim serverul =====
echo -e "${GREEN}Pas6: Pornim server…${NC}"
nohup java -jar "$JAR_FILE" > log.txt 2>&1 &

echo -e "${GREEN}GATA! Aplicatia ruleaza.${NC}"
echo -e "Urmaresti log-ul cu: ${GREEN}tail -f log.txt${NC}"
