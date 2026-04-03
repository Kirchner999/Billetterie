package fr.billetterie.controller;

import fr.billetterie.App;
import fr.billetterie.model.Client;
import fr.billetterie.model.Purchase;
import fr.billetterie.model.Seat;
import fr.billetterie.model.Ticket;
import fr.billetterie.repository.DaoTicketStoreRepository;
import fr.billetterie.repository.PurchaseOperationResult;
import fr.billetterie.repository.TicketStoreRepository;
import fr.billetterie.service.PurchaseService;
import fr.billetterie.utils.ThemeManager;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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

                Button buyButton = new Button("Choisir mes places");
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
        if (user == null) {
            contentPane.getChildren().setAll(new Label("Aucun utilisateur connecte."));
            return;
        }

        List<Purchase> purchases = ticketStoreRepository.getPurchasesByUser(user.getId());
        if (purchases.isEmpty()) {
            contentPane.getChildren().setAll(new Label("Aucun achat trouve."));
            return;
        }

        TableView<Purchase> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableView.setItems(FXCollections.observableArrayList(purchases));

        TableColumn<Purchase, String> eventColumn = new TableColumn<>("Evenement");
        eventColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().eventName()));

        TableColumn<Purchase, Integer> quantityColumn = new TableColumn<>("Quantite");
        quantityColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().quantity()));

        TableColumn<Purchase, String> totalColumn = new TableColumn<>("Total");
        totalColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().total() + " EUR"));

        TableColumn<Purchase, String> dateColumn = new TableColumn<>("Date d'achat");
        dateColumn.setCellValueFactory(cell -> new SimpleStringProperty(DATE_FORMAT.format(cell.getValue().purchaseDate())));

        tableView.getColumns().setAll(eventColumn, quantityColumn, totalColumn, dateColumn);
        contentPane.getChildren().setAll(tableView);
    }

    private void handlePurchase(Ticket ticket) {
        List<Seat> availableSeats = ticketStoreRepository.getAvailableSeats(ticket.id());
        PurchaseOperationResult purchaseResult;

        if (!availableSeats.isEmpty()) {
            List<Seat> selectedSeats = askSeatSelection(ticket, availableSeats);
            if (selectedSeats == null) {
                return;
            }
            purchaseResult = purchaseService.purchaseWithSeats(App.getCurrentUser(), ticket.id(), selectedSeats);
        } else {
            Optional<String> quantity = askQuantity(ticket);
            if (quantity.isEmpty()) {
                return;
            }
            purchaseResult = purchaseService.purchaseWithoutSeats(App.getCurrentUser(), ticket.id(), quantity.get());
        }

        Alert.AlertType type = purchaseResult.success() ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING;
        Alert alert = new Alert(type, purchaseResult.message());
        alert.showAndWait();

        if (purchaseResult.success()) {
            showSpectacles();
        }
    }

    private Optional<String> askQuantity(Ticket ticket) {
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Achat");
        dialog.setHeaderText("Acheter des places pour " + ticket.eventName());
        dialog.setContentText("Quantite :");
        return dialog.showAndWait();
    }

    private List<Seat> askSeatSelection(Ticket ticket, List<Seat> seats) {
        Dialog<List<Seat>> dialog = new Dialog<>();
        dialog.setTitle("Choix des sieges");
        dialog.setHeaderText("Selectionne tes sieges pour " + ticket.eventName());

        VBox wrapper = new VBox(12);
        wrapper.setPadding(new Insets(10));

        Label instructions = new Label("Clique sur une ou plusieurs places disponibles.");
        instructions.getStyleClass().add("seat-instructions");

        FlowPane grid = new FlowPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPrefWrapLength(340);

        List<ToggleButton> buttons = new ArrayList<>();
        List<Seat> sortedSeats = seats.stream()
                .sorted(Comparator.comparing(Seat::seatRow).thenComparingInt(Seat::seatNumber))
                .toList();

        for (Seat seat : sortedSeats) {
            ToggleButton seatButton = new ToggleButton(seat.displayLabel());
            seatButton.getStyleClass().add("seat-button");
            seatButton.setUserData(seat);
            seatButton.setMinWidth(68);
            seatButton.setPrefWidth(68);
            buttons.add(seatButton);
            grid.getChildren().add(seatButton);
        }

        HBox legend = new HBox(10,
                buildLegendChip("Libre", "seat-button"),
                buildLegendChip("Selectionne", "seat-button", "selected")
        );

        wrapper.getChildren().addAll(instructions, legend, grid);
        dialog.getDialogPane().setContent(wrapper);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return buttons.stream()
                        .filter(ToggleButton::isSelected)
                        .map(button -> (Seat) button.getUserData())
                        .toList();
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    private Label buildLegendChip(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }
}
