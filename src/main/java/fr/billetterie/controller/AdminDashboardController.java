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
    @FXML private Label expiredCleanupLabel;

    @FXML
    public void initialize() {
        refreshMetrics();
    }

    @FXML
    public void refreshMetrics() {
        int deletedExpired = TicketCatalogDAO.cleanupExpiredTickets();

        usersCountLabel.setText(String.valueOf(ClientDAO.getAll().size()));
        ticketsCountLabel.setText(String.valueOf(TicketCatalogDAO.countTickets()));
        purchasesCountLabel.setText(String.valueOf(TicketCatalogDAO.countPurchases()));
        expiredCleanupLabel.setText(String.valueOf(deletedExpired));
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
