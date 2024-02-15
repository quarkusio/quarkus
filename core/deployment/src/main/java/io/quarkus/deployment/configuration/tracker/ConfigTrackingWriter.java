package io.quarkus.deployment.configuration.tracker;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.quarkus.bootstrap.util.PropertyUtils;
import io.quarkus.deployment.configuration.BuildTimeConfigurationReader;
import io.quarkus.runtime.LaunchMode;

public class ConfigTrackingWriter {

    /**
     * Checks whether a given configuration option matches at least one of the patterns.
     * If the list of patterns is empty, the method will return false.
     *
     * @param name configuration option name
     * @param patterns a list of name patterns
     * @return true in case the option name matches at least one of the patterns, otherwise - false
     */
    private static boolean matches(String name, List<Pattern> patterns) {
        for (var pattern : patterns) {
            if (pattern.matcher(name).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Configuration writer that will persist collected configuration options and their values
     * to a file derived from the config.
     */
    public static void write(Map<String, String> readOptions, ConfigTrackingConfig config,
            BuildTimeConfigurationReader.ReadResult configReadResult,
            LaunchMode launchMode, Path buildDirectory) {
        if (!config.enabled()) {
            return;
        }

        Path file = config.file().orElse(null);
        if (file == null) {
            final Path dir = config.directory().orElseGet(() -> (buildDirectory.getParent() == null
                    ? buildDirectory
                    : buildDirectory.getParent()).resolve(".quarkus"));
            file = dir
                    .resolve(config.filePrefix() + "-" + launchMode.getDefaultProfile() + config.fileSuffix());
        } else if (!file.isAbsolute()) {
            file = config.directory().orElse(buildDirectory).resolve(file);
        }

        if (file.getParent() != null) {
            try {
                Files.createDirectories(file.getParent());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        write(readOptions, config, configReadResult, file);
    }

    /**
     * Configuration writer that will persist collected configuration options and their values
     * to a file.
     */
    public static void write(Map<String, String> readOptions, ConfigTrackingConfig config,
            BuildTimeConfigurationReader.ReadResult configReadResult, Path file) {
        final List<Pattern> excludePatterns = config.getExcludePatterns();
        final ConfigTrackingValueTransformer valueTransformer = ConfigTrackingValueTransformer.newInstance(config);

        final Map<String, String> allBuildTimeValues = configReadResult.getAllBuildTimeValues();
        final Map<String, String> buildTimeRuntimeValues = configReadResult.getBuildTimeRunTimeValues();
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            final List<String> names = new ArrayList<>(readOptions.size());
            for (var name : readOptions.keySet()) {
                if ((allBuildTimeValues.containsKey(name) || buildTimeRuntimeValues.containsKey(name))
                        && !matches(name, excludePatterns)) {
                    names.add(name);
                }
            }
            Collections.sort(names);
            for (String name : names) {
                var value = valueTransformer.transform(name, readOptions.get(name));
                write(writer, name, value);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes a config option with its value to the target writer,
     * possibly applying some transformations, such as character escaping
     * prior to writing.
     *
     * @param writer target writer
     * @param name option name
     * @param value option value
     * @throws IOException in case of a failure
     */
    public static void write(Writer writer, String name, String value) throws IOException {
        PropertyUtils.store(writer, name, value);
    }
}
