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
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ImageMode;
import io.smallrye.config.Config;
import io.smallrye.config.ConfigUnknownReporter;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Utility methods to log configuration problems.
 */
public final class ConfigDiagnostic {
    private static final Logger log = Logger.getLogger("io.quarkus.config");

    private ConfigDiagnostic() {
    }

    public static void deprecatedProperties(Map<String, String> deprecatedProperties) {
        Config config = Config.get();
        for (Map.Entry<String, String> entry : deprecatedProperties.entrySet()) {
            String propertyName = entry.getKey();
            ConfigValue configValue = config.getConfigValue(propertyName);
            if (configValue.getValue() != null && !configValue.isDefault()) {
                ConfigDiagnostic.deprecated(configValue, entry.getValue());
            }
        }
    }

    static void deprecated(ConfigValue configValue, String javadoc) {
        if (javadoc != null && !javadoc.isEmpty()) {
            log.warnf("""
                    Deprecated configuration property %s provided in %s; \
                    this property is deprecated and should not be used anymore; Deprecated message: %s""",
                    configValue.getName(), configValue.getConfigSourceName(), javadoc);
        } else {
            log.warnf("""
                    Deprecated configuration property %s provided in %s; \
                    this property is deprecated and should not be used anymore""",
                    configValue.getName(), configValue.getConfigSourceName());
        }
    }

    static void unknown(ConfigValue configValue) {
        log.warnf("""
                Unrecognized configuration property %s provided in %s; \
                it will be ignored; verify that the dependency extension for this configuration is set \
                or that you did not make a typo""",
                configValue.getName(), configValue.getConfigSourceName());
    }

    /**
     * Report any unused properties.
     * <br>
     * The set of unused properties must not contain names from environment variables, because an Environment variable
     * name may match multiple property names, and we cannot determine correctly if it was used or not.
     *
     * @param properties the set of possible unused properties
     */
    public static void unknownProperties(Set<String> properties) {
        if (properties.isEmpty()) {
            return;
        }
        Config config = Config.get();
        for (String property : properties) {
            // Indexed properties not supported by @ConfigRoot, but they can show up due to the YAML source. Just ignore them.
            if (property.indexOf('[') != -1 && property.indexOf(']') != -1) {
                continue;
            }

            ConfigValue configValue = config.getConfigValue(property);
            if (property.equals(configValue.getName())) {
                unknown(configValue);
            }
        }
    }

    /**
     * Used to generate bytecode by RunTimeConfigurationGenerator.
     */
    @SuppressWarnings("unused")
    public static void reportUnknown(SmallRyeConfigBuilder builder, Set<String> roots, Predicate<NameIterator> mapped) {
        if (ImageMode.current() == ImageMode.NATIVE_BUILD) {
            reportUnknownRuntime(builder, roots, mapped);
        }
    }

    /**
     * Used to generate bytecode by RunTimeConfigurationGenerator.
     */
    @SuppressWarnings("unused")
    public static void reportUnknownRuntime(SmallRyeConfigBuilder builder, Set<String> roots, Predicate<NameIterator> mapped) {
        builder.withUnknownReporter(new ConfigUnknownReporter() {
            @Override
            public boolean accepts(final String name) {
                NameIterator nameIterator = new NameIterator(name);
                if (!PropertiesUtil.isPropertyQuarkusCompoundName(nameIterator)
                        && !PropertiesUtil.isPropertyInRoots(name, roots)) {
                    return false;
                }
                // SmallRye Config only knows the mappings of its phase, so names of other phases are not used
                return !mapped.test(nameIterator);
            }

            @Override
            public void report(final String name, final ConfigValue configValue) {
                // Indexed properties not supported by @ConfigRoot, but they can show up due to the YAML source. Just ignore them.
                if (name.indexOf('[') != -1 && name.indexOf(']') != -1) {
                    return;
                }
                // A different name means that the property was relocated or has a fallback
                if (name.equals(configValue.getName())) {
                    unknown(configValue);
                }
            }
        });
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
        } catch (IOException e) {
            // this scan only produces warnings, so a location that cannot be read must not fail the startup
            log.debugf(e, "Unable to scan %s for configuration files", configFilesLocation.toAbsolutePath());
            return Collections.emptySet();
        }
        return configFiles;
    }

    public static Set<Path> configFilesFromLocations() throws Exception {
        Config config = Config.get();

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
        Config config = Config.get();
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
                            && !Files.exists(configFile.getParent().resolve(unprofiledConfigFile))) {
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
