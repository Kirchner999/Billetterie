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
import fr.billetterie.service.TicketArchiveService;
import fr.billetterie.service.TicketPdfService;
import fr.billetterie.service.TicketReceiptDocument;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
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
    private final TicketPdfService ticketPdfService = new TicketPdfService();
    private final TicketArchiveService ticketArchiveService = new TicketArchiveService();

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

        TableColumn<Purchase, String> seatsColumn = new TableColumn<>("Sieges");
        seatsColumn.setCellValueFactory(cell -> new SimpleStringProperty(displaySeatLabels(cell.getValue().seatLabels())));

        TableColumn<Purchase, String> totalColumn = new TableColumn<>("Total");
        totalColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().total() + " EUR"));

        TableColumn<Purchase, String> dateColumn = new TableColumn<>("Date d'achat");
        dateColumn.setCellValueFactory(cell -> new SimpleStringProperty(DATE_FORMAT.format(cell.getValue().purchaseDate())));

        tableView.getColumns().setAll(eventColumn, quantityColumn, seatsColumn, totalColumn, dateColumn);
        page.getChildren().add(tableView);
        contentPane.getChildren().setAll(page);
    }

    @FXML
    public void showMesBilletsPdf() {
        Client user = App.getCurrentUser();
        if (user == null) {
            contentPane.getChildren().setAll(buildEmptyState("Aucun utilisateur connecte."));
            return;
        }

        List<Purchase> purchases = ticketStoreRepository.getPurchasesByUser(user.getId()).stream()
                .filter(purchase -> purchase.ticketNumber() != null && !purchase.ticketNumber().isBlank())
                .toList();

        VBox page = createPageBox();
        page.getChildren().add(buildSectionTitle("Mes billets PDF", purchases.size() + " billet(s) disponible(s)"));

        if (purchases.isEmpty()) {
            page.getChildren().add(buildEmptyState("Aucun billet PDF enregistre pour le moment."));
            contentPane.getChildren().setAll(page);
            return;
        }

        long existingCount = purchases.stream().filter(this::hasExistingPdf).count();
        HBox tools = new HBox(12,
                buildStatusBadge(existingCount + " presents", "status-success"),
                buildStatusBadge((purchases.size() - existingCount) + " manquants", "status-warning")
        );
        Button exportButton = new Button("Exporter mes billets (.zip)");
        exportButton.setOnAction(event -> exportPurchasesArchive(purchases));
        tools.getChildren().add(exportButton);
        page.getChildren().add(tools);

        TableView<Purchase> tableView = new TableView<>();
        tableView.getStyleClass().add("data-table");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableView.setItems(FXCollections.observableArrayList(purchases));
        VBox.setVgrow(tableView, Priority.ALWAYS);

        TableColumn<Purchase, String> ticketNumberColumn = new TableColumn<>("Numero");
        ticketNumberColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().ticketNumber()));

        TableColumn<Purchase, String> eventColumn = new TableColumn<>("Evenement");
        eventColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().eventName()));

        TableColumn<Purchase, String> seatsColumn = new TableColumn<>("Sieges");
        seatsColumn.setCellValueFactory(cell -> new SimpleStringProperty(displaySeatLabels(cell.getValue().seatLabels())));

        TableColumn<Purchase, String> statusColumn = new TableColumn<>("Etat");
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolvePdfStatus(cell.getValue())));
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = buildStatusBadge(item, item.equals("present") ? "status-success" : item.equals("regenere") ? "status-info" : "status-warning");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Purchase, String> pathColumn = new TableColumn<>("Fichier");
        pathColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().pdfPath() != null ? cell.getValue().pdfPath() : "Non genere"));

        TableColumn<Purchase, Purchase> actionColumn = new TableColumn<>("Actions");
        actionColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button openButton = new Button("Ouvrir");
            private final Button regenerateButton = new Button("Regenerer");
            private final HBox box = new HBox(8, openButton, regenerateButton);

            {
                openButton.setOnAction(event -> {
                    Purchase purchase = getItem();
                    if (purchase != null) {
                        openPurchasePdf(purchase);
                    }
                });
                regenerateButton.setOnAction(event -> {
                    Purchase purchase = getItem();
                    if (purchase != null) {
                        regeneratePurchasePdf(purchase, true);
                    }
                });
            }

            @Override
            protected void updateItem(Purchase item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    openButton.setDisable(!hasExistingPdf(item));
                    setGraphic(box);
                }
            }
        });

        tableView.getColumns().setAll(ticketNumberColumn, eventColumn, seatsColumn, statusColumn, pathColumn, actionColumn);
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
                buildStatCard("Billets PDF", String.valueOf(purchases.stream().filter(p -> p.ticketNumber() != null && !p.ticketNumber().isBlank()).count()), "Documents retrouves")
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

    private Label buildStatusBadge(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("event-status", styleClass);
        return label;
    }

    private void handlePurchase(Ticket ticket) {
        List<Seat> seats = ticketStoreRepository.getAvailableSeats(ticket.id());
        PurchaseOperationResult purchaseResult;
        int purchasedQuantity;
        List<Seat> purchasedSeats = List.of();

        if (!seats.isEmpty()) {
            List<Seat> selectedSeats = askSeatSelection(ticket, seats);
            if (selectedSeats == null) {
                return;
            }

            if (!confirmSeatPurchase(ticket, selectedSeats)) {
                return;
            }

            purchasedSeats = selectedSeats;
            purchasedQuantity = selectedSeats.size();
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
                showPurchaseFailure(PurchaseOperationResult.failure("La quantite doit etre un nombre entier."));
                return;
            }

            purchasedQuantity = qty;
            if (!confirmQuantityPurchase(ticket, qty)) {
                return;
            }

            purchaseResult = purchaseService.purchaseWithoutSeats(App.getCurrentUser(), ticket.id(), quantity.get());
        }

        if (!purchaseResult.success()) {
            showPurchaseFailure(purchaseResult);
            return;
        }

        TicketReceiptDocument receipt = generateReceipt(ticket, purchasedQuantity, purchasedSeats, purchaseResult.purchaseId());
        showPurchaseSuccess(purchaseResult, receipt);
        showSpectacles();
    }

    private TicketReceiptDocument generateReceipt(Ticket ticket, int quantity, List<Seat> seats, Integer purchaseId) {
        Client user = App.getCurrentUser();
        if (user == null || purchaseId == null) {
            return null;
        }

        try {
            TicketReceiptDocument receipt = ticketPdfService.generateReceipt(user, ticket, quantity, seats);
            String seatLabels = seats == null || seats.isEmpty()
                    ? null
                    : seats.stream().map(Seat::displayLabel).collect(Collectors.joining(", "));
            boolean saved = ticketStoreRepository.saveReceiptDocument(purchaseId, receipt.ticketNumber(), receipt.pdfPath().toString(), seatLabels);
            return saved ? receipt : null;
        } catch (Exception e) {
            return null;
        }
    }

    private TicketReceiptDocument regeneratePurchasePdf(Purchase purchase, boolean refreshView) {
        Client user = App.getCurrentUser();
        if (user == null) {
            showPurchaseFailure(PurchaseOperationResult.failure("Aucun utilisateur connecte."));
            return null;
        }

        LocalDateTime eventDate = ticketStoreRepository.findTicketById(purchase.ticketId())
                .map(Ticket::eventDate)
                .orElse(null);

        try {
            TicketReceiptDocument receipt = ticketPdfService.regenerateReceipt(user, purchase, eventDate);
            boolean saved = ticketStoreRepository.saveReceiptDocument(purchase.id(), receipt.ticketNumber(), receipt.pdfPath().toString(), purchase.seatLabels());
            if (!saved) {
                showPurchaseFailure(PurchaseOperationResult.failure("Le PDF a ete regenere mais la base n'a pas pu etre mise a jour."));
                return null;
            }

            if (refreshView) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "Billet regenere.\nNumero: " + receipt.ticketNumber() + "\nChemin: " + receipt.pdfPath());
                ButtonType openButton = new ButtonType("Ouvrir");
                ButtonType closeButton = new ButtonType("Fermer");
                alert.getButtonTypes().setAll(openButton, closeButton);
                Optional<ButtonType> response = alert.showAndWait();
                if (response.isPresent() && response.get() == openButton) {
                    openReceiptPath(receipt.pdfPath());
                }
                showMesBilletsPdf();
            }
            return receipt;
        } catch (Exception e) {
            if (refreshView) {
                showPurchaseFailure(PurchaseOperationResult.failure("Impossible de regenerer le billet PDF."));
            }
            return null;
        }
    }

    private void exportPurchasesArchive(List<Purchase> purchases) {
        Client user = App.getCurrentUser();
        if (user == null) {
            return;
        }

        List<Path> pdfPaths = new ArrayList<>();
        int regenerated = 0;
        for (Purchase purchase : purchases) {
            if (hasExistingPdf(purchase)) {
                pdfPaths.add(Path.of(purchase.pdfPath()));
                continue;
            }

            TicketReceiptDocument regeneratedReceipt = regeneratePurchasePdf(purchase, false);
            if (regeneratedReceipt != null) {
                pdfPaths.add(regeneratedReceipt.pdfPath());
                regenerated++;
            }
        }

        if (pdfPaths.isEmpty()) {
            showPurchaseFailure(PurchaseOperationResult.failure("Aucun PDF exploitable pour l'archive."));
            return;
        }

        try {
            Path archivePath = ticketArchiveService.createArchive(user.getUsername(), pdfPaths);
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Archive creee: " + archivePath + "\nBillets inclus: " + pdfPaths.size() + "\nBillets regeneres: " + regenerated);
            ButtonType openButton = new ButtonType("Ouvrir l'archive");
            ButtonType closeButton = new ButtonType("Fermer");
            alert.getButtonTypes().setAll(openButton, closeButton);
            Optional<ButtonType> response = alert.showAndWait();
            if (response.isPresent() && response.get() == openButton) {
                openReceiptPath(archivePath);
            }
            showMesBilletsPdf();
        } catch (Exception e) {
            showPurchaseFailure(PurchaseOperationResult.failure("Impossible de creer l'archive ZIP."));
        }
    }

    private void showPurchaseSuccess(PurchaseOperationResult purchaseResult, TicketReceiptDocument receipt) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        ButtonType closeButton = new ButtonType("Fermer");
        alert.getButtonTypes().setAll(closeButton);
        alert.setTitle("Achat confirme");
        alert.setHeaderText("Reservation enregistree");

        String content = purchaseResult.message();
        if (receipt != null) {
            content += "\n\nNumero de billet: " + receipt.ticketNumber();
            content += "\nPDF genere: " + receipt.pdfPath();

            ButtonType openPdfButton = new ButtonType("Ouvrir le billet");
            alert.getButtonTypes().add(0, openPdfButton);
            alert.setContentText(content);
            Optional<ButtonType> response = alert.showAndWait();
            if (response.isPresent() && response.get() == openPdfButton) {
                openReceiptPath(receipt.pdfPath());
            }
            return;
        }

        alert.setContentText(content + "\n\nLe billet PDF n'a pas pu etre genere ou sauvegarde en base.");
        alert.showAndWait();
    }

    private void openPurchasePdf(Purchase purchase) {
        if (purchase.pdfPath() == null || purchase.pdfPath().isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Aucun fichier PDF n'est enregistre pour cet achat.");
            alert.showAndWait();
            return;
        }
        openReceiptPath(Path.of(purchase.pdfPath()));
    }

    private void openReceiptPath(Path filePath) {
        try {
            if (Desktop.isDesktopSupported() && Files.exists(filePath)) {
                Desktop.getDesktop().open(filePath.toFile());
                return;
            }
        } catch (Exception ignored) {
        }

        Alert alert = new Alert(Alert.AlertType.WARNING,
                "Impossible d'ouvrir automatiquement le fichier.\nChemin: " + filePath);
        alert.setTitle("Ouverture du billet");
        alert.setHeaderText("Le fichier existe peut-etre mais n'a pas pu etre ouvert");
        alert.showAndWait();
    }

    private boolean hasExistingPdf(Purchase purchase) {
        return purchase.pdfPath() != null && !purchase.pdfPath().isBlank() && Files.exists(Path.of(purchase.pdfPath()));
    }

    private String resolvePdfStatus(Purchase purchase) {
        if (hasExistingPdf(purchase)) {
            return "present";
        }
        return purchase.pdfPath() == null || purchase.pdfPath().isBlank() ? "manquant" : "regenere";
    }

    private String displaySeatLabels(String seatLabels) {
        return seatLabels == null || seatLabels.isBlank() ? "Attribution libre" : seatLabels;
    }

    private void showPurchaseFailure(PurchaseOperationResult purchaseResult) {
        Alert alert = new Alert(Alert.AlertType.WARNING, purchaseResult.message());
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
            showPurchaseFailure(PurchaseOperationResult.failure("La quantite doit etre superieure a 0."));
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
