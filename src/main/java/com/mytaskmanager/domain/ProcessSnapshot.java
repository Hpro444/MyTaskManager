package com.mytaskmanager.domain;

/**
 * Immutable point-in-time capture of a {@link ProcessModel}.
 * <p>
 * Created on the FX thread inside {@code AnalyticsService.publishSnapshot()} where it is safe
 * to read JavaFX properties. Background threads (analytics, file-io) consume this record
 * exclusively, avoiding any cross-thread access to the mutable {@link ProcessModel}.
 */
public record ProcessSnapshot(
        int pid,
        String name,
        Category category,
        long totalSeconds,
        String aliasName,
        double cpuUsagePercent,
        double ramUsagePercent,
        boolean trackingFreezed
) {
    /**
     * Creates an immutable snapshot from a mutable ProcessModel.
     *
     * @param m the ProcessModel to snapshot
     * @return a new ProcessSnapshot with all current values from the model
     */
    public static ProcessSnapshot of(ProcessModel m) {
        return new ProcessSnapshot(
                m.getPid(),
                m.getName(),
                m.getCategory(),
                m.getTotalSeconds(),
                m.getAliasName(),
                m.getCpuUsagePercent(),
                m.getRamUsagePercent(),
                m.isTrackingFreezed()
        );
    }

    /**
     * Formats the total seconds into a human-readable time string.
     *
     * @return formatted time string (e.g., "2h 30m 45s")
     */
    public String formattedTime() {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format("%dh %dm %ds", h, m, s);
    }

    /**
     * Returns a formatted string of RAM and CPU usage.
     *
     * @return string in format "X.X% / Y.Y%"
     */
    public String ramAndCpu() {
        return String.format("%.1f%% / %.1f%%", ramUsagePercent, cpuUsagePercent);
    }
}
