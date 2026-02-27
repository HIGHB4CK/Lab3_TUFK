module org.example.laba1_tfyk {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.laba1_tfyk to javafx.fxml;
    exports org.example.laba1_tfyk;
}