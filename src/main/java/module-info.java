module com.flowapp.NonNewtonian {
    requires org.jetbrains.annotations;
    requires javafx.fxml;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;
    requires DateTimeRCryptor;

    exports com.flowapp.NonNewtonian;
    exports com.flowapp.NonNewtonian.Controllers to javafx.fxml;
    opens com.flowapp.NonNewtonian;
    opens com.flowapp.NonNewtonian.Controllers to javafx.fxml;
}