package io.quarkus.liquibase.common.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Build-time generated logical→physical classpath paths for Liquibase changelogs (native image).
 */
public final class LiquibaseLogicalPathMappings {

    /**
     * JDBC / Agroal Liquibase extension mappings (all named datasources merged).
     */
    public static final String JDBC_MAPPING_RESOURCE = "META-INF/quarkus/quarkus-liquibase-logical-path-mappings.properties";

    /**
     * MongoDB Liquibase extension mappings (all clients merged).
     */
    public static final String MONGODB_MAPPING_RESOURCE = "META-INF/quarkus/quarkus-liquibase-mongodb-logical-path-mappings.properties";

    private LiquibaseLogicalPathMappings() {
    }

    public static Map<String, String> load(ClassLoader classLoader, String resourcePath) {
        try (InputStream in = classLoader.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return Collections.emptyMap();
            }
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.strip();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eq = line.indexOf('=');
                    if (eq <= 0) {
                        continue;
                    }
                    String logical = line.substring(0, eq).strip();
                    String physical = line.substring(eq + 1).strip();
                    if (!logical.isEmpty() && !physical.isEmpty()) {
                        map.put(logical, physical);
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
