module fr.billetterie {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.prefs;
    requires java.desktop;
    requires org.apache.pdfbox;
    requires com.google.zxing;
    requires com.google.zxing.javase;

    opens fr.billetterie.controller to javafx.fxml;
    opens fr.billetterie.model to javafx.base;

    exports fr.billetterie;
    exports fr.billetterie.controller;
}
