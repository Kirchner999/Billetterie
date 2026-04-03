module fr.billetterie {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.prefs;
    requires org.apache.pdfbox;

    opens fr.billetterie.controller to javafx.fxml;
    opens fr.billetterie.model to javafx.base;

    exports fr.billetterie;
    exports fr.billetterie.controller;
}
