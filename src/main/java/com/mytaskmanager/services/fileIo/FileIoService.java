package com.mytaskmanager.services.fileIo;

import com.mytaskmanager.domain.ProcessInfoEntry;
import com.mytaskmanager.domain.ProcessSnapshot;
import com.mytaskmanager.utils.CsvSnapshotWriter;
import com.mytaskmanager.utils.JsonProcessInfoReader;
import com.mytaskmanager.utils.JsonProcessInfoWriter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Performs blocking file I/O operations.
 * Never submits or schedules work — that is the responsibility of {@link FileIoScheduler}.
 */
public class FileIoService {

    /**
     * Saves process entries to a JSON file.
     *
     * @param entries the list of ProcessInfoEntry to save
     * @param path    the file path to write to
     * @throws UncheckedIOException if an I/O error occurs
     */
    public void save(List<ProcessInfoEntry> entries, String path) {
        try {
            JsonProcessInfoWriter.writeAll(path, entries);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Loads process entries from a JSON file.
     *
     * @param path the file path to read from
     * @return list of ProcessInfoEntry loaded from the file
     * @throws UncheckedIOException if an I/O error occurs
     */
    public List<ProcessInfoEntry> load(String path) {
        try {
            return JsonProcessInfoReader.readAll(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes a CSV snapshot of all processes to a directory.
     *
     * @param processes the list of ProcessSnapshot to write
     * @param directory the directory to write the snapshot file to
     * @throws UncheckedIOException if an I/O error occurs
     */
    public void writeCsvSnapshot(List<ProcessSnapshot> processes, String directory) {
        try {
            CsvSnapshotWriter.write(processes, directory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
