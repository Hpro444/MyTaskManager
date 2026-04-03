package com.mytaskmanager;

import com.mytaskmanager.gui.MainApplication;
import javafx.application.Application;

/**
 * Entry point for the Productivity Buddy application.
 * <p>
 * Launches the JavaFX application by calling {@link Application#launch(Class, String[])}.
 * This indirection allows the actual application class (MainApplication) to
 * maintain flexibility in its implementation.
 * </p>
 */
public class Launcher {
    /**
     * Entry point for the application. Launches the JavaFX application.
     *
     * @param args command-line arguments passed to the JavaFX Application
     */
    public static void main(String[] args) {
        Application.launch(MainApplication.class, args);
    }
}
