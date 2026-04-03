package com.mytaskmanager.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
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

    public String getMappingFilePath() {
        return properties.getProperty("mapping.file", "process_info.json");
    }

    public long getSnapshotSchedulerDelaySeconds() {
        return Long.parseLong(properties.getProperty("snapshot.scheduler.delay", "1"));
    }

    public long getSnapshotIntervalSeconds() {
        return Long.parseLong(properties.getProperty("snapshot.interval", "60"));
    }

    public List<LocalTime> getSnapshotFixedTimes() {
        List<LocalTime> times = new ArrayList<>();
        int i = 1;
        while (true) {
            String val = properties.getProperty("snapshot.fixed_time_" + i);
            if (val == null) break;
            times.add(LocalTime.parse(val));
            i++;
        }
        return times;
    }
}
