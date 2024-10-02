package io.quarkus.qute.deployment.identifiers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.test.QuarkusUnitTest;

public class InvalidTemplateFileNameIgnoredTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addAsResource(new StringAsset(
                            "ignored"),
                            "templates/foo o.txt"))
            .setLogRecordPredicate(log -> log.getLoggerName().contains("QuteProcessor"))
            .assertLogRecords(records -> {
                for (LogRecord r : records) {
                    if (r.getMessage().startsWith("Invalid file name detected")) {
                        return;
                    }
                }
                fail();
            });

    @Inject
    Engine engine;

    @Test
    public void testTemplateFileIgnored() {
        assertFalse(engine.isTemplateLoaded("foo o.txt"));
    }

}
