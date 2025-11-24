package fr.billetterie.model;

public class Spectacle {

    private int id;
    private String titre;
    private String description;
    private String lieu;

    // --- Constructeurs ---
    public Spectacle() {}

    public Spectacle(String titre, String description, String lieu) {
        this.titre = titre;
        this.description = description;
        this.lieu = lieu;
    }

    public Spectacle(int id, String titre, String description, String lieu) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.lieu = lieu;
    }

    // --- Getters et Setters ---
    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }

    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getLieu() { return lieu; }

    public void setLieu(String lieu) { this.lieu = lieu; }

    @Override
    public String toString() {
        return titre + " (" + lieu + ")";
    }
}
