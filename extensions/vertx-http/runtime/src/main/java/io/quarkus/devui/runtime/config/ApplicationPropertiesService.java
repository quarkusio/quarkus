package io.quarkus.devui.runtime.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import io.quarkus.dev.console.DevConsoleManager;

public class ApplicationPropertiesService {

    public static class ResourceDirectoryNotFoundException extends IOException {

    }

    public static class ApplicationPropertiesNotFoundException extends IOException {

        private final Path applicationPropertiesPath;

        public ApplicationPropertiesNotFoundException(Path applicationPropertiesPath) {
            this.applicationPropertiesPath = applicationPropertiesPath;
        }

        public Path getApplicationPropertiesPath() {
            return applicationPropertiesPath;
        }
    }

    public Path findOrCreateApplicationProperties() throws IOException {
        try {
            return findApplicationProperties();
        } catch (ApplicationPropertiesNotFoundException e) {
            Files.createDirectories(e.getApplicationPropertiesPath().getParent());
            Files.createFile(e.getApplicationPropertiesPath());
            return e.getApplicationPropertiesPath();
        }
    }

    public Path findApplicationProperties()
            throws ResourceDirectoryNotFoundException, ApplicationPropertiesNotFoundException {
        final var resourceDirectories = DevConsoleManager
                .getHotReplacementContext()
                .getResourcesDir();
        if (resourceDirectories.isEmpty()) {
            throw new ResourceDirectoryNotFoundException();
        }
        // TODO: we only use the first directory
        final var resourcesPath = resourceDirectories.get(0);
        final var applicationPropertiesPath = resourcesPath
                .resolve("application.properties");
        if (!Files.exists(applicationPropertiesPath)) {
            throw new ApplicationPropertiesNotFoundException(applicationPropertiesPath);
        }
        return applicationPropertiesPath;
    }

    public Reader createApplicationPropertiesReader() throws IOException {
        final var applicationPropertiesPath = this.findApplicationProperties();
        // This is the same way as SmallRye Config loads the file!
        // see io.smallrye.config.PropertiesConfigSource
        return new InputStreamReader(
                Files.newInputStream(applicationPropertiesPath),
                StandardCharsets.UTF_8);
    }

    public Properties readApplicationProperties() throws IOException {
        try (final var reader = createApplicationPropertiesReader()) {
            final var result = new Properties();
            result.load(reader);
            return result;
        }
    }

    public BufferedWriter createApplicationPropertiesWriter() throws IOException {
        final var applicationPropertiesPath = this.findOrCreateApplicationProperties();
        return Files.newBufferedWriter(applicationPropertiesPath);
    }

    public void saveApplicationProperties(String content) throws IOException {
        try (final var writer = createApplicationPropertiesWriter()) {
            if (content == null || content.isEmpty()) {
                writer.newLine();
            } else {
                writer.write(content);
            }
        }
    }

    /**
     * Used to manage the application.properties line parsing state.
     */
    private enum NextLineHandle {
        /**
         * Parse the line to find a property key.
         * (default)
         */
        PARSE,
        /**
         * Ignore the line, do not write out and do not parse.
         * (used when the line before was replaced, but the next line is part of the replacement)
         * (in case of line breaks in properties)
         */
        IGNORE,
        /**
         * Just write the line without parsing.
         * (used when the line before was NOT replaced, and the next line is continuing this line)
         */
        WRITE;
    }

    public void mergeApplicationProperties(Properties updates) throws IOException {
        if (!updates.isEmpty()) {
            final var handledKeys = new HashSet<String>();
            final List<String> lines = new ArrayList<>();
            // read line by line
            try (final var reader = new BufferedReader(
                    Optional.ofNullable(createApplicationPropertiesReader())
                            .orElseGet(() -> new StringReader("")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (ApplicationPropertiesNotFoundException e) {
                // don't do anything
            }
            // go line by line, replace and write out
            try (final var writer = createApplicationPropertiesWriter()) {
                var firstLine = true;
                var state = NextLineHandle.PARSE;
                for (String line : lines) {
                    final var linebreak = line.endsWith("\\");
                    switch (state) {
                        case IGNORE:
                            break;
                        case PARSE:
                            // parse property name from line (remove whitespaces)
                            state = NextLineHandle.WRITE;
                            final var assignmentIndex = line.indexOf('=');
                            if (assignmentIndex > 0) {
                                final String key = line.substring(0, assignmentIndex).trim();
                                if (updates.containsKey(key) && handledKeys.add(key)) {
                                    line = key + "=" + updates.getProperty(key);
                                    state = NextLineHandle.IGNORE;
                                }
                            }
                            // fallthrough
                        case WRITE:
                            if (!firstLine) {
                                writer.newLine();
                            }
                            writer.write(line);
                    }
                    if (!linebreak) {
                        state = NextLineHandle.PARSE;
                    }
                    firstLine = false;
                }
                // write new properties
                final var updatesCopy = new Properties();
                updatesCopy.putAll(updates);
                handledKeys.forEach(updatesCopy::remove);
                if (!updatesCopy.isEmpty()) {
                    writer.newLine();
                    updatesCopy.store(writer, "added by Dev UI");
                }
            }
        }
    }

}
