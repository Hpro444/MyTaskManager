package com.mytaskmanager.utils;

import com.mytaskmanager.domain.ProcessSnapshot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utility for writing process snapshots to timestamped CSV files.
 * <p>
 * Creates snapshot files in the format snapshot_YYYY_MM_DD_HH_MM_SS.csv with
 * each row containing process metrics (PID, name, CPU, RAM, category, alias).
 * Handles CSV escaping for special characters in process names and aliases.
 * </p>
 */
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
     *
     * @param processes the list of ProcessSnapshot to write
     * @param directory the directory to write the file to
     * @throws IOException if an I/O error occurs
     */
    public static void write(List<ProcessSnapshot> processes, String directory) throws IOException {
        Instant now = Instant.now();
        String filename = "snapshot_" + FILE_TS.format(now) + ".csv";
        File dir = new File(directory);
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, filename);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write("timestamp,pid,process_name,cpu_usage,ram_usage,category,alias_name");
            bw.newLine();

            String ts = ROW_TS.format(now);
            for (ProcessSnapshot p : processes) {
                bw.write(String.format("%s,%d,%s,%.2f,%.2f,%s,%s",
                        ts,
                        p.pid(),
                        escapeCsv(p.name()),
                        p.cpuUsagePercent(),
                        p.ramUsagePercent(),
                        p.category().name(),
                        escapeCsv(p.aliasName())));
                bw.newLine();
            }
        }
    }

    /**
     * Escapes CSV values by quoting and doubling quotes if necessary.
     *
     * @param value the value to escape
     * @return the escaped value
     */
    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
