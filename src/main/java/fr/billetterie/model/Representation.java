package fr.billetterie.model;

import java.time.LocalDateTime;

public class Representation {

    private int id;
    private int idSpectacle;
    private LocalDateTime dateHeure;
    private double prix;
    private int capacite;

    public Representation() {}

    public Representation(int idSpectacle, LocalDateTime dateHeure, double prix, int capacite) {
        this.idSpectacle = idSpectacle;
        this.dateHeure = dateHeure;
        this.prix = prix;
        this.capacite = capacite;
    }

    public Representation(int id, int idSpectacle, LocalDateTime dateHeure, double prix, int capacite) {
        this.id = id;
        this.idSpectacle = idSpectacle;
        this.dateHeure = dateHeure;
        this.prix = prix;
        this.capacite = capacite;
    }

    // ---------------- GETTERS / SETTERS ----------------

    public int getId() { return id; }
    public int getIdSpectacle() { return idSpectacle; }
    public LocalDateTime getDateHeure() { return dateHeure; }
    public double getPrix() { return prix; }
    public int getCapacite() { return capacite; }

    public void setId(int id) { this.id = id; }
    public void setIdSpectacle(int idSpectacle) { this.idSpectacle = idSpectacle; }
    public void setDateHeure(LocalDateTime dateHeure) { this.dateHeure = dateHeure; }
    public void setPrix(double prix) { this.prix = prix; }
    public void setCapacite(int capacite) { this.capacite = capacite; }

    @Override
    public String toString() {
        return "Representation{" +
                "id=" + id +
                ", idSpectacle=" + idSpectacle +
                ", dateHeure=" + dateHeure +
                ", prix=" + prix +
                ", capacite=" + capacite +
                '}';
    }
}
