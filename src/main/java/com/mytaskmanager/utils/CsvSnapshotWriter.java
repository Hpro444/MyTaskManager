package com.mytaskmanager.utils;

import com.mytaskmanager.domain.ProcessModel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CsvSnapshotWriter {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter
            .ofPattern("yyyy_MM_dd_HH_mm_ss")
            .withZone(ZoneOffset.UTC);

    private static final DateTimeFormatter ROW_TS = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);

    /**
     * Writes a CSV snapshot of all running processes.
     * Filename: snapshot_<timestamp>.csv in the given directory.
     * Header: timestamp,pid,process_name,cpu_usage,ram_usage,category,alias_name
     */
    public static void write(List<ProcessModel> processes, String directory) throws IOException {
        Instant now = Instant.now();
        String filename = "snapshot_" + FILE_TS.format(now) + ".csv";
        File dir = new File(directory);
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, filename);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write("timestamp,pid,process_name,cpu_usage,ram_usage,category,alias_name");
            bw.newLine();

            String ts = ROW_TS.format(now);
            for (ProcessModel p : processes) {
                bw.write(String.format("%s,%d,%s,%.2f,%.2f,%s,%s",
                        ts,
                        p.getPid(),
                        escapeCsv(p.getName()),
                        p.getCpuUsagePercent(),
                        p.getRamUsagePercent(),
                        p.getCategory().name(),
                        escapeCsv(p.getAliasName())));
                bw.newLine();
            }
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
