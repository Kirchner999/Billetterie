package fr.billetterie.controller;

import fr.billetterie.App;
import fr.billetterie.dao.ClientDAO;
import fr.billetterie.dao.TicketCatalogDAO;
import fr.billetterie.model.Ticket;
import fr.billetterie.repository.DaoTicketAdminRepository;
import fr.billetterie.service.EventFormResult;
import fr.billetterie.service.EventManagementService;
import fr.billetterie.utils.ThemeManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminDashboardController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private Label usersCountLabel;
    @FXML private Label ticketsCountLabel;
    @FXML private Label purchasesCountLabel;
    @FXML private Label expiredCleanupLabel;
    @FXML private TableView<Ticket> eventsTable;
    @FXML private TableColumn<Ticket, String> colEventName;
    @FXML private TableColumn<Ticket, String> colEventDate;
    @FXML private TableColumn<Ticket, String> colEventPrice;
    @FXML private TableColumn<Ticket, Integer> colEventStock;
    @FXML private TextField eventNameField;
    @FXML private TextField eventDateField;
    @FXML private TextField eventPriceField;
    @FXML private TextField eventStockField;

    private final EventManagementService eventManagementService = new EventManagementService(new DaoTicketAdminRepository());
    private int selectedTicketId;

    @FXML
    public void initialize() {
        configureTable();
        refreshMetrics();
        refreshEvents();
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
    public void createEvent() {
        EventFormResult result = eventManagementService.create(
                eventNameField.getText(),
                eventDateField.getText(),
                eventPriceField.getText(),
                eventStockField.getText()
        );
        showResult(result);
        if (result.success()) {
            resetForm();
            refreshEvents();
            refreshMetrics();
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
        showResult(result);
        if (result.success()) {
            resetForm();
            refreshEvents();
            refreshMetrics();
        }
    }

    @FXML
    public void deleteEvent() {
        EventFormResult result = eventManagementService.delete(selectedTicketId);
        showResult(result);
        if (result.success()) {
            resetForm();
            refreshEvents();
            refreshMetrics();
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

    private void configureTable() {
        colEventName.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().eventName()));
        colEventDate.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(DATE_FORMAT.format(cell.getValue().eventDate())));
        colEventPrice.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().price() + " EUR"));
        colEventStock.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cell.getValue().stock()));

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

    private void refreshEvents() {
        List<Ticket> tickets = TicketCatalogDAO.getAllTickets();
        eventsTable.setItems(FXCollections.observableArrayList(tickets));
    }

    private void showResult(EventFormResult result) {
        Alert.AlertType type = result.success() ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING;
        Alert alert = new Alert(type, result.message());
        alert.showAndWait();
    }
}
