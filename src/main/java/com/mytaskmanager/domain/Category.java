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

    public String color() {
        return switch (this) {
            case WORK -> "#4a6fa5";
            case FUN -> "#e05252";
            case OTHER -> "#d4880a";
            case UNCATEGORIZED -> "#6b7a8d";
        };
    }

    @Override
    public String toString() {
        return displayName();
    }
}
