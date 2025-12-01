module fr.billetterie {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.prefs;

    // Ouvre aux FXML
    opens fr.billetterie.controller to javafx.fxml;

    // Si tes modèles sont utilisés dans des TableView, nécessaire
    opens fr.billetterie.model to javafx.base;

    // Exportation
    exports fr.billetterie;
    exports fr.billetterie.controller;
}
