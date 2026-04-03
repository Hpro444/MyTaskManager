package com.mytaskmanager.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.mytaskmanager.domain.ProcessInfoEntry;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Utility for deserializing process entries from JSON files.
 * <p>
 * Handles loading user-configured process metadata from process_info.json,
 * including aliases, categories, freeze state, and accumulated time. Gracefully
 * handles missing files or unknown categories via Jackson defaults.
 * </p>
 */
public class JsonProcessInfoReader {

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /**
     * Reads all process entries from a file with the structure:
     * { "processes": [ { "originalName": "...", ... }, ... ] }
     *
     * @param filePath the path to the JSON file
     * @return list of ProcessInfoEntry, or empty list if file is missing
     * @throws IOException if an I/O error occurs
     */
    public static List<ProcessInfoEntry> readAll(String filePath) throws IOException {
        JsonNode root = mapper.readTree(new File(filePath));
        JsonNode processesNode = root.get("processes");
        if (processesNode == null || processesNode.isNull()) return List.of();
        return mapper.readerForListOf(ProcessInfoEntry.class).readValue(processesNode);
    }
}