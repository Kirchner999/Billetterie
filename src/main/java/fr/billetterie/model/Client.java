package fr.billetterie.model;

public class Client {

    private int id;
    private String username;
    private String password;
    private String role;

    public Client(int id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }

    public String getPseudo() {
        return username;
    }

    public String getNom() {
        return username;
    }

    public String getEmail() {
        return username;
    }
}
