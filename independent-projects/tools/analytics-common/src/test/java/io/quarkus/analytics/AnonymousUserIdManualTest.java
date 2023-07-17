package io.quarkus.analytics;

import static io.quarkus.analytics.common.TestFilesUtils.backupExisting;
import static io.quarkus.analytics.common.TestFilesUtils.restore;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.analytics.config.FileLocations;
import io.quarkus.analytics.config.FileLocationsImpl;
import io.quarkus.devtools.messagewriter.MessageWriter;

@Disabled("For manual testing purposes only")
class AnonymousUserIdManualTest {

    private final FileLocations fileLocations = FileLocationsImpl.INSTANCE;

    @Test
    void testUUID() throws IOException {
        backupExisting(fileLocations.getFolder().resolve("anonymousId.back"), fileLocations.getUUIDFile());

        AnonymousUserId user = AnonymousUserId.getInstance(fileLocations, MessageWriter.info());
        assertNotNull(user.getUuid());
        assertTrue(user.getUuid().length() > 15);
        assertTrue(Files.exists(fileLocations.getUUIDFile()));

        restore(fileLocations.getFolder().resolve("anonymousId.back"), fileLocations.getUUIDFile());
    }
}
