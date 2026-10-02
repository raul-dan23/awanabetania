package com.awanabetania.awanabetania;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Department;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Model.Sticker;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.DepartmentRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import com.awanabetania.awanabetania.Repository.StickerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Optional;

/**
 * Seeds the database with required initial data on every application startup.
 * Creates default departments, the admin leader account, and the ranked sticker catalog.
 * Also performs a one-time migration to generate usernames for legacy accounts
 * that were created before username generation was introduced.
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
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Runs all seed and migration tasks at startup.
     * Each section is idempotent — it checks before inserting to avoid duplicates.
     *
     * @param args command-line arguments (unused)
     * @throws Exception if any repository operation fails unexpectedly
     */
    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting data initialization...");

        // 1. Seed departments
        createDept("Lectie", 1, 3);
        createDept("Jocuri", 2, 6);
        createDept("Media", 1, 2);
        createDept("Social Media", 1, 2);
        createDept("Sala", 2, 4);
        createDept("Materiale", 1, 2);
        createDept("Secretariat", 1, 3);
        createDept("Agapa", 2, 5);

        // 2. Seed admin leader account (random one-time password, printed once)
        createLeader("Raul", "Macovei", "DIRECTOR", "0774650819", null);

        // 3. Seed stickers — regenerate the full set if imagePath is missing on any entry
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
            System.out.println("Regenerating stickers with image paths...");
            stickerRepository.deleteAll();
            for (int i = 1; i <= 30; i++) {
                Sticker s = new Sticker();
                s.setName("Rank " + i);
                s.setImagePath("/stickers/" + i + ".png");
                stickerRepository.save(s);
            }
        }

        // 4. Backfill usernames for children created before username generation
        System.out.println("Checking child usernames...");
        for (Child c : childRepository.findAll()) {
            if (c.getUsername() == null || c.getUsername().isEmpty()) {
                String baseUsername = generateCleanUsername(c.getName(), c.getSurname());
                if (childRepository.findByUsername(baseUsername).isPresent()) {
                    baseUsername += c.getId();
                }
                c.setUsername(baseUsername);
                childRepository.save(c);
            }
        }

        // 5. Backfill usernames for leaders created before username generation
        System.out.println("Checking leader usernames...");
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

        System.out.println("Data initialization complete.");
    }

    /**
     * Creates a department only if one with the given name does not already exist.
     *
     * @param name       display name (e.g. "Jocuri")
     * @param min        minimum number of assigned leaders
     * @param max        maximum number of assigned leaders
     */
    private void createDept(String name, int min, int max) {
        if (departmentRepository.findByName(name).isEmpty()) {
            departmentRepository.save(new Department(name, min, max));
        }
    }

    /**
     * Creates a leader account only if one with the same name and surname does not already exist.
     * <p>
     * The password used to be the fixed value "1234", which is public in this repository:
     * on a fresh database anyone could log in as the director. It is now random, stored as
     * a BCrypt hash, and printed once to the log so the operator can sign in and change it.
     *
     * @param name     first name
     * @param surname  last name
     * @param role     role string (e.g. "DIRECTOR", "LEADER")
     * @param phone    phone number
     * @param deptName optional department name to assign on creation; {@code null} for none
     */
    private void createLeader(String name, String surname, String role, String phone, String deptName) {
        if (leaderRepository.findByNameAndSurname(name, surname).isEmpty()) {
            String password = randomPassword();
            Leader l = new Leader(name, surname, role, passwordEncoder.encode(password), phone);
            System.out.println("Created leader " + name + " " + surname
                    + " with one-time password: " + password + " (change it after the first login)");
            if (deptName != null) {
                Optional<Department> d = departmentRepository.findByName(deptName);
                d.ifPresent(dep -> l.getDepartments().add(dep));
            }
            leaderRepository.save(l);
        }
    }

    /** 12 characters from an alphabet without look-alikes (0/O, 1/l/I). */
    private static String randomPassword() {
        String alphabet = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return sb.toString();
    }

    /**
     * Produces a safe login username from a first and last name by lowercasing,
     * stripping whitespace, and replacing Romanian diacritics with ASCII equivalents.
     * Example: "David Stefan" + "Popescu" → "davidstefanpopescu".
     *
     * @param name    first name (may be {@code null})
     * @param surname last name (may be {@code null})
     * @return normalized ASCII username, lower-case, no spaces
     */
    public static String generateCleanUsername(String name, String surname) {
        if (name == null) name = "";
        if (surname == null) surname = "";
        String raw = (name + surname).replaceAll("\\s+", "").toLowerCase();
        return raw
                .replace("ă", "a").replace("â", "a").replace("î", "i")
                .replace("ș", "s").replace("ț", "t")
                .replace("ş", "s").replace("ţ", "t");
    }
}
