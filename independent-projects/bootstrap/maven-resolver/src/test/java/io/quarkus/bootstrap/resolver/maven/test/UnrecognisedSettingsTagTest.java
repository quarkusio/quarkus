package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;

public class UnrecognisedSettingsTagTest extends BootstrapMavenContextTestBase {

    @Test
    public void warningWithoutStackTrace() throws Exception {
        final List<LogRecord> warnings = new ArrayList<>();
        final Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                    warnings.add(record);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        final Logger logger = Logger.getLogger(BootstrapMavenContext.class.getName());
        logger.addHandler(handler);
        try {
            final BootstrapMavenContext mvn = bootstrapMavenContextWithSettings("custom-settings/unrecognised-tag");
            assertEquals("custom-repo", mvn.getEffectiveSettings().getServers().get(0).getId());
        } finally {
            logger.removeHandler(handler);
        }

        assertEquals(1, warnings.size());
        final LogRecord warning = warnings.get(0);
        assertTrue(warning.getMessage().contains("Unrecognised tag: 'unknownElement'"), warning.getMessage());
        assertNull(warning.getThrown());
    }
}
