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

    /**
     * Initializes AppConfig by loading properties from config.properties on the classpath.
     * If the file is missing or cannot be read, an empty Properties object is used.
     */
    public AppConfig() {
        try (InputStream in = AppConfig.class.getResourceAsStream("/config.properties")) {
            if (in != null) properties.load(in);
        } catch (IOException ignored) {}
    }

    /**
     * Returns the process monitor interval in seconds.
     *
     * @return monitor interval; default is 5 seconds
     */
    public long getMonitorIntervalSeconds() {
        return Long.parseLong(properties.getProperty("monitor.interval", "5"));
    }

    /**
     * Returns the path to the process info mapping file.
     *
     * @return file path; default is "process_info.json"
     */
    public String getMappingFilePath() {
        return properties.getProperty("mapping.file", "process_info.json");
    }

    /**
     * Returns the initial delay before the snapshot scheduler starts.
     *
     * @return delay in seconds; default is 1 second
     */
    public long getSnapshotSchedulerDelaySeconds() {
        return Long.parseLong(properties.getProperty("snapshot.scheduler.delay", "1"));
    }

    /**
     * Returns the periodic snapshot interval in seconds.
     *
     * @return interval in seconds; default is 60 seconds
     */
    public long getSnapshotIntervalSeconds() {
        return Long.parseLong(properties.getProperty("snapshot.interval", "60"));
    }

    /**
     * Returns the list of fixed times at which to trigger snapshots daily.
     *
     * @return list of LocalTime objects; empty if none configured
     */
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
