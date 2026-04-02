package com.mytaskmanager.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads application configuration from config.properties on the classpath.
 * Falls back to sensible defaults when a key or the file is missing.
 */
public class AppConfig {

    private final Properties properties = new Properties();

    public AppConfig() {
        try (InputStream in = AppConfig.class.getResourceAsStream("/config.properties")) {
            if (in != null) properties.load(in);
        } catch (IOException ignored) {}
    }

    public long getMonitorIntervalSeconds() {
        return Long.parseLong(properties.getProperty("monitor.interval", "5"));
    }
}
