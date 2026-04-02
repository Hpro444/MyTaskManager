package com.mytaskmanager.domain;

public record ProcessInfoEntry(String originalName,
                               String aliasName,
                               Category category,
                               boolean isTrackingFreezed,
                               long totalTimeSeconds) {
}

