package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    /**
     * 1. Metoda pentru Admin / General
     * Gaseste toate notificarile care nu sunt sterse.
     */
    List<Notification> findByIsVisibleTrueOrderByIdDesc();

    /**
     * 2. Metoda care iti dadea EROARE ACUM (findMyActiveNotifications).
     * O definim aici explicit cu @Query ca sa functioneze.
     */
    @Query("SELECT n FROM Notification n WHERE (n.visibleTo = ?1 OR n.visibleTo = ?2) AND n.isVisible = true ORDER BY n.id DESC")
    List<Notification> findMyActiveNotifications(String leaderId, String all);

    /**
     * 3. Metoda care iti dadea EROARE INAINTE (findByVisibleToOrVisibleToOrderByIdDesc).
     * O pastram si pe asta ca sa fim siguri ca nu crapa in alte locuri.
     * Face exact acelasi lucru ca cea de sus.
     */
    @Query("SELECT n FROM Notification n WHERE (n.visibleTo = ?1 OR n.visibleTo = ?2) AND n.isVisible = true ORDER BY n.id DESC")
    List<Notification> findByVisibleToOrVisibleToOrderByIdDesc(String visibleTo1, String visibleTo2);
}