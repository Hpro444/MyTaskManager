package com.mytaskmanager.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.mytaskmanager.domain.ProcessInfoEntry;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JsonProcessInfoWriter {

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Writes all process entries to a file with the structure:
     * { "processes": [ { "originalName": "...", ... }, ... ] }
     */
    public static void writeAll(String filePath, List<ProcessInfoEntry> entries) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode processes = mapper.createArrayNode();

        for (ProcessInfoEntry e : entries) {
            ObjectNode node = mapper.createObjectNode();
            node.put("originalName", e.originalName());
            node.put("aliasName", e.aliasName());
            node.put("category", e.category().displayName());
            node.put("isTrackingFrozen", e.isTrackingFrozen());
            node.put("totalTimeSeconds", e.totalTimeSeconds());
            processes.add(node);
        }

        root.set("processes", processes);
        mapper.writeValue(new File(filePath), root);
    }
}
