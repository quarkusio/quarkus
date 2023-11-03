package io.quarkus.analytics;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.analytics.config.TestFileLocationsImpl;
import io.quarkus.devtools.messagewriter.MessageWriter;

class AnonymousUserIdTest {

    @Test
    void createWithNullLogger() throws IOException {
        TestFileLocationsImpl fileLocations = new TestFileLocationsImpl();
        fileLocations.setUuidFile(null);
        AnonymousUserId anonymousUserId = AnonymousUserId.getInstance(fileLocations, MessageWriter.info());
        assertNotNull(anonymousUserId);
    }

}
