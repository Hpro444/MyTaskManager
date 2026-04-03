package com.mytaskmanager.domain;

public enum Category {
    WORK, FUN, OTHER,
    @com.fasterxml.jackson.annotation.JsonEnumDefaultValue UNCATEGORIZED;

    public String displayName() {
        return switch (this) {
            case WORK -> "Work";
            case FUN -> "Fun";
            case OTHER -> "Other";
            case UNCATEGORIZED -> "Uncategorized";
        };
    }
}
