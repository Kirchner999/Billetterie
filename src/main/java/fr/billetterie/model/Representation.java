package fr.billetterie.model;

import java.time.LocalDateTime;

public class Representation {

    private int id;
    private int idSpectacle;
    private LocalDateTime dateHeure;
    private String salle;

    public Representation() {
    }

    public Representation(int idSpectacle, LocalDateTime dateHeure, String salle) {
        this(0, idSpectacle, dateHeure, salle);
    }

    public Representation(int id, int idSpectacle, LocalDateTime dateHeure, String salle) {
        this.id = id;
        this.idSpectacle = idSpectacle;
        this.dateHeure = dateHeure;
        this.salle = salle;
    }

    public int getId() {
        return id;
    }

    public int getIdSpectacle() {
        return idSpectacle;
    }

    public LocalDateTime getDateHeure() {
        return dateHeure;
    }

    public String getSalle() {
        return salle;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setIdSpectacle(int idSpectacle) {
        this.idSpectacle = idSpectacle;
    }

    public void setDateHeure(LocalDateTime dateHeure) {
        this.dateHeure = dateHeure;
    }

    public void setSalle(String salle) {
        this.salle = salle;
    }

    @Override
    public String toString() {
        return "Representation{" +
                "id=" + id +
                ", idSpectacle=" + idSpectacle +
                ", dateHeure=" + dateHeure +
                ", salle='" + salle + '\'' +
                '}';
    }
}
