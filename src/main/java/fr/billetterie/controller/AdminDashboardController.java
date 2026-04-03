package fr.billetterie.controller;

import fr.billetterie.App;
import fr.billetterie.dao.ClientDAO;
import fr.billetterie.dao.TicketCatalogDAO;
import fr.billetterie.model.AdminPurchaseRecord;
import fr.billetterie.model.AdminRowOccupancyStat;
import fr.billetterie.model.AdminSalesStat;
import fr.billetterie.model.AdminSalesTimelinePoint;
import fr.billetterie.model.AdminSeatOccupancyStat;
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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AdminDashboardController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int PURCHASES_PAGE_SIZE = 10;
    private static final int AUDIT_PAGE_SIZE = 12;

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
    @FXML private Label occupancySelectedEventLabel;
    @FXML private Label occupancySummaryLabel;
    @FXML private TableView<AdminSeatOccupancyStat> occupancyTable;
    @FXML private TableColumn<AdminSeatOccupancyStat, String> colOccupancyEvent;
    @FXML private TableColumn<AdminSeatOccupancyStat, Integer> colOccupancyTotal;
    @FXML private TableColumn<AdminSeatOccupancyStat, Integer> colOccupancyTaken;
    @FXML private TableColumn<AdminSeatOccupancyStat, Integer> colOccupancyAvailable;
    @FXML private TableColumn<AdminSeatOccupancyStat, String> colOccupancyRate;
    @FXML private TableView<AdminRowOccupancyStat> rowOccupancyTable;
    @FXML private TableColumn<AdminRowOccupancyStat, String> colRowLabel;
    @FXML private TableColumn<AdminRowOccupancyStat, Integer> colRowTotal;
    @FXML private TableColumn<AdminRowOccupancyStat, Integer> colRowTaken;
    @FXML private TableColumn<AdminRowOccupancyStat, Integer> colRowAvailable;
    @FXML private TableColumn<AdminRowOccupancyStat, String> colRowRate;

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
    @FXML private Button purchasesSortDateButton;
    @FXML private Button purchasesSortUserButton;
    @FXML private Button purchasesSortAmountButton;
    @FXML private Button purchasesPrevPageButton;
    @FXML private Button purchasesNextPageButton;
    @FXML private Label purchasesPageLabel;
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
    @FXML private TextField auditSearchField;
    @FXML private Button auditSortDateButton;
    @FXML private Button auditSortTypeButton;
    @FXML private Button auditSortUserButton;
    @FXML private Button auditPrevPageButton;
    @FXML private Button auditNextPageButton;
    @FXML private Label auditPageLabel;

    @FXML private TableView<AdminSalesStat> salesStatsTable;
    @FXML private TableColumn<AdminSalesStat, String> colSalesEvent;
    @FXML private TableColumn<AdminSalesStat, Integer> colSalesConfirmed;
    @FXML private TableColumn<AdminSalesStat, Integer> colSalesRefunded;
    @FXML private TableColumn<AdminSalesStat, Integer> colSalesCancelled;
    @FXML private TableColumn<AdminSalesStat, Integer> colSalesTicketsSold;
    @FXML private TableColumn<AdminSalesStat, String> colSalesRevenue;
    @FXML private Button salesPeriodAllButton;
    @FXML private Button salesPeriod7DaysButton;
    @FXML private Button salesPeriod30DaysButton;
    @FXML private BarChart<String, Number> salesRevenueChart;
    @FXML private BarChart<String, Number> salesVolumeChart;
    @FXML private PieChart salesStatusChart;
    @FXML private LineChart<String, Number> salesTimelineChart;

    @FXML private TextField eventNameField;
    @FXML private TextField eventDateField;
    @FXML private TextField eventPriceField;
    @FXML private TextField eventStockField;

    private final EventManagementService eventManagementService = new EventManagementService(new DaoTicketAdminRepository());
    private int selectedTicketId;
    private int selectedPurchaseId;
    private String purchaseFilterMode = "all";
    private String purchaseSearchText = "";
    private String auditSearchText = "";
    private String salesPeriodMode = "all";
    private String purchaseSortMode = "date-desc";
    private String auditSortMode = "date-desc";
    private int purchasesPageIndex;
    private int auditPageIndex;
    private List<AdminPurchaseRecord> filteredPurchases = List.of();
    private List<TicketEventLog> filteredAuditLogs = List.of();

    @FXML
    public void initialize() {
        configureEventTable();
        configureOccupancyTables();
        configurePurchasesTable();
        configureAuditTable();
        configurePurchaseAuditTable();
        configureSalesStatsTable();
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
        purchasesPageIndex = 0;
        refreshPurchases();
    }

    @FXML
    public void showConfirmedPurchases() {
        purchaseFilterMode = "confirmed";
        purchasesPageIndex = 0;
        refreshPurchases();
    }

    @FXML
    public void showCancelledPurchases() {
        purchaseFilterMode = "cancelled";
        purchasesPageIndex = 0;
        refreshPurchases();
    }

    @FXML
    public void showRefundedPurchases() {
        purchaseFilterMode = "refunded";
        purchasesPageIndex = 0;
        refreshPurchases();
    }

    @FXML
    public void applyPurchaseSearch() {
        purchaseSearchText = purchaseSearchField != null ? purchaseSearchField.getText() : "";
        purchasesPageIndex = 0;
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

    @FXML
    public void applyAuditSearch() {
        auditSearchText = auditSearchField != null ? auditSearchField.getText() : "";
        auditPageIndex = 0;
        refreshAudit();
    }

    @FXML
    public void sortPurchasesByDate() {
        purchaseSortMode = "date-desc";
        purchasesPageIndex = 0;
        refreshPurchases();
    }

    @FXML
    public void sortPurchasesByUser() {
        purchaseSortMode = "user-asc";
        purchasesPageIndex = 0;
        refreshPurchases();
    }

    @FXML
    public void sortPurchasesByAmount() {
        purchaseSortMode = "amount-desc";
        purchasesPageIndex = 0;
        refreshPurchases();
    }

    @FXML
    public void sortAuditByDate() {
        auditSortMode = "date-desc";
        auditPageIndex = 0;
        refreshAudit();
    }

    @FXML
    public void sortAuditByType() {
        auditSortMode = "type-asc";
        auditPageIndex = 0;
        refreshAudit();
    }

    @FXML
    public void sortAuditByUser() {
        auditSortMode = "user-asc";
        auditPageIndex = 0;
        refreshAudit();
    }

    @FXML
    public void showPreviousPurchasesPage() {
        if (purchasesPageIndex > 0) {
            purchasesPageIndex--;
            applyPurchasesPage();
        }
    }

    @FXML
    public void showNextPurchasesPage() {
        if ((purchasesPageIndex + 1) * PURCHASES_PAGE_SIZE < filteredPurchases.size()) {
            purchasesPageIndex++;
            applyPurchasesPage();
        }
    }

    @FXML
    public void showPreviousAuditPage() {
        if (auditPageIndex > 0) {
            auditPageIndex--;
            applyAuditPage();
        }
    }

    @FXML
    public void showNextAuditPage() {
        if ((auditPageIndex + 1) * AUDIT_PAGE_SIZE < filteredAuditLogs.size()) {
            auditPageIndex++;
            applyAuditPage();
        }
    }

    @FXML
    public void exportPurchasesCsv() {
        List<AdminPurchaseRecord> purchases = filteredPurchases;

        if (purchases.isEmpty()) {
            showResult(false, "Aucun achat a exporter avec les filtres actuels.");
            return;
        }

        try {
            Path exportDir = Path.of("target", "exports");
            Files.createDirectories(exportDir);
            Path csvPath = exportDir.resolve("achats-admin-" + System.currentTimeMillis() + ".csv");

            List<String> lines = new ArrayList<>();
            lines.add("purchase_id;username;event_name;ticket_number;status;quantity;total;seat_labels;purchase_date;refunded_at;pdf_path");
            for (AdminPurchaseRecord purchase : purchases) {
                lines.add(String.join(";",
                        csvValue(String.valueOf(purchase.purchaseId())),
                        csvValue(purchase.username()),
                        csvValue(purchase.eventName()),
                        csvValue(purchase.ticketNumber()),
                        csvValue(purchase.status()),
                        csvValue(String.valueOf(purchase.quantity())),
                        csvValue(purchase.total().toPlainString()),
                        csvValue(purchase.seatLabels()),
                        csvValue(DATE_FORMAT.format(purchase.purchaseDate())),
                        csvValue(purchase.refundedAt() != null ? DATE_FORMAT.format(purchase.refundedAt()) : ""),
                        csvValue(purchase.pdfPath())
                ));
            }

            Files.write(csvPath, lines, StandardCharsets.UTF_8);

            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Export CSV cree.\nChemin: " + csvPath + "\nLignes: " + purchases.size());
            Button openButton = new Button("Ouvrir le dossier");
            openButton.setOnAction(event -> openPdfPath(csvPath.getParent()));
            alert.getDialogPane().setExpandableContent(openButton);
            alert.showAndWait();
        } catch (IOException e) {
            showResult(false, "Impossible de generer le fichier CSV.");
        }
    }

    @FXML
    public void exportAuditCsv() {
        List<TicketEventLog> logs = filteredAuditLogs;
        if (logs.isEmpty()) {
            showResult(false, "Aucune ligne d'audit a exporter avec les filtres actuels.");
            return;
        }

        try {
            Path exportDir = Path.of("target", "exports");
            Files.createDirectories(exportDir);
            Path csvPath = exportDir.resolve("audit-billets-" + System.currentTimeMillis() + ".csv");

            List<String> lines = new ArrayList<>();
            lines.add("id;purchase_id;username;event_name;ticket_number;event_type;details;created_at");
            for (TicketEventLog log : logs) {
                lines.add(String.join(";",
                        csvValue(String.valueOf(log.id())),
                        csvValue(String.valueOf(log.purchaseId())),
                        csvValue(log.username()),
                        csvValue(log.eventName()),
                        csvValue(log.ticketNumber()),
                        csvValue(log.eventType()),
                        csvValue(log.details()),
                        csvValue(DATE_FORMAT.format(log.createdAt()))
                ));
            }

            Files.write(csvPath, lines, StandardCharsets.UTF_8);
            showResult(true, "Export CSV du journal cree.\nChemin: " + csvPath + "\nLignes: " + logs.size());
        } catch (IOException e) {
            showResult(false, "Impossible de generer le CSV du journal.");
        }
    }

    @FXML
    public void exportSalesStatsCsv() {
        List<AdminSalesStat> stats = TicketCatalogDAO.getSalesStatsByTicket(salesPeriodMode);
        if (stats.isEmpty()) {
            showResult(false, "Aucune statistique de vente a exporter.");
            return;
        }

        try {
            Path exportDir = Path.of("target", "exports");
            Files.createDirectories(exportDir);
            Path csvPath = exportDir.resolve("stats-spectacles-" + System.currentTimeMillis() + ".csv");

            List<String> lines = new ArrayList<>();
            lines.add("event_name;confirmed_purchases;refunded_purchases;cancelled_purchases;tickets_sold;revenue");
            for (AdminSalesStat stat : stats) {
                lines.add(String.join(";",
                        csvValue(stat.eventName()),
                        csvValue(String.valueOf(stat.confirmedPurchases())),
                        csvValue(String.valueOf(stat.refundedPurchases())),
                        csvValue(String.valueOf(stat.cancelledPurchases())),
                        csvValue(String.valueOf(stat.ticketsSold())),
                        csvValue(stat.revenue().toPlainString())
                ));
            }

            Files.write(csvPath, lines, StandardCharsets.UTF_8);
            showResult(true, "Export CSV des stats cree.\nChemin: " + csvPath + "\nLignes: " + stats.size());
        } catch (IOException e) {
            showResult(false, "Impossible de generer le CSV des stats.");
        }
    }

    @FXML
    public void showAllSalesPeriod() {
        salesPeriodMode = "all";
        refreshSalesStats();
    }

    @FXML
    public void show7DaysSalesPeriod() {
        salesPeriodMode = "7d";
        refreshSalesStats();
    }

    @FXML
    public void show30DaysSalesPeriod() {
        salesPeriodMode = "30d";
        refreshSalesStats();
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
        refreshSalesStats();
        refreshOccupancy();
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
            refreshRowOccupancy(newValue.id(), newValue.eventName());
        });
    }

    private void configureOccupancyTables() {
        if (occupancyTable != null) {
            colOccupancyEvent.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().eventName()));
            colOccupancyTotal.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().totalSeats()));
            colOccupancyTaken.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().takenSeats()));
            colOccupancyAvailable.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().availableSeats()));
            colOccupancyRate.setCellValueFactory(cell -> new SimpleStringProperty(formatPercent(cell.getValue().occupancyRate())));
            occupancyTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    refreshRowOccupancy(newValue.ticketId(), newValue.eventName());
                }
            });
        }

        if (rowOccupancyTable != null) {
            colRowLabel.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().rowLabel()));
            colRowTotal.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().totalSeats()));
            colRowTaken.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().takenSeats()));
            colRowAvailable.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().availableSeats()));
            colRowRate.setCellValueFactory(cell -> new SimpleStringProperty(formatPercent(cell.getValue().occupancyRate())));
        }
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
        if (auditSearchField != null) {
            auditSearchField.setOnAction(event -> applyAuditSearch());
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

    private void configureSalesStatsTable() {
        colSalesEvent.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().eventName()));
        colSalesConfirmed.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().confirmedPurchases()));
        colSalesRefunded.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().refundedPurchases()));
        colSalesCancelled.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().cancelledPurchases()));
        colSalesTicketsSold.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().ticketsSold()));
        colSalesRevenue.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().revenue() + " EUR"));
    }

    private void refreshEvents() {
        List<Ticket> tickets = TicketCatalogDAO.getAllTickets();
        eventsTable.setItems(FXCollections.observableArrayList(tickets));
    }

    private void refreshPurchases() {
        filteredPurchases = TicketCatalogDAO.getAdminPurchases().stream()
                .filter(purchase -> switch (purchaseFilterMode) {
                    case "confirmed" -> "CONFIRMED".equalsIgnoreCase(purchase.status());
                    case "cancelled" -> "CANCELLED".equalsIgnoreCase(purchase.status());
                    case "refunded" -> "REFUNDED".equalsIgnoreCase(purchase.status());
                    default -> true;
                })
                .filter(purchase -> matchesPurchaseSearch(purchase, purchaseSearchText))
                .sorted(buildPurchaseComparator())
                .toList();
        applyPurchasesPage();
        updatePurchaseFilterButtons();
        updateSortButtons();
    }

    private void refreshAudit() {
        filteredAuditLogs = TicketCatalogDAO.getRecentTicketEvents(100).stream()
                .filter(log -> matchesAuditSearch(log, auditSearchText))
                .sorted(buildAuditComparator())
                .toList();
        applyAuditPage();
        updateSortButtons();
    }

    private void refreshSalesStats() {
        List<AdminSalesStat> stats = TicketCatalogDAO.getSalesStatsByTicket(salesPeriodMode);
        salesStatsTable.setItems(FXCollections.observableArrayList(stats));
        refreshSalesCharts(stats);
        updateSalesPeriodButtons();
    }

    private void refreshOccupancy() {
        if (occupancyTable == null) {
            return;
        }
        List<AdminSeatOccupancyStat> stats = TicketCatalogDAO.getSeatOccupancyStats();
        occupancyTable.setItems(FXCollections.observableArrayList(stats));

        int totalSeats = stats.stream().mapToInt(AdminSeatOccupancyStat::totalSeats).sum();
        int takenSeats = stats.stream().mapToInt(AdminSeatOccupancyStat::takenSeats).sum();
        int availableSeats = stats.stream().mapToInt(AdminSeatOccupancyStat::availableSeats).sum();
        occupancySummaryLabel.setText("Total: " + totalSeats + " | Pris: " + takenSeats + " | Libres: " + availableSeats);

        AdminSeatOccupancyStat selected = occupancyTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            refreshRowOccupancy(selected.ticketId(), selected.eventName());
        } else if (!stats.isEmpty()) {
            occupancyTable.getSelectionModel().selectFirst();
            AdminSeatOccupancyStat first = occupancyTable.getSelectionModel().getSelectedItem();
            if (first != null) {
                refreshRowOccupancy(first.ticketId(), first.eventName());
            }
        } else {
            refreshRowOccupancy(0, null);
        }
    }

    private void refreshRowOccupancy(int ticketId, String eventName) {
        if (rowOccupancyTable == null) {
            return;
        }
        if (ticketId <= 0) {
            rowOccupancyTable.setItems(FXCollections.observableArrayList());
            occupancySelectedEventLabel.setText("-");
            return;
        }

        occupancySelectedEventLabel.setText(eventName != null ? eventName : "-");
        rowOccupancyTable.setItems(FXCollections.observableArrayList(TicketCatalogDAO.getRowOccupancyStats(ticketId)));
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

    private void updateSortButtons() {
        updateFilterButton(purchasesSortDateButton, "date-desc".equals(purchaseSortMode));
        updateFilterButton(purchasesSortUserButton, "user-asc".equals(purchaseSortMode));
        updateFilterButton(purchasesSortAmountButton, "amount-desc".equals(purchaseSortMode));
        updateFilterButton(auditSortDateButton, "date-desc".equals(auditSortMode));
        updateFilterButton(auditSortTypeButton, "type-asc".equals(auditSortMode));
        updateFilterButton(auditSortUserButton, "user-asc".equals(auditSortMode));
    }

    private void updateSalesPeriodButtons() {
        updateFilterButton(salesPeriodAllButton, "all".equals(salesPeriodMode));
        updateFilterButton(salesPeriod7DaysButton, "7d".equals(salesPeriodMode));
        updateFilterButton(salesPeriod30DaysButton, "30d".equals(salesPeriodMode));
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

    private void refreshSalesCharts(List<AdminSalesStat> stats) {
        if (salesRevenueChart != null) {
            salesRevenueChart.getData().clear();
            XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
            revenueSeries.setName("CA confirme");
            stats.stream()
                    .sorted(Comparator.comparing(AdminSalesStat::revenue).reversed())
                    .limit(5)
                    .forEach(stat -> revenueSeries.getData().add(new XYChart.Data<>(shortEventName(stat.eventName()), stat.revenue())));
            salesRevenueChart.getData().add(revenueSeries);
        }

        if (salesVolumeChart != null) {
            salesVolumeChart.getData().clear();
            XYChart.Series<String, Number> volumeSeries = new XYChart.Series<>();
            volumeSeries.setName("Billets vendus");
            stats.stream()
                    .sorted(Comparator.comparing(AdminSalesStat::ticketsSold).reversed())
                    .limit(5)
                    .forEach(stat -> volumeSeries.getData().add(new XYChart.Data<>(shortEventName(stat.eventName()), stat.ticketsSold())));
            salesVolumeChart.getData().add(volumeSeries);
        }

        if (salesStatusChart != null) {
            int confirmed = stats.stream().mapToInt(AdminSalesStat::confirmedPurchases).sum();
            int refunded = stats.stream().mapToInt(AdminSalesStat::refundedPurchases).sum();
            int cancelled = stats.stream().mapToInt(AdminSalesStat::cancelledPurchases).sum();
            salesStatusChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Confirmes", confirmed),
                    new PieChart.Data("Rembourses", refunded),
                    new PieChart.Data("Annules", cancelled)
            ));
            salesStatusChart.setLabelsVisible(true);
            salesStatusChart.setLegendVisible(true);
        }

        if (salesTimelineChart != null) {
            salesTimelineChart.getData().clear();

            XYChart.Series<String, Number> confirmedSeries = new XYChart.Series<>();
            confirmedSeries.setName("CA confirme");
            XYChart.Series<String, Number> refundedSeries = new XYChart.Series<>();
            refundedSeries.setName("CA rembourse");

            for (AdminSalesTimelinePoint point : TicketCatalogDAO.getSalesTimeline(salesPeriodMode)) {
                confirmedSeries.getData().add(new XYChart.Data<>(point.label(), point.confirmedRevenue()));
                refundedSeries.getData().add(new XYChart.Data<>(point.label(), point.refundedRevenue()));
            }

            salesTimelineChart.getData().add(confirmedSeries);
            salesTimelineChart.getData().add(refundedSeries);
            salesTimelineChart.setTitle(switch (salesPeriodMode) {
                case "7d" -> "Evolution du CA sur 7 jours";
                case "30d" -> "Evolution du CA sur 30 jours";
                default -> "Evolution du CA sur 12 mois";
            });
        }
    }

    private String shortEventName(String value) {
        if (value == null || value.length() <= 18) {
            return value;
        }
        return value.substring(0, 18) + "...";
    }

    private String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f %%", value);
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

    private Comparator<AdminPurchaseRecord> buildPurchaseComparator() {
        return switch (purchaseSortMode) {
            case "user-asc" -> Comparator.comparing(AdminPurchaseRecord::username, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(AdminPurchaseRecord::purchaseDate, Comparator.reverseOrder());
            case "amount-desc" -> Comparator.comparing(AdminPurchaseRecord::total, Comparator.reverseOrder())
                    .thenComparing(AdminPurchaseRecord::purchaseDate, Comparator.reverseOrder());
            default -> Comparator.comparing(AdminPurchaseRecord::purchaseDate, Comparator.reverseOrder())
                    .thenComparing(AdminPurchaseRecord::username, String.CASE_INSENSITIVE_ORDER);
        };
    }

    private Comparator<TicketEventLog> buildAuditComparator() {
        return switch (auditSortMode) {
            case "type-asc" -> Comparator.comparing(TicketEventLog::eventType, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(TicketEventLog::createdAt, Comparator.reverseOrder());
            case "user-asc" -> Comparator.comparing(TicketEventLog::username, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(TicketEventLog::createdAt, Comparator.reverseOrder());
            default -> Comparator.comparing(TicketEventLog::createdAt, Comparator.reverseOrder())
                    .thenComparing(TicketEventLog::eventType, String.CASE_INSENSITIVE_ORDER);
        };
    }

    private boolean matchesAuditSearch(TicketEventLog log, String searchText) {
        if (searchText == null || searchText.isBlank()) {
            return true;
        }
        String query = searchText.trim().toLowerCase(Locale.ROOT);
        return containsIgnoreCase(log.username(), query)
                || containsIgnoreCase(log.eventName(), query)
                || containsIgnoreCase(log.ticketNumber(), query)
                || containsIgnoreCase(log.eventType(), query)
                || containsIgnoreCase(log.details(), query);
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(";", ",").replace("\r", " ").replace("\n", " ");
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

    private void applyPurchasesPage() {
        int pageCount = Math.max(1, (int) Math.ceil((double) filteredPurchases.size() / PURCHASES_PAGE_SIZE));
        purchasesPageIndex = Math.min(purchasesPageIndex, pageCount - 1);
        int fromIndex = Math.min(purchasesPageIndex * PURCHASES_PAGE_SIZE, filteredPurchases.size());
        int toIndex = Math.min(fromIndex + PURCHASES_PAGE_SIZE, filteredPurchases.size());
        purchasesTable.setItems(FXCollections.observableArrayList(filteredPurchases.subList(fromIndex, toIndex)));
        if (purchasesPageLabel != null) {
            purchasesPageLabel.setText("Page " + (purchasesPageIndex + 1) + " / " + pageCount);
        }
        if (purchasesPrevPageButton != null) {
            purchasesPrevPageButton.setDisable(purchasesPageIndex == 0);
        }
        if (purchasesNextPageButton != null) {
            purchasesNextPageButton.setDisable(purchasesPageIndex >= pageCount - 1 || filteredPurchases.isEmpty());
        }
    }

    private void applyAuditPage() {
        int pageCount = Math.max(1, (int) Math.ceil((double) filteredAuditLogs.size() / AUDIT_PAGE_SIZE));
        auditPageIndex = Math.min(auditPageIndex, pageCount - 1);
        int fromIndex = Math.min(auditPageIndex * AUDIT_PAGE_SIZE, filteredAuditLogs.size());
        int toIndex = Math.min(fromIndex + AUDIT_PAGE_SIZE, filteredAuditLogs.size());
        eventsLogTable.setItems(FXCollections.observableArrayList(filteredAuditLogs.subList(fromIndex, toIndex)));
        if (auditPageLabel != null) {
            auditPageLabel.setText("Page " + (auditPageIndex + 1) + " / " + pageCount);
        }
        if (auditPrevPageButton != null) {
            auditPrevPageButton.setDisable(auditPageIndex == 0);
        }
        if (auditNextPageButton != null) {
            auditNextPageButton.setDisable(auditPageIndex >= pageCount - 1 || filteredAuditLogs.isEmpty());
        }
    }
}
