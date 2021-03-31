package io.quarkus.cli.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Properties;

import io.smallrye.common.classloader.ClassPathUtils;
import picocli.CommandLine;

public class QuarkusCliVersion implements CommandLine.IVersionProvider {

    private static String version;

    public static String version() {
        if (version != null) {
            return version;
        }

        final Properties props = new Properties();
        final URL quarkusPropertiesUrl = Thread.currentThread().getContextClassLoader().getResource("quarkus.properties");
        if (quarkusPropertiesUrl == null) {
            throw new RuntimeException("Failed to locate quarkus.properties on the classpath");
        }

        // we have a special case for file and jar as using getResourceAsStream() on Windows might cause file locks
        if ("file".equals(quarkusPropertiesUrl.getProtocol()) || "jar".equals(quarkusPropertiesUrl.getProtocol())) {
            ClassPathUtils.consumeAsPath(quarkusPropertiesUrl, p -> {
                try (BufferedReader reader = Files.newBufferedReader(p)) {
                    props.load(reader);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load quarkus.properties", e);
                }
            });
        } else {
            try {
                props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("quarkus.properties"));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load quarkus.properties", e);
            }
        }

        version = props.getProperty("quarkus-core-version");
        if (version == null) {
            throw new RuntimeException("Failed to locate quarkus-core-version property in the bundled quarkus.properties");
        }

        return version;
    }

    @Override
    public String[] getVersion() throws Exception {
        return new String[] { version() };
    }

}
