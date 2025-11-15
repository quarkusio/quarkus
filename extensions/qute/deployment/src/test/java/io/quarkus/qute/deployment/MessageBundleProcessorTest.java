package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MessageBundleProcessorTest {

    @Test
    void bundleNameMatchesFileName() {
        assertTrue(new MessageBundleProcessor.MessageFile(Path.of("messages.properties"), 0).matchesBundle("messages"));
        assertTrue(new MessageBundleProcessor.MessageFile(Path.of("started.properties"), 0).matchesBundle("started"));
        assertTrue(new MessageBundleProcessor.MessageFile(Path.of("startedValidation.properties"), 0)
                .matchesBundle("startedValidation"));
        assertTrue(new MessageBundleProcessor.MessageFile(Path.of("EmailBundles_startedValidation.properties"), 0)
                .matchesBundle("EmailBundles_startedValidation"));
        assertTrue(new MessageBundleProcessor.MessageFile(Path.of("EmailBundles_startedValidation_pt_BR.properties"), 0)
                .matchesBundle("EmailBundles_startedValidation"));
        assertFalse(
                new MessageBundleProcessor.MessageFile(Path.of("startedValidation.properties"), 0).matchesBundle("started"));
        assertFalse(new MessageBundleProcessor.MessageFile(Path.of("EmailBundles_startedValidation.properties"), 0)
                .matchesBundle("EmailBundles_started"));
        assertFalse(new MessageBundleProcessor.MessageFile(Path.of("EmailBundles_startedValidation_pt_BR.properties"), 0)
                .matchesBundle("EmailBundles_started"));
    }
}
