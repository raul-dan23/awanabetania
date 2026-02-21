package com.awanabetania.awanabetania;

import com.awanabetania.awanabetania.Model.Child; // Import nou pentru copii
import com.awanabetania.awanabetania.Model.Department;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Model.Sticker;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.DepartmentRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Repository.StickerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Această clasă se execută o singură dată, la pornirea aplicației.
 * Rolul ei este să populeze baza de date cu datele inițiale necesare (Seed Data),
 * cum ar fi: Departamentele, Contul de Admin și Stickerele pentru joc.
 * DE ASEMENEA: Curăță și generează username-uri pentru utilizatorii vechi.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private LeaderRepository leaderRepository;
    @Autowired
    private ChildRepository childRepository;
    @Autowired
    private StickerRepository stickerRepository;

    /**
     * Metoda principală care rulează la start-up.
     */
    @Override
    public void run(String... args) throws Exception {
        System.out.println("⏳ Inițializare date...");

        // 1. DEPARTAMENTE (Daca nu exista, le cream)
        createDept("Lecție", 1, 3);
        createDept("Jocuri", 2, 6);
        createDept("Media", 1, 2);
        createDept("Social Media", 1, 2);
        createDept("Sală", 2, 4);
        createDept("Materiale", 1, 2);
        createDept("Secretariat", 1, 3);
        createDept("Agapă", 2, 5);

        // 2. LIDERI (User: Admin / Pass: 1234)
        createLeader("Raul", "Macovei", "DIRECTOR", "0774650819", null);

        // 3. STICKERE (GAMIFICATION)
        // MODIFICARE: Verificăm dacă primul sticker are calea setată.
        // Dacă nu o are (e null), ștergem tot și regenerăm.
        boolean needRegeneration = false;
        if (stickerRepository.count() > 0) {
            Sticker first = stickerRepository.findAll().get(0);
            if (first.getImagePath() == null) {
                needRegeneration = true;
            }
        } else {
            needRegeneration = true;
        }

        if (needRegeneration) {
            System.out.println("🔄 Regenerare Stickere cu imagini...");
            stickerRepository.deleteAll(); // Ștergem vechiturile

            for (int i = 1; i <= 30; i++) {
                Sticker s = new Sticker();
                s.setName("Rank " + i);

                // Aici setăm calea corectă
                s.setImagePath("/stickers/" + i + ".png");

                stickerRepository.save(s);
            }
        }

        // ==========================================
        // 4. REPARARE COPII VECHI (Generare Username)
        // ==========================================
        System.out.println("🔧 Verificare username-uri copii...");
        for (Child c : childRepository.findAll()) {
            if (c.getUsername() == null || c.getUsername().isEmpty()) {
                String baseUsername = generateCleanUsername(c.getName(), c.getSurname());
                // Daca mai exista cineva cu exact acelasi username, ii adaugam ID-ul la final ca sa fie unic
                if (childRepository.findByUsername(baseUsername).isPresent()) {
                    baseUsername += c.getId();
                }
                c.setUsername(baseUsername);
                childRepository.save(c);
            }
        }

        // ==========================================
        // 5. REPARARE LIDERI VECHI (Generare Username)
        // ==========================================
        System.out.println("🔧 Verificare username-uri lideri...");
        for (Leader l : leaderRepository.findAll()) {
            if (l.getUsername() == null || l.getUsername().isEmpty()) {
                String baseUsername = generateCleanUsername(l.getName(), l.getSurname());
                if (leaderRepository.findByUsername(baseUsername).isPresent()) {
                    baseUsername += l.getId();
                }
                l.setUsername(baseUsername);
                leaderRepository.save(l);
            }
        }

        System.out.println("✅ Datele sunt gata!");
    }

    /**
     * Creează un departament doar dacă nu există deja unul cu același nume.
     * @param name Numele departamentului (ex: "Jocuri")
     * @param min Numărul minim de lideri
     * @param max Numărul maxim de lideri
     */
    private void createDept(String name, int min, int max) {
        if (departmentRepository.findByName(name).isEmpty()) {
            departmentRepository.save(new Department(name, min, max));
        }
    }

    /**
     * Creează un lider (sau Admin) doar dacă nu există deja.
     * @param name Prenume
     * @param surname Nume
     * @param role Rol (DIRECTOR, LEADER, COORDONATOR)
     * @param phone Telefon
     * @param deptName (Opțional) Numele departamentului unde să fie asignat
     */
    private void createLeader(String name, String surname, String role, String phone, String deptName) {
        if (leaderRepository.findByNameAndSurname(name, surname).isEmpty()) {
            Leader l = new Leader(name, surname, role, "1234", phone);
            if (deptName != null) {
                Optional<Department> d = departmentRepository.findByName(deptName);
                d.ifPresent(dep -> l.getDepartments().add(dep));
            }
            leaderRepository.save(l);
        }
    }

    /**
     * Funcția care curăță numele pentru a genera un username sigur:
     * Ex: "David Ștefan" + "Popescu" -> "davidstefanpopescu"
     * * @param name Prenumele utilizatorului
     * @param surname Numele de familie
     * @return String curățat, fără spații și diacritice, cu litere mici
     */
    public static String generateCleanUsername(String name, String surname) {
        if (name == null) name = "";
        if (surname == null) surname = "";
        String raw = (name + surname).replaceAll("\\s+", "").toLowerCase();

        // Scoatem diacriticele romanesti
        return raw.replace("ă", "a").replace("â", "a").replace("î", "i")
                .replace("ș", "s").replace("ț", "t")
                .replace("ş", "s").replace("ţ", "t");
    }
}