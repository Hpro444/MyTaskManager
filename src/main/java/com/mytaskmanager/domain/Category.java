package com.mytaskmanager.domain;

/**
 * Enumeration of process categories for organizing tracked time.
 * <p>
 * Each category has a display name and an associated color for UI rendering.
 * {@code UNCATEGORIZED} is marked with {@code @JsonEnumDefaultValue} to handle
 * unknown categories gracefully during JSON deserialization.
 * </p>
 */
public enum Category {
    WORK, FUN, OTHER,
    @com.fasterxml.jackson.annotation.JsonEnumDefaultValue UNCATEGORIZED;

    /**
     * Returns the display name for this category suitable for UI rendering.
     *
     * @return human-readable category name
     */
    public String displayName() {
        return switch (this) {
            case WORK -> "Work";
            case FUN -> "Fun";
            case OTHER -> "Other";
            case UNCATEGORIZED -> "Uncategorized";
        };
    }

    /**
     * Returns the CSS color code for this category.
     *
     * @return hex color string (e.g., "#4a6fa5")
     */
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
