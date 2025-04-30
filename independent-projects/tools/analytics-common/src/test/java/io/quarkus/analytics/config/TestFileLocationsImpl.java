package io.quarkus.analytics.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class TestFileLocationsImpl implements FileLocations {

    private final Path tempDir;
    private Path uuidFile;
    private final Path remoteConfigFile;
    private final Path lastTryFile;
    private final Path localConfigFile;

    public TestFileLocationsImpl() throws IOException {
        this(false);
    }

    public TestFileLocationsImpl(final boolean skipLocal) throws IOException {
        tempDir = Files.createTempDirectory("temp_test_" + UUID.randomUUID().toString());
        uuidFile = tempDir.resolve("anonymousId");
        remoteConfigFile = tempDir.resolve("io.quarkus.analytics.remoteconfig");
        lastTryFile = tempDir.resolve("io.quarkus.analytics.lasttry");
        localConfigFile = tempDir.resolve("io.quarkus.analytics.localconfig");
        if (!skipLocal) {
            Files.createFile(localConfigFile);
            Files.write(localConfigFile, "{\"disabled\":false}".getBytes());
        }
    }

    @Override
    public Path getFolder() {
        return tempDir;
    }

    @Override
    public Path getUUIDFile() {
        return uuidFile;
    }

    @Override
    public Path getRemoteConfigFile() {
        return remoteConfigFile;
    }

    @Override
    public Path getLastRemoteConfigTryFile() {
        return lastTryFile;
    }

    @Override
    public Path getLocalConfigFile() {
        return localConfigFile;
    }

    @Override
    public String lastTrackFileName() {
        return "lasttrack.json";
    }

    public void setUuidFile(Path uuidFile) {
        this.uuidFile = uuidFile;
    }

    public void deleteAll() throws IOException {
        Files.deleteIfExists(uuidFile);
        Files.deleteIfExists(remoteConfigFile);
        Files.deleteIfExists(lastTryFile);
        Files.deleteIfExists(localConfigFile);
        Files.deleteIfExists(Path.of(tempDir.toString() + "/" + lastTrackFileName()));
        Files.deleteIfExists(tempDir);
    }
}
