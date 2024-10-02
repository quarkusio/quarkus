package io.quarkus.arc.test.deployment;

import static io.quarkus.arc.deployment.BeanArchiveProcessor.archiveMatches;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;

public class TestArchiveMatching {

    public static final String GROUP_ID = "io.quarkus";
    public static final String ARTIFACT_ID = "test";
    public static final String CLASSIFIER = "classifier";

    @Test
    public void testMatch() {

        ArtifactKey key = GACT.fromString(GROUP_ID + ":" + ARTIFACT_ID);
        ArtifactKey keyWithClassifier = GACT.fromString(GROUP_ID + ":" + ARTIFACT_ID + ":" + CLASSIFIER);

        assertFalse(archiveMatches(key, GROUP_ID + ".different", Optional.empty(), Optional.empty()));
        assertTrue(archiveMatches(key, GROUP_ID, Optional.empty(), Optional.empty()));
        assertTrue(archiveMatches(key, GROUP_ID, Optional.of(ARTIFACT_ID), Optional.empty()));
        assertFalse(archiveMatches(key, GROUP_ID, Optional.of(ARTIFACT_ID), Optional.of(CLASSIFIER)));
        assertFalse(archiveMatches(key, GROUP_ID, Optional.of("test1"), Optional.empty()));

        assertTrue(archiveMatches(keyWithClassifier, GROUP_ID, Optional.of(ARTIFACT_ID), Optional.of(CLASSIFIER)));
        assertFalse(archiveMatches(keyWithClassifier, GROUP_ID, Optional.of("test1"), Optional.of(CLASSIFIER)));
        assertFalse(archiveMatches(keyWithClassifier, GROUP_ID, Optional.of(ARTIFACT_ID), Optional.empty()));
    }

}
