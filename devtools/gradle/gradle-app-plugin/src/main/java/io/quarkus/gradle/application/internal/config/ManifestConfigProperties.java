package io.quarkus.gradle.application.internal.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;

import org.gradle.api.GradleException;

public final class ManifestConfigProperties {

    private static final String ATTRIBUTES_PREFIX = "quarkus.package.jar.manifest.attributes";
    private static final String SECTIONS_PREFIX = "quarkus.package.jar.manifest.sections";

    private ManifestConfigProperties() {
    }

    public static Map<String, String> attributes(String context, Map<String, String> attributes) {
        return convert(context, null, attributes);
    }

    public static Map<String, String> section(String context, String section, Map<String, String> attributes) {
        validateSectionName(context, section);
        return convert(context, section, attributes);
    }

    private static Map<String, String> convert(String context, String section, Map<String, String> attributes) {
        Map<Attributes.Name, String> names = new HashMap<>();
        Map<String, String> properties = new TreeMap<>();
        attributes.forEach((name, value) -> {
            Attributes.Name manifestName = validateAttributeName(context, name);
            String previous = names.putIfAbsent(manifestName, name);
            if (previous != null) {
                throw new GradleException("Manifest attribute names '" + previous + "' and '" + name + "' for "
                        + context + " differ only by case");
            }
            properties.put(propertyName(section, name), value);
        });
        return Collections.unmodifiableMap(properties);
    }

    private static Attributes.Name validateAttributeName(String context, String name) {
        if (name.indexOf('"') >= 0) {
            throw new GradleException("Manifest attribute name '" + name + "' for " + context
                    + " is invalid: double quotes are not allowed");
        }
        try {
            return new Attributes.Name(name);
        } catch (IllegalArgumentException e) {
            throw new GradleException("Manifest attribute name '" + name + "' for " + context + " is invalid", e);
        }
    }

    private static void validateSectionName(String context, String section) {
        if (section.isBlank()) {
            throw new GradleException("Manifest section name for " + context + " must not be blank");
        }
        for (int i = 0; i < section.length(); i++) {
            char character = section.charAt(i);
            if (character == '"' || Character.isISOControl(character)) {
                throw new GradleException("Manifest section name '" + section + "' for " + context
                        + " is invalid: double quotes and control characters are not allowed");
            }
        }
    }

    private static String propertyName(String section, String name) {
        if (section == null) {
            return ATTRIBUTES_PREFIX + ".\"" + name + '"';
        }
        return SECTIONS_PREFIX + ".\"" + section + "\".\"" + name + '"';
    }
}
