module ru.golovkov.multimediadataprocessing {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires static lombok;
    requires java.desktop;

    opens ru.golovkov.multimediadataprocessing to javafx.fxml;
    exports ru.golovkov.multimediadataprocessing;
}