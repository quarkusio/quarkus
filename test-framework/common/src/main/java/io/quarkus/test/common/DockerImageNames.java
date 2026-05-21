package io.quarkus.test.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class DockerImageNames {

    private static final Map<String, String> IMAGES = new HashMap<>();

    static {
        InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("quarkus-test-container-images.properties");
        if (in == null) {
            in = DockerImageNames.class.getClassLoader()
                    .getResourceAsStream("quarkus-test-container-images.properties");
        }
        if (in == null) {
            throw new IllegalStateException("quarkus-test-container-images.properties not found on classpath");
        }
        try (InputStream propertiesStream = in) {
            Properties properties = new Properties();
            properties.load(propertiesStream);
            for (String name : properties.stringPropertyNames()) {
                IMAGES.put(name, properties.getProperty(name));
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DockerImageNames() {
    }

    /**
     * Resolves a docker image name for the given key.
     * System properties take precedence over the defaults defined in quarkus-test-container-images.properties.
     *
     * @param key the property key (e.g. "postgres.image")
     * @return the docker image name
     * @throws IllegalArgumentException if no image name is defined for the given key
     */
    public static String getImage(String key) {
        String override = System.getProperty(key);
        if (override != null) {
            return override;
        }
        String value = IMAGES.get(key);
        if (value == null) {
            throw new IllegalArgumentException("No docker image defined for key: " + key);
        }
        return value;
    }
}
