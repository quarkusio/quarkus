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
        if (value != null) {
            name = toWritableValue(name, true, true);
            value = toWritableValue(value, false, true);
            writer.write(name);
            writer.write("=");
            writer.write(value);
            writer.write(System.lineSeparator());
        }
    }

    /*
     * Converts unicodes to encoded &#92;uxxxx and escapes
     * special characters with a preceding slash
     */

    /**
     * Escapes characters that are expected to be escaped when {@link java.util.Properties} load
     * files from disk.
     *
     * @param str property name or value
     * @param escapeSpace whether to escape a whitespace (should be true for property names)
     * @param escapeUnicode whether to converts unicodes to encoded &#92;uxxxx
     * @return property name or value that can be written to a file
     */
    private static String toWritableValue(String str, boolean escapeSpace, boolean escapeUnicode) {
        int len = str.length();
        int bufLen = len * 2;
        if (bufLen < 0) {
            bufLen = Integer.MAX_VALUE;
        }
        StringBuilder outBuffer = new StringBuilder(bufLen);

        for (int x = 0; x < len; x++) {
            char aChar = str.charAt(x);
            // Handle common case first, selecting largest block that
            // avoids the specials below
            if ((aChar > 61) && (aChar < 127)) {
                if (aChar == '\\') {
                    outBuffer.append('\\');
                    outBuffer.append('\\');
                    continue;
                }
                outBuffer.append(aChar);
                continue;
            }
            switch (aChar) {
                case ' ':
                    if (x == 0 || escapeSpace) {
                        outBuffer.append('\\');
                    }
                    outBuffer.append(' ');
                    break;
                case '\t':
                    outBuffer.append('\\');
                    outBuffer.append('t');
                    break;
                case '\n':
                    outBuffer.append('\\');
                    outBuffer.append('n');
                    break;
                case '\r':
                    outBuffer.append('\\');
                    outBuffer.append('r');
                    break;
                case '\f':
                    outBuffer.append('\\');
                    outBuffer.append('f');
                    break;
                case '=': // Fall through
                case ':': // Fall through
                case '#': // Fall through
                case '!':
                    outBuffer.append('\\');
                    outBuffer.append(aChar);
                    break;
                default:
                    if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode) {
                        outBuffer.append('\\');
                        outBuffer.append('u');
                        outBuffer.append(toHex((aChar >> 12) & 0xF));
                        outBuffer.append(toHex((aChar >> 8) & 0xF));
                        outBuffer.append(toHex((aChar >> 4) & 0xF));
                        outBuffer.append(toHex(aChar & 0xF));
                    } else {
                        outBuffer.append(aChar);
                    }
            }
        }
        return outBuffer.toString();
    }

    /**
     * Convert a nibble to a hex character
     *
     * @param nibble the nibble to convert.
     */
    private static char toHex(int nibble) {
        return hexDigit[(nibble & 0xF)];
    }

    /** A table of hex digits */
    private static final char[] hexDigit = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
}
