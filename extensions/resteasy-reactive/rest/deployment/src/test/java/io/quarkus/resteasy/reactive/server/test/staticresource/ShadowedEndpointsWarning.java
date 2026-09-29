package io.quarkus.resteasy.reactive.server.test.staticresource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import org.jboss.logmanager.Level;

final class ShadowedEndpointsWarning {

    static final String LOGGER = "io.quarkus.resteasy.reactive.server.runtime.StaticResourceShadowingCheck";

    private ShadowedEndpointsWarning() {
    }

    static void assertShadowedEndpoints(List<LogRecord> records, String... expectedEntries) {
        assertEquals(1, records.size(),
                () -> "expected exactly one warning about shadowed endpoints, got: " + messages(records));
        LogRecord record = records.get(0);
        assertEquals(Level.WARN, record.getLevel(),
                () -> "expected a warning, got " + record.getLevel() + ": " + record.getMessage());
        List<String> entries = record.getMessage().lines().map(String::strip).filter(line -> line.contains(" -> "))
                .sorted().toList();
        assertEquals(Arrays.stream(expectedEntries).sorted().toList(), entries,
                () -> "unexpected shadowed endpoints in the warning:\n" + record.getMessage());
        assertTrue(record.getMessage().contains("set quarkus.log.category.\"" + LOGGER
                + "\".level=ERROR to disable this warning"),
                () -> "expected the warning to tell how to disable it:\n" + record.getMessage());
        // an entry lists "and <number> more" paths when they are left out
        boolean pathsLeftOut = entries.stream().anyMatch(entry -> entry.matches(".* and \\d+ more -> .*"));
        assertEquals(pathsLeftOut, record.getMessage().contains("Set quarkus.log.category.\"" + LOGGER
                + "\".level=DEBUG to list all the paths"),
                () -> "expected the warning to tell how to list all the paths only if some are left out:\n"
                        + record.getMessage());
    }

    static void assertNoShadowedEndpoints(List<LogRecord> records) {
        assertTrue(records.isEmpty(),
                () -> "expected no warning about shadowed endpoints, got: " + messages(records));
    }

    private static String messages(List<LogRecord> records) {
        return records.stream().map(LogRecord::getMessage).collect(Collectors.joining("\n"));
    }
}
