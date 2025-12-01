package fr.billetterie.model;

public class Client {

    private int id;
    private String pseudo;
    private String nom;
    private String prenom;
    private String numero;
    private String email;
    private String password;
    private String adresse;
    private boolean isAdmin;
    private String role;

    // ---------------------------------------------------------
    // ✅ CONSTRUCTEUR PRINCIPAL (celui utilisé dans tout le DAO)
    // ---------------------------------------------------------
    public Client(int id,
                  String pseudo,
                  String nom,
                  String prenom,
                  String numero,
                  String email,
                  String password,
                  String adresse,
                  boolean isAdmin,
                  String role) {

        this.id = id;
        this.pseudo = pseudo;
        this.nom = nom;
        this.prenom = prenom;
        this.numero = numero;
        this.email = email;
        this.password = password;
        this.adresse = adresse;
        this.isAdmin = isAdmin;
        this.role = role;
    }

    // ---------------------------------------------------------
    // ✅ CONSTRUCTEUR POUR L’INSCRIPTION (simplifié)
    // ---------------------------------------------------------
    public Client(String pseudo,
                  String nom,
                  String prenom,
                  String email,
                  String password,
                  String adresse,
                  String role) {

        this.id = 0;
        this.pseudo = pseudo;
        this.nom = nom;
        this.prenom = prenom;
        this.numero = "";
        this.email = email;
        this.password = password;
        this.adresse = adresse;
        this.isAdmin = role.equals("ADMIN");
        this.role = role;
    }

    // ---------------------------------------------------------
    // GETTERS / SETTERS
    // ---------------------------------------------------------

    public int getId() { return id; }
    public String getPseudo() { return pseudo; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getNumero() { return numero; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getAdresse() { return adresse; }
    public boolean isAdmin() { return isAdmin; }
    public String getRole() { return role; }

    public void setId(int id) { this.id = id; }
    public void setPseudo(String pseudo) { this.pseudo = pseudo; }
    public void setNom(String nom) { this.nom = nom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public void setNumero(String numero) { this.numero = numero; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
    public void setRole(String role) { this.role = role; }
}
