package io.quarkus.registry.config;

import java.nio.file.Path;

public interface ConfigSource {

    ConfigSource DEFAULT = new ConfigSource() {
        @Override
        public Path getFilePath() {
            return null;
        }

        @Override
        public String describe() {
            return "default configuration";
        }
    };

    ConfigSource MANUAL = new ConfigSource() {
        @Override
        public Path getFilePath() {
            return null;
        }

        @Override
        public String describe() {
            return "manually configured (programmatic)";
        }
    };

    ConfigSource ENV = new ConfigSource() {
        @Override
        public Path getFilePath() {
            return null;
        }

        @Override
        public String describe() {
            return String.format(
                    "environment variables: registries defined in %s, with supporting configuration in variables prefixed with %s",
                    RegistriesConfigLocator.QUARKUS_REGISTRIES,
                    RegistriesConfigLocator.QUARKUS_REGISTRY_ENV_VAR_PREFIX);
        }
    };

    /**
     * @return Path to source file, or null if config is not file-based.
     */
    Path getFilePath();

    /**
     * Describe the source of this registry configuration for use with info
     * and error messages.
     *
     * @return String describing configuration source
     */
    String describe();

    class FileConfigSource implements ConfigSource {
        final Path configFile;

        public FileConfigSource(Path configFile) {
            this.configFile = configFile;
        }

        @Override
        public Path getFilePath() {
            return configFile;
        }

        @Override
        public String describe() {
            return String.format("file: %s", configFile);
        }
    }
}
