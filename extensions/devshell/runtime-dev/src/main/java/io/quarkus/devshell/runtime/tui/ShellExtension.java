package io.quarkus.devshell.runtime.tui;

import java.util.List;

/**
 * Simplified extension data for the TUI.
 * This is a runtime representation passed from build time.
 */
public record ShellExtension(
        String namespace,
        String name,
        String description,
        String status,
        List<String> keywords,
        boolean active) {

    /**
     * Create from basic data.
     */
    public static ShellExtension of(String namespace, String name, String description, String status, boolean active) {
        return new ShellExtension(namespace, name, description, status, List.of(), active);
    }

    /**
     * Get a display-friendly short name.
     */
    public String getDisplayName() {
        if (name != null && !name.isEmpty()) {
            return name;
        }
        // Extract from namespace (e.g., "io.quarkus.resteasy-reactive" -> "resteasy-reactive")
        if (namespace != null && namespace.contains(".")) {
            return namespace.substring(namespace.lastIndexOf('.') + 1);
        }
        return namespace != null ? namespace : "Unknown";
    }

    /**
     * Check if this is an active extension (has Dev UI integration).
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Get a short description (first sentence or truncated).
     */
    public String getShortDescription() {
        if (description == null || description.isEmpty()) {
            return "";
        }
        int dotPos = description.indexOf('.');
        if (dotPos > 0 && dotPos < 100) {
            return description.substring(0, dotPos + 1);
        }
        if (description.length() > 100) {
            return description.substring(0, 97) + "...";
        }
        return description;
    }
}
