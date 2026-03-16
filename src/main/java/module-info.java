module org.example.Lab2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens org.example.Lab2 to javafx.fxml, javafx.base;
    exports org.example.Lab2;
}