package fr.billetterie.controller;

import fr.billetterie.dao.ClientDAO;
import fr.billetterie.model.Client;
import fr.billetterie.utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import java.util.List;

public class AdminDashboardController {

    @FXML private VBox sidebar;

    @FXML
    public void initialize() {
        List<Client> clients = ClientDAO.getAll();
        System.out.println("Clients chargés : " + clients.size());
    }

    @FXML
    public void switchTheme() {
        ThemeManager.switchTheme(sidebar.getScene());
    }

    @FXML
    public void logout() {
        // action de déconnexion
        System.out.println("Déconnexion admin");
    }
}
