package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MessageBundleProcessorTest {

    @Test
    void bundleNameMatchesFileName() {
        assertTrue(MessageBundleProcessor.bundleNameMatchesFileName("messages.properties", "messages"));
        assertTrue(MessageBundleProcessor.bundleNameMatchesFileName("started.properties", "started"));
        assertTrue(MessageBundleProcessor.bundleNameMatchesFileName("startedValidation.properties", "startedValidation"));
        assertTrue(MessageBundleProcessor.bundleNameMatchesFileName("EmailBundles_startedValidation.properties",
                "EmailBundles_startedValidation"));
        assertTrue(MessageBundleProcessor.bundleNameMatchesFileName("EmailBundles_startedValidation_pt_BR.properties",
                "EmailBundles_startedValidation"));

        assertFalse(MessageBundleProcessor.bundleNameMatchesFileName("startedValidation.properties", "started"));
        assertFalse(MessageBundleProcessor.bundleNameMatchesFileName("EmailBundles_startedValidation.properties",
                "EmailBundles_started"));
        assertFalse(MessageBundleProcessor.bundleNameMatchesFileName("EmailBundles_startedValidation_pt_BR.properties",
                "EmailBundles_started"));
    }
}
