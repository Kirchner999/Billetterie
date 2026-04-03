package fr.billetterie.controller;

import fr.billetterie.App;
import fr.billetterie.dao.ClientDAO;
import fr.billetterie.dao.TicketCatalogDAO;
import fr.billetterie.model.AdminPurchaseRecord;
import fr.billetterie.model.Ticket;
import fr.billetterie.model.TicketEventLog;
import fr.billetterie.repository.DaoTicketAdminRepository;
import fr.billetterie.repository.PurchaseOperationResult;
import fr.billetterie.service.EventFormResult;
import fr.billetterie.service.EventManagementService;
import fr.billetterie.utils.ThemeManager;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AdminDashboardController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private Label usersCountLabel;
    @FXML private Label ticketsCountLabel;
    @FXML private Label purchasesCountLabel;
    @FXML private Label cancelledCountLabel;
    @FXML private Label refundedCountLabel;
    @FXML private Label expiredCleanupLabel;

    @FXML private TableView<Ticket> eventsTable;
    @FXML private TableColumn<Ticket, String> colEventName;
    @FXML private TableColumn<Ticket, String> colEventDate;
    @FXML private TableColumn<Ticket, String> colEventPrice;
    @FXML private TableColumn<Ticket, Integer> colEventStock;

    @FXML private TableView<AdminPurchaseRecord> purchasesTable;
    @FXML private TableColumn<AdminPurchaseRecord, String> colPurchaseUser;
    @FXML private TableColumn<AdminPurchaseRecord, String> colPurchaseEvent;
    @FXML private TableColumn<AdminPurchaseRecord, Integer> colPurchaseQuantity;
    @FXML private TableColumn<AdminPurchaseRecord, String> colPurchaseTotal;
    @FXML private TableColumn<AdminPurchaseRecord, String> colPurchaseStatus;
    @FXML private TableColumn<AdminPurchaseRecord, String> colPurchaseDate;
    @FXML private TextField purchaseSearchField;
    @FXML private Button purchasesFilterAllButton;
    @FXML private Button purchasesFilterConfirmedButton;
    @FXML private Button purchasesFilterCancelledButton;
    @FXML private Button purchasesFilterRefundedButton;
    @FXML private Button openPdfButton;

    @FXML private Label detailUsernameLabel;
    @FXML private Label detailEventLabel;
    @FXML private Label detailTicketNumberLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailSeatsLabel;
    @FXML private Label detailTotalLabel;
    @FXML private Label detailPurchaseDateLabel;
    @FXML private Label detailRefundDateLabel;
    @FXML private Label detailPdfLabel;
    @FXML private TableView<TicketEventLog> purchaseAuditTable;
    @FXML private TableColumn<TicketEventLog, String> colPurchaseLogDate;
    @FXML private TableColumn<TicketEventLog, String> colPurchaseLogType;
    @FXML private TableColumn<TicketEventLog, String> colPurchaseLogDetails;

    @FXML private TableView<TicketEventLog> eventsLogTable;
    @FXML private TableColumn<TicketEventLog, String> colLogDate;
    @FXML private TableColumn<TicketEventLog, String> colLogType;
    @FXML private TableColumn<TicketEventLog, String> colLogUser;
    @FXML private TableColumn<TicketEventLog, String> colLogEvent;
    @FXML private TableColumn<TicketEventLog, String> colLogTicket;
    @FXML private TableColumn<TicketEventLog, String> colLogDetails;

    @FXML private TextField eventNameField;
    @FXML private TextField eventDateField;
    @FXML private TextField eventPriceField;
    @FXML private TextField eventStockField;

    private final EventManagementService eventManagementService = new EventManagementService(new DaoTicketAdminRepository());
    private int selectedTicketId;
    private int selectedPurchaseId;
    private String purchaseFilterMode = "all";
    private String purchaseSearchText = "";

    @FXML
    public void initialize() {
        configureEventTable();
        configurePurchasesTable();
        configureAuditTable();
        configurePurchaseAuditTable();
        refreshAdminView();
    }

    @FXML
    public void refreshMetrics() {
        refreshAdminView();
    }

    @FXML
    public void createEvent() {
        EventFormResult result = eventManagementService.create(
                eventNameField.getText(),
                eventDateField.getText(),
                eventPriceField.getText(),
                eventStockField.getText()
        );
        showResult(result.success(), result.message());
        if (result.success()) {
            resetForm();
            refreshAdminView();
        }
    }

    @FXML
    public void updateEvent() {
        EventFormResult result = eventManagementService.update(
                selectedTicketId,
                eventNameField.getText(),
                eventDateField.getText(),
                eventPriceField.getText(),
                eventStockField.getText()
        );
        showResult(result.success(), result.message());
        if (result.success()) {
            resetForm();
            refreshAdminView();
        }
    }

    @FXML
    public void deleteEvent() {
        EventFormResult result = eventManagementService.delete(selectedTicketId);
        showResult(result.success(), result.message());
        if (result.success()) {
            resetForm();
            refreshAdminView();
        }
    }

    @FXML
    public void cancelSelectedPurchase() {
        if (selectedPurchaseId == 0) {
            showResult(false, "Selectionne un achat dans le tableau.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Motif d'annulation");
        dialog.setHeaderText("Annulation admin");
        dialog.setContentText("Motif :");

        Optional<String> reason = dialog.showAndWait();
        if (reason.isEmpty()) {
            return;
        }

        PurchaseOperationResult result = TicketCatalogDAO.cancelPurchase(selectedPurchaseId, reason.get());
        showResult(result.success(), result.message());
        if (result.success()) {
            selectedPurchaseId = 0;
            if (purchasesTable != null) {
                purchasesTable.getSelectionModel().clearSelection();
            }
            refreshAdminView();
        }
    }

    @FXML
    public void refundSelectedPurchase() {
        if (selectedPurchaseId == 0) {
            showResult(false, "Selectionne un achat dans le tableau.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Motif de remboursement");
        dialog.setHeaderText("Remboursement admin");
        dialog.setContentText("Motif :");

        Optional<String> reason = dialog.showAndWait();
        if (reason.isEmpty()) {
            return;
        }

        PurchaseOperationResult result = TicketCatalogDAO.refundPurchase(selectedPurchaseId, reason.get());
        showResult(result.success(), result.message());
        if (result.success()) {
            selectedPurchaseId = 0;
            if (purchasesTable != null) {
                purchasesTable.getSelectionModel().clearSelection();
            }
            refreshAdminView();
        }
    }

    @FXML
    public void resetForm() {
        selectedTicketId = 0;
        eventNameField.clear();
        eventDateField.clear();
        eventPriceField.clear();
        eventStockField.clear();
        if (eventsTable != null) {
            eventsTable.getSelectionModel().clearSelection();
        }
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
    public void showAllPurchases() {
        purchaseFilterMode = "all";
        refreshPurchases();
    }

    @FXML
    public void showConfirmedPurchases() {
        purchaseFilterMode = "confirmed";
        refreshPurchases();
    }

    @FXML
    public void showCancelledPurchases() {
        purchaseFilterMode = "cancelled";
        refreshPurchases();
    }

    @FXML
    public void showRefundedPurchases() {
        purchaseFilterMode = "refunded";
        refreshPurchases();
    }

    @FXML
    public void applyPurchaseSearch() {
        purchaseSearchText = purchaseSearchField != null ? purchaseSearchField.getText() : "";
        refreshPurchases();
    }

    @FXML
    public void openSelectedPurchasePdf() {
        AdminPurchaseRecord purchase = purchasesTable != null ? purchasesTable.getSelectionModel().getSelectedItem() : null;
        if (purchase == null) {
            showResult(false, "Selectionne un achat avec un billet PDF.");
            return;
        }
        if (purchase.pdfPath() == null || purchase.pdfPath().isBlank()) {
            showResult(false, "Aucun PDF enregistre pour cet achat.");
            return;
        }
        openPdfPath(Path.of(purchase.pdfPath()));
    }

    private void refreshAdminView() {
        int deletedExpired = TicketCatalogDAO.cleanupExpiredTickets();

        usersCountLabel.setText(String.valueOf(ClientDAO.getAll().size()));
        ticketsCountLabel.setText(String.valueOf(TicketCatalogDAO.countTickets()));
        purchasesCountLabel.setText(String.valueOf(TicketCatalogDAO.countConfirmedPurchases()));
        cancelledCountLabel.setText(String.valueOf(TicketCatalogDAO.countCancelledPurchases()));
        refundedCountLabel.setText(String.valueOf(TicketCatalogDAO.countRefundedPurchases()));
        expiredCleanupLabel.setText(String.valueOf(deletedExpired));

        refreshEvents();
        refreshPurchases();
        refreshAudit();
    }

    private void configureEventTable() {
        colEventName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().eventName()));
        colEventDate.setCellValueFactory(cell -> new SimpleStringProperty(DATE_FORMAT.format(cell.getValue().eventDate())));
        colEventPrice.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().price() + " EUR"));
        colEventStock.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().stock()));

        eventsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            selectedTicketId = newValue.id();
            eventNameField.setText(newValue.eventName());
            eventDateField.setText(DATE_FORMAT.format(newValue.eventDate()));
            eventPriceField.setText(newValue.price().toPlainString());
            eventStockField.setText(String.valueOf(newValue.stock()));
        });
    }

    private void configurePurchasesTable() {
        colPurchaseUser.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().username()));
        colPurchaseEvent.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().eventName()));
        colPurchaseQuantity.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().quantity()));
        colPurchaseTotal.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().total() + " EUR"));
        colPurchaseStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().status()));
        colPurchaseDate.setCellValueFactory(cell -> new SimpleStringProperty(DATE_FORMAT.format(cell.getValue().purchaseDate())));

        purchasesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedPurchaseId = newValue != null ? newValue.purchaseId() : 0;
            showPurchaseDetails(newValue);
        });
        if (purchaseSearchField != null) {
            purchaseSearchField.setOnAction(event -> applyPurchaseSearch());
        }
    }

    private void configureAuditTable() {
        colLogDate.setCellValueFactory(cell -> new SimpleStringProperty(DATE_FORMAT.format(cell.getValue().createdAt())));
        colLogType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().eventType()));
        colLogUser.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().username()));
        colLogEvent.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().eventName()));
        colLogTicket.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().ticketNumber() != null ? cell.getValue().ticketNumber() : "-"));
        colLogDetails.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().details() != null ? cell.getValue().details() : "-"));
    }

    private void configurePurchaseAuditTable() {
        colPurchaseLogDate.setCellValueFactory(cell -> new SimpleStringProperty(DATE_FORMAT.format(cell.getValue().createdAt())));
        colPurchaseLogType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().eventType()));
        colPurchaseLogDetails.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().details() != null ? cell.getValue().details() : "-"));
    }

    private void refreshEvents() {
        List<Ticket> tickets = TicketCatalogDAO.getAllTickets();
        eventsTable.setItems(FXCollections.observableArrayList(tickets));
    }

    private void refreshPurchases() {
        List<AdminPurchaseRecord> purchases = TicketCatalogDAO.getAdminPurchases().stream()
                .filter(purchase -> switch (purchaseFilterMode) {
                    case "confirmed" -> "CONFIRMED".equalsIgnoreCase(purchase.status());
                    case "cancelled" -> "CANCELLED".equalsIgnoreCase(purchase.status());
                    case "refunded" -> "REFUNDED".equalsIgnoreCase(purchase.status());
                    default -> true;
                })
                .filter(purchase -> matchesPurchaseSearch(purchase, purchaseSearchText))
                .toList();
        purchasesTable.setItems(FXCollections.observableArrayList(purchases));
        updatePurchaseFilterButtons();
    }

    private void refreshAudit() {
        List<TicketEventLog> logs = TicketCatalogDAO.getRecentTicketEvents(30);
        eventsLogTable.setItems(FXCollections.observableArrayList(logs));
    }

    private void showResult(boolean success, String message) {
        Alert.AlertType type = success ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING;
        Alert alert = new Alert(type, message);
        alert.showAndWait();
    }

    private void updatePurchaseFilterButtons() {
        updateFilterButton(purchasesFilterAllButton, "all".equals(purchaseFilterMode));
        updateFilterButton(purchasesFilterConfirmedButton, "confirmed".equals(purchaseFilterMode));
        updateFilterButton(purchasesFilterCancelledButton, "cancelled".equals(purchaseFilterMode));
        updateFilterButton(purchasesFilterRefundedButton, "refunded".equals(purchaseFilterMode));
    }

    private void updateFilterButton(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.getStyleClass().removeAll("catalog-chip", "catalog-chip-active");
        button.getStyleClass().add(active ? "catalog-chip-active" : "catalog-chip");
    }

    private void showPurchaseDetails(AdminPurchaseRecord purchase) {
        if (purchase == null) {
            detailUsernameLabel.setText("-");
            detailEventLabel.setText("-");
            detailTicketNumberLabel.setText("-");
            detailStatusLabel.setText("-");
            detailSeatsLabel.setText("-");
            detailTotalLabel.setText("-");
            detailPurchaseDateLabel.setText("-");
            detailRefundDateLabel.setText("-");
            detailPdfLabel.setText("-");
            purchaseAuditTable.setItems(FXCollections.observableArrayList());
            if (openPdfButton != null) {
                openPdfButton.setDisable(true);
            }
            return;
        }

        detailUsernameLabel.setText(purchase.username());
        detailEventLabel.setText(purchase.eventName());
        detailTicketNumberLabel.setText(purchase.ticketNumber() != null ? purchase.ticketNumber() : "-");
        detailStatusLabel.setText(purchase.status());
        detailSeatsLabel.setText(purchase.seatLabels() != null && !purchase.seatLabels().isBlank() ? purchase.seatLabels() : "Attribution libre");
        detailTotalLabel.setText(purchase.total() + " EUR");
        detailPurchaseDateLabel.setText(DATE_FORMAT.format(purchase.purchaseDate()));
        detailRefundDateLabel.setText(purchase.refundedAt() != null ? DATE_FORMAT.format(purchase.refundedAt()) : "-");
        detailPdfLabel.setText(purchase.pdfPath() != null && !purchase.pdfPath().isBlank() ? purchase.pdfPath() : "-");
        purchaseAuditTable.setItems(FXCollections.observableArrayList(TicketCatalogDAO.getTicketEventsForPurchase(purchase.purchaseId())));
        if (openPdfButton != null) {
            openPdfButton.setDisable(purchase.pdfPath() == null || purchase.pdfPath().isBlank());
        }
    }

    private boolean matchesPurchaseSearch(AdminPurchaseRecord purchase, String searchText) {
        if (searchText == null || searchText.isBlank()) {
            return true;
        }
        String query = searchText.trim().toLowerCase(Locale.ROOT);
        return containsIgnoreCase(purchase.username(), query)
                || containsIgnoreCase(purchase.eventName(), query)
                || containsIgnoreCase(purchase.ticketNumber(), query);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private void openPdfPath(Path filePath) {
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
}
