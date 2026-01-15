package com.awanabetania.awanabetania.Controller;

import com.awanabetania.awanabetania.Model.*;
import com.awanabetania.awanabetania.Repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Acest Controller gestionează operațiunile sensibile legate de conturile utilizatorilor.
 * Funcționalitatea principală este procesul de "Ștergere a Contului" în doi pași (Two-Step Deletion):
 * 1. Utilizatorul solicită ștergerea -> Se generează un cod secret trimis Administratorului.
 * 2. Utilizatorul introduce codul -> Contul și toate datele asociate sunt șterse definitiv din baza de date.
 */
@RestController
@RequestMapping("/api/account")
@CrossOrigin(origins = "*")
public class AccountController {

    // Repozitorii necesare pentru a accesa și modifica datele din tabele
    @Autowired private LeaderRepository leaderRepository;
    @Autowired private ChildRepository childRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private MeetingAssignmentRepository assignmentRepository;
    @Autowired private LeaderEvaluationRepository evaluationRepository;
    @Autowired private ScoreRepository scoreRepository;
    @Autowired private WarningRepository warningRepository;

    /**
     * EntityManager ne permite să executăm interogări SQL directe (Native Queries).
     * Este folosit aici pentru a șterge rapid datele corelate (ex: note, prezențe) înainte de a șterge utilizatorul.
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * PASUL 1: Solicitare Ștergere Cont.
     * <p>
     * Această metodă este apelată când utilizatorul apasă butonul "Solicită cod ștergere".
     * Nu șterge nimic încă, doar pregătește terenul.
     * </p>
     *
     * @param payload Un obiect JSON care conține:
     * - "id": ID-ul utilizatorului care cere ștergerea.
     * - "role": Rolul utilizatorului ("LEADER" sau "CHILD").
     * @return Un mesaj de confirmare sau eroare.
     */
    @PostMapping("/request-deletion")
    public ResponseEntity<?> requestDeletion(@RequestBody Map<String, Object> payload) {
        Integer id = (Integer) payload.get("id");
        String role = (String) payload.get("role"); // "LEADER" sau "CHILD"

        // Protectie: Adminul principal (ID 1) nu poate fi șters pentru a nu bloca aplicația.
        if ("LEADER".equalsIgnoreCase(role) && id == 1) {
            return ResponseEntity.badRequest().body("Administratorul nu poate fi șters!");
        }

        // Generăm un cod unic, scurt și aleatoriu (ex: A3F2)
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String userName = "";

        // Salvăm codul temporar în contul utilizatorului (în baza de date)
        if ("CHILD".equalsIgnoreCase(role)) {
            Child c = childRepository.findById(id).orElse(null);
            if (c == null) return ResponseEntity.badRequest().body("Utilizator inexistent");
            c.setDeletionCode(code);
            childRepository.save(c);
            userName = c.getName() + " " + c.getSurname() + " (Copil)";
        } else {
            Leader l = leaderRepository.findById(id).orElse(null);
            if (l == null) return ResponseEntity.badRequest().body("Utilizator inexistent");
            l.setDeletionCode(code);
            leaderRepository.save(l);
            userName = l.getName() + " " + l.getSurname() + " (Lider)";
        }

        // Trimitem o notificare automată către Administrator (ID 1) cu acest cod.
        // Doar administratorul vede codul și îl poate comunica utilizatorului.
        String adminMsg = String.format("🗑️ SOLICITARE ȘTERGERE: %s vrea să își șteargă contul. Codul generat este: %s. Trimite-i acest cod pentru confirmare.", userName, code);
        Notification n = new Notification(adminMsg, "ALERT", "1", LocalDate.now());
        notificationRepository.save(n);

        return ResponseEntity.ok("Solicitare trimisă! Așteaptă codul de la Director.");
    }

    /**
     * PASUL 2: Ștergere Definitivă.
     * <p>
     * Această metodă execută ștergerea fizică a datelor.
     * Este marcată cu @Transactional pentru ca toate operațiunile să fie atomice:
     * ori se șterge totul cu succes, ori nu se șterge nimic (în caz de eroare), pentru a nu corupe baza de date.
     * </p>
     *
     * @param payload Un obiect JSON care conține:
     * - "id": ID-ul utilizatorului.
     * - "role": Rolul ("LEADER" sau "CHILD").
     * - "code": Codul primit de la director, introdus de utilizator.
     * @return Mesaj de succes sau eroare dacă codul este greșit.
     */
    @PostMapping("/delete")
    @Transactional // Foarte important pentru integritatea datelor
    public ResponseEntity<?> deleteAccount(@RequestBody Map<String, Object> payload) {
        Integer id = (Integer) payload.get("id");
        String role = (String) payload.get("role");
        String code = (String) payload.get("code");

        // Protectie suplimentară pentru Admin
        if ("LEADER".equalsIgnoreCase(role) && id == 1) return ResponseEntity.badRequest().body("Adminul nu se poate șterge.");

        // --- LOGICA PENTRU COPIL ---
        if ("CHILD".equalsIgnoreCase(role)) {
            Child c = childRepository.findById(id).orElse(null);

            // Verificăm dacă codul introdus corespunde cu cel salvat în baza de date
            if (c == null || c.getDeletionCode() == null || !c.getDeletionCode().equals(code)) {
                return ResponseEntity.badRequest().body("Cod incorect sau utilizator invalid!");
            }

            // --- CURĂȚENIE ÎN CASCADĂ (COPIL) ---
            // Ștergem manual toate datele asociate pentru a evita erori de tip "Foreign Key Constraint"

            // 1. Ștergem punctajele (istoricul prezențelor)
            entityManager.createNativeQuery("DELETE FROM scores WHERE child_id = ?1").setParameter("1", id).executeUpdate();

            // 2. Ștergem avertismentele și sancțiunile
            entityManager.createNativeQuery("DELETE FROM warnings WHERE child_id = ?1").setParameter("1", id).executeUpdate();

            // 3. Ștergem legătura cu echipele (tabelul de legătură children_teams)
            entityManager.createNativeQuery("DELETE FROM children_teams WHERE child_id = ?1").setParameter("1", id).executeUpdate();

            // 4. La final, ștergem copilul propriu-zis
            childRepository.delete(c);

            return ResponseEntity.ok("Cont copil șters definitiv!");

        }
        // --- LOGICA PENTRU LIDER ---
        else {
            Leader l = leaderRepository.findById(id).orElse(null);

            // Verificare cod
            if (l == null || l.getDeletionCode() == null || !l.getDeletionCode().equals(code)) {
                return ResponseEntity.badRequest().body("Cod incorect sau utilizator invalid!");
            }

            // --- CURĂȚENIE ÎN CASCADĂ (LIDER) ---
            // Un lider are mult mai multe responsabilități, deci curățenia este mai complexă.

            // 1. Îl scoatem de la conducerea departamentelor (Dacă era șef, departamentul rămâne fără șef, nu se șterge departamentul)
            List<Department> deps = departmentRepository.findAll();
            for(Department d : deps) {
                if(d.getHeadLeader() != null && d.getHeadLeader().getId().equals(id)) {
                    d.setHeadLeader(null);
                    departmentRepository.save(d);
                }
            }

            // 2. Îl scoatem de la conducerea serilor (Dacă era Director de zi într-o seară trecută sau viitoare)
            List<Meeting> meetings = meetingRepository.findAll();
            for(Meeting m : meetings) {
                if(m.getDirectorDay() != null && m.getDirectorDay().getId().equals(id)) {
                    m.setDirectorDay(null);
                    meetingRepository.save(m);
                }
            }

            // 3. Ștergem asignările din orar (planificările unde trebuia să slujească)
            entityManager.createNativeQuery("DELETE FROM meeting_assignments WHERE leader_id = ?1").setParameter("1", id).executeUpdate();

            // 4. Ștergem evaluările (feedback-ul) primit ca lider SAU dat ca director altora
            entityManager.createNativeQuery("DELETE FROM leader_evaluations WHERE leader_id = :id OR evaluated_by = ?1").setParameter("1", id).executeUpdate();

            // 5. Ștergem legătura cu departamentele (apartenența ca membru)
            entityManager.createNativeQuery("DELETE FROM leaders_departments WHERE leader_id = ?1").setParameter("1", id).executeUpdate();

            // 6. Ștergem notificările care îi erau destinate
            entityManager.createNativeQuery("DELETE FROM notifications WHERE visible_to = ?1").setParameter("1", String.valueOf(id)).executeUpdate();

            // 7. La final, ștergem contul liderului
            leaderRepository.delete(l);

            return ResponseEntity.ok("Cont lider șters definitiv!");
        }
    }
}