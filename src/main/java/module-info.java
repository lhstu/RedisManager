module com.redismanager {
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires lettuce.core;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.slf4j;

    exports com.redismanager.app;
    exports com.redismanager.ui.shell;
}
