package io.quarkus.analytics;

import static io.quarkus.analytics.common.TestFilesUtils.backupExisting;
import static io.quarkus.analytics.common.TestFilesUtils.restore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.analytics.common.TestRestClient;
import io.quarkus.analytics.config.FileLocations;
import io.quarkus.analytics.config.FileLocationsImpl;
import io.quarkus.analytics.dto.config.NoopRemoteConfig;
import io.quarkus.analytics.dto.config.RemoteConfig;
import io.quarkus.analytics.util.FileUtils;
import io.quarkus.devtools.messagewriter.MessageWriter;

@Disabled("For manual testing only")
class ConfigServiceManualTest {

    private final FileLocations fileLocations = FileLocationsImpl.INSTANCE;

    @Test
    void activeWithConfig() throws IOException {
        backupExisting(getBackupConfigFile(),
                fileLocations.getRemoteConfigFile());

        RemoteConfig remoteConfig = RemoteConfig.builder()
                .active(true)
                .denyQuarkusVersions(Collections.emptyList())
                .denyUserIds(Collections.emptyList())
                .refreshInterval(Duration.ofHours(12)).build();

        FileUtils.write(remoteConfig, fileLocations.getRemoteConfigFile());
        long lastModified = fileLocations.getRemoteConfigFile().toFile().lastModified();

        ConfigService configService = new ConfigService(new TestRestClient(remoteConfig),
                AnonymousUserId.getInstance(fileLocations, MessageWriter.info()),
                fileLocations,
                MessageWriter.info());
        assertNotNull(configService);
        assertTrue(configService.isActive());
        assertEquals(lastModified, fileLocations.getRemoteConfigFile().toFile().lastModified(), "File must not change");

        restore(getBackupConfigFile(),
                fileLocations.getRemoteConfigFile());
    }

    @Test
    void activeWithoutConfig() throws IOException {
        backupExisting(getBackupConfigFile(),
                fileLocations.getRemoteConfigFile());

        RemoteConfig remoteConfig = RemoteConfig.builder()
                .active(true)
                .denyQuarkusVersions(Collections.emptyList())
                .denyUserIds(Collections.emptyList())
                .refreshInterval(Duration.ZERO).build();

        assertFalse(Files.exists(fileLocations.getRemoteConfigFile()));

        ConfigService configService = new ConfigService(new TestRestClient(remoteConfig),
                AnonymousUserId.getInstance(fileLocations, MessageWriter.info()),
                fileLocations,
                MessageWriter.info());
        assertNotNull(configService);
        assertTrue(configService.isActive());

        assertTrue(Files.exists(fileLocations.getRemoteConfigFile()));

        restore(getBackupConfigFile(),
                fileLocations.getRemoteConfigFile());
    }

    @Test
    void remoteConfigOff() throws IOException {
        backupExisting(getBackupConfigFile(),
                fileLocations.getRemoteConfigFile());

        ConfigService configService = new ConfigService(new TestRestClient(NoopRemoteConfig.INSTANCE),
                AnonymousUserId.getInstance(fileLocations, MessageWriter.info()),
                fileLocations,
                MessageWriter.info());
        assertNotNull(configService);
        assertFalse(configService.isActive());

        restore(getBackupConfigFile(),
                fileLocations.getRemoteConfigFile());
    }

    @Test
    void isArtifactActive() {

    }

    @Test
    void isArtifactInactive() {

    }

    private Path getBackupConfigFile() {
        return fileLocations.getFolder().resolve("com.redhat.devtools.quarkus.analytics.back");
    }
}
