package io.quarkus.analytics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

import io.quarkus.analytics.config.FileLocations;
import io.quarkus.analytics.util.FileUtils;
import io.quarkus.devtools.messagewriter.MessageWriter;

/**
 * Anonymous user identity generated and stored locally.
 */
public class AnonymousUserId {
    private static AnonymousUserId INSTANCE = null;

    public static AnonymousUserId getInstance(final FileLocations fileLocations, final MessageWriter log) {
        if (INSTANCE == null) {
            INSTANCE = new AnonymousUserId(fileLocations, log);
        }
        return INSTANCE;
    }

    private final String uuid;
    private boolean isNew;
    private final MessageWriter log;

    // Singleton
    private AnonymousUserId(final FileLocations fileLocations, final MessageWriter log) {
        this.log = log;
        this.uuid = loadOrCreate(fileLocations.getUUIDFile());
    }

    public String getUuid() {
        return uuid;
    }

    public boolean isNew() {
        return isNew;
    }

    private String loadOrCreate(Path file) {
        try {
            if (Files.exists(file)) {
                return load(file);
            } else {
                String uuid = UUID.randomUUID().toString();
                write(uuid, file);
                isNew = true;
                return uuid;
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("[Quarkus build analytics] Could not create UUID file at " + file.toAbsolutePath(), e);
            }
            return "N/A";
        }
    }

    private String load(Path file) {
        String uuid = "N/A";
        try (Stream<String> lines = Files.lines(file)) {
            uuid = lines
                    .findAny()
                    .map(String::trim)
                    .orElse("empty");
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("[Quarkus build analytics] Could not read redhat anonymous UUID file at " + file.toAbsolutePath(), e);
            }
        }
        return uuid;
    }

    private void write(String uuid, Path uuidFile) {
        try {
            FileUtils.createFileAndParent(uuidFile);
            FileUtils.append(uuid, uuidFile);
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("[Quarkus build analytics] Could not write redhat anonymous UUID to file at "
                        + uuidFile.toAbsolutePath(), e);
            }
        }
    }
}
