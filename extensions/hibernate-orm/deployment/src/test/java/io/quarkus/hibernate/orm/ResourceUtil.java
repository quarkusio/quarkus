package io.quarkus.hibernate.orm;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.io.IOUtils;

public final class ResourceUtil {
    private ResourceUtil() {
    }

    public static String loadResourceAndReplacePlaceholders(String resourceName, Map<String, String> placeholders) {
        String content = null;
        try (var stream = ResourceUtil.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (stream == null) {
                throw new RuntimeException("Could not load '" + resourceName + "' from classpath");
            }
            content = IOUtils.toString(stream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (var entry : placeholders.entrySet()) {
            content = content.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return content;
    }
}
