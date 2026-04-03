package com.mytaskmanager.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProcessInfoEntry(
        @JsonProperty("originalName") String originalName,
        @JsonProperty("aliasName") String aliasName,
        @JsonProperty("category") Category category,
        @JsonProperty("isTrackingFrozen") boolean isTrackingFrozen,
        @JsonProperty("totalTimeSeconds") long totalTimeSeconds
) {
}