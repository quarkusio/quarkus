package io.quarkus.it.picocli;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import picocli.CommandLine;

@Singleton
public class VersionProvider implements CommandLine.IVersionProvider {

    private final String version;

    public VersionProvider(@ConfigProperty(name = "some.version", defaultValue = "0.0.1") String version) {
        this.version = version;
    }

    @Override
    public String[] getVersion() throws Exception {
        return new String[] { version };
    }
}
