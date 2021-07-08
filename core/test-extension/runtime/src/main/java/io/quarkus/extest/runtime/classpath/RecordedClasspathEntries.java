package io.quarkus.extest.runtime.classpath;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RecordedClasspathEntries {

    /*
     * This needs to be a static method, because CDI beans are available in all phases
     * where we need to record classpath entries.
     *
     * Also, we need to push the result to a file, not to a static variable or a build item,
     * because this may be executed multiple times from multiple JVMs.
     */
    public static synchronized void put(Path recordFilePath, Record record) {
        // Assuming that this will never get called concurrently from separate JVMs.
        try {
            Files.writeString(recordFilePath, Record.serialize(record) + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Path recordFilePath;

    RecordedClasspathEntries(Path recordFilePath) {
        this.recordFilePath = recordFilePath;
    }

    public List<Record> get(Phase phase, String resourceName) {
        // It may seem strange, but a given phase (e.g. augmentation) can be executed multiple times.
        // So this method returns a list instead of a single record.
        List<Record> result;
        try {
            result = Files.readAllLines(recordFilePath).stream()
                    .map(Record::deserialize)
                    .filter(r -> r.phase == phase && r.resourceName.equals(resourceName))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (result.isEmpty()) {
            throw new IllegalStateException("Classpath entries for phase '" + phase
                    + "' and resource '" + resourceName + "' were not recorded;"
                    + " make sure to set application property 'quarkus.bt.classpath-recording.resources' correctly.");
        }
        return result;
    }

    public enum Phase {
        AUGMENTATION,
        STATIC_INIT,
        RUNTIME_INIT;
    }

    public static class Record {
        private static final String SEPARATOR = "\t";

        static String serialize(Record record) {
            StringBuilder builder = new StringBuilder();
            builder.append(record.phase.name());
            builder.append(SEPARATOR);
            builder.append(record.resourceName);
            for (String classpathEntry : record.classpathEntries) {
                builder.append(SEPARATOR);
                builder.append(classpathEntry);
            }
            return builder.toString();
        }

        static Record deserialize(String line) {
            String[] elements = line.split(SEPARATOR);
            Phase phase = Phase.valueOf(elements[0]);
            String resourceName = elements[1];
            List<String> classpathEntries = new ArrayList<>();
            for (int i = 2; i < elements.length; i++) {
                classpathEntries.add(elements[i]);
            }
            return new Record(phase, resourceName, classpathEntries);
        }

        private final Phase phase;
        private final String resourceName;
        private final List<String> classpathEntries;

        public Record(Phase phase, String resourceName, List<String> classpathEntries) {
            this.phase = phase;
            this.resourceName = resourceName;
            this.classpathEntries = classpathEntries;
        }

        public Phase getPhase() {
            return phase;
        }

        public String getResourceName() {
            return resourceName;
        }

        public List<String> getClasspathEntries() {
            return classpathEntries;
        }
    }
}
