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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClientDashboardController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private VBox contentPane;

    private final TicketStoreRepository ticketStoreRepository = new DaoTicketStoreRepository();
    private final PurchaseService purchaseService = new PurchaseService(ticketStoreRepository);

    @FXML
    public void initialize() {
        showClientHome();
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
        VBox page = createPageBox();
        page.getChildren().addAll(
                buildSectionTitle("Evenements disponibles", tickets.size() + " spectacle(s) a venir"),
                tickets.isEmpty() ? buildEmptyState("Aucun evenement disponible pour le moment.") : buildEventsList(tickets)
        );

        contentPane.getChildren().setAll(page);
    }

    @FXML
    public void showMesBillets() {
        Client user = App.getCurrentUser();
        if (user == null) {
            contentPane.getChildren().setAll(buildEmptyState("Aucun utilisateur connecte."));
            return;
        }

        List<Purchase> purchases = ticketStoreRepository.getPurchasesByUser(user.getId());
        VBox page = createPageBox();
        page.getChildren().add(buildSectionTitle("Mes achats", purchases.size() + " operation(s) enregistree(s)"));

        if (purchases.isEmpty()) {
            page.getChildren().add(buildEmptyState("Aucun achat trouve."));
            contentPane.getChildren().setAll(page);
            return;
        }

        TableView<Purchase> tableView = new TableView<>();
        tableView.getStyleClass().add("data-table");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableView.setItems(FXCollections.observableArrayList(purchases));
        VBox.setVgrow(tableView, Priority.ALWAYS);

        TableColumn<Purchase, String> eventColumn = new TableColumn<>("Evenement");
        eventColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().eventName()));

        TableColumn<Purchase, Integer> quantityColumn = new TableColumn<>("Quantite");
        quantityColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().quantity()));

        TableColumn<Purchase, String> totalColumn = new TableColumn<>("Total");
        totalColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().total() + " EUR"));

        TableColumn<Purchase, String> dateColumn = new TableColumn<>("Date d'achat");
        dateColumn.setCellValueFactory(cell -> new SimpleStringProperty(DATE_FORMAT.format(cell.getValue().purchaseDate())));

        tableView.getColumns().setAll(eventColumn, quantityColumn, totalColumn, dateColumn);
        page.getChildren().add(tableView);
        contentPane.getChildren().setAll(page);
    }

    private void showClientHome() {
        Client user = App.getCurrentUser();
        List<Ticket> tickets = ticketStoreRepository.getAvailableTickets();
        List<Purchase> purchases = user != null ? ticketStoreRepository.getPurchasesByUser(user.getId()) : List.of();

        VBox page = createPageBox();
        page.getChildren().add(buildSectionTitle(
                "Bienvenue, " + (user != null ? user.getUsername() : "utilisateur") + "",
                "Accede rapidement aux evenements, a tes places et a ton historique."
        ));

        HBox stats = new HBox(16,
                buildStatCard("Evenements a venir", String.valueOf(tickets.size()), "Catalogue disponible"),
                buildStatCard("Achats effectues", String.valueOf(purchases.size()), "Historique client"),
                buildStatCard("Prochain evenement", tickets.isEmpty() ? "-" : DATE_FORMAT.format(tickets.getFirst().eventDate()), "Date la plus proche")
        );
        page.getChildren().add(stats);

        if (!tickets.isEmpty()) {
            page.getChildren().add(buildSectionTitle("A la une", "Selection rapide des prochains spectacles"));
            page.getChildren().add(buildEventsList(tickets.stream().limit(3).toList()));
        } else {
            page.getChildren().add(buildEmptyState("Le catalogue est vide pour le moment."));
        }

        contentPane.getChildren().setAll(page);
    }

    private VBox buildEventsList(List<Ticket> tickets) {
        VBox list = new VBox(14);
        for (Ticket ticket : tickets) {
            list.getChildren().add(buildEventCard(ticket));
        }
        return list;
    }

    private VBox buildEventCard(Ticket ticket) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("card", "event-card");

        Label title = new Label(ticket.eventName());
        title.getStyleClass().add("card-title");

        Label date = new Label("Date: " + DATE_FORMAT.format(ticket.eventDate()));
        date.getStyleClass().add("event-meta");

        Label price = new Label("Prix: " + ticket.price() + " EUR");
        price.getStyleClass().add("event-meta");

        Label stock = new Label("Places restantes: " + ticket.stock());
        stock.getStyleClass().add("event-meta");

        Label status = new Label(ticket.stock() <= 5 ? "Places limitees" : "Disponible");
        status.getStyleClass().addAll("event-status", ticket.stock() <= 5 ? "status-warning" : "status-success");

        Button action = new Button("Choisir mes places");
        action.setOnAction(event -> handlePurchase(ticket));

        HBox footer = new HBox(12, status, action);
        footer.setPadding(new Insets(6, 0, 0, 0));

        card.getChildren().addAll(title, date, price, stock, footer);
        return card;
    }

    private VBox buildSectionTitle(String titleText, String subtitleText) {
        VBox box = new VBox(4);
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("section-subtitle");
        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private VBox buildStatCard(String title, String value, String subtitle) {
        VBox card = new VBox(6);
        card.getStyleClass().addAll("card", "stat-card");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("stat-subtitle");
        card.getChildren().addAll(titleLabel, valueLabel, subtitleLabel);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox buildEmptyState(String message) {
        VBox box = new VBox();
        box.getStyleClass().addAll("card", "empty-state");
        box.getChildren().add(new Label(message));
        return box;
    }

    private VBox createPageBox() {
        VBox box = new VBox(18);
        box.setFillWidth(true);
        return box;
    }

    private void handlePurchase(Ticket ticket) {
        List<Seat> seats = ticketStoreRepository.getAvailableSeats(ticket.id());
        PurchaseOperationResult purchaseResult;

        if (!seats.isEmpty()) {
            List<Seat> selectedSeats = askSeatSelection(ticket, seats);
            if (selectedSeats == null) {
                return;
            }

            if (!confirmSeatPurchase(ticket, selectedSeats)) {
                return;
            }

            purchaseResult = purchaseService.purchaseWithSeats(App.getCurrentUser(), ticket.id(), selectedSeats);
        } else {
            Optional<String> quantity = askQuantity(ticket);
            if (quantity.isEmpty()) {
                return;
            }

            int qty;
            try {
                qty = Integer.parseInt(quantity.get().trim());
            } catch (Exception e) {
                showPurchaseAlert(PurchaseOperationResult.failure("La quantite doit etre un nombre entier."));
                return;
            }

            if (!confirmQuantityPurchase(ticket, qty)) {
                return;
            }

            purchaseResult = purchaseService.purchaseWithoutSeats(App.getCurrentUser(), ticket.id(), quantity.get());
        }

        showPurchaseAlert(purchaseResult);
        if (purchaseResult.success()) {
            showSpectacles();
        }
    }

    private void showPurchaseAlert(PurchaseOperationResult purchaseResult) {
        Alert.AlertType type = purchaseResult.success() ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING;
        Alert alert = new Alert(type, purchaseResult.message());
        alert.showAndWait();
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

        Label instructions = new Label("Les sieges grises sont deja pris. Clique sur les places libres.");
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
            if (seat.taken()) {
                seatButton.getStyleClass().add("seat-button-taken");
                seatButton.setDisable(true);
            }
            seatButton.setUserData(seat);
            seatButton.setMinWidth(68);
            seatButton.setPrefWidth(68);
            buttons.add(seatButton);
            grid.getChildren().add(seatButton);
        }

        HBox legend = new HBox(10,
                buildLegendChip("Libre", "seat-button"),
                buildLegendChip("Selectionne", "seat-button", "selected"),
                buildLegendChip("Pris", "seat-button", "seat-button-taken")
        );

        wrapper.getChildren().addAll(instructions, legend, grid);
        dialog.getDialogPane().setContent(wrapper);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return buttons.stream()
                        .filter(button -> button.isSelected() && !button.isDisable())
                        .map(button -> (Seat) button.getUserData())
                        .toList();
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    private boolean confirmSeatPurchase(Ticket ticket, List<Seat> selectedSeats) {
        String seatsLabel = selectedSeats.stream().map(Seat::displayLabel).collect(Collectors.joining(", "));
        BigDecimal total = ticket.price().multiply(BigDecimal.valueOf(selectedSeats.size()));

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation d'achat");
        confirm.setHeaderText("Verifier le recapitulatif avant validation");
        confirm.setContentText(
                "Evenement: " + ticket.eventName() + "\n" +
                "Date: " + DATE_FORMAT.format(ticket.eventDate()) + "\n" +
                "Sieges: " + seatsLabel + "\n" +
                "Quantite: " + selectedSeats.size() + "\n" +
                "Total: " + total + " EUR"
        );

        return confirm.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private boolean confirmQuantityPurchase(Ticket ticket, int quantity) {
        if (quantity <= 0) {
            showPurchaseAlert(PurchaseOperationResult.failure("La quantite doit etre superieure a 0."));
            return false;
        }

        BigDecimal total = ticket.price().multiply(BigDecimal.valueOf(quantity));
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation d'achat");
        confirm.setHeaderText("Verifier le recapitulatif avant validation");
        confirm.setContentText(
                "Evenement: " + ticket.eventName() + "\n" +
                "Date: " + DATE_FORMAT.format(ticket.eventDate()) + "\n" +
                "Quantite: " + quantity + "\n" +
                "Total: " + total + " EUR"
        );

        return confirm.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private Label buildLegendChip(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }
}
