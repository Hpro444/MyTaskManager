/**
 * MyTaskManager module descriptor.
 * <p>
 * Declares all required modules (JavaFX, OSHI, Jackson, etc.) and exports the
 * public API packages. Opens internal utility packages to Jackson for reflective
 * deserialization of JSON records.
 * </p>
 */
module com.mytaskmanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.github.oshi;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.slf4j.nop;
    requires static lombok;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.module.paramnames;
    requires java.logging;

    exports com.mytaskmanager;
    exports com.mytaskmanager.config;
    exports com.mytaskmanager.gui;
    exports com.mytaskmanager.domain;

    opens com.mytaskmanager.utils to com.fasterxml.jackson.databind;
    opens com.mytaskmanager.domain to com.fasterxml.jackson.databind;
    exports com.mytaskmanager.services.watcher;
    exports com.mytaskmanager.services.fileIo;
    exports com.mytaskmanager.services.analytics;
    exports com.mytaskmanager.services.snapshot;
    exports com.mytaskmanager.services.scanner;
}
