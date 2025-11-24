package fr.billetterie.model;

import java.time.LocalDateTime;

public class Billet {

    private int id;
    private String numero;
    private int idRepresentation;
    private int idClient;
    private String statut;
    private LocalDateTime dateAchat;

    // --- CONSTRUCTEURS ---
    public Billet() {}

    public Billet(String numero, int idRepresentation, int idClient, String statut) {
        this.numero = numero;
        this.idRepresentation = idRepresentation;
        this.idClient = idClient;
        this.statut = statut;
    }

    public Billet(int id, String numero, int idRepresentation, int idClient, String statut, LocalDateTime dateAchat) {
        this.id = id;
        this.numero = numero;
        this.idRepresentation = idRepresentation;
        this.idClient = idClient;
        this.statut = statut;
        this.dateAchat = dateAchat;
    }

    // --- GETTERS ---
    public int getId() { return id; }
    public String getNumero() { return numero; }
    public int getIdRepresentation() { return idRepresentation; }
    public int getIdClient() { return idClient; }
    public String getStatut() { return statut; }
    public LocalDateTime getDateAchat() { return dateAchat; }

    // --- SETTERS ---
    public void setId(int id) { this.id = id; }
    public void setNumero(String numero) { this.numero = numero; }
    public void setIdRepresentation(int idRepresentation) { this.idRepresentation = idRepresentation; }
    public void setIdClient(int idClient) { this.idClient = idClient; }
    public void setStatut(String statut) { this.statut = statut; }
    public void setDateAchat(LocalDateTime dateAchat) { this.dateAchat = dateAchat; }

    @Override
    public String toString() {
        return "Billet{" +
                "id=" + id +
                ", numero='" + numero + '\'' +
                ", idRepresentation=" + idRepresentation +
                ", idClient=" + idClient +
                ", statut='" + statut + '\'' +
                ", dateAchat=" + dateAchat +
                '}';
    }
}
