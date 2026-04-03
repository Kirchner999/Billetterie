package fr.billetterie.model;

public class Spectacle {

    private int id;
    private String titre;
    private String lieu;
    private String affiche;
    private String tags;
    private Integer duree;
    private String descriptionCourte;
    private String descriptionLongue;
    private String langue;
    private Integer ageMinimum;
    private String photos;

    public Spectacle() {
    }

    public Spectacle(String titre, String lieu, String affiche, String tags, Integer duree,
                     String descriptionCourte, String descriptionLongue, String langue,
                     Integer ageMinimum, String photos) {
        this(0, titre, lieu, affiche, tags, duree, descriptionCourte, descriptionLongue, langue, ageMinimum, photos);
    }

    public Spectacle(int id, String titre, String lieu, String affiche, String tags, Integer duree,
                     String descriptionCourte, String descriptionLongue, String langue,
                     Integer ageMinimum, String photos) {
        this.id = id;
        this.titre = titre;
        this.lieu = lieu;
        this.affiche = affiche;
        this.tags = tags;
        this.duree = duree;
        this.descriptionCourte = descriptionCourte;
        this.descriptionLongue = descriptionLongue;
        this.langue = langue;
        this.ageMinimum = ageMinimum;
        this.photos = photos;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public String getAffiche() {
        return affiche;
    }

    public void setAffiche(String affiche) {
        this.affiche = affiche;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getDuree() {
        return duree;
    }

    public void setDuree(Integer duree) {
        this.duree = duree;
    }

    public String getDescriptionCourte() {
        return descriptionCourte;
    }

    public void setDescriptionCourte(String descriptionCourte) {
        this.descriptionCourte = descriptionCourte;
    }

    public String getDescriptionLongue() {
        return descriptionLongue;
    }

    public void setDescriptionLongue(String descriptionLongue) {
        this.descriptionLongue = descriptionLongue;
    }

    public String getLangue() {
        return langue;
    }

    public void setLangue(String langue) {
        this.langue = langue;
    }

    public Integer getAgeMinimum() {
        return ageMinimum;
    }

    public void setAgeMinimum(Integer ageMinimum) {
        this.ageMinimum = ageMinimum;
    }

    public String getPhotos() {
        return photos;
    }

    public void setPhotos(String photos) {
        this.photos = photos;
    }

    public String getDescription() {
        return descriptionCourte;
    }

    public void setDescription(String description) {
        this.descriptionCourte = description;
    }

    @Override
    public String toString() {
        return titre + " (" + lieu + ")";
    }
}
