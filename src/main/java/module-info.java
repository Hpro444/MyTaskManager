module com.mytaskmanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.github.oshi;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.slf4j.nop;
    requires static lombok;

    exports com.mytaskmanager;
    exports com.mytaskmanager.config;
    exports com.mytaskmanager.gui;
    exports com.mytaskmanager.services;
    exports com.mytaskmanager.domain;
}
