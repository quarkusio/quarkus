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
import java.util.Map;
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
import io.smallrye.config.DefaultValuesConfigSource;
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

    public static void deprecatedProperties(Map<String, String> deprecatedProperties) {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        for (Map.Entry<String, String> entry : deprecatedProperties.entrySet()) {
            String propertyName = entry.getKey();
            ConfigValue configValue = config.getConfigValue(propertyName);
            if (configValue.getValue() != null && !DefaultValuesConfigSource.NAME.equals(configValue.getConfigSourceName())) {
                ConfigDiagnostic.deprecated(propertyName, entry.getValue());
            }
        }
    }

    public static void deprecated(String name, String javadoc) {
        if (javadoc != null) {
            log.warnf("The \"%s\" config property is deprecated and should not be used anymore. Deprecated message: %s", name,
                    javadoc);
        } else {
            log.warnf("The \"%s\" config property is deprecated and should not be used anymore.", name);
        }
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

    private static final String APPLICATION = "application";
    private static final int APPLICATION_LENGTH = APPLICATION.length();

    private static final DirectoryStream.Filter<Path> CONFIG_FILES_FILTER = new DirectoryStream.Filter<>() {
        @Override
        public boolean accept(final Path entry) {
            String filename = entry.getFileName().toString();
            // Include application files with any extension and profiled files
            return Files.isRegularFile(entry)
                    && filename.length() > APPLICATION_LENGTH
                    && filename.startsWith(APPLICATION)
                    && (filename.charAt(APPLICATION_LENGTH) == '.' || filename.charAt(APPLICATION_LENGTH) == '-');
        }
    };

    public static Set<Path> configFiles(Path configFilesLocation) throws IOException {
        if (!Files.exists(configFilesLocation)) {
            return Collections.emptySet();
        }

        Set<Path> configFiles = new HashSet<>();
        try (DirectoryStream<Path> candidates = Files.newDirectoryStream(configFilesLocation, CONFIG_FILES_FILTER)) {
            for (Path candidate : candidates) {
                configFiles.add(candidate);
            }
        } catch (NotDirectoryException ignored) {
            log.debugf("File %s is not a directory", configFilesLocation.toAbsolutePath());
            return Collections.emptySet();
        }
        return configFiles;
    }

    public static Set<Path> configFilesFromLocations() throws Exception {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);

        Set<Path> configFiles = new HashSet<>();
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

    public static void unknownConfigFiles(final Set<Path> configFiles) throws Exception {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        Set<String> configNames = new HashSet<>();
        for (ConfigSource configSource : config.getConfigSources()) {
            if (configSource.getName() != null && configSource.getName().contains(APPLICATION)) {
                configNames.add(configSource.getName());
            }
        }

        // Config sources names include the full path of the file, so we can check for unknowns if the file was not loaded by a source
        for (Path configFile : configFiles) {
            boolean found = false;
            for (String configName : configNames) {
                if (configName.contains(configFile.toUri().toURL().toString())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                String filename = configFile.getFileName().toString();
                // is a Profile aware file
                if (filename.charAt(APPLICATION_LENGTH) == '-' && filename.lastIndexOf('.') != -1) {
                    String unprofiledConfigFile = APPLICATION + "." + filename.substring(filename.lastIndexOf('.') + 1);
                    String profile = filename.substring(APPLICATION_LENGTH + 1, filename.lastIndexOf('.'));
                    if (config.getProfiles().contains(profile)
                            && !Files.exists(Path.of(configFile.getParent().toString(), unprofiledConfigFile))) {
                        log.warnf(
                                "Profiled configuration file %s is ignored; a main %s configuration file must exist in the same location to load %s",
                                configFile, unprofiledConfigFile, filename);
                    }
                } else {
                    log.warnf(
                            "Unrecognized configuration file %s found; Please, check if your are providing the proper extension to load the file",
                            configFile);
                }
            }
        }
    }
}
