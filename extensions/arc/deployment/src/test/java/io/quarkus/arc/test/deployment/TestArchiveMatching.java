package io.quarkus.arc.test.deployment;

import static io.quarkus.arc.deployment.BeanArchiveProcessor.archiveMatches;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.AppArtifactKey;

public class TestArchiveMatching {

    public static final String GROUP_ID = "io.quarkus";
    public static final String ARTIFACT_ID = "test";
    public static final String CLASSIFIER = "classifier";

    @Test
    public void testMatch() {

        AppArtifactKey key = AppArtifactKey.fromString(GROUP_ID + ":" + ARTIFACT_ID);
        AppArtifactKey keyWithClassifier = AppArtifactKey.fromString(GROUP_ID + ":" + ARTIFACT_ID + ":" + CLASSIFIER);

        assertTrue(archiveMatches(key, GROUP_ID, ARTIFACT_ID, Optional.empty()));
        assertFalse(archiveMatches(key, GROUP_ID, ARTIFACT_ID, Optional.of(CLASSIFIER)));
        assertFalse(archiveMatches(key, GROUP_ID, "test1", Optional.empty()));

        assertTrue(archiveMatches(keyWithClassifier, GROUP_ID, ARTIFACT_ID, Optional.of(CLASSIFIER)));
        assertFalse(archiveMatches(keyWithClassifier, GROUP_ID, "test1", Optional.of(CLASSIFIER)));
        assertFalse(archiveMatches(keyWithClassifier, GROUP_ID, ARTIFACT_ID, Optional.empty()));
    }

}
