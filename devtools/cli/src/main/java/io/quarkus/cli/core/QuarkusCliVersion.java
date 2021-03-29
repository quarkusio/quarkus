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
        final URL quarkusPropsUrl = Thread.currentThread().getContextClassLoader().getResource("quarkus.properties");
        if (quarkusPropsUrl == null) {
            throw new RuntimeException("Failed to locate quarkus.properties on the classpath");
        }
        final Properties props = new Properties();
        ClassPathUtils.consumeAsPath(quarkusPropsUrl, p -> {
            try (BufferedReader reader = Files.newBufferedReader(p)) {
                props.load(reader);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load quarkus.properties", e);
            }
        });
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
