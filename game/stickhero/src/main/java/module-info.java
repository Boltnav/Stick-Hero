module com.javafx.game {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.graphics;
    requires java.desktop;
    requires junit;

    opens com.javafx.game to javafx.fxml;
    exports com.javafx.game;
}