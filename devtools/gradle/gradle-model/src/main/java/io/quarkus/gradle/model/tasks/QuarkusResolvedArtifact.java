package io.quarkus.gradle.model.tasks;

import java.io.File;
import java.io.Serializable;

import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;

/**
 * Serializable task-execution snapshot of the Gradle artifact fields required by application-model assembly.
 * <p>
 * Keeping this value free of a live variant object limits the Gradle model retained while artifacts are grouped and
 * translated.
 */
class QuarkusResolvedArtifact implements Serializable {

    private static final long serialVersionUID = 1L;

    final ComponentArtifactIdentifier id;
    final String type;
    final File file;

    QuarkusResolvedArtifact(ComponentArtifactIdentifier id, File file, String type) {
        this.id = id;
        this.type = type;
        this.file = file;
    }
}
