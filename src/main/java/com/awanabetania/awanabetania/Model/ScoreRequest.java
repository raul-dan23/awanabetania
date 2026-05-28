package com.awanabetania.awanabetania.Model;

/**
 * DTO carrying the scoring form data from the secretariat screen to {@code ScoreController}.
 * Each boolean flag corresponds to a point-eligible criterion; {@code extraPoints} allows
 * the secretariat to award ad-hoc bonus points.
 */
public class ScoreRequest {

    private Integer childId;
    private Boolean attended;
    private Boolean hasBible;
    private Boolean hasHandbook;
    private Boolean lesson;
    private Boolean friend;
    private Boolean hasUniform;
    private Integer extraPoints;

    /**
     * @return the ID of the child being scored
     */
    public Integer getChildId() { return childId; }

    /**
     * @return {@code true} if the child was present at the meeting
     */
    public Boolean getAttended() { return attended; }

    /**
     * @return {@code true} if the child brought their Bible
     */
    public Boolean getHasBible() { return hasBible; }

    /**
     * @return {@code true} if the child brought their handbook
     */
    public Boolean getHasHandbook() { return hasHandbook; }

    /**
     * @return {@code true} if the child completed the weekly lesson
     */
    public Boolean getLesson() { return lesson; }

    /**
     * @return {@code true} if the child brought a friend to the meeting
     */
    public Boolean getFriend() { return friend; }

    /**
     * @return {@code true} if the child wore their uniform
     */
    public Boolean getHasUniform() { return hasUniform; }

    /**
     * @return number of extra bonus points to award, or {@code null} if none
     */
    public Integer getExtraPoints() { return extraPoints; }

    public void setChildId(Integer childId) { this.childId = childId; }
    public void setAttended(Boolean attended) { this.attended = attended; }
    public void setHasBible(Boolean hasBible) { this.hasBible = hasBible; }
    public void setHasHandbook(Boolean hasHandbook) { this.hasHandbook = hasHandbook; }
    public void setLesson(Boolean lesson) { this.lesson = lesson; }
    public void setFriend(Boolean friend) { this.friend = friend; }
    public void setHasUniform(Boolean hasUniform) { this.hasUniform = hasUniform; }
    public void setExtraPoints(Integer extraPoints) { this.extraPoints = extraPoints; }
}
