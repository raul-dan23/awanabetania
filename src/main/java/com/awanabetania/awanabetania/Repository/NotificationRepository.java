package com.awanabetania.awanabetania.Repository;

import com.awanabetania.awanabetania.Model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    // 1. Metoda pentru DashboardController (caută strict după un rol, ex: DIRECTOR sau ALL)
    @Query("SELECT n FROM Notification n WHERE n.visibleTo = ?1 AND n.isVisible = true ORDER BY n.id DESC")
    List<Notification> findByVisibleTo(String role);

    // 2. Metoda pentru ChildController (caută notificări de premii active pentru un copil)
    @Query("SELECT n FROM Notification n WHERE n.childId = ?1 AND n.type = ?2 AND n.isVisible = true")
    List<Notification> findActiveByChildAndType(Integer childId, String type);

    // 3. Metoda care îți lipsea (folosită de NotificationController)
    // Caută mesajele mele (ID) SAU mesajele publice (ALL)
    @Query("SELECT n FROM Notification n WHERE (n.visibleTo = ?1 OR n.visibleTo = ?2) AND n.isVisible = true ORDER BY n.id DESC")
    List<Notification> findMyActiveNotifications(String leaderId, String all);


    void deleteByChildId(Integer childId);
    void deleteByVisibleTo(String visibleTo);



}