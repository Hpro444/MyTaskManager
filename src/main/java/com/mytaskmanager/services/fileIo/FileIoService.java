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

    public void save(List<ProcessInfoEntry> entries, String path) {
        try {
            JsonProcessInfoWriter.writeAll(path, entries);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<ProcessInfoEntry> load(String path) {
        try {
            return JsonProcessInfoReader.readAll(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeCsvSnapshot(List<ProcessSnapshot> processes, String directory) {
        try {
            CsvSnapshotWriter.write(processes, directory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
