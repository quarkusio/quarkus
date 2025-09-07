package io.quarkus.cli.common;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Helper class to get client version without circular dependencies
 */
public class VersionHelper {
    private static String version;

    public static String clientVersion() {
        if (version != null) {
            return version;
        }

        final Properties props = new Properties();
        final URL quarkusPropertiesUrl = Thread.currentThread().getContextClassLoader().getResource("quarkus.properties");
        if (quarkusPropertiesUrl == null) {
            return "999-SNAPSHOT"; // fallback version
        }

        try {
            // Handle file and jar URLs differently to avoid file locks on Windows
            if (quarkusPropertiesUrl.getProtocol().equals("file")) {
                try (InputStream is = Files.newInputStream(Paths.get(quarkusPropertiesUrl.toURI()))) {
                    props.load(is);
                }
            } else {
                try (InputStream is = quarkusPropertiesUrl.openStream()) {
                    props.load(is);
                }
            }
            version = props.getProperty("quarkus.version", "999-SNAPSHOT");
        } catch (Exception e) {
            version = "999-SNAPSHOT"; // fallback version
        }
        return version;
    }
}
