package fr.billetterie.controller;

import fr.billetterie.App;
import fr.billetterie.dao.ClientDAO;
import fr.billetterie.dao.TicketCatalogDAO;
import fr.billetterie.utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AdminDashboardController {

    @FXML private Label usersCountLabel;
    @FXML private Label ticketsCountLabel;
    @FXML private Label purchasesCountLabel;

    @FXML
    public void initialize() {
        usersCountLabel.setText("Utilisateurs: " + ClientDAO.getAll().size());
        ticketsCountLabel.setText("Evenements: " + TicketCatalogDAO.countTickets());
        purchasesCountLabel.setText("Achats: " + TicketCatalogDAO.countPurchases());
    }

    @FXML
    public void switchTheme() {
        ThemeManager.toggleTheme();
    }

    @FXML
    public void logout() {
        App.setCurrentUser(null);
        App.loadPage("Login.fxml");
    }
}
