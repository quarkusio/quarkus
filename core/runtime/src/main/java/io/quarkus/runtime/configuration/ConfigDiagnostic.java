package io.quarkus.runtime.configuration;

import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ImageMode;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.common.utils.StringUtil;

/**
 * Utility methods to log configuration problems.
 */
public final class ConfigDiagnostic {
    private static final Logger log = Logger.getLogger("io.quarkus.config");

    private static final List<String> errorsMessages = new CopyOnWriteArrayList<>();
    private static final Set<String> errorKeys = new CopyOnWriteArraySet<>();

    private ConfigDiagnostic() {
    }

    public static void invalidValue(String name, IllegalArgumentException ex) {
        final String message = ex.getMessage();
        final String loggedMessage = message != null ? message
                : String.format("An invalid value was given for configuration key \"%s\"", name);
        errorsMessages.add(loggedMessage);
        errorKeys.add(name);
    }

    public static void missingValue(String name, NoSuchElementException ex) {
        final String message = ex.getMessage();
        final String loggedMessage = message != null ? message
                : String.format("Configuration key \"%s\" is required, but its value is empty/missing", name);
        errorsMessages.add(loggedMessage);
        errorKeys.add(name);
    }

    public static void duplicate(String name) {
        final String loggedMessage = String.format("Configuration key \"%s\" was specified more than once", name);
        errorsMessages.add(loggedMessage);
        errorKeys.add(name);
    }

    public static void deprecated(String name) {
        log.warnf("Configuration key \"%s\" is deprecated", name);
    }

    public static void unknown(String name) {
        log.warnf(
                "Unrecognized configuration key \"%s\" was provided; it will be ignored; verify that the dependency extension for this configuration is set or that you did not make a typo",
                name);
    }

    public static void unknown(NameIterator name) {
        unknown(name.getName());
    }

    /**
     * Report any unused properties.
     * <br>
     * The list of unused properties may contain false positives. This is caused when an environment variable is set up,
     * and we cannot determine correctly if it was used or not.
     * <br>
     * Environment variables require a conversion to regular property names so a Map can be properly populated when
     * iterating {@link Config#getPropertyNames()}. Because an Environment variable name may match multiple property
     * names, we try the best effort to report unknowns by matching used properties in their Environment variable name
     * format.
     *
     * @param properties the set of possible unused properties
     */
    public static void unknownProperties(Set<String> properties) {
        if (properties.isEmpty()) {
            return;
        }
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        Set<String> usedProperties = new HashSet<>();
        StringBuilder tmp = null;
        for (String property : config.getPropertyNames()) {
            if (properties.contains(property)) {
                continue;
            }
            if (tmp == null) {
                tmp = new StringBuilder(property.length());
            } else {
                tmp.setLength(0);
            }
            String usedProperty = StringUtil.replaceNonAlphanumericByUnderscores(property, tmp);
            if (properties.contains(usedProperty)) {
                continue;
            }
            usedProperties.add(usedProperty);
        }
        for (String property : properties) {
            // Indexed properties not supported by @ConfigRoot, but they can show up due to the YAML source. Just ignore them.
            if (property.indexOf('[') != -1 && property.indexOf(']') != -1) {
                continue;
            }

            boolean found = false;
            if (!usedProperties.isEmpty()) {
                if (tmp == null) {
                    tmp = new StringBuilder(property.length());
                } else {
                    tmp.setLength(0);
                }
                String propertyWithUnderscores = StringUtil.replaceNonAlphanumericByUnderscores(property, tmp);
                for (String usedProperty : usedProperties) {
                    if (usedProperty.equalsIgnoreCase(propertyWithUnderscores)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                ConfigValue configValue = config.getConfigValue(property);
                if (property.equals(configValue.getName())) {
                    unknown(property);
                }
            }
        }
    }

    public static void reportUnknown(Set<String> properties) {
        if (ImageMode.current() == ImageMode.NATIVE_BUILD) {
            unknownProperties(properties);
        }
    }

    public static void reportUnknownRuntime(Set<String> properties) {
        unknownProperties(properties);
    }

    /**
     * Determine if a fatal configuration error has occurred.
     *
     * @return {@code true} if a fatal configuration error has occurred
     */
    public static boolean isError() {
        return !errorsMessages.isEmpty();
    }

    /**
     * Reset the config error status (for e.g. testing).
     */
    public static void resetError() {
        errorKeys.clear();
        errorsMessages.clear();
    }

    public static String getNiceErrorMessage() {
        StringBuilder b = new StringBuilder();
        for (String errorsMessage : errorsMessages) {
            b.append("  - ");
            b.append(errorsMessage);
            b.append(System.lineSeparator());
        }
        return b.toString();
    }

    public static Set<String> getErrorKeys() {
        return new HashSet<>(errorKeys);
    }

    private static final DirectoryStream.Filter<Path> CONFIG_FILES_FILTER = new DirectoryStream.Filter<>() {
        @Override
        public boolean accept(final Path entry) {
            // Ignore .properties, because we know these are have a default loader in core
            // Ignore profile files. The loading rules require the main file to be present, so we only need the type
            String filename = entry.getFileName().toString();
            return Files.isRegularFile(entry) && filename.startsWith("application.") && !filename.endsWith(".properties");
        }
    };

    public static Set<String> configFiles(Path configFilesLocation) throws IOException {
        if (!Files.exists(configFilesLocation)) {
            return Collections.emptySet();
        }

        Set<String> configFiles = new HashSet<>();
        try (DirectoryStream<Path> candidates = Files.newDirectoryStream(configFilesLocation, CONFIG_FILES_FILTER)) {
            for (Path candidate : candidates) {
                configFiles.add(candidate.toUri().toURL().toString());
            }
        } catch (NotDirectoryException ignored) {
            log.debugf("File %s is not a directory", configFilesLocation.toAbsolutePath());
            return Collections.emptySet();
        }
        return configFiles;
    }

    public static Set<String> configFilesFromLocations() throws Exception {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);

        Set<String> configFiles = new HashSet<>();
        configFiles.addAll(configFiles(Paths.get(System.getProperty("user.dir"), "config")));
        Optional<List<URI>> optionalLocations = config.getOptionalValues(SMALLRYE_CONFIG_LOCATIONS, URI.class);
        optionalLocations.ifPresent(new Consumer<List<URI>>() {
            @Override
            public void accept(final List<URI> locations) {
                for (URI location : locations) {
                    Path path = location.getScheme() != null && location.getScheme().equals("file") ? Paths.get(location)
                            : Paths.get(location.getPath());
                    if (Files.isDirectory(path)) {
                        try {
                            configFiles.addAll(configFiles(path));
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                }
            }
        });

        return configFiles;
    }

    public static void unknownConfigFiles(final Set<String> configFiles) {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        Set<String> configNames = new HashSet<>();
        for (ConfigSource configSource : config.getConfigSources()) {
            if (configSource.getName() != null && configSource.getName().contains("application")) {
                configNames.add(configSource.getName());
            }
        }

        for (String configFile : configFiles) {
            boolean found = false;
            for (String configName : configNames) {
                if (configName.contains(configFile)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                log.warnf(
                        "Unrecognized configuration file %s found; Please, check if your are providing the proper extension to load the file",
                        configFile);
            }
        }
    }
}
