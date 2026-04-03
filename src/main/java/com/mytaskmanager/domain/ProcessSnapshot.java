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

    public String formattedTime() {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format("%dh %dm %ds", h, m, s);
    }

    public String ramAndCpu() {
        return String.format("%.1f%% / %.1f%%", ramUsagePercent, cpuUsagePercent);
    }
}
