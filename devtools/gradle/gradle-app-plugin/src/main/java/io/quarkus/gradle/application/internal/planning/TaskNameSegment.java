package io.quarkus.gradle.application.internal.planning;

import java.util.Locale;

public record TaskNameSegment(String name, String value) {

    public TaskNameSegment {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Quarkus application name must not be empty");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Quarkus application task segment must not be empty");
        }
    }

    public static TaskNameSegment of(String name) {
        return new TaskNameSegment(name, toTaskSegment(name));
    }

    static String toTaskSegment(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Quarkus application name must not be empty");
        }

        var segment = new StringBuilder();
        var nextUpper = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                if (segment.isEmpty() && Character.isDigit(c)) {
                    throw new IllegalArgumentException(
                            "Quarkus application name '" + name + "' must not start with a digit");
                }
                segment.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            } else if (c == '-' || c == '_') {
                if (nextUpper) {
                    throw new IllegalArgumentException(
                            "Quarkus application name '" + name + "' has an empty name segment");
                }
                nextUpper = true;
            } else {
                throw new IllegalArgumentException("Quarkus application name '" + name
                        + "' contains unsupported character '" + c + "'");
            }
        }
        if (nextUpper) {
            throw new IllegalArgumentException(
                    "Quarkus application name '" + name + "' has an empty name segment");
        }
        if (segment.isEmpty()) {
            throw new IllegalArgumentException("Quarkus application name must not be empty");
        }
        return segment.toString();
    }

    public String collisionKey() {
        return value.toLowerCase(Locale.ROOT);
    }
}
