package com.awanabetania.awanabetania.Model;

public class ScoreRequest {

    private Integer childId;
    private Boolean attended;    // Prezent
    private Boolean hasBible;    // Manual
    private Boolean hasHandbook; // Carte
    private Boolean lesson;      // Tema
    private Boolean friend;      // Prieten
    private Boolean hasUniform;  // Uniforma
    private Integer extraPoints; // Puncte extra

    // --- GETTERI (Asta cauta Controllerul cand scrie request.get...) ---

    public Integer getChildId() {
        return childId;
    }

    public Boolean getAttended() {
        return attended;
    }

    public Boolean getHasBible() {
        return hasBible;
    }

    public Boolean getHasHandbook() {
        return hasHandbook;
    }

    public Boolean getLesson() {
        return lesson;
    }

    public Boolean getFriend() {
        return friend;
    }

    public Boolean getHasUniform() {
        return hasUniform;
    }

    public Integer getExtraPoints() {
        return extraPoints;
    }

    // --- SETTERI (Asta foloseste Java ca sa puna datele venite din site) ---

    public void setChildId(Integer childId) {
        this.childId = childId;
    }

    public void setAttended(Boolean attended) {
        this.attended = attended;
    }

    public void setHasBible(Boolean hasBible) {
        this.hasBible = hasBible;
    }

    public void setHasHandbook(Boolean hasHandbook) {
        this.hasHandbook = hasHandbook;
    }

    public void setLesson(Boolean lesson) {
        this.lesson = lesson;
    }

    public void setFriend(Boolean friend) {
        this.friend = friend;
    }

    public void setHasUniform(Boolean hasUniform) {
        this.hasUniform = hasUniform;
    }

    public void setExtraPoints(Integer extraPoints) {
        this.extraPoints = extraPoints;
    }
}