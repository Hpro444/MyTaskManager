package com.mytaskmanager.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable record storing persistent process metadata persisted in JSON.
 * <p>
 * Represents user-configured information about a process including its original name,
 * user alias, category, tracking freeze state, and accumulated total time. Deserialized
 * from and serialized to process_info.json during save/load operations.
 * </p>
 */
public record ProcessInfoEntry(
        @JsonProperty("originalName") String originalName,
        @JsonProperty("aliasName") String aliasName,
        @JsonProperty("category") Category category,
        @JsonProperty("isTrackingFrozen") boolean isTrackingFrozen,
        @JsonProperty("totalTimeSeconds") long totalTimeSeconds
) {
}