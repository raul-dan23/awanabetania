package com.awanabetania.awanabetania;

import com.awanabetania.awanabetania.Model.Child;
import com.awanabetania.awanabetania.Model.Department;
import com.awanabetania.awanabetania.Model.Leader;
import com.awanabetania.awanabetania.Repository.ChildRepository;
import com.awanabetania.awanabetania.Repository.DepartmentRepository;
import com.awanabetania.awanabetania.Repository.LeaderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private LeaderRepository leaderRepository;
    @Autowired
    private ChildRepository childRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("⏳ Inițializare date...");

        // 1. DEPARTAMENTE
        createDept("Lecție", 1, 3);
        createDept("Jocuri", 2, 6);
        createDept("Verset", 2, 5);
        createDept("Media", 1, 2);
        createDept("Social Media", 1, 2);
        createDept("Sală", 2, 4);
        createDept("Materiale", 1, 2);
        createDept("Secretariat", 1, 3);
        createDept("Agapă", 2, 5);

        // 2. LIDERI (User: Admin / Pass: 1234)
        createLeader("Raul", "Macovei", "DIRECTOR", "0774650819", null);


        System.out.println("✅ Datele sunt gata!");
    }

    private void createDept(String name, int min, int max) {
        if (departmentRepository.findByName(name).isEmpty()) {
            departmentRepository.save(new Department(name, min, max));
        }
    }

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