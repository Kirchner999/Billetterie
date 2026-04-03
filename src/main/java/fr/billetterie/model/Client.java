package fr.billetterie.model;

import java.time.LocalDateTime;

public class Client {

    private int id;
    private String pseudo;
    private String nom;
    private String prenom;
    private String numero;
    private String email;
    private String password;
    private String adresse;
    private String role;
    private boolean admin;
    private LocalDateTime dateCreation;

    public Client(int id, String pseudo, String password, String role) {
        this(id, pseudo, pseudo, "", null, pseudo, password, null, role, "ADMIN".equalsIgnoreCase(role), null);
    }

    public Client(int id, String pseudo, String nom, String prenom, String numero, String email,
                  String password, String adresse, String role, boolean admin, LocalDateTime dateCreation) {
        this.id = id;
        this.pseudo = pseudo;
        this.nom = nom;
        this.prenom = prenom;
        this.numero = numero;
        this.email = email;
        this.password = password;
        this.adresse = adresse;
        this.role = role;
        this.admin = admin;
        this.dateCreation = dateCreation;
    }

    public Client(String pseudo, String nom, String prenom, String numero, String email,
                  String password, String adresse, String role, boolean admin) {
        this(0, pseudo, nom, prenom, numero, email, password, adresse, role, admin, null);
    }

    public int getId() {
        return id;
    }

    public String getPseudo() {
        return pseudo;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getNumero() {
        return numero;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getAdresse() {
        return adresse;
    }

    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return admin || "ADMIN".equalsIgnoreCase(role);
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public String getUsername() {
        return pseudo;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public void setUsername(String username) {
        this.pseudo = username;
    }
}
