package com.awanabetania.awanabetania;

import com.awanabetania.awanabetania.Model.Department;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Model.Sticker; // Import nou
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.DepartmentRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Repository.StickerRepository; // Import nou
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Această clasă se execută o singură dată, la pornirea aplicației.
 * Rolul ei este să populeze baza de date cu datele inițiale necesare (Seed Data),
 * cum ar fi: Departamentele, Contul de Admin și Stickerele pentru joc.
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
    private StickerRepository stickerRepository; // Repository pentru stickere

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
        // Generăm cele 30 de niveluri dacă tabelul este gol
        if (stickerRepository.count() == 0) {
            System.out.println("Generating Stickers (Rank 1-30)...");
            for (int i = 1; i <= 30; i++) {
                Sticker s = new Sticker();
                s.setName("Rank " + i);
                // Aici poți pune calea către imagini reale în viitor (ex: "/img/stickers/1.png")
                // Momentan lăsăm null, iar Frontend-ul va afișa numărul.
                s.setImagePath(null);
                stickerRepository.save(s);
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
}