package fr.billetterie.controller;

import fr.billetterie.App;
import fr.billetterie.model.Client;
import fr.billetterie.model.Purchase;
import fr.billetterie.model.Ticket;
import fr.billetterie.repository.DaoTicketStoreRepository;
import fr.billetterie.repository.PurchaseOperationResult;
import fr.billetterie.repository.TicketStoreRepository;
import fr.billetterie.service.PurchaseService;
import fr.billetterie.utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ClientDashboardController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private VBox contentPane;

    private final TicketStoreRepository ticketStoreRepository = new DaoTicketStoreRepository();
    private final PurchaseService purchaseService = new PurchaseService(ticketStoreRepository);

    @FXML
    public void initialize() {
        Client user = App.getCurrentUser();
        String nom = user != null ? user.getUsername() : "utilisateur";
        contentPane.getChildren().setAll(new Label("Bienvenue, " + nom + " !"));
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

    @FXML
    public void showSpectacles() {
        List<Ticket> tickets = ticketStoreRepository.getAvailableTickets();
        VBox box = new VBox(12);
        box.getChildren().add(new Label("Evenements disponibles"));

        if (tickets.isEmpty()) {
            box.getChildren().add(new Label("Aucun evenement disponible."));
        } else {
            for (Ticket ticket : tickets) {
                VBox details = new VBox(6);
                details.getChildren().addAll(
                        new Label(ticket.eventName()),
                        new Label("Date: " + DATE_FORMAT.format(ticket.eventDate())),
                        new Label("Prix: " + ticket.price() + " EUR"),
                        new Label("Stock: " + ticket.stock())
                );

                Button buyButton = new Button("Acheter");
                buyButton.setOnAction(event -> handlePurchase(ticket));

                HBox row = new HBox(16, details, buyButton);
                box.getChildren().add(row);
            }
        }

        contentPane.getChildren().setAll(box);
    }

    @FXML
    public void showMesBillets() {
        Client user = App.getCurrentUser();
        VBox box = new VBox(10);
        box.getChildren().add(new Label("Mes achats"));

        if (user == null) {
            box.getChildren().add(new Label("Aucun utilisateur connecte."));
        } else {
            List<Purchase> purchases = ticketStoreRepository.getPurchasesByUser(user.getId());
            if (purchases.isEmpty()) {
                box.getChildren().add(new Label("Aucun achat trouve."));
            } else {
                for (Purchase purchase : purchases) {
                    box.getChildren().add(new Label(
                            purchase.eventName() + " | quantite: " + purchase.quantity() + " | total: " + purchase.total() + " EUR | " + DATE_FORMAT.format(purchase.purchaseDate())
                    ));
                }
            }
        }

        contentPane.getChildren().setAll(box);
    }

    private void handlePurchase(Ticket ticket) {
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Achat");
        dialog.setHeaderText("Acheter des places pour " + ticket.eventName());
        dialog.setContentText("Quantite :");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        PurchaseOperationResult purchaseResult = purchaseService.purchase(App.getCurrentUser(), ticket.id(), result.get());
        Alert.AlertType type = purchaseResult.success() ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING;
        Alert alert = new Alert(type, purchaseResult.message());
        alert.showAndWait();

        if (purchaseResult.success()) {
            showSpectacles();
        }
    }
}
