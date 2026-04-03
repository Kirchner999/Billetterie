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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.util.Duration;

public class ClientDashboardController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Duration AUTO_REFRESH_INTERVAL = Duration.seconds(5);

    @FXML private VBox contentPane;

    private final TicketStoreRepository ticketStoreRepository = new DaoTicketStoreRepository();
    private final PurchaseService purchaseService = new PurchaseService(ticketStoreRepository);
    private final TicketPdfService ticketPdfService = new TicketPdfService();
    private final TicketArchiveService ticketArchiveService = new TicketArchiveService();
    private final Timeline autoRefreshTimeline = new Timeline(new KeyFrame(AUTO_REFRESH_INTERVAL, event -> refreshCurrentView()));

    private String currentSection = "home";
    private String currentSearchText = "";
    private String currentFilterMode = "all";
    private String currentSortMode = "date";

    @FXML
    public void initialize() {
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
        showClientHome();
    }

    @FXML
    public void switchTheme() {
        ThemeManager.toggleTheme();
    }

    @FXML
    public void refreshNow() {
        refreshCurrentView();
    }

    @FXML
    public void logout() {
        autoRefreshTimeline.stop();
        App.setCurrentUser(null);
        App.loadPage("Login.fxml");
    }

    @FXML
    public void showSpectacles() {
        renderSpectaclesPage("", "all", "date");
    }

    private void renderSpectaclesPage(String searchText, String filterMode, String sortMode) {
        currentSection = "spectacles";
        currentSearchText = searchText == null ? "" : searchText;
        currentFilterMode = filterMode == null ? "all" : filterMode;
        currentSortMode = sortMode == null ? "date" : sortMode;

        List<Ticket> tickets = ticketStoreRepository.getAvailableTickets();
        List<Ticket> visibleTickets = filterTickets(tickets, searchText, filterMode, sortMode);

        VBox page = createPageBox();
        page.getChildren().add(buildSectionTitle("Evenements disponibles", tickets.size() + " spectacle(s) a venir"));
        page.getChildren().add(buildSpectaclesToolbar(tickets, searchText, filterMode, sortMode, visibleTickets.size()));
        page.getChildren().add(buildSpectaclesStats(visibleTickets));

        if (visibleTickets.isEmpty()) {
            page.getChildren().add(buildEmptyState("Aucun spectacle ne correspond aux filtres actuels."));
        } else {
            page.getChildren().add(buildEventsList(visibleTickets));
        }

        contentPane.getChildren().setAll(page);
    }

    @FXML
    public void showMesBillets() {
        currentSection = "mes-achats";
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
        currentSection = "mes-billets-pdf";
        Client user = App.getCurrentUser();
        if (user == null) {
            contentPane.getChildren().setAll(buildEmptyState("Aucun utilisateur connecte."));
            return;
        }

        List<Purchase> allPurchases = ticketStoreRepository.getPurchasesByUser(user.getId()).stream()
                .filter(purchase -> purchase.ticketNumber() != null && !purchase.ticketNumber().isBlank())
                .toList();

        VBox page = createPageBox();
        page.getChildren().add(buildSectionTitle("Mes billets PDF", allPurchases.size() + " billet(s) disponible(s)"));

        if (allPurchases.isEmpty()) {
            page.getChildren().add(buildEmptyState("Aucun billet PDF enregistre pour le moment."));
            contentPane.getChildren().setAll(page);
            return;
        }

        long presentCount = allPurchases.stream().filter(this::hasExistingPdf).count();
        long missingCount = allPurchases.stream().filter(purchase -> resolvePdfStatus(purchase).equals("manquant")).count();
        long regeneratedCount = allPurchases.stream().filter(purchase -> resolvePdfStatus(purchase).equals("regenere")).count();

        TableView<Purchase> tableView = new TableView<>();
        tableView.getStyleClass().add("data-table");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        ObservableList<Purchase> visiblePurchases = FXCollections.observableArrayList(allPurchases);
        tableView.setItems(visiblePurchases);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        HBox filters = new HBox(10);
        Button allButton = new Button("Tous");
        Button presentButton = new Button("Presents");
        Button missingButton = new Button("Manquants");
        Button regeneratedButton = new Button("Regeneres");
        filters.getChildren().addAll(
                allButton,
                presentButton,
                missingButton,
                regeneratedButton,
                buildStatusBadge(presentCount + " presents", "status-success"),
                buildStatusBadge(missingCount + " manquants", "status-warning"),
                buildStatusBadge(regeneratedCount + " regeneres", "status-info")
        );

        allButton.setOnAction(event -> visiblePurchases.setAll(allPurchases));
        presentButton.setOnAction(event -> visiblePurchases.setAll(allPurchases.stream().filter(this::hasExistingPdf).toList()));
        missingButton.setOnAction(event -> visiblePurchases.setAll(allPurchases.stream().filter(purchase -> resolvePdfStatus(purchase).equals("manquant")).toList()));
        regeneratedButton.setOnAction(event -> visiblePurchases.setAll(allPurchases.stream().filter(purchase -> resolvePdfStatus(purchase).equals("regenere")).toList()));

        Label selectionHint = new Label("Selection multiple: Ctrl+clic dans le tableau, puis exporte uniquement la selection.");
        selectionHint.getStyleClass().add("section-subtitle");

        Button exportSelectionButton = new Button("Exporter la selection (.zip)");
        exportSelectionButton.setOnAction(event -> exportSelectedPurchases(tableView));

        HBox tools = new HBox(12, exportSelectionButton);

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
                    Label badge = buildStatusBadge(item, item.equals("present") ? "status-success" : item.equals("regenere") ? "status-info" : item.equals("annule") ? "status-warning" : "status-warning");
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
        page.getChildren().addAll(filters, selectionHint, tools, tableView);
        contentPane.getChildren().setAll(page);
    }

    private void showClientHome() {
        currentSection = "home";
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

    private void refreshCurrentView() {
        switch (currentSection) {
            case "spectacles" -> renderSpectaclesPage(currentSearchText, currentFilterMode, currentSortMode);
            case "mes-achats" -> showMesBillets();
            case "mes-billets-pdf" -> showMesBilletsPdf();
            default -> showClientHome();
        }
    }

    private VBox buildEventsList(List<Ticket> tickets) {
        VBox list = new VBox(14);
        for (Ticket ticket : tickets) {
            list.getChildren().add(buildEventCard(ticket));
        }
        return list;
    }

    private VBox buildSpectaclesToolbar(List<Ticket> allTickets, String searchText, String filterMode, String sortMode, int visibleCount) {
        VBox wrapper = new VBox(12);
        wrapper.getStyleClass().addAll("card", "catalog-toolbar");

        TextField searchField = new TextField(searchText);
        searchField.setPromptText("Rechercher un spectacle, un lieu, un mot-cle...");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button applySearchButton = new Button("Appliquer");
        applySearchButton.setOnAction(event -> renderSpectaclesPage(searchField.getText(), filterMode, sortMode));
        searchField.setOnAction(event -> renderSpectaclesPage(searchField.getText(), filterMode, sortMode));

        HBox searchRow = new HBox(10, searchField, applySearchButton);

        HBox filterRow = new HBox(10,
                buildCatalogFilterButton("Tous", filterMode.equals("all"), () -> renderSpectaclesPage(searchField.getText(), "all", sortMode)),
                buildCatalogFilterButton("Petits stocks", filterMode.equals("low-stock"), () -> renderSpectaclesPage(searchField.getText(), "low-stock", sortMode)),
                buildCatalogFilterButton("Budget malin", filterMode.equals("budget"), () -> renderSpectaclesPage(searchField.getText(), "budget", sortMode)),
                buildCatalogFilterButton("Premium", filterMode.equals("premium"), () -> renderSpectaclesPage(searchField.getText(), "premium", sortMode))
        );

        HBox sortRow = new HBox(10,
                buildCatalogFilterButton("Par date", sortMode.equals("date"), () -> renderSpectaclesPage(searchField.getText(), filterMode, "date")),
                buildCatalogFilterButton("Prix croissant", sortMode.equals("price-asc"), () -> renderSpectaclesPage(searchField.getText(), filterMode, "price-asc")),
                buildCatalogFilterButton("Prix decroissant", sortMode.equals("price-desc"), () -> renderSpectaclesPage(searchField.getText(), filterMode, "price-desc"))
        );

        Label summary = new Label(visibleCount + " resultat(s) sur " + allTickets.size() + " evenement(s) disponibles");
        summary.getStyleClass().add("section-subtitle");

        wrapper.getChildren().addAll(searchRow, filterRow, sortRow, summary);
        return wrapper;
    }

    private Button buildCatalogFilterButton(String text, boolean active, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("button-secondary");
        button.getStyleClass().add(active ? "catalog-chip-active" : "catalog-chip");
        button.setOnAction(event -> action.run());
        return button;
    }

    private HBox buildSpectaclesStats(List<Ticket> tickets) {
        long lowStock = tickets.stream().filter(ticket -> ticket.stock() <= 5).count();
        String averagePrice = tickets.isEmpty()
                ? "-"
                : tickets.stream()
                .map(Ticket::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(tickets.size()), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + " EUR";
        String nextDate = tickets.isEmpty() ? "-" : DATE_FORMAT.format(tickets.getFirst().eventDate());

        return new HBox(16,
                buildStatCard("Catalogue affiche", String.valueOf(tickets.size()), "Spectacles apres filtres"),
                buildStatCard("Prix moyen", averagePrice, "Repere rapide"),
                buildStatCard("Places limitees", String.valueOf(lowStock), "Stocks sous tension"),
                buildStatCard("Plus proche date", nextDate, "Prochain rideau")
        );
    }

    private List<Ticket> filterTickets(List<Ticket> tickets, String searchText, String filterMode, String sortMode) {
        String normalizedSearch = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);

        Comparator<Ticket> comparator = switch (sortMode) {
            case "price-asc" -> Comparator.comparing(Ticket::price).thenComparing(Ticket::eventDate);
            case "price-desc" -> Comparator.comparing(Ticket::price).reversed().thenComparing(Ticket::eventDate);
            default -> Comparator.comparing(Ticket::eventDate).thenComparing(Ticket::price);
        };

        return tickets.stream()
                .filter(ticket -> normalizedSearch.isBlank() || ticket.eventName().toLowerCase(Locale.ROOT).contains(normalizedSearch))
                .filter(ticket -> switch (filterMode) {
                    case "low-stock" -> ticket.stock() <= 5;
                    case "budget" -> ticket.price().compareTo(BigDecimal.valueOf(70)) <= 0;
                    case "premium" -> ticket.price().compareTo(BigDecimal.valueOf(80)) >= 0;
                    default -> true;
                })
                .sorted(comparator)
                .toList();
    }

    private VBox buildEventCard(Ticket ticket) {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("card", "event-card");

        ImageView poster = buildPosterView(ticket);

        HBox topRow = new HBox(12);
        Label title = new Label(ticket.eventName());
        title.getStyleClass().add("card-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        Label pricePill = new Label(ticket.price() + " EUR");
        pricePill.getStyleClass().addAll("event-status", "status-info");
        topRow.getChildren().addAll(title, pricePill);

        Label date = new Label("Date: " + DATE_FORMAT.format(ticket.eventDate()));
        date.getStyleClass().add("event-meta");

        String stockTone = ticket.stock() <= 5 ? "Places limitees" : ticket.stock() <= 10 ? "Bon rythme de vente" : "Large disponibilite";
        Label stock = new Label("Places restantes: " + ticket.stock() + "  •  " + stockTone);
        stock.getStyleClass().add("event-meta");

        HBox badges = new HBox(10,
                buildStatusBadge(ticket.stock() <= 5 ? "Urgent" : "Reserve ouverte", ticket.stock() <= 5 ? "status-warning" : "status-success"),
                buildStatusBadge(ticket.price().compareTo(BigDecimal.valueOf(70)) <= 0 ? "Budget" : ticket.price().compareTo(BigDecimal.valueOf(80)) >= 0 ? "Premium" : "Standard", "status-info")
        );

        Label teaser = new Label(buildEventTeaser(ticket));
        teaser.getStyleClass().add("event-teaser");
        teaser.setWrapText(true);

        Button action = new Button("Choisir mes places");
        action.setOnAction(event -> handlePurchase(ticket));
        action.getStyleClass().add("event-action");

        HBox footer = new HBox(12, badges, action);
        footer.setPadding(new Insets(6, 0, 0, 0));
        HBox.setHgrow(badges, Priority.ALWAYS);

        card.getChildren().addAll(poster, topRow, date, stock, teaser, footer);
        return card;
    }

    private ImageView buildPosterView(Ticket ticket) {
        String posterPath = resolvePosterPath(ticket);
        Image image;
        try (var stream = getClass().getResourceAsStream(posterPath)) {
            if (stream == null) {
                throw new IllegalStateException("Poster introuvable");
            }
            image = new Image(stream);
        } catch (Exception e) {
            try (var fallback = getClass().getResourceAsStream("/posters/default-show.png")) {
                image = new Image(fallback);
            } catch (Exception fallbackError) {
                image = null;
            }
        }

        ImageView poster = new ImageView(image);
        poster.setFitHeight(190);
        poster.setFitWidth(320);
        poster.setPreserveRatio(false);
        poster.getStyleClass().add("event-poster");
        return poster;
    }

    private String resolvePosterPath(Ticket ticket) {
        String eventName = ticket.eventName().toLowerCase(Locale.ROOT);
        if (eventName.contains("miserables")) {
            return "/posters/les-miserables.png";
        }
        if (eventName.contains("romeo") || eventName.contains("juliette")) {
            return "/posters/romeo-juliette.png";
        }
        if (eventName.contains("cygnes")) {
            return "/posters/lac-cygnes.png";
        }
        if (eventName.contains("starmania")) {
            return "/posters/starmania.png";
        }
        if (eventName.contains("roi lion")) {
            return "/posters/roi-lion.png";
        }
        return "/posters/default-show.png";
    }

    private String buildEventTeaser(Ticket ticket) {
        if (ticket.price().compareTo(BigDecimal.valueOf(70)) <= 0) {
            return "Bon point d'entree pour reserver rapidement sans sacrifier la date. Ideal si tu veux un achat simple et efficace.";
        }
        if (ticket.stock() <= 5) {
            return "Le stock commence a tirer. Si cette date t'interesse vraiment, ce n'est plus le moment d'attendre.";
        }
        return "Une seance confortable avec encore du choix. Tu peux viser les meilleures places sans pression immediate.";
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
        renderSpectaclesPage("", "all", "date");
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
            if (saved) {
                ticketStoreRepository.logTicketEvent(
                        purchaseId,
                        "GENERATED",
                        "Billet genere: " + receipt.ticketNumber() + " | pdf=" + receipt.pdfPath()
                );
                return receipt;
            }
            return null;
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
            ticketStoreRepository.logTicketEvent(
                    purchase.id(),
                    "REGENERATED",
                    "Billet regenere: " + receipt.ticketNumber() + " | pdf=" + receipt.pdfPath()
            );

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

    private void exportSelectedPurchases(TableView<Purchase> tableView) {
        List<Purchase> selectedPurchases = new ArrayList<>(tableView.getSelectionModel().getSelectedItems());
        if (selectedPurchases.isEmpty()) {
            showPurchaseFailure(PurchaseOperationResult.failure("Selectionne au moins un billet dans le tableau."));
            return;
        }

        Client user = App.getCurrentUser();
        if (user == null) {
            return;
        }

        List<Path> pdfPaths = new ArrayList<>();
        int regenerated = 0;
        for (Purchase purchase : selectedPurchases) {
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
            for (Purchase purchase : selectedPurchases) {
                ticketStoreRepository.logTicketEvent(
                        purchase.id(),
                        "EXPORTED",
                        "Billet exporte dans l'archive: " + archivePath
                );
            }
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


